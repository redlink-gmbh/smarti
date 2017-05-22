/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Locale;

/**
 */
@Service
public class SpeakService {

    @Value("${message.locale:de_DE}")
    private Locale messageLocale = Locale.GERMANY;

    @Value("${message.source:#{null}}")
    private String externalResourceBundle = null;

    private ReloadableResourceBundleMessageSource messageSource;

    @PostConstruct
    protected void initMessageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages/conversation");
        messageSource.setDefaultEncoding("utf-8");
        messageSource.setCacheSeconds(3600);

        if (externalResourceBundle != null) {
            ReloadableResourceBundleMessageSource external = new ReloadableResourceBundleMessageSource();
            external.setBasename(externalResourceBundle);
            external.setDefaultEncoding("utf-8");
            external.setCacheSeconds(500);
            external.setParentMessageSource(messageSource);

            this.messageSource = external;
        } else {
            this.messageSource = messageSource;
        }
    }


    public String getMessage(String code, String defaultMessage) {
        return messageSource.getMessage(code, null, defaultMessage, messageLocale);
    }

    public String getMessage(String code, String defaultMessage, Object... args) {
        return messageSource.getMessage(code, args, defaultMessage, messageLocale);
    }

    public String getMessage(String code) throws NoSuchMessageException {
        return messageSource.getMessage(code, null, messageLocale);
    }

    public String getMessage(String code, Object... args) throws NoSuchMessageException {
        return messageSource.getMessage(code, args, messageLocale);
    }

    public String getMessage(MessageSourceResolvable resolvable) throws NoSuchMessageException {
        return messageSource.getMessage(resolvable, messageLocale);
    }
}
