package dev.jvmguard.installer

import com.install4j.api.test.session.InstallerSessionBuilder
import com.install4j.api.test.session.onCurrentScreen
import com.install4j.api.test.session.onScreen
import com.install4j.api.test.session.useSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.time.Duration

/**
 * Drives the GUI of the built Unix installer through a fresh install and asserts that the values
 * entered in the configuration forms end up in config/application.yaml and in the response file.
 * Runs without privileges and without the service, so no system state is touched.
 */
class InstallerUiTest {

    @Test
    fun freshInstallAppliesFormValuesToYamlConfig(@TempDir tempDir: File) {
        val media = File(System.getProperty("test.media", ""))
        assertTrue(media.isFile) { "Installer media not found: $media" }

        // must not exist, the installation directory chooser warns about existing directories
        val installDir = File(tempDir, "server")
        val dataDir = File(tempDir, "data")

        InstallerSessionBuilder
            .forMediaFile(media)
            .installationDirectory(installDir)
            .arg("-VnoPrivileges=true", "-Vjvmguard.noService=true")
            .timeout(Duration.ofMinutes(10))
            .logFile(File(System.getProperty("java.io.tmpdir"), "jvmguard-installer-test.log"))
            .useSession {
                onScreen("welcome") { nextScreen() }

                onScreen("license") {
                    license("license").scrollToBottom().accept()
                    nextScreen()
                }

                onScreen("installationDir") {
                    directoryChooser("installationDirChooser").directory = installDir
                    nextScreen()
                }

                onScreen("webServer") {
                    integerTextField("httpPort").setValue(8099)
                    checkBox("useHttps").isSelected = true
                    nextScreen()
                }

                onScreen("dataDir") {
                    directoryChooser("dataDirectory").directory = dataDir
                    nextScreen()
                }

                onScreen("configLevel") {
                    radioButtons("configLevel").selectLabel("Show advanced configuration now")
                    nextScreen()
                }

                onScreen("externalAccess") {
                    checkBox("mcpEnabled").isSelected = false
                    nextScreen()
                }

                onScreen("ssl") {
                    checkBox("vmUseSsl").isSelected = true
                    nextScreen()
                }

                onScreen("monitoredJvms") {
                    integerTextField("vmPort").setValue(8851)
                    nextScreen()
                }

                // the Installation screen is transient and auto-advances to the Finish screen
                navigateUntilScreen("finish", 5)
                onCurrentScreen { finish() }
                assertEquals(0, awaitExit())
            }

        val yaml = Files.readString(File(installDir, "config/application.yaml").toPath())
        assertTrue(yaml.contains("httpPort: 8099")) { yaml }
        assertTrue(yaml.contains("useHttps: true")) { yaml }
        assertTrue(yaml.contains("vmPort: 8851")) { yaml }
        assertTrue(yaml.contains("vmUseSsl: true")) { yaml }
        assertTrue(yaml.contains("mcpEnabled: false")) { yaml }
        assertTrue(yaml.contains("dataDirectory: ${dataDir.path}")) { yaml }

        val responseFile = File(installDir, "config/jvmguard.varfile")
        assertTrue(responseFile.isFile) { "Response file was not created" }
        assertTrue(Files.readString(responseFile.toPath()).contains("8099"))

        assertTrue(dataDir.isDirectory) { "Data directory was not created" }
    }
}
