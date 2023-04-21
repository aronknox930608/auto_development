package cc.unitmesh.devti.actions

import cc.unitmesh.devti.DevtiIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiMethod

class CodeReviewAction(methodName: @NlsSafe String, method: PsiMethod) : AnAction({ "Code Review for $methodName" }, DevtiIcons.AI_COPILOT) {
    override fun actionPerformed(e: AnActionEvent) {

    }
}
