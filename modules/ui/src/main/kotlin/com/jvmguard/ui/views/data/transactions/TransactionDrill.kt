package com.jvmguard.ui.views.data.transactions

import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI

object TransactionDrill {

    private const val KEY = "transactionDrill"

    data class Target(val time: Long, val mode: TransactionMode)

    fun set(time: Long, mode: TransactionMode) {
        UI.getCurrent()?.let { ComponentUtil.setData(it, KEY, Target(time, mode)) }
    }

    fun take(): Target? {
        val ui = UI.getCurrent() ?: return null
        val target = ComponentUtil.getData(ui, KEY) as? Target
        ComponentUtil.setData(ui, KEY, null)
        return target
    }
}
