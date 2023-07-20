package cc.unitmesh.devti.context.model

@Deprecated("Use [MethodContextBuilder] for multiple language support")
data class DtMethod(val name: String, val returnType: String, val parameters: List<DtParameter>)

@Deprecated("Use [VariableContextBuilder] for multiple language support")
data class DtField(val name: String, val type: String)

@Deprecated("Use [VariableContextBuilder] for multiple language support")
data class DtParameter(val name: String, val type: String)

@Deprecated("Use [ClassContextBuilder] for multiple language support")
class DtClass(
    val name: String,
    val methods: List<DtMethod>,
    val packageName: String = "",
    val fields: List<DtField> = listOf(),
    val path: String = ""
) {
    /**
     * Output:
     * ```
     * // package: cc.unitmesh.untitled.demo.service
     * // class BlogService {
     * // blogRepository: BlogRepository
     * //  + createBlog(blogDto: CreateBlogDto): BlogPost
     * //  + getAllBlogPosts(): List<BlogPost>
     * //}
     * ```
     */
    // todo: support by comment
    // val commenter = LanguageCommenters.INSTANCE.forLanguage(language)
    // val commentPrefix = commenter?.lineCommentPrefix
    fun commentFormat(): String {
        val output = StringBuilder()
        output.append("package: $packageName\n")
        output.append("class $name {\n")
        output.append(fields.joinToString("\n") { field ->
            "   ${field.name}: ${field.type}"
        })

        // remove getter and setter, and add them to getterSetter
        var getterSetter: List<String> = listOf()
        val methodsWithoutGetterSetter = methods
            .filter { method ->
                val isGetter = method.name.startsWith("get") && method.parameters.isEmpty()
                val isSetter = method.name.startsWith("set") && method.parameters.size == 1
                if (isGetter || isSetter) {
                    getterSetter = listOf(method.name)
                    return@filter false
                }

                return@filter true
            }

        if (getterSetter.isNotEmpty()) {
            output.append("\n   'getter/setter: ${getterSetter.joinToString(", ")}\n")
        }

        val methodCodes = methodsWithoutGetterSetter
            .filter { it.name != this.name }
            .joinToString("\n") { method ->
                val params = method.parameters.joinToString("") { parameter -> "${parameter.name}: ${parameter.type}" }
                "   + ${method.name}($params)" + if (method.returnType.isNotBlank()) ": ${method.returnType}" else ""
            }

        if (methodCodes.isNotBlank()) {
            output.append("\n")
            output.append(methodCodes)
        }

        output.append("\n ' some getters and setters\n")
        output.append(" }\n")

        // TODO: split output and add comments line
        return output.split("\n").joinToString("\n") {
            "// $it"
        }
    }

    fun formatDto(): String {
        if (fields.isNotEmpty()) {
            val output = StringBuilder()
            output.append("$packageName.$name(")
            output.append(fields.joinToString(", ") { "${it.name}: ${it.type}" })
            output.append(")")
            return output.toString()
        }

        return "$packageName.$name\n"
    }

    fun format(): String {
        val output = StringBuilder()
        output.append("class $name ")

        val constructor = methods.find { it.name == this.name }
        if (constructor != null) {
            output.append("constructor(")
            output.append(constructor.parameters.joinToString(", ") { "${it.name}: ${it.type}" })
            output.append(")\n")
        }

        if (methods.isNotEmpty()) {
            output.append("- methods: ")
            // filter out constructor
            output.append(methods.filter { it.name != this.name }.joinToString(", ") { method ->
                "${method.name}(${method.parameters.joinToString(", ") { parameter -> "${parameter.name}: ${parameter.type}" }}): ${method.returnType}"
            })
        }

        return output.toString()
    }

    companion object {
        fun from() {}
    }
}

