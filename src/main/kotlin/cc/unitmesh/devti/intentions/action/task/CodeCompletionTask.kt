package cc.unitmesh.devti.intentions.action.task

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.temporary.similar.chunks.SimilarChunksWithPaths
import cc.unitmesh.devti.llms.LLMProviderFactory
import cc.unitmesh.devti.LLMCoroutineScope
import cc.unitmesh.devti.intentions.action.CodeCompletionIntention
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import kotlin.jvm.internal.Ref

class CodeCompletionTask(private val request: CodeCompletionRequest) :
    Task.Backgroundable(request.project, AutoDevBundle.message("intentions.chat.code.complete.name")) {

    private val LLMProviderFactory = LLMProviderFactory()

    private val writeActionGroupId = "code.complete.intention.write.action"
    private val codeMessage = AutoDevBundle.message("intentions.chat.code.complete.name")

    private val chunksString = SimilarChunksWithPaths.createQuery(request.element, 60)
    private val commenter = LanguageCommenters.INSTANCE.forLanguage(request.element.language)
    private val commentPrefix = commenter?.lineCommentPrefix
    private var isCanceled: Boolean = false

    override fun run(indicator: ProgressIndicator) {
        val prompt = promptText()

        val flow: Flow<String> = LLMProviderFactory.connector(request.project).stream(prompt, "")
        logger.info("Prompt: $prompt")

        DumbAwareAction.create {
            isCanceled = true
        }.registerCustomShortcutSet(
            CustomShortcutSet(
                KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), null),
            ),
            request.editor.component
        )

        val editor = request.editor
        LLMCoroutineScope.scope(request.project).launch {
            val currentOffset = Ref.IntRef()
            currentOffset.element = request.offset

            val project = request.project
            val suggestion = StringBuilder()

            flow.cancellable().collect {
                if (isCanceled) {
                    cancel()
                    return@collect
                }

                suggestion.append(it)
                invokeLater {
                    if (!isCanceled) {
                        insertStreamingToDoc(project, it, editor, currentOffset.element)
                        currentOffset.element += it.length
                    }
                }
            }

            logger.info("Suggestion: $suggestion")
        }
    }

    fun insertStreamingToDoc(project: Project, char: String, editor: Editor, currentOffset: Int) {
        WriteCommandAction.runWriteCommandAction(project, codeMessage, writeActionGroupId, {
            insertStringAndSaveChange(project, char, editor.document, currentOffset, false)
        })


        editor.caretModel.moveToOffset(currentOffset + char.length)
        editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    }

    override fun onCancel() {
        this.isCanceled = true
        super.onCancel()
    }

    private fun promptText(): String {
        val documentLength = request.editor.document.textLength
        val prefix = if (request.offset > documentLength) {
            request.prefixText
        } else {
            val text = request.editor.document.text
            text.substring(0, request.offset)
        }

        val prompt = if (chunksString == null) {
            "complete code for given code: \n$prefix"
        } else {
            "complete code for given code: \n$commentPrefix\n$chunksString\n$prefix"
        }

        return prompt
    }

    companion object {
        val logger = logger<CodeCompletionIntention>()

        fun insertStringAndSaveChange(
            project: Project,
            suggestion: String,
            document: Document,
            startOffset: Int,
            withReformat: Boolean,
        ) {
            document.insertString(startOffset, suggestion)
            PsiDocumentManager.getInstance(project).commitDocument(document)

            if (!withReformat) return

            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
            psiFile?.let { file ->
                val reformatRange = TextRange(startOffset, startOffset + suggestion.length)
                CodeStyleManager.getInstance(project).reformatText(file, listOf(reformatRange))
            }
        }
    }
}