package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.custom.compile.CustomVariable
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.MouseInputAdapter

class AutoDevVariableList(
    val list: List<AutoDevVariableListItemComponent>,
    val callback: ((AutoDevVariableListItemComponent) -> Unit),
) : JBList<AutoDevVariableListItemComponent>(list) {
    init {
        border = BorderFactory.createEmptyBorder(0, 4, 0, 4)
        setCellRenderer(VariableListCellRenderer())
        addMouseListener(object : MouseInputAdapter() {
            override fun mouseClicked(event: MouseEvent?) {
                val item = selectedValue ?: return
                callback.invoke(item)
            }
        })
    }

    companion object {
        fun from(all: List<CustomVariable>, function: (AutoDevVariableListItemComponent) -> Unit): AutoDevVariableList {
            val list = all.map {
                AutoDevVariableListItemComponent(it)
            }
            return AutoDevVariableList(list, function)
        }
    }
}

class VariableListCellRenderer : ListCellRenderer<AutoDevVariableListItemComponent> {
    private var emptyBorder: Border = BorderFactory.createEmptyBorder(1, 1, 1, 1)

    override fun getListCellRendererComponent(
        jList: JList<out AutoDevVariableListItemComponent>,
        value: AutoDevVariableListItemComponent?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        value!!.background = if (isSelected) jList.selectionBackground else jList.background
        value.foreground = if (isSelected) jList.selectionForeground else jList.foreground
        value.isEnabled = jList.isEnabled
        value.font = jList.font
        value.border = if (cellHasFocus) {
            if (isSelected) {
                UIManager.getBorder("List.focusSelectedCellHighlightBorder")
            } else {
                UIManager.getBorder("List.focusCellHighlightBorder")
            }
        } else {
            emptyBorder
        }

        return value
    }
}

class AutoDevVariableListItemComponent(val customVariable: CustomVariable) : JPanel(BorderLayout()) {
    init {
        add(JLabel("$${customVariable.variable}"), BorderLayout.WEST)
        val label = JLabel(customVariable.description)
        label.border = BorderFactory.createEmptyBorder(0, 8, 0, 0)
        label.foreground = JBColor.namedColor("Component.infoForeground", JBColor(Gray.x99, Gray.x78))
        add(label, BorderLayout.EAST)
    }
}
