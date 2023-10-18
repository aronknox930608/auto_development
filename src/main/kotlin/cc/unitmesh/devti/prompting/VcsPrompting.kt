// MIT License
//
//Copyright (c) Jakob Maležič
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.

package cc.unitmesh.devti.prompting

import com.intellij.openapi.components.Service
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.*
import com.intellij.project.stateStore
import com.intellij.vcs.log.VcsFullCommitDetails
import git4idea.repo.GitRepositoryManager
import org.jetbrains.annotations.NotNull
import java.io.IOException
import java.io.StringWriter
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.util.stream.Collectors
import kotlin.math.min

@Service(Service.Level.PROJECT)
class VcsPrompting(private val project: Project) {
    private val gitRepositoryManager = GitRepositoryManager.getInstance(project)

    fun calculateDiff(collection: List<Change>, project: Project): String {
        try {
            val writer = StringWriter()
            val destination = collection.filterNot { isBinaryOrTooLarge(it) }
            val basePath = project.basePath ?: throw RuntimeException("Project base path is null.")
            val patches = IdeaTextPatchBuilder.buildPatch(
                project,
                destination,
                Path.of(basePath),
                false,
                true
            )

            UnifiedDiffWriter.write(
                project,
                project.stateStore.projectBasePath,
                patches,
                writer,
                "\n",
                null as CommitContext?,
                emptyList()
            )
            val diffString = writer.toString()
            return trimDiff(diffString)
        } catch (e: VcsException) {
            throw RuntimeException("Error calculating diff: ${e.message}", e)
        }
    }

    fun prepareContext(): String {
        val changeListManager = ChangeListManagerImpl.getInstance(project)
        val changes = changeListManager.changeLists.flatMap {
            it.changes
        }

        return this.calculateDiff(changes, project)
    }

    @Throws(VcsException::class, IOException::class)
    fun buildDiffPrompt(
        list: List<VcsFullCommitDetails>,
        project: Project,
        ignoreFilePatterns: List<PathMatcher> = listOf(),
    ): Pair<List<String>, String>? {
        val writer = StringWriter()
        var isEmpty = true

        val summary: MutableList<String> = ArrayList()
        for (detail in list) {
            writer.write("""Commit Message: ${detail.fullMessage}\n\nCode Changes:\n\n""")
            val subject = detail.subject

            summary.add('"'.toString() + subject + "\"")
            val filteredChanges = detail.changes.stream()
                .filter { change -> !isBinaryOrTooLarge(change!!) }
                .filter {
                    val filePath = it.afterRevision?.file
                    if (filePath != null) {
                        ignoreFilePatterns.none { pattern ->
                            pattern.matches(Path.of(it.afterRevision!!.file.path))
                        }
                    } else {
                        true
                    }
                }
                .toList()

            if (filteredChanges.isEmpty()) {
                continue
            }

            val patches = IdeaTextPatchBuilder.buildPatch(
                project,
                filteredChanges.subList(0, min(filteredChanges.size, 500)),
                Path.of(project.basePath!!),
                false,
                true
            )

            isEmpty = false

            UnifiedDiffWriter.write(
                project,
                project.stateStore.projectBasePath,
                patches,
                writer,
                "\n",
                null, emptyList()
            )
        }

        if (isEmpty) {
            return null
        }

        val stringWriter = writer.toString()
        val diff = trimDiff(stringWriter)
        return Pair<List<String>, String>(summary, diff)
    }

    fun computeDiff(includedChanges: List<Change>): String {
        val changesByRepository = includedChanges
            .mapNotNull { change ->
                change.virtualFile?.let { file ->
                    gitRepositoryManager.getRepositoryForFileQuick(
                        file
                    ) to change
                }
            }
            .groupBy({ it.first }, { it.second })

        return changesByRepository
            .map { (repository, changes) ->
                repository?.let {
                    val filePatches = IdeaTextPatchBuilder.buildPatch(
                        project,
                        changes,
                        repository.root.toNioPath(), false, true
                    )

                    val stringWriter = StringWriter()
                    stringWriter.write("Repository: ${repository.root.path}\n")
                    UnifiedDiffWriter.write(project, filePatches, stringWriter, "\n", null)
                    stringWriter.toString()
                }
            }
            .joinToString("\n")
    }

    @Throws(VcsException::class, IOException::class)
    fun singleCommitCalculateDiff(list: List<VcsFullCommitDetails>, project: Project): Pair<List<String>, String> {
        val writer = StringWriter()
        val summary: MutableList<String> = ArrayList()
        for (detail in list) {
            writer.write(
                """
    Message: ${detail.fullMessage}
    
    Changes:
    
    """.trimIndent()
            )
            val subject = detail.subject
            summary.add('"'.toString() + subject + "\"")
            val filteredChanges = detail.changes.stream()
                .filter { change: Change? ->
                    !isBinaryOrTooLarge(change!!)
                }
                .collect(Collectors.toList())

            val shortChange = filteredChanges.subList(0, min(filteredChanges.size, 500))
            val patches = IdeaTextPatchBuilder.buildPatch(
                project,
                shortChange,
                Path.of(project.basePath!!),
                false,
                true
            )
            UnifiedDiffWriter.write(
                project, project.stateStore.projectBasePath, patches, writer, "\n", null, emptyList()
            )
        }
        val stringWriter = writer.toString()
        val diff = trimDiff(stringWriter)
        return Pair<List<String>, String>(summary, diff)
    }

    private val revisionRegex = Regex("\\(revision [^)]+\\)")

    @NotNull
    fun trimDiff(@NotNull diffString: String): String {
        val lines = diffString.lines()
        val destination = ArrayList<String>()
        for (line in lines) {
            if (line.startsWith("diff --git ") || line.startsWith("index ") || line.startsWith("Index ")) continue

            if (line == "===================================================================") continue

            if (line.startsWith("---") || line.startsWith("+++")) {
                // remove revision number with regex
                val result = revisionRegex.replace(line, "")
                destination.add(result)
            } else {
                destination.add(line)
            }
        }
        return destination.joinToString("\n")
    }

    private fun isBinaryOrTooLarge(@NotNull change: Change): Boolean {
        return isBinaryOrTooLarge(change.beforeRevision) || isBinaryOrTooLarge(change.afterRevision)
    }

    private fun isBinaryOrTooLarge(revision: ContentRevision?): Boolean {
        val virtualFile = (revision as? CurrentContentRevision)?.virtualFile
        return revision != null && (IdeaTextPatchBuilder.isBinaryRevision(revision) || (virtualFile != null && FileUtilRt.isTooLarge(
            virtualFile.length
        )))
    }
}