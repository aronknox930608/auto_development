package cc.unitmesh.devti.gui.chat.block

import cc.unitmesh.devti.gui.chat.ChatRole
import cc.unitmesh.devti.parser.Code
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorMarkupModel
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.editor.ex.MarkupModelEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import javax.swing.JComponent
import kotlin.jvm.internal.Intrinsics

class CodePartView(private val block: CodeBlock, private val project: Project, private val disposable: Disposable) :
    MessageBlockView {
    private var editorInfo: CodePartEditorInfo? = null

    init {
        getBlock().addTextListener {
            if (editorInfo == null) return@addTextListener
            updateOrCreateCodeView()
        }
    }

    override fun getBlock(): CodeBlock {
        return block
    }

    override fun getComponent(): JComponent {
        val codePartEditorInfo = editorInfo
        if (codePartEditorInfo != null) {
            return codePartEditorInfo.component
        }

        return updateOrCreateCodeView()!!.component
    }

    val codeContent: String
        get() {
            return editorInfo?.code?.get() ?: ""
        }

    override fun initialize() {
        if (editorInfo == null) {
            updateOrCreateCodeView()
        }
    }

    fun updateOrCreateCodeView(): CodePartEditorInfo? {
        val code: Code = getBlock().code
        if (editorInfo == null) {
            val editorInfo: CodePartEditorInfo = createCodeViewer(
                project,
                PropertyGraph(null as String?, false)
                    .property(code.text),
                disposable,
                code.language,
                getBlock().getMessage()
            )
            this.editorInfo = editorInfo
        } else {
            val codePartEditorInfo = editorInfo
            if (codePartEditorInfo!!.language == code.language) {
                editorInfo!!.language = code.language
            }

            editorInfo!!.code.set(code.text)
        }

        return editorInfo
    }

    companion object {
        private fun createCodeViewerFile(language: Language, content: String): LightVirtualFile {
            val file = LightVirtualFile("AutoDevSnippet", language, content)
            if (Intrinsics.areEqual(file.fileType, UnknownFileType.INSTANCE)) {
                file.setFileType(PlainTextFileType.INSTANCE)
            }
            return file
        }


        private fun createCodeViewerEditor(
            project: Project,
            file: LightVirtualFile,
            document: Document,
            disposable: Disposable
        ): EditorEx {
            val language = file.language
            val editor: Editor = EditorFactory.getInstance().createViewer(document, project);
            (editor as EditorEx).setFile(file)
            val highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, file)

            editor.highlighter = highlighter

            val markupModel: MarkupModelEx = editor.markupModel
            (markupModel as EditorMarkupModel).isErrorStripeVisible = false

            val editorSettings = editor.getSettings()
            editorSettings.isDndEnabled = false
            editorSettings.isLineNumbersShown = false
            editorSettings.additionalLinesCount = 0
            editorSettings.isLineMarkerAreaShown = false
            editorSettings.isFoldingOutlineShown = false
            editorSettings.isRightMarginShown = false
            editorSettings.isShowIntentionBulb = false
            editorSettings.isUseSoftWraps = true
            editorSettings.setPaintSoftWraps(false)
            editorSettings.isRefrainFromScrolling = true
            editorSettings.isAdditionalPageAtBottom = false
            editorSettings.isCaretRowShown = false

            editor.addFocusListener(object : FocusChangeListener {
                override fun focusGained(focusEditor: Editor) {
                    editor.getSettings().isCaretRowShown = true
                }

                override fun focusLost(focusEditor: Editor) {
                    editor.getSettings().isCaretRowShown = false
                    editor.markupModel.removeAllHighlighters()
                }
            })
            return editor
        }

        fun createCodeViewer(
            project: Project,
            graphProperty: GraphProperty<String>,
            disposable: Disposable,
            language: Language,
            message: CompletableMessage
        ): CodePartEditorInfo {
            val forceFoldEditorByDefault = message.getRole() === ChatRole.User
            val content = graphProperty.get() as String

            val createCodeViewerFile: VirtualFile = createCodeViewerFile(language, content)
            val document: Document =
                createCodeViewerFile.findDocument() ?: throw IllegalStateException("Document not found")

            val editor: EditorEx =
                createCodeViewerEditor(project, createCodeViewerFile as LightVirtualFile, document, disposable)
            editor.scrollPane.setBorder(JBUI.Borders.empty())
            editor.component.setBorder(JBUI.Borders.empty())

            return CodePartEditorInfo(graphProperty, BorderLayoutPanel(), editor, createCodeViewerFile)
        }
    }
}

@RequiresReadLock
fun VirtualFile.findDocument(): Document? {
    return FileDocumentManager.getInstance().getDocument(this)
}