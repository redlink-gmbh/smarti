/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.db.kmp.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.springframework.http.HttpHeaders.*;

/**
 */
@Component
public class CorsFilter extends org.springframework.web.filter.CorsFilter  {

    @Value("${cors.enabled:true}")
    private boolean corsEnabled = true;

    public CorsFilter(CorsConfigurationSource configSource) {
        super(configSource);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (corsEnabled) {
            if (CorsUtils.isCorsRequest(request)) {
                response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, defaultIfBlank(request.getHeader(ORIGIN), "*"));
                response.setHeader(ACCESS_CONTROL_ALLOW_METHODS, defaultIfBlank(request.getHeader(ACCESS_CONTROL_REQUEST_METHOD),
                        "HEAD, POST, PUT, GET, OPTIONS, DELETE"));
                response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, defaultIfBlank(request.getHeader(ACCESS_CONTROL_REQUEST_HEADERS),
                        join(new String[]{CONTENT_TYPE, ACCEPT}, ',')));
                response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, LOCATION);
                response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
                response.setHeader(ACCESS_CONTROL_MAX_AGE, "3601");
            }

            if (!CorsUtils.isPreFlightRequest(request)) {
                filterChain.doFilter(request, response);
            }
        } else {
            super.doFilterInternal(request, response, filterChain);
        }
    }
}
