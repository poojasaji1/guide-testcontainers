// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
// end::copyright[]
package it.io.openliberty.guides.inventory;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

// imports for a JAXRS client to simplify the code
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
// logger imports
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// testcontainers imports
// tag::GenericContainer[]
import org.testcontainers.containers.GenericContainer;
// end::GenericContainer[]
import org.testcontainers.containers.wait.strategy.Wait;

import jakarta.ws.rs.client.ClientBuilder;
// simple import to build a URI/URL
import jakarta.ws.rs.core.UriBuilder;

// tag::LibertyContainer[]
public class LibertyContainer extends GenericContainer<LibertyContainer> {

    // tag::logger[]
    static final Logger LOGGER = LoggerFactory.getLogger(LibertyContainer.class);
    // end::logger[]

    private String baseURL;

    private KeyStore keystore;
    private SSLContext sslContext;

    // tag::getProtocol[]
    public static String getProtocol() {
        return System.getProperty("test.protocol", "https");
    }
    // end::getProtocol[]

    // tag::testHttps[]
    public static boolean testHttps() {
        return getProtocol().equalsIgnoreCase("https");
    }
    // end::testHttps[]

    // tag::constructor[]
    public LibertyContainer(final String dockerImageName) {
        super(dockerImageName);
        // wait for smarter planet message by default
        waitingFor(Wait.forLogMessage("^.*CWWKF0011I.*$", 1));
        init();
    }
    // end::constructor[]

    // tag::createRestClient[]
    public <T> T createRestClient(Class<T> clazz, String applicationPath) {
        String urlPath = getBaseURL();
        if (applicationPath != null) {
            urlPath += applicationPath;
        }
        ClientBuilder builder = ResteasyClientBuilder.newBuilder();
        if (testHttps()) {
            builder.sslContext(sslContext);
            builder.trustStore(keystore);
        }
        ResteasyClient client = (ResteasyClient) builder.build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(urlPath));
        return target.proxy(clazz);
    }
    // end::createRestClient[]

    // tag::getBaseURL[]
    public String getBaseURL() throws IllegalStateException {
        if (baseURL != null) {
            return baseURL;
        }
        if (!this.isRunning()) {
            throw new IllegalStateException(
                "Container must be running to determine hostname and port");
        }
        baseURL =  getProtocol() + "://" + this.getHost() + ":" + this.getFirstMappedPort();
        System.out.println("TEST: " + baseURL);
        return baseURL;
    }
    // end::getBaseURL[]

    // tag::init[]
    private void init() {

        if (!testHttps()) {
            // tag::addExposedPorts1[]
            this.addExposedPorts(9080);
            // end::addExposedPorts1[]
            return;
        }

        // tag::addExposedPorts2[]
        this.addExposedPorts(9443, 9080);
        // end::addExposedPorts2[]
        try {
            // tag::key-p12[]
            String keystoreFile = System.getProperty("user.dir")
                    + "/src/main/liberty/config/resources/security/key.p12";
            // end::key-p12[]
            keystore = KeyStore.getInstance("PKCS12");
            keystore.load(new FileInputStream(keystoreFile), "secret".toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                                        KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keystore, "secret".toCharArray());
            X509TrustManager xtm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException { }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException { }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            TrustManager[] tm = new TrustManager[] {
                                    xtm
                                };
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tm, new SecureRandom());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // end::init[]
}
// end::LibertyContainer[]
