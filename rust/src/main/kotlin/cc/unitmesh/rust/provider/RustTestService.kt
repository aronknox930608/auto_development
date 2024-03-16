package cc.unitmesh.rust.provider

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.AutoTestService
import cc.unitmesh.devti.provider.context.TestFileContext
import cc.unitmesh.rust.context.RustClassContextBuilder
import cc.unitmesh.rust.context.RustMethodContextBuilder
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.rust.cargo.runconfig.command.CargoCommandConfiguration
import org.rust.lang.RsLanguage
import org.rust.lang.core.psi.*

class RustTestService : AutoTestService() {
    override fun runConfigurationClass(project: Project): Class<out RunProfile> = CargoCommandConfiguration::class.java

    override fun isApplicable(element: PsiElement): Boolean {
        return element.language is RsLanguage
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, psiElement: PsiElement): TestFileContext? {
        val testable = psiElement is RsImplItem || psiElement is RsFunction
        var elementName = sourceFile.name
        val element = ReadAction.compute<PsiElement, Throwable> {
            if (!testable) {
                val value = when (val parent = psiElement.parent) {
                    is RsFunction -> parent
                    is RsImplItem -> parent
                    is RsStructItem -> parent
                    is RsEnumItem -> parent
                    else -> {
                        PsiTreeUtil.getParentOfType(psiElement, RsFunction::class.java, RsImplItem::class.java)
                    }
                }

                elementName = when (value) {
                    is RsFunction -> value.name
                    is RsImplItem -> value.name
                    is RsStructItem -> value.name
                    is RsEnumItem -> value.name
                    else -> null
                } ?: ""

                return@compute value
            } else {
                return@compute psiElement
            }
        } ?: psiElement

        val currentObject = ReadAction.compute<String, Throwable> {
            return@compute when (element) {
                is RsFunction -> {
                    RustMethodContextBuilder().getMethodContext(element, true, false)?.format()
                }

                is RsImplItem -> {
                    RustClassContextBuilder().getClassContext(element, false)?.format() ?: ""
                }

                else -> null
            }
        }

        val imports = PsiTreeUtil.getChildrenOfTypeAsList(sourceFile, RsUseItem::class.java).map {
            it.text
        }

        val relevantClasses = lookupRelevantClass(project, element)

        return TestFileContext(
            false,
            sourceFile.virtualFile,
            relevantClasses,
            elementName,
            RsLanguage,
            currentObject,
            imports,
            element
        )
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        when (element) {
            is RsFunction -> {
                val returnType = element.retType?.typeReference
                val input = element.valueParameterList?.valueParameterList?.map {
                    it.typeReference
                } ?: emptyList()

                val refs = (listOf(returnType) + input).filterNotNull()
                val types = resolveReferenceTypes(refs)

                return types.mapNotNull {
                    RustClassContextBuilder().getClassContext(it, false)
                }
            }
        }

        return listOf()
    }

    private fun resolveReferenceTypes(rsTypeReferences: List<RsTypeReference>): List<PsiElement> {
        val mapNotNull = rsTypeReferences.mapNotNull {
            it.reference?.resolve()
        }

        return mapNotNull
    }
}
