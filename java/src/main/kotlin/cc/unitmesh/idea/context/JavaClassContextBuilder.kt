package cc.unitmesh.idea.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

class JavaClassContextBuilder : ClassContextBuilder {
    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {
        if (psiElement !is PsiClass) return null

        val supers = psiElement.extendsList?.referenceElements?.mapNotNull {
            it.text
        }

        val fields = psiElement.fields.toList()
        val methods = psiElement.methods.toList()

        val usages =
            if (gatherUsages) JavaContextCollection.findUsages(psiElement as PsiNameIdentifierOwner) else emptyList()

        return ClassContext(
            psiElement, psiElement.text, psiElement.name, methods, fields, supers, usages,
            displayName = psiElement.qualifiedName
        )
    }
}
