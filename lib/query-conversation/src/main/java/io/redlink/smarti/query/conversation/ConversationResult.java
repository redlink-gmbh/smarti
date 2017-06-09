/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.query.conversation;

import io.redlink.smarti.model.result.Result;

import java.util.Date;

/**
 * Created by jakob on 10.02.17.
 */
public class ConversationResult extends Result {

    private double score;
    private String conversationId, messageId;
    private String content;
    private String userName;
    private int messageIdx, votes;
    private Date timestamp;

    public ConversationResult(String creator) {
        super(creator);
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public int getMessageIdx() {
        return messageIdx;
    }

    public void setMessageIdx(int messageIdx) {
        this.messageIdx = messageIdx;
    }

    public int getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
