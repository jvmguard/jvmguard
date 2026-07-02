package com.jvmguard.agent.comm;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class JvmGuardTrustManager implements X509TrustManager {
    final X509Certificate certificate;

    JvmGuardTrustManager(KeyStore keyStore) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        certificate = (X509Certificate)keyStore.getCertificate(JvmGuardKeyManager.JVMGUARD);

    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (!chain[0].equals(certificate)) {
            throw new CertificateException("unknown certificate");
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (!chain[0].equals(certificate)) {
            throw new CertificateException("unknown certificate");
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
