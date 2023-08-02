package cc.unitmesh.idea.action

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingComponent
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.intentions.AbstractChatIntention
import cc.unitmesh.devti.llms.ConnectorFactory
import cc.unitmesh.devti.provider.DevFlowProvider
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiFile

class AutoCrudAction : AbstractChatIntention() {
    override fun getText(): String = AutoDevBundle.message("intentions.crud.new.name")
    override fun getFamilyName(): String = AutoDevBundle.message("intentions.crud.new.family.name")

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val openAIRunner = ConnectorFactory.getInstance().connector(project)

        val chatCodingService = ChatCodingService(ChatActionType.CHAT, project)
        val contentPanel = ChatCodingComponent(chatCodingService)

        val toolWindowManager = ToolWindowManager.getInstance(project).getToolWindow(AutoDevToolWindowFactory.Util.id)
        val contentManager = toolWindowManager?.contentManager

        val content = contentManager?.factory?.createContent(contentPanel, chatCodingService.getLabel(), false)

        contentManager?.removeAllContents(true)
        contentManager?.addContent(content!!)

        // TODO: support other language
        val flowProvider = DevFlowProvider.flowProvider("java")
        if (flowProvider == null) {
            logger.error("current Language don't implementation DevFlow")
            return
        }

        var selectedText = editor?.selectionModel?.selectedText ?: throw IllegalStateException("no select text")


        toolWindowManager?.activate {
            flowProvider.initContext(null, openAIRunner, contentPanel, project)
            ProgressManager.getInstance().run(executeCrud(flowProvider, project, selectedText))
        }
    }

    private fun executeCrud(flowProvider: DevFlowProvider, project: Project, selectedText: @NlsSafe String) =
        object : Task.Backgroundable(project, "Loading retained test failure", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.fraction = 0.2

                indicator.text = AutoDevBundle.message("devti.generatingDtoAndEntity")
                flowProvider.updateOrCreateDtoAndEntity(selectedText)

                indicator.fraction = 0.4

                indicator.text = AutoDevBundle.message("devti.progress.fetchingSuggestEndpoint")
                val target = flowProvider.fetchSuggestEndpoint(selectedText)

                indicator.fraction = 0.6

                indicator.text = AutoDevBundle.message("devti.progress.updatingEndpointMethod")
                flowProvider.updateOrCreateEndpointCode(target, selectedText)

                indicator.fraction = 0.8

                indicator.text = AutoDevBundle.message("devti.progress.creatingServiceAndRepository")
                flowProvider.updateOrCreateServiceAndRepository()

                indicator.fraction = 1.0
            }
        }


    companion object {
        val logger = logger<AutoCrudAction>()
    }
}
