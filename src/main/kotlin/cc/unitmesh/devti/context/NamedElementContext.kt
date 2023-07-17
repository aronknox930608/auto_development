package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.base.LLMQueryContext
import com.intellij.psi.PsiElement

abstract class NamedElementContext(open val root: PsiElement, open val text: String?, open val name: String?) :
    LLMQueryContext {
    override fun toQuery(): String = TODO()

    override fun toJson(): String = TODO()
}
