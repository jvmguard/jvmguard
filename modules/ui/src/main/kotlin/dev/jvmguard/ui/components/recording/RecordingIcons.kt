package dev.jvmguard.ui.components.recording

import dev.jvmguard.data.config.triggers.TriggerType
import dev.jvmguard.data.config.triggers.actions.ActionType
import com.vaadin.flow.component.icon.VaadinIcon

fun triggerTypeIcon(type: TriggerType): VaadinIcon = when (type) {
    TriggerType.THRESHOLD -> VaadinIcon.WARNING
    TriggerType.POLICY -> VaadinIcon.FLAG
    TriggerType.CONNECTION -> VaadinIcon.CONNECT
}

fun actionTypeIcon(type: ActionType): VaadinIcon = when (type) {
    ActionType.RECORD_JPS -> VaadinIcon.CAMERA
    ActionType.RECORD_JFR -> VaadinIcon.FILM
    ActionType.THREAD_DUMP -> VaadinIcon.LINES
    ActionType.HEAP_DUMP -> VaadinIcon.DATABASE
    ActionType.EMAIL -> VaadinIcon.ENVELOPE
    ActionType.WEBHOOK -> VaadinIcon.GLOBE
    ActionType.LOG -> VaadinIcon.FILE_TEXT_O
    ActionType.INBOX -> VaadinIcon.INBOX
}
