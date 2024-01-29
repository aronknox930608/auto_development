package cc.unitmesh.devti.util

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    throwable.printStackTrace()
}

@Service(Service.Level.PROJECT)
class LLMCoroutineScope(val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + coroutineExceptionHandler)){
    companion object {
        fun scope(project: Project): CoroutineScope = project.service<LLMCoroutineScope>().coroutineScope
    }
}
