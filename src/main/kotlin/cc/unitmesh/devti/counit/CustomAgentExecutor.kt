package cc.unitmesh.devti.counit

import cc.unitmesh.devti.counit.configurable.customAgentSetting
import cc.unitmesh.devti.counit.model.AuthType
import cc.unitmesh.devti.counit.model.CustomAgentConfig
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

@Service(Service.Level.PROJECT)
class CustomAgentExecutor(val project: Project) {
    private var client = OkHttpClient()

    fun execute(input: String, agent: CustomAgentConfig): String? {
        val serverAddress = project.customAgentSetting.serverAddress ?: return null
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), input)
        val builder = Request.Builder()

        val auth = agent.auth
        when (auth?.type) {
            AuthType.Bearer -> {
                builder.addHeader("Authorization", "Bearer ${auth.token}")
                builder.addHeader("Content-Type", "application/json")
            }

            null -> TODO()
        }

        client = client.newBuilder().build()
        val call = client.newCall(builder.url(serverAddress).post(body).build())

        call.execute().use { response ->
            if (!response.isSuccessful) {
                return null
            }

            return response.body?.string()
        }
    }
}