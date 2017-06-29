/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.webservice.pojo;

import org.bson.types.ObjectId;

/**
 */
public class SmartiUpdatePing {

    private final ObjectId conversationId;
    private final String token;

    public SmartiUpdatePing(ObjectId conversationId, String token) {
        this.conversationId = conversationId;
        this.token = token;
    }

    public ObjectId getConversationId() {
        return conversationId;
    }

    public String getToken() {
        return token;
    }
}
