package com.jvmguard.common.update

import com.install4j.api.launcher.Variables
import com.install4j.api.update.ApplicationDisplayMode
import com.install4j.api.update.UpdateChecker
import com.install4j.api.update.UpdateDescriptorEntry
import com.jvmguard.common.Loggers
import com.jvmguard.common.JvmGuardUrls
import com.jvmguard.common.config.ConfigManager
import com.jvmguard.common.notification.InboxManager
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.user.UserManager
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.concurrent.TimeUnit

@Component
class UpdateManager(
    private val inboxManager: InboxManager,
    private val configManager: ConfigManager,
    private val userManager: UserManager,
    private val jdbcClient: JdbcClient,
) {

    fun checkForUpdates(): UpdateResult? {
        val installedVersion = getInstalledVersion()
        if (installedVersion == NO_VERSION) {
            return if (java.lang.Boolean.getBoolean("jvmguard.testUpdateCheck")) {
                UpdateResult("1.0", "1.1")
            } else {
                null
            }
        }

        val updateDescriptor = getUpdateDescriptor()
        return if (updateDescriptor != null) {
            UpdateResult(installedVersion, updateDescriptor.newVersion)
        } else {
            null
        }
    }

    fun getInstallationInfo(): InstallationInfo = InstallationInfo(getInstalledVersion(), getBuild())

    private fun isCheckForUpdates(): Boolean = configManager.getGlobalConfig(false).checkForUpdates

    @Scheduled(initialDelay = 10, fixedDelay = (24 * 60 * 60).toLong(), timeUnit = TimeUnit.SECONDS)
    fun automaticUpdateCheck() {
        val installedVersion = getInstalledVersion()
        if (installedVersion == NO_VERSION) {
            return
        }
        if (!isCheckForUpdates()) {
            return
        }

        val updateDescriptorEntry = getUpdateDescriptor()
        if (updateDescriptorEntry != null) {
            val newUpdateVersion = updateDescriptorEntry.newVersion
            if (!hasBeenChecked(newUpdateVersion)) {
                createUpdateInboxMessage(installedVersion, newUpdateVersion)
            }
        } else {
            SERVER_LOGGER.info("No updates are available")
        }
    }

    private fun hasBeenChecked(newUpdateVersion: String): Boolean {
        return try {
            val count = jdbcClient.sql("select count(*) from $UPDATE_CHECK where newVersion=?")
                .param(newUpdateVersion)
                .query(Int::class.javaObjectType)
                .single()
            val hasBeenChecked = count > 0
            if (!hasBeenChecked) {
                jdbcClient.sql("insert into $UPDATE_CHECK values (?)")
                    .param(newUpdateVersion)
                    .update()
            }
            hasBeenChecked
        } catch (e: Exception) {
            SERVER_LOGGER.error("could not query the update check table", e)
            true
        }
    }

    private fun createUpdateInboxMessage(installedVersion: String, newUpdateVersion: String) {
        val message = StringBuilder()
        message.append("<html>An update for jvmguard is available.")
        message.append("<p>The installed version is <b>").append(installedVersion).append("</b>, the new version is <b>").append(newUpdateVersion)
            .append("</b>.</p>")
        message.append("<p><a target=\"_blank\" href=\"").append(JvmGuardUrls.CHANGELOG_URL).append("\">Change log</a>")
        message.append("<br/><a target=\"_blank\" href=\"").append(JvmGuardUrls.DOWNLOAD_URL).append("\">Download</a>")
        message.append("</p>")
        message.append("</html>")

        SERVER_LOGGER.info("An update for jvmguard is available. The installed version is {}. The new version is {}", installedVersion, newUpdateVersion)
        for (user in userManager.getAllUsers()) {
            if (user.accessLevel == AccessLevel.ADMIN) {
                inboxManager.createInboxItem(user, null, null, "Update available", message.toString())
            }
        }
    }

    private fun getInstalledVersion(): String =
        try {
            Variables.getCompilerVariable("sys.version")
        } catch (_: IOException) {
            NO_VERSION
        }

    private fun getBuild(): String =
        try {
            Variables.getCompilerVariable("build")
        } catch (_: IOException) {
            "0"
        }

    private fun getUpdateDescriptor(): UpdateDescriptorEntry? =
        try {
            val updatesUrl = UPDATE_URL_BASE + "updates" + SERIES + ".xml" //TODO implement upload, also change in install4j project file
            UpdateChecker.getUpdateDescriptor(updatesUrl, ApplicationDisplayMode.UNATTENDED).possibleUpdateEntry
        } catch (e: Exception) {
            SERVER_LOGGER.error("Could not check for update", e)
            null
        }

    companion object {
        const val SERIES = 4

        private val SERVER_LOGGER = Loggers.SERVER

        private const val UPDATE_URL_BASE = "https://jvmguard.dev/"
        private const val NO_VERSION = "@VERSION@"
        private const val UPDATE_CHECK = "update_check"
    }
}
