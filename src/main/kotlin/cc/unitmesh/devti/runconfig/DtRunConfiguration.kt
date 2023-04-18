package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.prompt.openai.DtOpenAIVersion
import cc.unitmesh.devti.runconfig.config.DevtiStoryConfigure
import cc.unitmesh.devti.runconfig.options.AutoCRUDConfigurationOptions
import cc.unitmesh.devti.runconfig.ui.DtSettingsEditor
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationPerRunnerSettings
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jdom.Element

class DtRunConfiguration(project: Project, name: String, factory: ConfigurationFactory) :
    RunConfigurationBase<AutoCRUDConfigurationOptions>(project, factory, name) {

    public override fun getOptions(): AutoCRUDConfigurationOptions {
        return super.getOptions() as AutoCRUDConfigurationOptions
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return DtSettingsEditor(project)
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return DtRunState(environment, this, project, options)
    }

    override fun checkRunnerSettings(
        runner: ProgramRunner<*>,
        runnerSettings: RunnerSettings?,
        configurationPerRunnerSettings: ConfigurationPerRunnerSettings?
    ) {
        super.checkRunnerSettings(runner, runnerSettings, configurationPerRunnerSettings)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)

        element.writeString("githubToken", options.githubToken())
        element.writeString("githubRepo", options.githubRepo())
        element.writeString("openAiApiKey", options.openAiApiKey())
        element.writeString("aiEngineVersion", options.aiEngineVersion().toString())
        element.writeString("aiMaxTokens", options.aiMaxTokens().toString())
        element.writeString("storyId", options.storyId())
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)

        element.readString("githubToken")?.let { this.options.setGithubToken(it) }
        element.readString("openAiApiKey")?.let { this.options.setOpenAiApiKey(it) }
        element.readString("aiEngineVersion")?.let { this.options.setAiEngineVersion(it.toInt()) }
        element.readString("aiMaxTokens")?.let { this.options.setAiMaxTokens(it.toInt()) }
        element.readString("githubRepo")?.let { this.options.setGithubRepo(it) }
        element.readString("storyId")?.let { this.options.setStoryId(it) }
    }

    fun setGithubToken(text: String) {
        this.options.setGithubToken(text)
    }

    fun setOpenAiApiKey(text: String) {
        this.options.setOpenAiApiKey(text)
    }

    fun setAiVersion(fromIndex: DtOpenAIVersion) {
        this.options.setAiEngineVersion(fromIndex.ordinal)
    }

    fun setAiMaxTokens(openAiMaxTokens: Int) {
        this.options.setAiMaxTokens(openAiMaxTokens)
    }

    fun setGithubRepo(text: String) {
        this.options.setGithubRepo(text)
    }

    fun setStoryId(text: String) {
        this.options.setStoryId(text)
    }

    fun setStoryConfig(config: DevtiStoryConfigure) {
        this.options.setStoryId(config.storyId.toString())
    }
}

fun Element.writeString(name: String, value: String) {
    val opt = Element("option")
    opt.setAttribute("name", name)
    opt.setAttribute("value", value)
    addContent(opt)
}

fun Element.readString(name: String): String? =
    children
        .find { it.name == "option" && it.getAttributeValue("name") == name }
        ?.getAttributeValue("value")
