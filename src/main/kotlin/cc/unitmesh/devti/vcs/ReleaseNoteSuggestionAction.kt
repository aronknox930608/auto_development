package cc.unitmesh.devti.vcs

import cc.unitmesh.devti.gui.DevtiFlowToolWindowFactory
import cc.unitmesh.devti.gui.chat.ChatBotActionType
import cc.unitmesh.devti.gui.chat.ChatCodingComponent
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.gui.chat.ChatContext
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.vcs.log.VcsLogDataKeys

// TODO: for 232 , we
class ReleaseNoteSuggestionAction : AnAction() {
    companion object {
        val logger = Logger.getInstance(ReleaseNoteSuggestionAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project

        val vcsLog = e.getData(VcsLogDataKeys.VCS_LOG)
        val stringList = vcsLog?.let { log ->
            log.selectedShortDetails.map { it.fullMessage }
        } ?: return

        val actionType = ChatBotActionType.CREATE_CHANGELOG

        val toolWindowManager = ToolWindowManager.getInstance(project!!).getToolWindow(DevtiFlowToolWindowFactory.id)
        val contentManager = toolWindowManager?.contentManager
        val chatCodingService = ChatCodingService(actionType, project)
        val contentPanel = ChatCodingComponent(chatCodingService)
        val content = contentManager?.factory?.createContent(contentPanel, chatCodingService.getLabel(), false)

        contentManager?.removeAllContents(true)
        contentManager?.addContent(content!!)

        val prompter = ContextPrompter.prompter("")
        prompter?.initContext(actionType, "", null, project, 0)

        toolWindowManager?.activate {
            val chatContext = ChatContext(null, stringList.joinToString(","), "")

            chatCodingService.handlePromptAndResponse(contentPanel, prompter!!, chatContext)
        }
    }
}
