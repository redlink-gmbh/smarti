/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.query.conversation;

import io.redlink.smarti.model.MessageTopic;
import io.redlink.smarti.model.result.Result;

/**
 * Created by jakob on 10.02.17.
 */
public class ConversationResult extends Result {

    private double score;
    private String content;
    private String conversationId;
    private int messageIdx, votes;

    public ConversationResult(String creator, MessageTopic topic) {
        super(creator, topic);
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
}
