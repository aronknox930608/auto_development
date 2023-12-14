package cc.unitmesh.devti.gui.snippet

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent

class AutoDevLanguageLabelAction : DumbAwareAction(), CustomComponentAction {
    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val languageId = snippetLanguageName(presentation)
        val jBLabel: JComponent = JBLabel(languageId)
        jBLabel.setOpaque(false)
        jBLabel.setForeground(UIUtil.getLabelInfoForeground())
        return jBLabel
    }

    override fun updateCustomComponent(component: JComponent, presentation: Presentation) {
        if (component !is JBLabel) return

        val languageId = snippetLanguageName(presentation)
        if (languageId.isNotBlank()) {
            component.text = languageId
        }
    }

    override fun actionPerformed(e: AnActionEvent) {

    }

    override fun update(e: AnActionEvent) {
        val data = e.dataContext.getData(CommonDataKeys.EDITOR) ?: return
        val file: VirtualFile = FileDocumentManager.getInstance().getFile(data.document) ?: return
        val lightVirtualFile = file as LightVirtualFile
        e.presentation.putClientProperty(LANGUAGE_PRESENTATION_KEY, lightVirtualFile.language.displayName)
    }

    private fun snippetLanguageName(presentation: Presentation): String {
        return presentation.getClientProperty(LANGUAGE_PRESENTATION_KEY) ?: ""
    }

    companion object {
        val LANGUAGE_PRESENTATION_KEY: Key<String> = Key.create("LanguagePresentationKey")
    }
}
