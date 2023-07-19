package cc.unitmesh.devti.intentions.editor

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.editor.presentation.LLMInlayRenderer
import cc.unitmesh.devti.models.ConnectorFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.actions.EditorActionUtil
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlin.math.min


class CodeCompletionIntention : AbstractChatIntention() {
    companion object {
        val logger = Logger.getInstance(CodeCompletionIntention::class.java)
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return (editor != null) && (file != null)
    }

    override fun getText(): String = AutoDevBundle.message("intentions.chat.code.complete.name")

    override fun getFamilyName(): String = AutoDevBundle.message("intentions.chat.code.complete.family.name")

    override fun getPrompt(project: Project, elementToExplain: PsiElement?): String {
        return "Complete code for "
    }

    /**
     * Invokes the method and performs auto-completion based on the current caret position in the editor.
     *
     * @param project the current project
     * @param editor the editor in which the completion is performed
     * @param file the PSI file in the editor
     */
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return

        val document = editor.document
        val offset = editor.caretModel.offset
        val promptLength = 512
        var promptStart = (offset - promptLength).coerceAtLeast(0)
        val isOutBoundary = !EditorActionUtil.isWordBoundary(editor.document.text, promptStart, false, false)
        while (promptStart < offset && isOutBoundary) {
            promptStart++
        }
        if (promptStart == offset) {
            promptStart = (offset - promptLength).coerceAtLeast(0)
        }

        var prompt = document.getText(TextRange.create(promptStart, offset))

        val suffixLength = 256
        var suffixEnd = min((offset + suffixLength).toDouble(), document.textLength.toDouble()).toInt()
        while (suffixEnd > offset && !EditorActionUtil.isWordBoundary(editor.document.text, suffixEnd, false, false)) {
            suffixEnd--
        }
//
//        val element = PsiUtilBase.getElementAtCaret(editor) ?: file
//        val chunksWithPaths = SimilarChunksWithPaths().similarChunksWithPaths(element)
//        val size = chunksWithPaths.chunks?.size ?: 0
//        val similarCode = if (size > 0) {
//            chunksWithPaths.toQuery()
//        } else {
//            ""
//        }
//        prompt = "Code complete for follow code \n$similarCode\n$prompt"
        // TODO: use suffix to improve the completion
//        val suffix = document.getText(TextRange(offset, suffixEnd))

        logger.warn("Prompt: $prompt")
        runBlocking {
            renderInlay(prompt, editor, offset)
        }
    }

    /**
     * Renders an inlay with the given prompt in the specified editor at the specified offset.
     *
     * @param prompt The prompt to display in the inlay.
     * @param editor The editor in which to render the inlay.
     * @param offset The offset at which to render the inlay.
     */
    private suspend fun renderInlay(
        prompt: @NlsSafe String,
        editor: Editor,
        offset: Int
    ) {
        val flow: Flow<String> = connectorFactory.connector().stream(prompt)
        var text = ""

//        val renderer: EditorCustomElementRenderer = LLMInlayRenderer(editor, text.lines())
//        val addAfterLineEndElement = editor.inlayModel.addAfterLineEndElement(
//            offset,
//            true,
//            renderer
//        )!!

        val project = editor.project!!
        var currentOffset = offset
        flow.collect {
            text += it

            ApplicationManager.getApplication().invokeAndWait {
                WriteCommandAction.runWriteCommandAction(project) {
                    insertStringAndSaveChange(project, it, editor.document, currentOffset, false)
                }
                currentOffset += it.length
                editor.caretModel.moveToOffset(currentOffset)
            }
        }

//        EditorFactory.getInstance().addEditorFactoryListener(object : EditorFactoryListener {
//            override fun editorReleased(event: EditorFactoryEvent) {
//                Disposer.dispose(addAfterLineEndElement as Disposable)
//            }
//        }, addAfterLineEndElement as Disposable)
//
//        WriteCommandAction.runWriteCommandAction(editor.project!!) {
//            insertStringAndSaveChange(editor.project!!, text, editor.document, offset, false)
//        }
//        val instance = LLMInlayManager.getInstance()
//        instance.applyCompletion(editor.project!!, editor)
//
//        ApplicationManager.getApplication().messageBus.syncPublisher(LLMInlayListener.TOPIC)
//            .inlaysUpdated(editor, listOf(inlay))
    }

    private val connectorFactory = ConnectorFactory.getInstance()

    fun insertStringAndSaveChange(
        project: Project,
        suggestion: String,
        document: Document,
        startOffset: Int,
        withReformat: Boolean
    ) {
        document.insertString(startOffset, suggestion)
        PsiDocumentManager.getInstance(project).commitDocument(document)

        if (withReformat) {
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
            psiFile?.let { file ->
                val reformatRange = TextRange(startOffset, startOffset + suggestion.length)
                CodeStyleManager.getInstance(project).reformatText(file, listOf(reformatRange))
            }
        }
    }
}
