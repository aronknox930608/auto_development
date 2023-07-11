package cc.unitmesh.devti.connector

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface CodeCopilot {
    fun codeCompleteFor(text: String, className: String): String
    fun autoComment(text: String): String
    fun codeReviewFor(text: String): String
    fun findBug(text: String): String

    fun prompt(promptText: String): String
    fun stream(promptText: String): Flow<String> {
        return callbackFlow {
            val prompt = prompt(promptText)
            offer(prompt)
        }
    }
}