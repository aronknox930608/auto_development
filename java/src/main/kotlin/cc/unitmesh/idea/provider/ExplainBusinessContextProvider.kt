package cc.unitmesh.idea.provider

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.idea.context.JavaMethodContextBuilder
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod

class ExplainBusinessContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return creationContext.sourceFile?.language is JavaLanguage && creationContext.action == ChatActionType.EXPLAIN_BUSINESS
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        when (creationContext.element) {
            is PsiMethod -> {
                val javaMethodContextBuilder = JavaMethodContextBuilder()
                javaMethodContextBuilder.getMethodContext(
                    creationContext.element as PsiMethod,
                    false,
                    gatherUsages = true
                )?.let {
                    val text = "```markdown\n${it.toQuery()}\n```"

                    return listOf(ChatContextItem(ExplainBusinessContextProvider::class, text))
                }
            }
        }

        return emptyList()
    }

}
