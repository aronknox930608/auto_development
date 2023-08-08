package cc.unitmesh.devti.custom.variable

enum class CustomIntentionVariableType(@JvmField val description: String) {
    SELECTION("Currently selected code fragment with language name"),
    METHOD_INPUT_OUTPUT("Method input parameters's class as code snippets"),
    SPEC_VARIABLE("Load from spec config, and config to items"),
    ;
}