package io.github.spair.strongdmm.gui.instancelist

import io.github.spair.strongdmm.common.DEFAULT_DIR
import io.github.spair.strongdmm.common.VAR_ICON
import io.github.spair.strongdmm.common.VAR_ICON_STATE
import io.github.spair.strongdmm.common.VAR_NAME
import io.github.spair.strongdmm.gui.TabbedObjectPanelView
import io.github.spair.strongdmm.gui.common.BorderUtil
import io.github.spair.strongdmm.gui.common.View
import io.github.spair.strongdmm.gui.map.MapView
import io.github.spair.strongdmm.logic.EnvCleanable
import io.github.spair.strongdmm.logic.Environment
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

object InstanceListView : View, EnvCleanable {

    var selectedInstance: ItemInstance? = null

    private val customVariablesLabel: JLabel = JLabel()
    private val instanceList: JList<ItemInstance> = JList<ItemInstance>(DefaultListModel<ItemInstance>())

    init {
        with(instanceList) {
            cellRenderer = InstanceListRenderer()

            addListSelectionListener {
                selectedValue?.let {
                    showInstanceVars(it.customVars)
                    selectedInstance = it
                }
            }
        }
    }

    override fun clean() {
        selectedInstance = null
        (instanceList.model as DefaultListModel).clear()
        setEmptyInstanceVars()
    }

    override fun initComponent(): JComponent {
        return JPanel(BorderLayout()).apply {
            add(JScrollPane(instanceList), BorderLayout.CENTER)
            add(JPanel(BorderLayout()).apply {
                preferredSize = Dimension(Int.MAX_VALUE, 200)

                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY),
                    BorderUtil.createEmptyBorder(5)
                )

                add(JLabel("<html><h4>Instance variables:</h4></html>"), BorderLayout.NORTH)
                add(JScrollPane(
                    JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        add(customVariablesLabel)
                    }
                ).apply {
                    border = BorderFactory.createMatteBorder(1, 0, 1, 0, Color.GRAY)
                })

                setEmptyInstanceVars()
            }, BorderLayout.SOUTH)
        }
    }

    fun selectInstanceByCustomVars(customVars: Map<String, String>?) {
        val model = instanceList.model as DefaultListModel<ItemInstance>
        for (i in 0 until model.size()) {
            if (model[i].customVars == customVars) {
                instanceList.selectedIndex = i
                break
            }
        }
    }

    fun findAndSelectInstancesByType(type: String) {
        val items = LinkedHashSet<ItemInstance>()
        val dmeItem = Environment.dme.getItem(type)!!

        val initialInstance = ItemInstance(
            dmeItem.getVarText(VAR_NAME) ?: "",
            dmeItem.getVarText(VAR_ICON) ?: "",
            dmeItem.getVarText(VAR_ICON_STATE) ?: "",
            dmeItem.type,
            DEFAULT_DIR,
            null
        )

        selectedInstance = initialInstance
        items.add(initialInstance)

        val instances = mutableSetOf<ItemInstance>()

        MapView.getOpenedMaps().forEach { dmm ->
            dmm.getAllTileItemsByType(type).forEach {
                instances.add(ItemInstance(
                    it.getVarText(VAR_NAME) ?: "", it.icon, it.iconState, it.type, it.dir, it.customVars)
                )
            }
        }

        items.addAll(instances.sortedBy { it.name }.sortedBy { it.customVars?.size }.sortedBy { it.iconState })

        val model = instanceList.model as DefaultListModel
        model.clear()
        items.forEach(model::addElement)

        instanceList.selectedIndex = 0
        TabbedObjectPanelView.setInstanceCount(items.size)
    }

    fun updateSelectedInstanceInfo() {
        selectedInstance?.let {
            findAndSelectInstancesByType(it.type)
        }
    }

    private fun showInstanceVars(variables: Map<String, String>?) {
        if (variables == null || variables.isEmpty()) {
            setEmptyInstanceVars()
            return
        }

        customVariablesLabel.text = buildString {
            append("<html>")
            variables.forEach { (k, v) -> append("- <b>$k</b>: $v<br>") }
            append("</html>")
        }
    }

    private fun setEmptyInstanceVars() {
        customVariablesLabel.text = "empty (instance with initial vars)"
    }
}
