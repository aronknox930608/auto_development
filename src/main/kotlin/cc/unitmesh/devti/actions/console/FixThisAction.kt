package cc.unitmesh.devti.actions.console

import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.sendToChatWindow
import com.intellij.temporary.error.ErrorDescription
import com.intellij.temporary.error.ErrorMessageProcessor
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.actionSystem.AnActionEvent


class FixThisAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType = ChatActionType.FIX_ISSUE

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val description: ErrorDescription = ErrorMessageProcessor.getErrorDescription(event) ?: return

        val prompt = ErrorMessageProcessor.extracted(project, description)

        sendToChatWindow(project, getActionType()) { panel, service ->
            service.handlePromptAndResponse(panel, object : ContextPrompter() {
                override fun displayPrompt(): String = prompt?.displayText ?: ""
                override fun requestPrompt(): String = prompt?.requestText ?: ""
            }, null)
        }
    }
}
