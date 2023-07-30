package cc.unitmesh.devti.gui.chat


import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*

enum class LLMChatRole {
    System,
    Assistant,
    User
}

class MessageComponent(private val message: String, role: LLMChatRole) : JBPanel<MessageComponent>() {
    private val component: DisplayComponent = DisplayComponent(message)
    private var answer: String? = null

    init {
        isDoubleBuffered = true
        isOpaque = true
        background = when (role) {
            LLMChatRole.System -> JBColor(0xEAEEF7, 0x45494A)
            LLMChatRole.Assistant -> JBColor(0xE0EEF7, 0x2d2f30)
            LLMChatRole.User -> JBColor(0xE0EEF7, 0x2d2f30)
        }

        this.border = JBEmptyBorder(8)
        layout = BorderLayout(JBUI.scale(8), 0)

        val centerPanel = JPanel(VerticalLayout(JBUI.scale(8)))
        centerPanel.isOpaque = false
        centerPanel.border = JBUI.Borders.emptyRight(8)

        component.updateMessage(message)
        component.revalidate()
        component.repaint()
        centerPanel.add(component)

        add(centerPanel, BorderLayout.CENTER)
    }

    fun updateContent(content: String) {
        MessageWorker(content).execute()
    }

    fun updateSourceContent(source: String?) {
        answer = source
    }

    fun scrollToBottom() {
        SwingUtilities.invokeLater {
            val bounds: Rectangle = bounds
            scrollRectToVisible(bounds)
        }
    }

    internal inner class MessageWorker(private val message: String) : SwingWorker<Void?, String?>() {
        @Throws(Exception::class)
        override fun doInBackground(): Void? {
            return null
        }

        override fun done() {
            try {
                get()
                component.updateMessage(message)
                component.updateUI()
            } catch (e: Exception) {
                logger.error(message, e.message)
            }
        }
    }

    companion object {
        private val logger = logger<MessageComponent>()

    }
}