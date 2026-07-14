package dev.jvmguard.collector.connection

import dev.jvmguard.agent.comm.JvmGuardKeyManager
import dev.jvmguard.common.JvmGuardProperties
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

@Component
class SslManager(
    @Qualifier("dataDirectory") dataDirectory: File,
    properties: JvmGuardProperties,
) {
    private val pastValidity: Long = TimeUnit.DAYS.toMillis(properties.certPastValidity.toLong())
    private val validity: Long = TimeUnit.DAYS.toMillis(properties.certValidity.toLong())
    private val keyAlgorithm: String = properties.keyAlgorithm
    private val keySize: Int = properties.keySize
    private val signatureAlgorithm: String = properties.signatureAlgorithm

    private val sslDirectory: File = File(dataDirectory, SSL_DIR_NAME)

    init {
        if (!sslDirectory.exists()) {
            sslDirectory.mkdirs()
        }
    }

    fun generateWebCertificate(file: File) {
        val keyGen = KeyPairGenerator.getInstance(keyAlgorithm)
        keyGen.initialize(keySize, SecureRandom())
        val serverKeyPair = keyGen.generateKeyPair()

        val serverCertificate = getX509Certificate(serverKeyPair)

        writeKeyStore(file, serverCertificate, serverKeyPair, null)
    }

    fun generateVmCertificates() {
        val keyGen = KeyPairGenerator.getInstance(keyAlgorithm)
        keyGen.initialize(keySize, SecureRandom())
        val serverKeyPair = keyGen.generateKeyPair()
        val clientKeyPair = keyGen.generateKeyPair()

        val serverCertificate = getX509Certificate(serverKeyPair)
        val clientCertificate = getX509Certificate(clientKeyPair)

        writeKeyStore(getServerKeystore(), serverCertificate, serverKeyPair, clientCertificate)
        writeKeyStore(getAgentKeystore(), clientCertificate, clientKeyPair, serverCertificate)
    }

    fun getServerKeystore(): File {
        return getFile(JvmGuardKeyManager.SERVER_STORE)
    }

    fun getAgentKeystore(): File {
        return getFile(JvmGuardKeyManager.AGENT_STORE)
    }

    fun getFile(fileName: String): File {
        return File(sslDirectory, fileName)
    }

    private fun writeKeyStore(file: File, keyCert: X509Certificate, keyPair: KeyPair, trustedCert: X509Certificate?) {
        val keyStore = KeyStore.getInstance("jks")
        keyStore.load(null, null)
        keyStore.setKeyEntry(JvmGuardKeyManager.JVMGUARD_KEY, keyPair.private, JvmGuardKeyManager.JVMGUARD.toCharArray(), arrayOf<Certificate>(keyCert))
        if (trustedCert != null) {
            keyStore.setCertificateEntry(JvmGuardKeyManager.JVMGUARD, trustedCert)
        }
        FileOutputStream(file).use { out ->
            keyStore.store(out, JvmGuardKeyManager.JVMGUARD.toCharArray())
        }
    }

    private fun getX509Certificate(keyPair: KeyPair): X509Certificate {
        val startDate = Instant.now()
        val certificateBuilder = X509v3CertificateBuilder(
            X500Name("CN=" + JvmGuardKeyManager.JVMGUARD),
            BigInteger.valueOf(startDate.toEpochMilli()),
            Date.from(startDate.minusMillis(pastValidity)),
            Date.from(startDate.plusMillis(validity)),
            X500Name("CN=" + JvmGuardKeyManager.JVMGUARD),
            SubjectPublicKeyInfo.getInstance(keyPair.public.encoded),
        )

        val certificateHolder = certificateBuilder.build(JcaContentSignerBuilder(signatureAlgorithm).build(keyPair.private))

        return CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(certificateHolder.encoded)) as X509Certificate
    }

    companion object {
        private const val SSL_DIR_NAME = "ssl"
    }
}
