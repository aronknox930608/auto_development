package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.llms.ConnectorFactory
import cc.unitmesh.devti.editor.LLMCoroutineScopeService
import cc.unitmesh.devti.parser.PostCodeProcessor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ChatCodingService(var actionType: ChatActionType, val project: Project) {
    private val connectorFactory = ConnectorFactory.getInstance()

    val action = actionType.instruction()

    fun getLabel(): String {
        val capitalizedAction = actionType
        return "$capitalizedAction Code"
    }

    fun handlePromptAndResponse(
        ui: ChatCodingComponent,
        prompt: ContextPrompter,
        context: ChatContext? = null
    ) {
        ui.add(prompt.displayPrompt(), true)
        ui.add(AutoDevBundle.message("devti.loading"))

        ApplicationManager.getApplication().executeOnPooledThread {
            val response = this.makeChatBotRequest(prompt.requestPrompt())
            LLMCoroutineScopeService.scope(project).launch {
                when {
                    actionType === ChatActionType.REFACTOR -> ui.updateReplaceableContent(response) {
                        context?.replaceSelectedText?.invoke(getCodeSection(it, context.prefixText, context.suffixText))
                    }

                    actionType === ChatActionType.CODE_COMPLETE -> ui.updateReplaceableContent(response) {
                        context?.replaceSelectedText?.invoke(getCodeSection(it, context.prefixText, context.suffixText))
                    }

                    else -> ui.updateMessage(response)
                }
            }
        }
    }

    private fun makeChatBotRequest(requestPrompt: String): Flow<String> {
        return connectorFactory.connector(project).stream(requestPrompt)
    }

    private fun getCodeSection(content: String, prefixText: String, suffixText: String): String {
        val pattern = "```(.+?)```".toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(content)

        if (match != null) return match.groupValues[1].trim()

        return PostCodeProcessor(prefixText, suffixText, content).execute()
    }
}
