package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.language.compiler.model.FileFunc
import cc.unitmesh.devti.language.utils.canBeAdded
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile

class FileFuncInsCommand(val myProject: Project, val prop: String) : InsCommand {
    override fun execute(): String? {
        val (functionName, args) = parseRegex(prop)
            ?: return """<DevInsError>: file-func is not in the format @file-func:<functionName>(<arg1>, <arg2>, ...)
            |Example: @file-func:regex(".*\.kt")
        """.trimMargin()

        val fileFunction = FileFunc.fromString(functionName) ?: return "<DevInsError>: Unknown function: $functionName"
        when (fileFunction) {
            FileFunc.Regex -> {
                try {
                    val regex = Regex(args[0])
                    return regexFunction(regex, myProject).joinToString(", ")
                } catch (e: Exception) {
                    return "<DevInsError>: ${e.message}"
                }
            }
        }
    }

    private fun regexFunction(regex: Regex, project: Project): MutableList<VirtualFile> {
        val files: MutableList<VirtualFile> = mutableListOf()
        ProjectFileIndex.getInstance(project).iterateContent {
            if (canBeAdded(it)) {
                if (regex.matches(it.path)) {
                    files.add(it)
                }
            }

            true
        }
        return files
    }
}

/**
 * Parses a given property string to extract the function name and its arguments.
 *
 * The property string is in the format <functionName>(<arg1>, <arg2>, ...).
 *
 * @param prop The property string to parse.
 * @return The function name and the list of arguments as a Pair object.
 * @throws IllegalArgumentException if the property string has invalid regex pattern.
 */
fun parseRegex(prop: String): Pair<String, List<String>>? {
    val regexPattern = Regex("""(\w+)\(([^)]+)\)""")
    val matchResult = regexPattern.find(prop)

    if (matchResult != null && matchResult.groupValues.size == 3) {
        val functionName = matchResult.groupValues[1]
        val args = matchResult.groupValues[2].split(',').map { it.trim() }
        return Pair(functionName, args)
    } else {
        return null
    }
}
