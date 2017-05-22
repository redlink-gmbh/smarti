/*
 * Copyright (c) 2017 DB Systel GmbH.
 */

package io.redlink.smarti.configuration;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Optional AJP-Connector for tomcat
 * @author JakobPannier
 */
@Configuration
@EnableConfigurationProperties(TomcatConfiguration.class)
@ConfigurationProperties(prefix = "tomcat.ajp")
public class TomcatConfiguration {

    private boolean enabled = false;

    private boolean secure = false;
    private boolean allowTrace = false;

    private int port = 9090;

    private String protocol = "AJP/1.3";
    private String scheme = "http";

    @Bean
    public EmbeddedServletContainerFactory servletContainer() {

        final TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
        if (isEnabled()) {
            final Connector ajpConnector = new Connector(getProtocol());
            ajpConnector.setPort(getPort());
            ajpConnector.setSecure(isSecure());
            ajpConnector.setAllowTrace(isAllowTrace());
            ajpConnector.setScheme(getScheme());

            tomcat.addAdditionalTomcatConnectors(ajpConnector);
        }

        return tomcat;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isAllowTrace() {
        return allowTrace;
    }

    public void setAllowTrace(boolean allowTrace) {
        this.allowTrace = allowTrace;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }
}
