package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.base.NamedElementContext
import com.google.gson.Gson
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

class ClassContext(
    override val root: PsiElement,
    override val text: String?,
    override val name: String?,
    val methods: List<PsiElement> = emptyList(),
    val fields: List<PsiElement> = emptyList(),
    val superClasses: List<String>? = null,
    val usages: List<PsiReference> = emptyList()
) : NamedElementContext(root, text, name) {
    private fun getFieldNames(): List<String> = fields.mapNotNull {
        VariableContextProvider(false, false, false).from(it).name
    }

    private fun getMethodSignatures(): List<String> = methods.mapNotNull {
        MethodContextProvider(false, gatherUsages = false).from(it).signature
    }

    /**
     * Output:
     * ```
     * class BlogService {
     *   blogRepository: BlogRepository
     *   + createBlog(blogDto: CreateBlogDto): BlogPost
     *   + getAllBlogPosts(): List<BlogPost>
     * }
     * ```
     */
    override fun toQuery(): String {
        val className = name ?: "_"
        val classFields = getFieldNames().joinToString(separator = "\n  ")

        val methodSignatures = getMethodSignatures()
            .filter { it.isNotBlank() }
            .map { method ->
                "+ ${method}"
            }
            .joinToString(separator = "\n  ")

        return """
        |class $className {
        |  $classFields
        |  $methodSignatures
        |}
    """.trimMargin()
    }

    override fun toJson(): String = Gson().toJson({
        mapOf(
            "name" to name,
            "methods" to getMethodSignatures(),
            "fields" to getFieldNames()
        )
    }).toString()
}
