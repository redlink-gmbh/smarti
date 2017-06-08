/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.webservice.pojo;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class RocketMessage {

    private String text = "";

    private boolean parseUrls = false;

    private List<RocketMessageAttachement> attachments = new ArrayList<>();

    public RocketMessage() {}

    public RocketMessage(String text) {
        this();
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isParseUrls() {
        return parseUrls;
    }

    public void setParseUrls(boolean parseUrls) {
        this.parseUrls = parseUrls;
    }

    public List<RocketMessageAttachement> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<RocketMessageAttachement> attachments) {
        this.attachments = attachments;
    }
}
