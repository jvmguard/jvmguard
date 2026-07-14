package export

import dev.jvmguard.common.export.base.AbstractExport
import dev.jvmguard.common.export.base.AbstractExport.FileType
import dev.jvmguard.common.io.FileUtil
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import java.io.*
import java.nio.charset.StandardCharsets
import java.time.Instant

object ExportTestHelper {

    const val KNOWN_DATE: Long = 1387116000000L
    const val DATA_DIR: String = "data/"

    fun exportAndCompare(export: AbstractExport<*>, fileName: String) {
        export.addProperty("name", "test export")
        export.addProperty("vms", "my vms")
        export.addProperty("date", Instant.ofEpochMilli(KNOWN_DATE))

        var error = false
        try {
            compareJson(export, fileName)
        } catch (t: Throwable) {
            t.printStackTrace()
            error = true
        }
        try {
            compareXml(export, fileName)
        } catch (t: Throwable) {
            t.printStackTrace()
            error = true
        }
        if (export.isCsvSupported()) {
            try {
                compareCsv(export, fileName)
            } catch (t: Throwable) {
                t.printStackTrace()
                error = true
            }
        }
        if (error) {
            throw RuntimeException("failed")
        }
    }

    private fun compareCsv(export: AbstractExport<*>, fileName: String) {
        val newFile = File("$fileName.csv")

        val out = ByteArrayOutputStream()
        export.fileType(FileType.CSV).export(out)

        val newCsv = out.toString(StandardCharsets.UTF_8).replace("\r", "")

        if (fileName.endsWith("_empty") && newCsv.isEmpty()) {
            return
        }

        var storedCsv: String? = null
        try {
            storedCsv = getString(getResource(newFile)).trim()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (newCsv.trim() != storedCsv) {
            export.fileType(FileType.CSV).export(FileOutputStream(newFile))
            throw RuntimeException(newFile.name + " not equal")
        }
        newFile.delete()
    }

    private fun compareJson(export: AbstractExport<*>, fileName: String) {
        val newFile = File("$fileName.json")
        val out = ByteArrayOutputStream()
        export.fileType(FileType.JSON).export(out)

        val newJson = out.toString(StandardCharsets.UTF_8).replace("\r", "")
        var storedJson = ""
        try {
            storedJson = getString(getResource(newFile))
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (newJson.trim() != storedJson.trim()) {
            export.fileType(FileType.JSON).export(FileOutputStream(newFile))
            throw RuntimeException(newFile.name + " not equal")
        }
        newFile.delete()
    }

    private fun getString(bufferedInputStream: BufferedInputStream): String {
        val writer = StringWriter()
        val reader = InputStreamReader(bufferedInputStream, StandardCharsets.UTF_8)
        FileUtil.pumpCharStream(reader, writer)
        reader.close()
        writer.close()
        return writer.toString().trim().replace("\r", "")
    }

    private fun compareXml(export: AbstractExport<*>, fileName: String) {
        val newFile = File("$fileName.xml")
        export.fileType(FileType.XML).export(FileOutputStream(newFile))

        val newXml = getXmlString(BufferedInputStream(FileInputStream(newFile)))
        val storedXml = getXmlString(getResource(newFile))

        if (newXml != storedXml) {
            throw RuntimeException(newFile.name + " not equal")
        }
        newFile.delete()
    }

    private fun getResource(newFile: File): BufferedInputStream =
        BufferedInputStream(checkNotNull(ExportTestHelper::class.java.getResourceAsStream(DATA_DIR + newFile.name)))

    private fun getXmlString(input: InputStream): String {
        val builder = SAXBuilder()
        val document = builder.build(input)
        input.close()

        val outputter = XMLOutputter(Format.getCompactFormat())
        val writer = StringWriter()
        outputter.output(document, writer)
        return writer.toString()
    }
}
