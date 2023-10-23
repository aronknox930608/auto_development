package cc.unitmesh.kotlin.provider

import cc.unitmesh.devti.context.builder.CodeModifier
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

class KotlinCodeModifier : CodeModifier {
    companion object {
        val log = logger<KotlinCodeModifier>()
    }

    override fun isApplicable(language: Language): Boolean {
        return language is KotlinLanguage
    }

    fun lookupFile(
        project: Project,
        sourceFile: VirtualFile
    ) = PsiManager.getInstance(project).findFile(sourceFile) as KtFile

    override fun insertTestCode(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        if (!code.contains("@Test")) {
            log.error("methodCode does not contain @Test annotation: $code")
            return false
        }

        if (code.startsWith("import") && code.contains("class ")) {
            return insertClass(sourceFile, project, code)
        }

        insertMethod(sourceFile, project, code)
        return true
    }

    override fun insertMethod(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        ApplicationManager.getApplication().invokeLater {
            val rootElement = runReadAction {
                val ktFile = lookupFile(project, sourceFile)
                val psiClass = ktFile.classes.firstOrNull()
                if (psiClass == null) {
                    log.error("Failed to find PsiClass in the source file: $ktFile, code: $code")
                    return@runReadAction null
                }

                return@runReadAction psiClass
            } ?: return@invokeLater


            val newTestMethod =  KtPsiFactory(project).createFunction(code)
            if (rootElement.findMethodsByName(newTestMethod.name, false).isNotEmpty()) {
                log.error("Method already exists in the class: ${newTestMethod.name}")
            }

            log.info("newTestMethod: ${newTestMethod.text}")

            WriteCommandAction.runWriteCommandAction(project) {
                val lastMethod = rootElement.methods.lastOrNull()
                val lastMethodEndOffset = lastMethod?.textRange?.endOffset ?: 0

                val document = PsiDocumentManager.getInstance(project).getDocument(rootElement.containingFile)
                document?.insertString(lastMethodEndOffset, "\n    ")
                document?.insertString(lastMethodEndOffset, newTestMethod.text)
            }

            project.guessProjectDir()?.refresh(true, true)
        }

        return true
    }

    override fun insertClass(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        log.info("start insertClassCode: $code")
        WriteCommandAction.runWriteCommandAction(project) {
            val psiFile = lookupFile(project, sourceFile)
            val document = psiFile.viewProvider.document!!
            document.insertString(document.textLength, code)
        }

        return true
    }
}
