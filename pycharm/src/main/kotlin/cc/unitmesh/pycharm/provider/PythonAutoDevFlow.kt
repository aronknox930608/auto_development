package cc.unitmesh.pycharm.provider

import cc.unitmesh.devti.context.model.DtClass
import cc.unitmesh.devti.flow.kanban.Kanban
import cc.unitmesh.devti.prompting.code.TargetEndpoint
import cc.unitmesh.devti.gui.chat.ChatCodingComponent
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.provider.DevFlowProvider
import com.intellij.openapi.project.Project

class PythonAutoDevFlow : DevFlowProvider() {
    override fun initContext(
        kanban: Kanban,
        aiRunner: LLMProvider,
        component: ChatCodingComponent,
        project: Project
    ) {
    }

    override fun getOrCreateStoryDetail(id: String): String {
        return ""
    }

    override fun updateOrCreateDtoAndEntity(storyDetail: String) {}
    override fun fetchSuggestEndpoint(storyDetail: String): TargetEndpoint {
        return TargetEndpoint("", DtClass("", listOf()), false)
    }

    override fun updateOrCreateEndpointCode(target: TargetEndpoint, storyDetail: String) {}
    override fun updateOrCreateServiceAndRepository() {}
}
