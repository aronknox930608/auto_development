// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.language.parser

import cc.unitmesh.language.psi.DevInTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.tree.injected.InjectionBackgroundSuppressor
import com.intellij.psi.templateLanguages.OuterLanguageElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.*

class CodeBlockElement(node: ASTNode) : ASTWrapperPsiElement(node), PsiLanguageInjectionHost,
    InjectionBackgroundSuppressor {
    override fun isValidHost(): Boolean {
        // use MarkdownCodeFenceUtils.isAbleToAcceptInjections
        return true
    }

    override fun updateText(text: String): PsiLanguageInjectionHost {
        return ElementManipulators.handleContentChange(this, text)
    }

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> {
        return CodeBlockLiteralTextEscaper(this)
    }

    fun getLanguageId(): PsiElement? {
        return findChildByType(DevInTypes.LANGUAGE_ID)
    }

    fun getContents(): PsiElement? {
        return findChildByType(DevInTypes.CODE_CONTENTS)
    }

    companion object {
        fun obtainFenceContent(element: CodeBlockElement): List<PsiElement>? {
            return CachedValuesManager.getCachedValue(element) {
                CachedValueProvider.Result.create(getContent(element), element)
            }
        }

        private fun getContent(host: CodeBlockElement): List<PsiElement>? {
            val children = host.firstChild
                ?.siblings(forward = true, withSelf = true) ?: return null

            val elements =
                children.filter {
                    it !is OuterLanguageElement
                            && (it.node.elementType == DevInTypes.CODE_CONTENTS || it == DevInTypes.NEWLINE)
                }
                    .toList()

            if (elements.isNotEmpty() && elements.first() == DevInTypes.NEWLINE) {
                elements.drop(1)
            }
            if (elements.isNotEmpty() && elements.last() == DevInTypes.NEWLINE) {
                elements.dropLast(1)
            }

            return elements.takeIf { it.isNotEmpty() }
        }

        fun obtainRelevantTextRange(element: CodeBlockElement): TextRange {
            val elements = obtainFenceContent(element) ?: return getEmptyRange(element)
            val first = elements.first()
            val last = elements.last()

            return TextRange.create(first.startOffsetInParent, last.startOffsetInParent + last.textLength)
        }

        private fun getEmptyRange(host: CodeBlockElement): TextRange {
            val start = host.children.find { it.hasType(DevInTypes.LANGUAGE_ID) }
                ?: host.children.find { it.hasType(DevInTypes.CODE_BLOCK_START) }

            return TextRange.from(start!!.startOffsetInParent + start.textLength + 1, 0)
        }
    }
}

fun PsiElement.hasType(type: IElementType): Boolean {
    return PsiUtilCore.getElementType(this) == type
}

class CodeBlockLiteralTextEscaper(host: CodeBlockElement) : LiteralTextEscaper<CodeBlockElement>(host) {
    override fun isOneLine(): Boolean = false;

    override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
        val elements = CodeBlockElement.obtainFenceContent(myHost) ?: return true
        for (element in elements) {
            val intersected = rangeInsideHost.intersection(element.textRangeInParent) ?: continue
            outChars.append(intersected.substring(myHost.text))
        }

        return true
    }

    override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
        val elements = CodeBlockElement.obtainFenceContent(myHost) ?: return -1
        var cur = 0
        for (element in elements) {
            val intersected = rangeInsideHost.intersection(element.textRangeInParent)
            if (intersected == null || intersected.isEmpty) continue
            if (cur + intersected.length == offsetInDecoded) {
                return intersected.startOffset + intersected.length
            } else if (cur == offsetInDecoded) {
                return intersected.startOffset
            } else if (cur < offsetInDecoded && cur + intersected.length > offsetInDecoded) {
                return intersected.startOffset + (offsetInDecoded - cur)
            }
            cur += intersected.length
        }

        val last = elements[elements.size - 1]
        val intersected = rangeInsideHost.intersection(last.textRangeInParent)
        if (intersected == null || intersected.isEmpty) return -1
        val result = intersected.startOffset + (offsetInDecoded - (cur - intersected.length))
        return if (rangeInsideHost.startOffset <= result && result <= rangeInsideHost.endOffset) {
            result
        } else -1
    }

    override fun getRelevantTextRange(): TextRange {
        return CodeBlockElement.obtainRelevantTextRange(myHost)
    }
}
