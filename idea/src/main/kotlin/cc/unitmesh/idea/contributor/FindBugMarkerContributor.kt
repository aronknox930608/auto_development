package cc.unitmesh.idea.contributor

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.runconfig.AutoDevRunProfileState
import cc.unitmesh.idea.action.FindBugAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod

class FindBugMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element !is PsiIdentifier) return null
        val method = element.parent
        if (method !is PsiMethod) return null

        val methodName = method.name
        val runAction = FindBugAction(methodName, method)

        return Info(
            AutoDevIcons.AI_COPILOT,
            { "Find Bug" },
            runAction
        )
    }

    companion object {
        private val log: Logger = logger<AutoDevRunProfileState>()
    }
}
