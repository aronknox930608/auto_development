package cc.unitmesh.database.actions

import cc.unitmesh.database.actions.base.SqlMigrationContext
import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.sendToChatWindow
import cc.unitmesh.devti.intentions.action.base.ChatBaseIntention
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.template.TemplateRender
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.sql.dialects.oracle.OraDialect

class GenerateFunctionAction : ChatBaseIntention() {
    override fun priority() = 901

    override fun getFamilyName(): String = AutoDevBundle.message("migration.database.plsql")

    override fun getText(): String = AutoDevBundle.message("migration.database.plsql.generate.function")

    val logger = logger<GenerateFunctionAction>()

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (editor == null || file == null) return false
        val isOracle = file.language is OraDialect
        val selectedText = editor.selectionModel
        val element = file.findElementAt(selectedText.selectionStart)

        if (element != null) {
            logger.info("element: ${element.text}")
        }

        return isOracle
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val selectedText = editor.selectionModel.selectedText ?: return

        val templateRender = TemplateRender("genius/migration")
        val template = templateRender.getTemplate("gen-function.vm")
        templateRender.context = SqlMigrationContext(
            lang = file.language.displayName ?: "",
            sql = selectedText,
        )

        val prompter = templateRender.renderTemplate(template)

        logger.info("Prompt: $prompter")

        sendToChatWindow(project, getActionType()) { panel, service ->
            service.handlePromptAndResponse(panel, object : ContextPrompter() {
                override fun displayPrompt(): String = prompter
                override fun requestPrompt(): String = prompter
            }, null, false)
        }
    }

}


