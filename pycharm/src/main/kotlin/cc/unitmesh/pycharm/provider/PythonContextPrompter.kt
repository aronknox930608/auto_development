package cc.unitmesh.pycharm.provider

import cc.unitmesh.devti.context.chunks.SimilarChunksWithPaths
import cc.unitmesh.devti.provider.ContextPrompter

class PythonContextPrompter : ContextPrompter() {
    override fun displayPrompt(): String {
        val chunkContext = SimilarChunksWithPaths().similarChunksWithPaths(file!!).toQuery()
        return "$action\n```${lang}\n$chunkContext\n$selectedText\n```"
    }

    override fun requestPrompt(): String {
        val chunkContext = SimilarChunksWithPaths().similarChunksWithPaths(file!!).toQuery()
        return "$action\n```${lang}\n$chunkContext\n$selectedText\n```"
    }
}
