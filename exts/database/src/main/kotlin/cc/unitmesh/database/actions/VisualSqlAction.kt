package cc.unitmesh.database.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.intentions.action.base.AbstractChatIntention
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class VisualSqlAction : AbstractChatIntention() {
    override fun priority(): Int = 1001

    override fun startInWriteAction(): Boolean = false

    override fun getFamilyName(): String = AutoDevBundle.message("migration.database.plsql")

    override fun getText(): String = AutoDevBundle.message("migration.database.plsql.visual")

    override fun isAvailable(project: Project, editor: Editor?, psiFile: PsiFile?): Boolean {
        return false
    }

    override fun invoke(project: Project, editor: Editor?, psiFile: PsiFile?) {
        TODO("Not yet implemented")
    }
}
