/*
 * Copyright 2017 Redlink GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
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

    /**
     * enable ajp-connector
     */
    private boolean enabled = false;

    /**
     * set ajp-secure flag
     */
    private boolean secure = false;
    /**
     * set allow-trace flag
     */
    private boolean allowTrace = false;

    /**
     * port for the ajp-connector
     */
    private int port = 9090;

    /**
     * ajp-protocol version
     */
    private String protocol = "AJP/1.3";
    /**
     * scheme to use for ajp-links
     */
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
