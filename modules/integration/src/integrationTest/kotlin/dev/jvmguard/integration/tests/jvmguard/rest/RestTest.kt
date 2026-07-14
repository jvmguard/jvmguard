package dev.jvmguard.integration.tests.jvmguard.rest

import com.beust.klaxon.JsonObject
import com.install4j.runtime.util.Base64
import dev.jvmguard.integration.*
import dev.jvmguard.integration.config.VMConfig
import dev.jvmguard.integration.util.attr
import dev.jvmguard.integration.util.nonNullAttr
import dev.jvmguard.integration.util.nonNullLong
import dev.jvmguard.integration.util.nonNullString
import dev.jvmguard.integration.util.parseXmlString
import java.io.InputStream
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import org.jdom2.Element

val DATE_FORMAT_Z = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").apply { timeZone = TimeZone.getTimeZone("UTC") }

class RestTest : JvmGuardTest() {

    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = 2

    override fun getServerOptions(runNo: Int, libraryNo: Int) =
        super.getServerOptions(runNo, libraryNo) + mapOf(
            "jvmguard.restApiEnabled" to true,
            "jvmguard.useHttps" to (libraryNo % 2 == 1)
        )

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        if (libraryNo % 2 == 1) {
            enableTrustAll()
        }

        waitForConnections(serverConnection)
        val url = getUrl("vms")
        val node = parseXmlString(getReader(url, RestContentType.XML).use { it.readText() })
        assertEqual(node.name, "vms")
        assertEqual(node.children.size, 2)
        assertEqual(node.children.count { child -> child.name == "vm" && child.attr("name") == "default/JVM" }, 1)
        assertEqual(node.children.count { child -> child.name == "vm" && child.attr("name") == "default/JVM2" }, 1)
    }

    private fun getUrl(path: String) =
        URI("http${if (libraryNo % 2 == 1) "s" else ""}://localhost:$httpPort/api/$path").toURL()

    private fun enableTrustAll() {
        val trustAllCerts = arrayOf<X509TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun getAcceptedIssuers() = null
        })
        val sc = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
    }

}

fun parseDate(jsonDate: String?) = DATE_FORMAT_Z.parse(jsonDate!!).time

fun encodeURIComponent(s: String) = try {
    URLEncoder.encode(s, "UTF-8")
        .replace(Regex("\\+"), "%20")
        .replace(Regex("%21"), "!")
        .replace(Regex("%27"), "'")
        .replace(Regex("%28"), "(")
        .replace(Regex("%29"), ")")
        .replace(Regex("%7E"), "~")
} catch (_: UnsupportedEncodingException) {
    s
}

fun JvmGuardTest.readTextAndSplit(url: URL) = getReader(url, RestContentType.TEXT).readText().lines().filter { it.isNotEmpty() }

fun getStringField(node: Any, name: String) = when (node) {
    is Element -> node.nonNullAttr(name)
    is JsonObject -> node.nonNullString(name)
    else -> throw RuntimeException(node.toString())
}

fun getLongField(node: Any, name: String) = when (node) {
    is Element -> node.nonNullAttr(name).toLong()
    is JsonObject -> node.nonNullLong(name)
    else -> throw RuntimeException(node.toString())
}

fun JvmGuardTest.checkAuthorization(url: URL) {
    assertEqual((url.openConnection() as HttpURLConnection).responseCode, 401)
    val connection = url.openConnection() as HttpURLConnection
    connection.setRequestProperty("Authorization", "Basic " + Base64.encode((Credentials.LOGIN + ":wrong").toByteArray()))
    assertEqual(connection.responseCode, 401)
}


fun JvmGuardTest.getInputStream(url: URL, contentType: RestContentType = RestContentType.JSON): InputStream {
    checkAuthorization(url)
    val connection = url.openConnection() as HttpURLConnection
    if (contentType != RestContentType.NONE) {
        connection.setRequestProperty("Accept", contentType.toString())
    }
    println("USING API KEY " + Credentials.API_KEY)
    connection.setRequestProperty("Authorization", "Basic " + Base64.encode((Credentials.LOGIN + ":" + Credentials.API_KEY).toByteArray()))
    if (connection.responseCode != 200) {
        throw AssertionError(InputStreamReader(connection.errorStream, "UTF-8").readText())
    }
    assertEqual(connection.contentType.lowercase(Locale.getDefault()), contentType.expectedReturn.lowercase(Locale.getDefault()))
    return connection.inputStream
}

fun JvmGuardTest.getReader(url: URL, contentType: RestContentType = RestContentType.JSON) = getInputStream(url, contentType).reader()

fun JvmGuardTest.checkResponseCode(url: URL, expectedResponse: Int, expectedMessage: String? = null) {
    val connection = url.openConnection() as HttpURLConnection
    connection.setRequestProperty("Authorization", "Basic " + Base64.encode((Credentials.LOGIN + ":" + Credentials.API_KEY).toByteArray()))
    assertEqual(connection.responseCode, expectedResponse)
    if (expectedMessage != null) {
        assertEqual(String(connection.errorStream.readBytes()), expectedMessage)
    }
}
