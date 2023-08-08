package cc.unitmesh.devti.settings

import com.intellij.json.JsonLanguage
import com.intellij.openapi.editor.colors.EditorColorsUtil
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.Dimension
import java.awt.FontMetrics
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Settings component
 *
 * only provide [AutoDevSettingsState] and with api set it up
 */
class AppSettingsComponent(settings: AutoDevSettingsState) {
    val panel: JPanel
    private val openAiKey = JBPasswordField()
    private val githubToken = JBPasswordField()
    private val customOpenAiHost = JBTextField()
    private val openAiModel = ComboBox(OPENAI_MODEL)

    private val aiEngine = ComboBox(AI_ENGINES)
    private val customEngineServer = JBTextField()
    private val customEngineToken = JBTextField()
    private val language = ComboBox(HUMAN_LANGUAGES)
    private val maxTokenLengthInput = JBTextField(MAX_TOKEN_LENGTH)

    private val customEnginePrompt by lazy {
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        object : LanguageTextField(JsonLanguage.INSTANCE, project, "") {

            override fun createEditor(): EditorEx {
                return super.createEditor().apply {
                    setShowPlaceholderWhenFocused(true)
                    setHorizontalScrollbarVisible(true)
                    setVerticalScrollbarVisible(true)
                    setPlaceholder("Enter custom prompt here")

                    val scheme = EditorColorsUtil.getColorSchemeForBackground(this.colorsScheme.defaultBackground)
                    this.colorsScheme = this.createBoundColorSchemeDelegate(scheme)
                }
            }
        }
    }

    init {
        val metrics: FontMetrics = customEnginePrompt.getFontMetrics(customEnginePrompt.font)
        val columnWidth = metrics.charWidth('m')
        customEnginePrompt.setOneLineMode(false)
        customEnginePrompt.preferredSize = Dimension(25 * columnWidth, 16 * metrics.height)

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Language: "), language, 1, false)
            .addLabeledComponent(JBLabel("AI Engine: "), aiEngine, 1, false)
            .addLabeledComponent(JBLabel("Max Token Length: "), maxTokenLengthInput, 1, false)
            .addSeparator()
            .addTooltip("GitHub Token is for AutoDev")
            .addLabeledComponent(JBLabel("GitHub Token: "), githubToken, 1, false)
            .addSeparator()
            .addLabeledComponent(JBLabel("OpenAI Model: "), openAiModel, 1, false)
            .addLabeledComponent(JBLabel("OpenAI Key: "), openAiKey, 1, false)
            .addLabeledComponent(JBLabel("Custom OpenAI Host: "), customOpenAiHost, 1, false)
            .addSeparator()
            .addLabeledComponent(JBLabel("Custom Engine Server: "), customEngineServer, 1, false)
            .addLabeledComponent(JBLabel("Custom Engine Token: "), customEngineToken, 1, false)
            .addVerticalGap(2)
            .addLabeledComponent(JBLabel("Custom Engine Prompt (Json): "), customEnginePrompt, 1, true)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        applySettings(settings)
    }

    val preferredFocusedComponent: JComponent
        get() = openAiKey

    private fun getOpenAiKey(): String {
        return openAiKey.password.joinToString("")
    }

    private fun setOpenAiKey(newText: String) {
        openAiKey.text = newText
    }

    private fun getGithubToken(): String {
        return githubToken.password.joinToString("")
    }

    private fun setGithubToken(newText: String) {
        githubToken.text = newText
    }

    private fun getOpenAiModel(): String {
        return openAiModel.selectedItem?.toString() ?: OPENAI_MODEL[0]
    }

    private fun setOpenAiModel(newText: String) {
        openAiModel.selectedItem = newText
    }

    private fun getOpenAiHost(): String {
        return customOpenAiHost.text
    }

    private fun setOpenAiHost(newText: String) {
        customOpenAiHost.text = newText
    }

    private fun getAiEngine(): String {
        return aiEngine.selectedItem?.toString() ?: "OpenAI"
    }

    private fun setAiEngine(newText: String) {
        aiEngine.selectedItem = newText
    }

    fun getCustomEngineServer(): String {
        return customEngineServer.text
    }

    private fun setCustomEngineServer(newText: String) {
        customEngineServer.text = newText
    }

    private fun getCustomEngineToken(): String {
        return customEngineToken.text
    }

    private fun setCustomEngineToken(newText: String) {
        customEngineToken.text = newText
    }

    private fun getCustomEnginePrompt(): String {
        return customEnginePrompt.text
    }

    private fun setCustomEnginePrompt(newText: String) {
        customEnginePrompt.text = newText
    }

    private fun getLanguage(): String {
        return language.selectedItem?.toString() ?: HUMAN_LANGUAGES[0]
    }

    private fun setLanguage(newText: String) {
        language.selectedItem = newText
    }

    private fun getMaxTokenLength(): String {
        return maxTokenLengthInput.text
    }

    private fun setMaxTokenLength(newText: String) {
        maxTokenLengthInput.text = newText
    }

    fun isModified(settings: AutoDevSettingsState): Boolean {
        return settings.openAiKey != getOpenAiKey() ||
                settings.githubToken != getGithubToken() ||
                settings.openAiModel != getOpenAiModel() ||
                settings.customOpenAiHost != getOpenAiHost() ||
                settings.aiEngine != getAiEngine() ||
                settings.customEngineServer != getCustomEngineServer() ||
                settings.customEngineToken != getCustomEngineToken() ||
                settings.customEnginePrompts != getCustomEnginePrompt() ||
                settings.language != getLanguage() ||
                settings.maxTokenLength != getMaxTokenLength()
    }

    /**
     * export settings to [target]
     */
    fun exportSettings(target: AutoDevSettingsState) {
        target.apply {
            openAiKey = getOpenAiKey()
            githubToken = getGithubToken()
            openAiModel = getOpenAiModel()
            customOpenAiHost = getOpenAiHost()
            aiEngine = getAiEngine()
            customEngineServer = getCustomEngineServer()
            customEngineToken = getCustomEngineToken()
            customEnginePrompts = getCustomEnginePrompt()
            language = getLanguage()
            maxTokenLength = getMaxTokenLength()
        }
    }

    /**
     * apply settings to setting UI
     */
    fun applySettings(settings: AutoDevSettingsState) {
        settings.also {
            setOpenAiKey(it.openAiKey)
            setGithubToken(it.githubToken)
            setOpenAiModel(it.openAiModel)
            setOpenAiHost(it.customOpenAiHost)
            setAiEngine(it.aiEngine)
            setCustomEngineServer(it.customEngineServer)
            setCustomEngineToken(it.customEngineToken)
            setCustomEnginePrompt(it.customEnginePrompts)
            setLanguage(it.language)
            setMaxTokenLength(it.maxTokenLength)
        }
    }
}
