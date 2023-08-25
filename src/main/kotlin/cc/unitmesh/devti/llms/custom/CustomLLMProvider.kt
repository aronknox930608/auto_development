package cc.unitmesh.devti.llms.custom

import cc.unitmesh.devti.custom.CustomPromptConfig
import cc.unitmesh.devti.gui.chat.ChatRole
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jayway.jsonpath.JsonPath
import com.theokanning.openai.completion.chat.ChatCompletionResult
import com.theokanning.openai.service.SSE
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.kotlin.idea.gradleTooling.get
import java.time.Duration

@Serializable
data class Message(val role: String, val message: String)

@Serializable
data class CustomRequest(val messages: List<Message>)

@Service(Service.Level.PROJECT)
class CustomLLMProvider(val project: Project) : LLMProvider {
    private val autoDevSettingsState = AutoDevSettingsState.getInstance()
    private val url get() = autoDevSettingsState.customEngineServer
    private val key get() = autoDevSettingsState.customEngineToken

    private val requestHeaderFormat: String get() = autoDevSettingsState.customEngineRequestHeaderFormat
    private val requestBodyFormat: String get() = autoDevSettingsState.customEngineRequestBodyFormat
    private val responseFormat get() = autoDevSettingsState.customEngineResponseFormat
    private val customPromptConfig: CustomPromptConfig
        get() {
            val prompts = autoDevSettingsState.customPrompts
            return CustomPromptConfig.tryParse(prompts)
        }
    private var client = OkHttpClient()
    private val timeout = Duration.ofSeconds(600)
    private val messages: MutableList<Message> = ArrayList()

    private val logger = logger<CustomLLMProvider>()

    override fun clearMessage() {
        messages.clear()
    }

    override fun appendLocalMessage(msg: String, role: ChatRole) {
        messages += Message(role.roleName(), msg)
    }

    override fun prompt(promptText: String): String {
        return this.prompt(promptText, "")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun stream(promptText: String, systemPrompt: String): Flow<String> {
        messages += Message("user", promptText)

        val customRequest = CustomRequest(messages)
        val requestContent = Json.encodeToString<CustomRequest>(customRequest)

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), requestContent)
        logger.warn("Requesting from $body")

        val builder = Request.Builder()
        if (key.isNotEmpty()) {
            builder.addHeader("Authorization", "Bearer $key")
        }
        client = client.newBuilder()
                .readTimeout(timeout)
                .build()
        val request = builder
                .url(url)
                .post(body)
                .build()

        val call = client.newCall(request)
        val emitDone = false

        val sseFlowable = Flowable
                .create({ emitter: FlowableEmitter<SSE> ->
                    call.enqueue(cc.unitmesh.devti.llms.azure.ResponseBodyCallback(emitter, emitDone))
                }, BackpressureStrategy.BUFFER)

        try {
            logger.warn("Starting to stream:")
            return callbackFlow {
                withContext(Dispatchers.IO) {
                    sseFlowable
                            .doOnError(Throwable::printStackTrace)
                            .blockingForEach { sse ->
                                if (responseFormat.isNotEmpty()) {
                                    val chunk: String = JsonPath.parse(sse!!.data)?.read(responseFormat)
                                            ?: throw Exception("Failed to parse chunk")
                                    logger.warn(" $chunk")
                                    trySend(chunk)
                                } else {
                                    val result: ChatCompletionResult =
                                            ObjectMapper().readValue(sse!!.data, ChatCompletionResult::class.java)

                                    val completion = result.choices[0].message
                                    if (completion != null && completion.content != null) {
                                        trySend(completion.content)
                                    }
                                }
                            }

                    close()
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to stream", e)
            return callbackFlow {
                close()
            }
        }
    }

    fun prompt(instruction: String, input: String): String {
        messages += Message("user", instruction)
        val customRequest = CustomRequest(messages)
        val requestContent = Json.encodeToString<CustomRequest>(customRequest)

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), requestContent)

        logger.warn("Requesting from $body")
        val builder = Request.Builder()
        if (key.isNotEmpty()) {
            builder.addHeader("Authorization", "Bearer $key")
        }

        try {
            client = client.newBuilder()
                    .readTimeout(timeout)
                    .build()

            val request = builder
                    .url(url)
                    .post(body)
                    .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                logger.error("$response")
                return ""
            }

            return response.body?.string() ?: ""
        } catch (e: IllegalArgumentException) {
            logger.error("Failed to set timeout", e)
            return ""
        }
    }
}

@VisibleForTesting
fun Request.Builder.appendCustomHeaders(customRequestHeader: String): Request.Builder = apply {
    runCatching {
        Json.parseToJsonElement(customRequestHeader)
                .jsonObject["customHeaders"].let { customFields ->
            customFields?.jsonObject?.forEach { (key, value) ->
                header(key, value.jsonPrimitive.content)
            }
        }
    }.onFailure {
        logger<CustomLLMProvider>().error("Failed to parse custom request header", it)
    }
}

@VisibleForTesting
fun JsonObject.updateCustomBody(customRequest: String): JsonObject {
    return runCatching {
        buildJsonObject {
            // copy origin object
            this@updateCustomBody.forEach { u, v -> put(u, v) }

            val customRequestJson = Json.parseToJsonElement(customRequest).jsonObject

            customRequestJson["customFields"]?.let { customFields ->
                customFields.jsonObject.forEach { (key, value) ->
                    put(key, value.jsonPrimitive.content)
                }
            }



            // TODO clean code with magic literals
            var roleKey = "role"
            var contentKey  = "message"
            customRequestJson.jsonObject["messageKeys"]?.let {
                roleKey = it.jsonObject["role"]?.jsonPrimitive?.content ?: "role"
                contentKey = it.jsonObject["content"]?.jsonPrimitive?.content ?: "message"
            }

            val messages: JsonArray = this@updateCustomBody["messages"]?.jsonArray ?: buildJsonArray { }


            this.put("messages", buildJsonArray {
                messages.forEach { message ->
                    val role: String = message.jsonObject["role"]?.jsonPrimitive?.content ?: "user"
                    val content: String = message.jsonObject["message"]?.jsonPrimitive?.content ?: ""
                    add(buildJsonObject {
                        put(roleKey, role)
                        put(contentKey, content)
                    })
                }
            })
        }
    }.getOrElse {
        logger<CustomLLMProvider>().error("Failed to parse custom request body", it)
        this
    }
}