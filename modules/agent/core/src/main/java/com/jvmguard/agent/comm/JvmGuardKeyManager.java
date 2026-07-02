package com.jvmguard.agent.comm;

import com.jvmguard.agent.AgentProperties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static com.jvmguard.agent.util.Util.JAVA_MAJOR_VERSION;

public class JvmGuardKeyManager extends X509ExtendedKeyManager {
    private static final String PROTOCOL = AgentProperties.getProperty("sslProtocol", JAVA_MAJOR_VERSION < 8 ? "TLSv1" : "TLSv1.2");
    private static final String PROVIDER = AgentProperties.getProperty("sslProvider");

    public static final String JVMGUARD = "jvmguard";
    public static final String JVMGUARD_KEY = "jvmguard_key";
    public static final String AGENT_STORE = "agent.ks";
    public static final String SERVER_STORE = "server.ks";
    private final X509Certificate[] certificates;
    private final PrivateKey privateKey;

    JvmGuardKeyManager(KeyStore keyStore) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        Certificate[] chain = keyStore.getCertificateChain(JVMGUARD_KEY);
        certificates = new X509Certificate[chain.length];
        for (int i = 0; i < chain.length; i++) {
            certificates[i] = (X509Certificate)chain[i];
        }

        privateKey = (PrivateKey)keyStore.getKey(JVMGUARD_KEY, JVMGUARD.toCharArray());

    }

    public static SSLContext getContext(File keyStoreFile) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException, NoSuchProviderException {
        final KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(new FileInputStream(keyStoreFile), JVMGUARD.toCharArray());

        JvmGuardKeyManager keyManager = new JvmGuardKeyManager(keyStore);
        JvmGuardTrustManager trustManager = new JvmGuardTrustManager(keyStore);

        SSLContext sc = PROVIDER != null && !PROVIDER.isEmpty() ? SSLContext.getInstance(PROTOCOL, PROVIDER) : SSLContext.getInstance(PROTOCOL);
        sc.init(new JvmGuardKeyManager[] {keyManager}, new TrustManager[] {trustManager}, null);
        return sc;
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return new String[] {JVMGUARD_KEY};
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return JVMGUARD_KEY;
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return new String[] {JVMGUARD_KEY};
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return JVMGUARD_KEY;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return certificates;
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return privateKey;
    }
}
