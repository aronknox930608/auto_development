package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.flow.kanban.Kanban
import cc.unitmesh.devti.flow.kanban.impl.GitHubIssue
import cc.unitmesh.devti.flow.kanban.impl.GitLabIssue
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.provider.DevFlowProvider
import cc.unitmesh.devti.runconfig.config.AutoDevConfiguration
import cc.unitmesh.devti.runconfig.options.AutoDevConfigurationOptions
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

class AutoDevRunProfileState(
    val environment: ExecutionEnvironment,
    private val configuration: AutoDevConfiguration,
    val project: Project,
    val options: AutoDevConfigurationOptions,
) : RunProfileState {
    private val githubToken: String
    private val gitlabToken: String
    private val gitType: String
    private val gitlabUrl: String

    init {
        val instance = AutoDevSettingsState.getInstance()
        githubToken = instance.githubToken
        gitlabToken = instance.gitlabToken
        gitType = instance.gitType
        gitlabUrl = instance.gitlabUrl
    }

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        val gitHubIssue: Kanban
        when (gitType.lowercase()) {
            "gitlab" -> {
                gitHubIssue = GitLabIssue(options.githubRepo(), gitlabToken, gitlabUrl)
            }

            "github" -> {
                gitHubIssue = GitHubIssue(options.githubRepo(), githubToken)
            }

            else -> {
                gitHubIssue = GitHubIssue(options.githubRepo(), githubToken)
            }
        }


        // TODO: support other language
        val flowProvider = DevFlowProvider.flowProvider("java")
        if (flowProvider == null) {
            logger.error("current Language don't implementation DevFlow")
            return null
        }
        val openAIRunner = LlmFactory().create(project)

        sendToChatPanel(project) { contentPanel, _ ->
            flowProvider.initContext(gitHubIssue, openAIRunner, contentPanel, project)
            ProgressManager.getInstance().run(executeCrud(flowProvider))
        }

        return null
    }

    private fun executeCrud(flowProvider: DevFlowProvider) =
        object : Task.Backgroundable(project, "Loading retained test failure", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.fraction = 0.0

                indicator.text = AutoDevBundle.message("devti.progress.creatingStory")

                // todo: check create story
                val storyId = options.storyId()
                val storyDetail = flowProvider.getOrCreateStoryDetail(storyId)

                indicator.fraction = 0.2

                indicator.text = AutoDevBundle.message("devti.generatingDtoAndEntity")
                flowProvider.updateOrCreateDtoAndEntity(storyDetail)

                indicator.fraction = 0.4

                indicator.text = AutoDevBundle.message("devti.progress.fetchingSuggestEndpoint")
                val target = flowProvider.fetchSuggestEndpoint(storyDetail)

                indicator.fraction = 0.6

                indicator.text = AutoDevBundle.message("devti.progress.updatingEndpointMethod")
                flowProvider.updateOrCreateEndpointCode(target, storyDetail)

                indicator.fraction = 0.8

                indicator.text = AutoDevBundle.message("devti.progress.creatingServiceAndRepository")
                flowProvider.updateOrCreateServiceAndRepository()

                indicator.fraction = 1.0
            }
        }

    companion object {
        private val logger: Logger = logger<AutoDevRunProfileState>()
    }
}