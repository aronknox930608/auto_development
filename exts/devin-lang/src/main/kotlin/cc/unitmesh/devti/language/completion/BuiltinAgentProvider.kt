package cc.unitmesh.devti.language.completion

import cc.unitmesh.devti.language.DevInIcons
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

enum class BuiltinAgent(val agentName: String, val description: String) {
    FILE("file", "Read the content of a file"),
    REV("rev", "Read git change by file"),
    SYMBOL("symbol", "Read content by Java/Kotlin canonicalName"),
    WRITE("write", "Write content to a file, format: /write:/path/to/file:L1-C2"),
    ;

    companion object {
        fun all(): List<BuiltinAgent> {
            return values().toList()
        }
    }
}

class BuiltinAgentProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        BuiltinAgent.all().forEach {
            result.addElement(
                LookupElementBuilder.create(it.agentName)
                    .withIcon(DevInIcons.DEFAULT)
                    .withTypeText(it.description, true)
            )
        }
    }
}

