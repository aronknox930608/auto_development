package cc.unitmesh.devti.provider

import cc.unitmesh.devti.custom.document.LivingDocumentationType
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

/**
 * The living documentation provider is responsible for providing the living documentation
 * 1. normal documentation
 * 2. annotated like Swagger
 * 3. living documentation
 */
interface LivingDocumentation {
    val forbiddenRules: List<String>

    val parameterTagInstruction: String get() = "use @param tag"
    val returnTagInstruction: String get() = "use @return tag"

    fun startEndString(type: LivingDocumentationType): Pair<String, String>

    fun updateDoc(target: PsiElement, newDoc: String, type: LivingDocumentationType, editor: Editor)

    fun findNearestDocumentationTarget(psiElement: PsiElement): PsiNameIdentifierOwner?

    fun findDocTargetsInSelection(root: PsiElement, selectionModel: SelectionModel): List<PsiNameIdentifierOwner>
    fun containsElement(selectionModel: SelectionModel, element: PsiElement): Boolean {
        return selectionModel.selectionStart <= element.textRange.startOffset && element.textRange.endOffset <= selectionModel.selectionEnd
    }

    fun buildDocFromSuggestion(suggestion: String, commentStart: String, commentEnd: String): String {
        val startIndex = suggestion.indexOf(commentStart)
        if (startIndex < 0) {
            return ""
        }

        val docComment = suggestion.substring(startIndex)
        val endIndex = docComment.indexOf(commentEnd, commentStart.length)
        if (endIndex < 0) {
            return docComment + commentEnd
        }

        val substring = docComment.substring(0, endIndex + commentEnd.length)
        return substring
    }

    companion object {
        private val languageExtension: LanguageExtension<LivingDocumentation> =
            LanguageExtension("cc.unitmesh.livingDocumentation")

        fun forLanguage(language: Language): LivingDocumentation? {
            return languageExtension.forLanguage(language)
        }
    }
}