package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.language.dataprovider.BuiltinCommand
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.completion.weighers.VariableOrParameterNameWithTypeWeigher.nameWithTypePriority

class BuiltinCommandCompletion : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        BuiltinCommand.all().forEach {
            result.addElement(
                PrioritizedLookupElement.withPriority(
                    LookupElementBuilder.create(it.commandName)
                        .withIcon(it.icon)
                        .withTypeText(it.description, true)
                        .withInsertHandler { context, _ ->
                            if (!it.hasCompletion) return@withInsertHandler

                            context.document.insertString(context.tailOffset, ":")
                            context.editor.caretModel.moveCaretRelatively(1, 0, false, false, false)

                            val editor = context.editor
                            AutoPopupController.getInstance(editor.project!!).scheduleAutoPopup(editor)
                        },
                    // before custom
                    99.0
                )
            )
        }
    }
}

