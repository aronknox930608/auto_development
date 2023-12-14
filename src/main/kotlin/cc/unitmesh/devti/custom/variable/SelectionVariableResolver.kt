package cc.unitmesh.devti.custom.variable

class SelectionVariableResolver(
    val languageName: String,
    val code: String,
) : VariableResolver {
    override val type: CustomVariableType = CustomVariableType.SELECTION

    override fun resolve(): String {
        return """
            |```$languageName
            |$code
            |```""".trimMargin()
    }
}