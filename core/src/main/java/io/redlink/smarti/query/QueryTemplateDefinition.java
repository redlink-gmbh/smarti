/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.smarti.query;

import io.redlink.smarti.model.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public abstract class QueryTemplateDefinition {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());

    public static final String TOPIC = "topic";
    
    private final MessageTopic type;

    protected QueryTemplateDefinition(MessageTopic type){
        if(type == null){
            throw new NullPointerException("the topic MUST NOT be NULL");
        }
        this.type = type;
    }
    
    public final MessageTopic getType(){
        return type;
    }
    
    /**
     * Creates a {@link QuerySlot} instance correctly setting
     * {@link QuerySlot#isRequired()} and {@link QuerySlot#getTokenType()}
     * based on the template definition
     * @param role the name of the query slot to create
     * @return the initialized slot or <code>null</code> if no slot with that
     * name is known by the {@link QueryTemplateDefinition}
     */
    public final QuerySlot createSlot(String role){
        switch (role) {
        case TOPIC:
            return new QuerySlot(TOPIC, Token.Type.Topic, null, true);
        default:
            return createSlotForName(role);
        }
    }
    
    protected abstract QuerySlot createSlotForName(String name);

    public final boolean isValid(QueryTemplate template, List<Token> tokens){
        Optional<QuerySlot> topicSlot = template.getSlots().stream()
            .filter(s -> TOPIC.equals(s.getRole()))
            .findFirst();
        if(topicSlot.isPresent() && topicSlot.get().getTokenIndex() >= 0){
            return validate(template.getSlots(), tokens);
        } else {
            return false;
        }
    }
    
    public final boolean validateToken(List<Token> tokens, QuerySlot querySlot){
        return validateToken(tokens,querySlot.getTokenIndex(), querySlot.getTokenType());
    }
    
    protected final boolean validateToken(List<Token> tokens, int idx, Token.Type type){
        if(idx < 0){
            return false;
        } else if(idx > tokens.size()){
            return false;
        } else {
            Token token = tokens.get(idx);
            return (type == null || token.getType() == type) && token.getState() != State.Rejected;
        }
    }
    
    protected final boolean validateSlot(QuerySlot slot){
        QuerySlot expected = createSlot(slot.getRole());
        if(expected == null){
            return true;
        } else {
            return expected.getTokenType() == slot.getTokenType() && expected.isRequired() == expected.isRequired();
        }
    }
    
    protected abstract boolean validate(Collection<QuerySlot> slots, List<Token> tokens);
    
    protected Set<String> getPresentAndValidSlots(Collection<QuerySlot> slots, List<Token> tokens) {
        final Set<String> present = new HashSet<>();
        slots.stream()
            .filter(this::validateSlot)
            .filter(slot -> validateToken(tokens, slot))
            .forEach(slot -> present.add(slot.getRole()));
        return present;
    }
    /**
     * Getter for any (valid) {@link QuerySlot} part of the parsed {@link QueryTemplate}
     * that has the parsed Role
     * @param role the role
     * @param template the query template
     * @return the Token or <code>null</code> if no valid QuerySlot is present
     */
    public final QuerySlot getSlot(String role, QueryTemplate template) {
        QuerySlot expected = createSlot(role); //for known token also check the type
        final Optional<QuerySlot> slot = template.getSlots().stream()
                .filter(s -> StringUtils.equals(s.getRole(), role))
                .filter(s -> expected == null || expected.getTokenType() == s.getTokenType())
                .findAny();
        if (slot.isPresent()) {
            return slot.get();
        } else {
            return null;
        }
    }

    /**
     * Getter for all (valid) {@link QuerySlot}s part of the parsed {@link QueryTemplate}
     * @param role the role
     * @param template the query template
     * @return all valid {@link QuerySlot}s with the parsed role an empty List if none
     */
    public final List<QuerySlot> getSlots(String role, QueryTemplate template) {
        QuerySlot expected = createSlot(role); //for known token also check the type
        return template.getSlots().stream()
                .filter(s -> StringUtils.equals(s.getRole(), role))
                .filter(s -> expected == null || expected.getTokenType() == s.getTokenType())
                .collect(Collectors.toList());
    }
    
}
