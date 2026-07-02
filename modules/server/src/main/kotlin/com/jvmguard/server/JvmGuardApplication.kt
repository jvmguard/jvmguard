package com.jvmguard.server

import com.vaadin.flow.spring.annotation.EnableVaadin
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Import

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableVaadin("com.jvmguard.ui")
@Import(SpringConfiguration::class)
class JvmGuardApplication
