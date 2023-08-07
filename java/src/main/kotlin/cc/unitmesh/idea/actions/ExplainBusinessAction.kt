package cc.unitmesh.idea.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.context.MethodContext
import cc.unitmesh.devti.getElementToAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.sendToChatWindow
import cc.unitmesh.devti.intentions.AbstractChatIntention
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import cc.unitmesh.idea.context.JavaMethodContextBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod

class ExplainBusinessAction : AbstractChatIntention() {
    override fun getText(): String = AutoDevBundle.message("intentions.explain.business.new.name")
    override fun getFamilyName(): String = AutoDevBundle.message("intentions.explain.business.family.name")
    override fun getActionType(): ChatActionType = ChatActionType.EXPLAIN_BUSINESS
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val actionType = getActionType()
        if (editor == null) return;
        val lang = file?.language?.displayName ?: ""
        var selectedText = editor.selectionModel.selectedText

        val element = getElementToAction(project, editor)

        if (selectedText == null) {
            if (element == null) return

            selectElement(element, editor)
            selectedText = editor.selectionModel.selectedText
        }

        if (selectedText == null) return

        val creationContext = ChatCreationContext(ChatOrigin.ChatAction, actionType, file, listOf(), element)


        val task: Task.Backgroundable = object : Task.Backgroundable(project, "Collect context") {
            override fun run(indicator: ProgressIndicator) {
                // prepare context
                val contextItems = when (element) {
                    is PsiMethod -> {
                        val methodContext = ReadAction.compute<MethodContext?, Throwable> {
                            return@compute JavaMethodContextBuilder().getMethodContext(
                                creationContext.element as PsiMethod,
                                true,
                                gatherUsages = true
                            )
                        }
                        // collect usages should run in [ProgressIndicator]
                        methodContext?.let {
                            val toQuery = ReadAction.compute<String?, Throwable> {
                                return@compute it.toQuery()
                            }
                            val contextItem = ChatContextItem(ExplainBusinessAction::class, toQuery)
                            listOf(contextItem)
                        } ?: emptyList()
                    }

                    else -> {
                        emptyList()
                    }
                }

                val instruction = actionType.instruction(lang)
                sendToChat(contextItems, selectedText, lang, project, instruction)
            }
        }

        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun sendToChat(
        contextItems: List<ChatContextItem>,
        selectedText: @NlsSafe String,
        lang: @NlsSafe String,
        project: Project,
        instruction: String,
    ) {
        var chatContext = "\n"
        contextItems.forEach {
            chatContext += it.text
        }

        ApplicationManager.getApplication().invokeLater {
            if (chatContext.isEmpty()) {
                chatContext = selectedText
            } else {
                chatContext = "// Compare this snippet $chatContext\norigin code:\n```$lang$selectedText```\n"
            }

            sendToChatWindow(project, getActionType()) { contentPanel, _ ->
                contentPanel.setInput("\n$instruction\n```markdown\n$chatContext")
            }
        }
    }
}

