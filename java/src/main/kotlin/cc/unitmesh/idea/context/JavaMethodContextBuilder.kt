package cc.unitmesh.idea.context

import cc.unitmesh.devti.context.MethodContext
import cc.unitmesh.devti.context.builder.MethodContextBuilder
import cc.unitmesh.idea.service.JavaTypeUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner

class JavaMethodContextBuilder : MethodContextBuilder {
    override fun getMethodContext(
        psiElement: PsiElement,
        includeClassContext: Boolean,
        gatherUsages: Boolean,
    ): MethodContext? {
        if (psiElement !is PsiMethod) {
            return null
        }

        val parameterList = psiElement.parameters.mapNotNull { it.name }
        val variableContextList = parameterList.map { it }

        val usagesList = if (gatherUsages) {
            JavaContextCollection.findUsages(psiElement as PsiNameIdentifierOwner)
        } else {
            emptyList()
        }

        val ios: List<PsiElement> = JavaTypeUtil.resolveByMethod(psiElement).values.mapNotNull { it }

        return MethodContext(
            psiElement,
            text = psiElement.text,
            name = psiElement.name,
            signature = getSignatureString(psiElement),
            enclosingClass = psiElement.containingClass,
            language = psiElement.language.displayName,
            returnType = processReturnTypeText(psiElement.returnType?.presentableText),
            variableContextList,
            includeClassContext,
            usagesList,
            ios
        )
    }

    private fun processReturnTypeText(returnType: String?): String? {
        return if (returnType == "void") null else returnType
    }
}

fun getSignatureString(method: PsiMethod): String {
    val bodyStart = method.body?.startOffsetInParent ?: method.textLength
    val text = method.text
    val substring = text.substring(0, bodyStart)
    val trimmed = substring.replace('\n', ' ').trim()
    return trimmed
}