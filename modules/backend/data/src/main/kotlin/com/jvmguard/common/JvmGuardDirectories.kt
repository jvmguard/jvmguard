package com.jvmguard.common

import com.jvmguard.common.util.LoadingDescriptor
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException

class JvmGuardDirectories private constructor(dataDirectorySpec: String, integrationTest: Boolean, dataDirectoryExplicit: Boolean) {

    val externalConfigDir: File
    val distDirectory: File
    val demoDirectory: File
    val logbackFile: File
    val dataDirectory: File

    init {
        val layout = computeLayout()
        externalConfigDir = layout.externalConfigDir
        distDirectory = layout.distDirectory
        demoDirectory = layout.demoDirectory
        logbackFile = layout.logbackFile
        try {
            if (integrationTest && !dataDirectoryExplicit) {
                dataDirectory = File.createTempFile("jvmguardData", "")
                dataDirectory.delete()
            } else {
                dataDirectory = File(dataDirectorySpec).canonicalFile
            }
            if (!dataDirectory.exists()) {
                dataDirectory.mkdirs()
                File(dataDirectory, "log").mkdir()
            }
            System.setProperty("dataDirectory", dataDirectory.absolutePath) // needed for the logback variable
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    val databaseDirectory: File
        get() = File(dataDirectory, DATABASE_DIRECTORY)

    val backupDirectory: File
        get() = File(dataDirectory, BACKUP_DIRECTORY)

    val jprofilerPackagesDirectory: File
        get() = File(File(dataDirectory, JPROFILER_DIRECTORY), PACKAGES_DIRECTORY)

    private data class Layout(
        val externalConfigDir: File,
        val distDirectory: File,
        val demoDirectory: File,
        val logbackFile: File,
    )

    companion object {
        private const val DEMO_DIR_NAME = "demo"
        private const val DATABASE_DIRECTORY = "db"
        private const val BACKUP_DIRECTORY = "backup"
        private const val JPROFILER_DIRECTORY = "jprofiler"
        private const val PACKAGES_DIRECTORY = "packages"

        @Volatile
        private var instance: JvmGuardDirectories? = null

        private fun computeLayout(): Layout {
            try {
                val loadingDescriptor = LoadingDescriptor.getInstance(JvmGuardDirectories::class.java)
                var baseDirectory = loadingDescriptor.baseDir
                val propertiesBaseDir: File
                val distDirectory: File
                val demoDirectory: File
                val logbackFile: File
                if (loadingDescriptor.isLoadedFromJAR) {
                    baseDirectory = File(baseDirectory, "../..").canonicalFile
                    propertiesBaseDir = baseDirectory
                    demoDirectory = File(baseDirectory, DEMO_DIR_NAME)
                    distDirectory = baseDirectory
                    logbackFile = File(baseDirectory, "logback.xml")
                } else {
                    baseDirectory = File(baseDirectory, "../..").canonicalFile
                    propertiesBaseDir = File(baseDirectory, "../../dist-template").canonicalFile
                    demoDirectory = File(baseDirectory, "../../dist-template/$DEMO_DIR_NAME")
                    distDirectory = File(baseDirectory, "../../dist")
                    logbackFile = File(baseDirectory, "../../dist-template/logback.xml")
                }
                return Layout(File(propertiesBaseDir, "config"), distDirectory, demoDirectory, logbackFile)
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }
        }

        // The external config directory depends only on the installation layout, so it can be resolved before any
        // configuration value is known.
        fun resolveExternalConfigDir(): File = computeLayout().externalConfigDir

        fun getInstance(): JvmGuardDirectories =
            instance ?: throw IllegalStateException("JvmGuardDirectories have not been initialized")

        @Synchronized
        fun init(dataDirectorySpec: String, integrationTest: Boolean, dataDirectoryExplicit: Boolean): JvmGuardDirectories {
            if (instance == null) {
                instance = JvmGuardDirectories(dataDirectorySpec, integrationTest, dataDirectoryExplicit)
            }
            return instance!!
        }
    }
}
