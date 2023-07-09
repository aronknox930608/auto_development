package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.analysis.DtClass
import cc.unitmesh.devti.analysis.DtClass.Companion.fromPsiClass
import cc.unitmesh.devti.connector.custom.PromptConfig
import cc.unitmesh.devti.connector.custom.PromptItem
import cc.unitmesh.devti.settings.DevtiSettingsState
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.PsiJavaFileImpl
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


interface PromptFormatter {
    fun getUIPrompt(): String

    fun getRequestPrompt(): String
}

data class ControllerContext(
    val services: List<PsiClass>,
    val models: List<PsiClass>,
)

class BotActionPrompting(
    private val action: ChatBotActionType,
    private val lang: String,
    private val selectedText: String,
    private val file: PsiFile?,
    project: Project,
) : PromptFormatter {
    private val devtiSettingsState = DevtiSettingsState.getInstance()
    private var promptConfig: PromptConfig? = null
    val prompts = devtiSettingsState?.customEnginePrompts

    private val searchScope = GlobalSearchScope.allScope(project)
    private val javaPsiFacade = JavaPsiFacade.getInstance(project)

    private val fileName = file?.name ?: ""

    init {
        val prompts = devtiSettingsState?.customEnginePrompts
        try {
            if (prompts != null) {
                promptConfig = Json.decodeFromString(prompts)
            }
        } catch (e: Exception) {
            println("Error parsing prompts: $e")
        }

        if (promptConfig == null) {
            promptConfig = PromptConfig(
                PromptItem("Auto complete", "{code}"),
                PromptItem("Auto comment", "{code}"),
                PromptItem("Code review", "{code}"),
                PromptItem("Find bug", "{code}")
            )
        }
    }

    override fun getUIPrompt(): String {
        val prompt = createPrompt()

        return """$prompt:
         <pre><code>$selectedText</pre></code>
        """.trimMargin()
    }

    override fun getRequestPrompt(): String {
        val prompt = createPrompt()

        return """$prompt:
            $selectedText
        """.trimMargin()
    }


    private fun prepareControllerContext(controllerFile: PsiJavaFileImpl?): ControllerContext? {
        return runReadAction {
            if (controllerFile == null) return@runReadAction null

            val allImportStatements = controllerFile.importList?.allImportStatements

            val services = allImportStatements?.filter {
                it.importReference?.text?.endsWith("Service", true) ?: false
            }?.mapNotNull {
                val importText = it.importReference?.text ?: return@mapNotNull null
                javaPsiFacade.findClass(importText, searchScope)
            } ?: emptyList()

            // filter out model, entity, dto, from import statements
            val entities = allImportStatements?.filter {
                it.importReference?.text?.matches(Regex(".*\\.(model|entity|dto)\\..*")) ?: false
            }?.mapNotNull {
                val importText = it.importReference?.text ?: return@mapNotNull null
                javaPsiFacade.findClass(importText, searchScope)
            } ?: emptyList()

            return@runReadAction ControllerContext(
                services = services,
                models = entities
            )
        }
    }

    private fun createPrompt(): String {
        var prompt = """$action this $lang code"""

        when (action) {
            ChatBotActionType.REVIEW -> {
                val codeReview = promptConfig?.codeReview
                prompt = if (codeReview?.instruction?.isNotEmpty() == true) {
                    codeReview.instruction
                } else {
                    "请检查如下的 $lang 代码"
                }
            }

            ChatBotActionType.EXPLAIN -> {
                val autoComment = promptConfig?.autoComment
                prompt = if (autoComment?.instruction?.isNotEmpty() == true) {
                    autoComment.instruction
                } else {
                    "请解释如下的 $lang 代码"
                }
            }

            ChatBotActionType.REFACTOR -> {
                val refactor = promptConfig?.refactor
                prompt = if (refactor?.instruction?.isNotEmpty() == true) {
                    refactor.instruction
                } else {
                    "请重构如下的 $lang 代码"
                }
            }

            ChatBotActionType.CODE_COMPLETE -> {
                prompt = "补全如下的 $lang 代码"

                val isController = fileName.endsWith("Controller.java")
                val isService = fileName.endsWith("Service.java") || fileName.endsWith("ServiceImpl.java")

                when {
                    isController -> {
                        prompt = createControllerPrompt()
                    }

                    isService -> {
                        prompt = createServicePrompt()
                    }
                }

            }
        }

        return prompt
    }

    private fun createServicePrompt(): String {
        val file = file as? PsiJavaFileImpl
        val clazz = DtClass.fromJavaFile(file)

        return """代码补其 $lang 要求：
                |- 直接调用 repository 的方法时，使用 get, find, count, delete, save, update 这类方法
                |- Service 层应该捕获并处理可能出现的异常。通常情况下，应该将异常转换为应用程序自定义异常并抛出。
                |- // current package: ${clazz.packageName}
                |- // current class information: ${clazz.format()}
                |- // current class: ${clazz.name}
                """.trimMargin()
    }

    private fun createControllerPrompt(): String {
        val file = file as? PsiJavaFileImpl
        val services = prepareControllerContext(file)
        val servicesList = services?.services?.map {
            DtClass.fromPsiClass(it).format()
        }

        val servicePrompt = if (servicesList.isNullOrEmpty()) {
            ""
        } else {
            """|相关 Service 的信息如下： ```$servicesList```""".trimMargin()
        }
        val models = services?.models?.map {
            DtClass.fromPsiClass(it).format()
        }

        val modelsPrompt = if (models.isNullOrEmpty()) {
            ""
        } else {
            """|相关 Model 的信息如下： ```$models```""".trimMargin()
        }

        val clazz = DtClass.fromJavaFile(file)
        return """代码补全 $lang 要求：
                |- 在 Controller 中使用 BeanUtils 完成 DTO 的转换
                |- 不允许把 json，map 这类对象传到 service 中 
                |$servicePrompt
                |$modelsPrompt
                |- // current package: ${clazz.packageName}
                |- // current class: ${clazz.name}
                |- 需要补全的代码如下：
            """.trimMargin()
    }
}