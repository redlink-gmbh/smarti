/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 */
@ApiModel
@Document(collection = "conversations")
public class Conversation {

    @Id
    @ApiModelProperty(position = 0)
    @Indexed
    private String id = UUID.randomUUID().toString();

    @ApiModelProperty(position = 0, value = "metadata")
    private ConversationMeta meta = new ConversationMeta();

    @JsonProperty(required = true)
    @ApiModelProperty(position = 1, required = true)
    private User user = new User(); // TODO: needs discussion for REISEBUDDY-28

    @ApiModelProperty(position = 2, required = true, value = "List of Messages")
    private List<Message> messages = new ArrayList<>();

    @ApiModelProperty(position = 3, value = "Tokens extracted")
    private List<Token> tokens = new ArrayList<>();

    @ApiModelProperty(position = 4, value = "Templates for possible queries")
    private List<Intend> queryTemplates = new ArrayList<>();

    @ApiModelProperty(position = 5, value = "conversation context")
    private Context context = new Context();

    public String getId() {
        return id;
    }

    public ConversationMeta getMeta() {
        return meta;
    }

    public void setMeta(ConversationMeta meta) {
        this.meta = meta;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Intend> getQueryTemplates() {
        return queryTemplates;
    }

    public void setQueryTemplates(List<Intend> queryTemplates) {
        this.queryTemplates = queryTemplates;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
