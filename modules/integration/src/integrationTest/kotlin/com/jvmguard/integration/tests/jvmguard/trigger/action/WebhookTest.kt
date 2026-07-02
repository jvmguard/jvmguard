package com.jvmguard.integration.tests.jvmguard.trigger.action

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.collector.main.WebhookHelper
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.triggers.ConnectionTrigger
import com.jvmguard.data.config.triggers.TimeUnit
import com.jvmguard.data.config.triggers.Trigger
import com.jvmguard.data.config.triggers.actions.BodyContentType
import com.jvmguard.data.config.triggers.actions.HttpRequestMethod
import com.jvmguard.data.config.triggers.actions.WebhookAction
import java.net.URLEncoder


open class WebhookTest : JvmGuardTest() {

    private val wireMockServer: WireMockServer = WireMockServer(options().dynamicPort().dynamicHttpsPort())

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        wireMockServer.start()
        configureStubs()
        configureTrigger(rootConfig)
    }

    private fun configureStubs() {
        wireMockServer.stubFor(
            get(urlEqualTo(GET_TARGET_PATH))
                .willReturn(ok())
        )
        wireMockServer.stubFor(
            get(urlPathEqualTo(GET_WITH_TOKEN_TARGET_PATH))
                .withQueryParam(TOKEN_KEY, equalTo("Triggered by event on root group"))
                .willReturn(ok())
        )
        wireMockServer.stubFor(
            get(urlPathEqualTo(GET_WITH_DATA_TARGET_PATH))
                .withQueryParam(DATA1_KEY, equalTo(DATA1_VALUE))
                .withQueryParam(DATA2_KEY, equalTo(DATA2_VALUE))
                .willReturn(ok())
        )
        wireMockServer.stubFor(
            post(urlEqualTo(POST_TARGET_PATH))
                .withHeader(HEADER1_KEY, equalTo(HEADER1_VALUE))
                .withHeader(HEADER2_KEY, equalTo(HEADER2_VALUE))
                .withRequestBody(equalTo(JSON_TEXT))
                .willReturn(ok())
        )
        wireMockServer.stubFor(
            post(urlEqualTo(POST_WITH_DATA_TARGET_PATH))
                .withRequestBody(containing("$DATA1_KEY=${URLEncoder.encode(DATA1_VALUE, "UTF-8")}"))
                .withRequestBody(containing("$DATA2_KEY=${URLEncoder.encode(DATA2_VALUE, "UTF-8")}"))
                .willReturn(ok())
        )
    }

    private fun configureTrigger(rootConfig: GroupConfig) {
        rootConfig.triggerSettings.triggers.add(ConnectionTrigger().apply {
            count = 10
            minimumTime = 1
            minimumTimeUnit = TimeUnit.SECONDS
            startMode = ConnectionTrigger.StartMode.IMMEDIATELY
            addWebhookAction {
                url = getTargetUrl(GET_TARGET_PATH)
                httpRequestMethod = HttpRequestMethod.GET
            }
            addWebhookAction {
                url = getTargetUrl(GET_WITH_TOKEN_TARGET_PATH)
                httpRequestMethod = HttpRequestMethod.GET
                formData = "$TOKEN_KEY=${WebhookHelper.TRIGGER_TOKEN}"
            }
            addWebhookAction {
                url = getTargetUrl(GET_WITH_DATA_TARGET_PATH)
                httpRequestMethod = HttpRequestMethod.GET
                formData = DATA_TEXT
            }
            addWebhookAction {
                url = getTargetUrl(POST_TARGET_PATH)
                httpRequestMethod = HttpRequestMethod.POST
                bodyContentType = BodyContentType.JSON
                json = JSON_TEXT
                headers = HEADERS_TEXT
            }
            addWebhookAction {
                url = getTargetUrl(POST_WITH_DATA_TARGET_PATH)
                httpRequestMethod = HttpRequestMethod.POST
                bodyContentType = BodyContentType.FORM_DATA
                formData = DATA_TEXT
            }
        })
    }

    private fun Trigger.addWebhookAction(init: WebhookAction.() -> Unit) {
        triggerActions.add(WebhookAction().apply {
            isAcceptAllCertificates = true
            init()
        })
    }

    private fun getTargetUrl(path: String) = "https://localhost:${wireMockServer.httpsPort()}${path}"

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        waitForConnections(serverConnection)
        sleep(30 * 1000)

        wireMockServer.verify(getRequestedFor(urlEqualTo(GET_TARGET_PATH)))
        wireMockServer.verify(getRequestedFor(urlPathEqualTo(GET_WITH_TOKEN_TARGET_PATH)))
        wireMockServer.verify(getRequestedFor(urlPathEqualTo(GET_WITH_DATA_TARGET_PATH)))
        wireMockServer.verify(postRequestedFor(urlEqualTo(POST_TARGET_PATH)))
        wireMockServer.verify(postRequestedFor(urlEqualTo(POST_WITH_DATA_TARGET_PATH)))

        wireMockServer.shutdownServer()
    }

    companion object {
        const val GET_TARGET_PATH = "/getTarget"
        const val GET_WITH_TOKEN_TARGET_PATH = "/getWithTokenTarget"
        const val GET_WITH_DATA_TARGET_PATH = "/getWithDataTarget"
        const val POST_TARGET_PATH = "/postTarget"
        const val POST_WITH_DATA_TARGET_PATH = "/postWithDataTarget"
        const val JSON_TEXT = "{ msg: \"Message\"}"
        const val HEADER1_KEY = "header1"
        const val HEADER1_VALUE = "value 1"
        const val HEADER2_KEY = "header2"
        const val HEADER2_VALUE = "value 2"
        const val HEADERS_TEXT = "$HEADER1_KEY=$HEADER1_VALUE\n$HEADER2_KEY=$HEADER2_VALUE"
        const val TOKEN_KEY = "token"
        const val DATA1_KEY = "data1"
        const val DATA1_VALUE = "value 1"
        const val DATA2_KEY = "data2"
        const val DATA2_VALUE = "value 2 \u00e4"
        const val DATA_TEXT = "$DATA1_KEY=$DATA1_VALUE\n$DATA2_KEY=$DATA2_VALUE"
    }
}
