package cc.unitmesh.devti.runconfig

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.components.StoredProperty
import com.intellij.openapi.diagnostic.Logger

// ModuleBasedConfigurationOptions
class DtRunConfigurationOptions : RunConfigurationOptions() {
    private val githubToken: StoredProperty<String?> = string("").provideDelegate(this, "githubToken")
    private val openAiApiKey: StoredProperty<String?> = string("").provideDelegate(this, "openAiApiKey")
    private val openAiEngine: StoredProperty<String?> = string("").provideDelegate(this, "openAiEngine")
    private val openAiMaxTokens: StoredProperty<Int> = property(4096).provideDelegate(this, "openAiMaxTokens")

    fun setFrom(configure: DevtiConfigure) {
        this.githubToken.setValue(this, configure.githubToken)
        this.openAiApiKey.setValue(this, configure.openAiApiKey)
        this.openAiEngine.setValue(this, configure.aiVersion)
        this.openAiMaxTokens.setValue(this, configure.aiMaxTokens)
    }

    fun toConfigure(): DevtiConfigure {
        return DevtiConfigure(
            githubToken.getValue(this) ?: "",
            openAiApiKey.getValue(this) ?: "",
            openAiEngine.getValue(this) ?: "",
            openAiMaxTokens.getValue(this) ?: 4096
        )
    }

    companion object {
       val logger = Logger.getInstance(DtRunConfigurationOptions::class.java)
    }
}
