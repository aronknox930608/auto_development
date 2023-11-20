package cc.unitmesh.kotlin.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import cc.unitmesh.idea.context.JavaContextCollection
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter

class KotlinClassContextBuilder : ClassContextBuilder {

    private fun getPrimaryConstructorFields(kotlinClass: KtClassOrObject): List<KtParameter> {
        return kotlinClass.getPrimaryConstructorParameters().filter { it.hasValOrVar() }
    }

    @Nullable
    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {
        if (psiElement !is KtClassOrObject) return null

        val text = psiElement.text
        val name = psiElement.name
        val ktNamedFunctions = Companion.getFunctions(psiElement)
        val primaryConstructorFields = getPrimaryConstructorFields(psiElement)
        val allFields = ktNamedFunctions + primaryConstructorFields
        val usages =
            if (gatherUsages) JavaContextCollection.findUsages(psiElement as PsiNameIdentifierOwner) else emptyList()

        return ClassContext(psiElement, text, name, ktNamedFunctions, allFields, null, usages)
    }

    companion object {
        fun getFunctions(kotlinClass: KtClassOrObject): List<KtFunction> {
            return kotlinClass.getDeclarations().filterIsInstance<KtFunction>()
        }
    }
}
