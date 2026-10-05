package dev.jvmguard.ui.components.recording

import dev.jvmguard.agent.config.transactions.AnnotationValueFilter
import dev.jvmguard.agent.config.transactions.OtelTransactionDef
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.TextField

class OtelTransactionDefDialog(
    def: OtelTransactionDef,
    isNew: Boolean,
    onSave: (OtelTransactionDef) -> Unit,
) : AbstractTransactionDefDialog<OtelTransactionDef>(def, isNew, onSave) {

    override val typeKey: String get() = "otel"

    private val spanNameFilter = TextField(t("recording.transaction.otel.spanNameFilter")).apply {
        setWidthFull()
        helperText = t("recording.transaction.classFilter.helper")
    }

    init {
        build()
    }

    override fun definitionTab(): Component? = null

    override fun infoLine(): Component = Span(t("recording.transaction.otel.info"))

    override fun filterTabExtras(): List<Component> = listOf(spanNameFilter)

    override fun readDefinition(def: OtelTransactionDef) {
        spanNameFilter.value = def.annotationValueFilter?.value ?: "*"
    }

    override fun writeDefinition(def: OtelTransactionDef): Boolean {
        val value = spanNameFilter.value.trim()
        if (value.isEmpty() || value == "*") {
            def.annotationValueFilter = null
        } else {
            val filter = def.annotationValueFilter ?: AnnotationValueFilter()
            filter.value = value
            def.annotationValueFilter = filter
        }
        return true
    }

    override fun namingForm(): NamingForm = OtelNamingForm()
}
