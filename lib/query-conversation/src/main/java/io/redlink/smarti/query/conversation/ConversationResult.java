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
package io.redlink.smarti.query.conversation;

import io.redlink.smarti.model.result.Result;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    private List<ConversationResult> answers = new ArrayList<>();

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

    public List<ConversationResult> getAnswers() {
        return answers;
    }

    public void setAnswers(List<ConversationResult> answers) {
        this.answers = answers;
    }

    public void addAnswer(ConversationResult answer) {
        answers.add(answer);
    }
}
