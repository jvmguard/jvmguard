package com.jvmguard.server

import com.jvmguard.common.JvmGuardConfig
import com.jvmguard.common.JvmGuardDirectories
import com.jvmguard.common.JvmGuardProperties
import com.jvmguard.server.sso.JvmGuardOidcUserService
import com.jvmguard.server.sso.MutableClientRegistrationRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.io.File

@Configuration
@EnableScheduling
@ComponentScan(
    basePackages = [
        "com.jvmguard.common", "com.jvmguard.data", "com.jvmguard.collector",
        "com.jvmguard.database", "com.jvmguard.rest", "com.jvmguard.connector",
        "com.jvmguard.mcp",
    ],
)
@Import(
    ServerMain::class, WebServerSupport::class, WebServerCustomizer::class, SecurityConfiguration::class,
    IntegrationTestControlController::class, SnapshotDirectoryInitializer::class,
    JvmGuardOidcUserService::class, MutableClientRegistrationRepository::class,
)
class SpringConfiguration {

    @Bean
    fun jvmguardProperties(): JvmGuardProperties = JvmGuardConfig.properties()

    @Bean
    fun jvmguardDirectories(): JvmGuardDirectories = JvmGuardDirectories.getInstance()

    @Bean
    fun taskScheduler(properties: JvmGuardProperties): ThreadPoolTaskScheduler = ThreadPoolTaskScheduler().apply {
        poolSize = properties.schedulerPoolSize
        setThreadNamePrefix("jvmguard-scheduler-")
        setRemoveOnCancelPolicy(true)
    }

    @Bean
    fun databaseWriterExecutor(properties: JvmGuardProperties): ThreadPoolTaskExecutor =
        fixedExecutor("writer-", properties.writerPoolSize)

    @Bean
    fun backupExecutor(): ThreadPoolTaskExecutor = fixedExecutor("backup-", 1)

    // With several TaskExecutor beans present, vaadin-spring cannot pick one for its async work and fails
    // startup unless one is named "VaadinTaskExecutor"
    @Bean("VaadinTaskExecutor")
    fun vaadinTaskExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 8
        keepAliveSeconds = 60
        setAllowCoreThreadTimeOut(true)
        isDaemon = true
        setThreadNamePrefix("vaadin-")
    }

    @Bean
    fun messageScheduler(): ThreadPoolTaskScheduler = ThreadPoolTaskScheduler().apply {
        poolSize = 1
        setThreadNamePrefix("message-service-")
        setRemoveOnCancelPolicy(true)
    }

    @Bean
    fun dataDirectory(directories: JvmGuardDirectories): File = directories.dataDirectory

    @Bean
    fun agentDirectory(directories: JvmGuardDirectories): File = File(directories.distDirectory, "agent")

    @Bean
    fun vmPort(properties: JvmGuardProperties): Int = properties.vmPort

    @Bean
    fun vmUseSsl(properties: JvmGuardProperties): Boolean = properties.isVmUseSsl

    companion object {
        private fun fixedExecutor(threadNamePrefix: String, poolSize: Int): ThreadPoolTaskExecutor =
            ThreadPoolTaskExecutor().apply {
                corePoolSize = poolSize
                maxPoolSize = poolSize
                setThreadNamePrefix(threadNamePrefix)
            }
    }
}
