package com.jvmguard.integration.util.mail

import jakarta.mail.internet.MimeMessage

class TestMessage(
    val sender: String,
    val receiver: List<String>,
    val message: MimeMessage,
)
