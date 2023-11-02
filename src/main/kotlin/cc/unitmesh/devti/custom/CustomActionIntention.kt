package cc.unitmesh.devti.custom

import cc.unitmesh.devti.custom.action.CustomIntentionConfig
import cc.unitmesh.devti.custom.action.CustomIntentionPrompt
import cc.unitmesh.devti.custom.variable.*
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.intentions.action.base.AbstractChatIntention
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import java.io.StringWriter

class CustomActionIntention(private val intentionConfig: CustomIntentionConfig) : AbstractChatIntention() {
    override fun getText(): String = intentionConfig.title

    override fun getFamilyName(): String = "Custom Intention"
    override fun priority(): Int {
        return intentionConfig.priority
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (editor == null || file == null) return false

        val regexString = intentionConfig.matchRegex
        return try {
            val regex = Regex(regexString)
            regex.matches(file.name)
        } catch (e: Exception) {
            false
        }
    }

    override fun getActionType(): ChatActionType = ChatActionType.CUSTOM_ACTION

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val withRange = elementWithRange(editor, file, project) ?: return
        val selectedText = withRange.first
        val psiElement = withRange.second
        val prompt: CustomIntentionPrompt = buildCustomPrompt(psiElement!!, selectedText)

        if (intentionConfig.autoInvoke) {
            sendToChatPanel(project, getActionType(), object : ContextPrompter() {
                override fun displayPrompt(): String {
                    return prompt.displayPrompt
                }

                override fun requestPrompt(): String {
                    return prompt.requestPrompt
                }
            })
        } else {
            sendToChatPanel(project) { panel, _ ->
                panel.setInput(prompt.displayPrompt)
            }
        }
    }

    private fun buildCustomPrompt(psiElement: PsiElement, selectedText: @NlsSafe String): CustomIntentionPrompt {
        val stringBuilderWriter = StringWriter()
        val velocityContext = VelocityContext()

        val variableResolvers = arrayOf(
            MethodInputOutputVariableResolver(psiElement),
            SimilarChunkVariableResolver(psiElement),
            SelectionVariableResolver(psiElement.language.displayName ?: "", selectedText),
        ) + SpecResolverService.getInstance().resolvers()

        val resolverMap = LinkedHashMap<String, VariableResolver>(20)
        variableResolvers.forEach { resolver ->
            resolverMap[resolver.variableName()] = resolver
        }

        resolverMap.forEach { (variableType, resolver) ->
            val value = resolver.resolve()
            velocityContext.put(variableType, value)
        }

        Velocity.evaluate(velocityContext, stringBuilderWriter, "", intentionConfig.template)
        val output = stringBuilderWriter.toString()

        return CustomIntentionPrompt(output, output, listOf())
    }

    companion object {
        fun create(intentionConfig: CustomIntentionConfig): CustomActionIntention {
            return CustomActionIntention(intentionConfig)
        }
    }
}