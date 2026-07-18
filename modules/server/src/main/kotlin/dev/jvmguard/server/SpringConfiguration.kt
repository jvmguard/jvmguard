package dev.jvmguard.server

import dev.jvmguard.common.JvmGuardConfig
import dev.jvmguard.common.JvmGuardDirectories
import dev.jvmguard.common.JvmGuardProperties
import dev.jvmguard.agent.util.JvmGuardThreadFactory
import dev.jvmguard.server.sso.JvmGuardOidcUserService
import dev.jvmguard.server.sso.MutableClientRegistrationRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Configuration
@EnableScheduling
@ComponentScan(
    basePackages = [
        "dev.jvmguard.common", "dev.jvmguard.data", "dev.jvmguard.collector",
        "dev.jvmguard.database", "dev.jvmguard.rest", "dev.jvmguard.connector",
        "dev.jvmguard.mcp",
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

    @Bean(destroyMethod = "shutdownNow")
    fun commExecutorService(properties: JvmGuardProperties): ExecutorService {
        val threadFactory = JvmGuardThreadFactory("comm", false, Thread.NORM_PRIORITY)
        return if (properties.commPoolSize > 0) {
            ThreadPoolExecutor(properties.commPoolSize, properties.commPoolSize, 80L, TimeUnit.SECONDS, LinkedBlockingQueue(), threadFactory)
        } else {
            ThreadPoolExecutor(0, Int.MAX_VALUE, 80L, TimeUnit.SECONDS, SynchronousQueue(), threadFactory)
        }
    }

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
