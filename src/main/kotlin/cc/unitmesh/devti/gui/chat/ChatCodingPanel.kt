package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.alignRight
import cc.unitmesh.devti.counit.model.CustomAgentConfig
import cc.unitmesh.devti.fullHeight
import cc.unitmesh.devti.fullWidth
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.NullableComponent
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.temporary.gui.block.whenDisposed
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class ChatCodingPanel(private val chatCodingService: ChatCodingService, val disposable: Disposable?) :
    SimpleToolWindowPanel(true, true),
    NullableComponent {
    private var progressBar: JProgressBar
    private val myTitle = JBLabel("Conversation")
    private val myList = JPanel(VerticalLayout(JBUI.scale(10)))
    private var inputSection: AutoDevInputSection
    private val focusMouseListener: MouseAdapter
    private var panelContent: DialogPanel
    private val myScrollPane: JBScrollPane
    private val delaySeconds: String
        get() = AutoDevSettingsState.getInstance().delaySeconds

    init {
        focusMouseListener = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                focusInput()
            }
        }

        myTitle.foreground = JBColor.namedColor("Label.infoForeground", JBColor(Gray.x80, Gray.x8C))
        myTitle.font = JBFont.label()

        myList.isOpaque = true
        myList.background = UIUtil.getListBackground()

        myScrollPane = JBScrollPane(
            myList,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        )
        myScrollPane.verticalScrollBar.autoscrolls = true
        myScrollPane.background = UIUtil.getListBackground()

        progressBar = JProgressBar()

        val actionLink = ActionLink(AutoDevBundle.message("label.submit.issue")) {
            BrowserUtil.browse(AutoDevBundle.message("chat.panel.submit.issue.url"))
        }
        actionLink.setExternalLinkIcon()

        inputSection = AutoDevInputSection(chatCodingService.project, disposable)
        inputSection.addListener(object : AutoDevInputListener {
            override fun onSubmit(component: AutoDevInputSection, trigger: AutoDevInputTrigger) {
                val prompt = component.text
                component.text = ""
                if (prompt.isEmpty()) {
                    return
                }

                val context = ChatContext(null, "", "")

                chatCodingService.actionType = ChatActionType.CHAT
                chatCodingService.handlePromptAndResponse(this@ChatCodingPanel, object : ContextPrompter() {
                    override fun displayPrompt() = prompt
                    override fun requestPrompt() = prompt
                }, context, false)
            }
        })

        panelContent = panel {
            row { cell(myScrollPane).fullWidth().fullHeight() }.resizableRow()
            row { cell(progressBar).fullWidth() }
            row { cell(actionLink).alignRight() }
            row {
                border = JBUI.Borders.empty(8)
                cell(inputSection).fullWidth()
            }
        }

        setContent(panelContent)

        disposable?.whenDisposed(disposable) {
            myList.removeAll()
        }
    }

    fun focusInput() {
        val focusManager = IdeFocusManager.getInstance(chatCodingService.project)
        focusManager.doWhenFocusSettlesDown {
            focusManager.requestFocus(this.inputSection.focusableComponent, true)
        }
    }

    /**
     * Add a message to the chat panel and update ui
     */
    fun addMessage(message: String, isMe: Boolean = false, displayPrompt: String = ""): MessageView {
        val role = if (isMe) ChatRole.User else ChatRole.Assistant
        val displayText = displayPrompt.ifEmpty { message }

        val messageView = MessageView(message, role, displayText)

        myList.add(messageView)
        updateLayout()
        scrollToBottom()
        progressBar.isIndeterminate = true
        updateUI()
        return messageView
    }

    private fun updateLayout() {
        val layout = myList.layout
        for (i in 0 until myList.componentCount) {
            layout.removeLayoutComponent(myList.getComponent(i))
            layout.addLayoutComponent(null, myList.getComponent(i))
        }
    }

    suspend fun updateMessage(content: Flow<String>): String {
        if (myList.componentCount > 0) {
            myList.remove(myList.componentCount - 1)
        }

        progressBar.isVisible = true

        val result = updateMessageInUi(content)

        progressBar.isIndeterminate = false
        progressBar.isVisible = false
        updateUI()

        return result
    }

    private fun scrollToBottom() {
        SwingUtilities.invokeLater {
            val verticalScrollBar = myScrollPane.verticalScrollBar
            verticalScrollBar.value = verticalScrollBar.maximum
        }
    }

    override fun isNull(): Boolean {
        return !isVisible
    }

    suspend fun updateReplaceableContent(content: Flow<String>, replaceSelectedText: (text: String) -> Unit) {
        myList.remove(myList.componentCount - 1)
        val text = updateMessageInUi(content)

        val jButton = JButton(AutoDevBundle.message("chat.panel.replaceSelection"))
        val listener = ActionListener {
            replaceSelectedText(text)
            myList.remove(myList.componentCount - 1)
        }
        jButton.addActionListener(listener)
        myList.add(jButton)

        progressBar.isIndeterminate = false
        progressBar.isVisible = false
        updateUI()
    }

    private val logger = logger<ChatCodingPanel>()

    private suspend fun updateMessageInUi(content: Flow<String>): String {
        val messageView = MessageView("", ChatRole.Assistant, "")
        myList.add(messageView)
        val startTime = System.currentTimeMillis() // 记录代码开始执行的时间

        var text = ""
        content.onCompletion {
            logger.info("onCompletion ${it?.message}")
        }.catch {
            it.printStackTrace()
        }.collect {
            text += it

            messageView.updateContent(text)
            messageView.scrollToBottom()
        }

        if (delaySeconds.isNotEmpty()) {
            val elapsedTime = System.currentTimeMillis() - startTime
            // waiting for the last message to be rendered, like sleep 5 ms?
            // 此处的 20s 出自 openAI 免费账户访问 3/min
            withContext(Dispatchers.IO) {
                val delaySec = delaySeconds.toLong() ?: 20L
                val remainingTime = maxOf(delaySec * 1000 - elapsedTime, 0)
                delay(remainingTime)
            }
        }

        messageView.reRenderAssistantOutput()

        return text
    }

    fun setInput(trimMargin: String) {
        inputSection.text = trimMargin
        this.focusInput()
    }

    // TODO: add session and stop manage
    fun clearChat() {
        chatCodingService.clearSession()
        progressBar.isVisible = false
        myList.removeAll()
        this.hiddenProgressBar()
        this.resetAgent()
        updateUI()
    }

    fun resetAgent() {
        inputSection.resetAgent()
    }

    fun hasSelectedCustomAgent(): Boolean {
        return inputSection.hasSelectedAgent()
    }

    fun getSelectedCustomAgent(): CustomAgentConfig {
        return inputSection.getSelectedAgent()
    }

    fun hiddenProgressBar() {
        progressBar.isVisible = false
    }
}
