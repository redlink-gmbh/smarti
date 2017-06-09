/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.smarti.model;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public abstract class TemplateDefinition {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());

    public static final String TOPIC = "topic";
    
    private final String type;

    protected TemplateDefinition(String type){
        if(StringUtils.isBlank(type)){
            throw new IllegalArgumentException("the template type MUST NOT be NULL nor empty!");
        }
        this.type = type;
    }
    
    public final String getType(){
        return type;
    }
    
    /**
     * Creates a {@link Slot} instance correctly setting
     * {@link Slot#isRequired()} and {@link Slot#getTokenType()}
     * based on the template definition
     * @param role the name of the query slot to create
     * @return the initialized slot or <code>null</code> if no slot with that
     * name is known by the {@link TemplateDefinition}
     */
    public final Slot createSlot(String role){
        switch (role) {
        case TOPIC:
            return new Slot(TOPIC, Token.Type.Topic, null, true);
        default:
            return createSlotForName(role);
        }
    }
    
    protected abstract Slot createSlotForName(String name);

    public final boolean isValid(Template template, List<Token> tokens){
        if(template != null){
            return validate(template.getSlots(), tokens);
        } else {
            return false;
        }
    }
    
    public final boolean validateToken(List<Token> tokens, Slot querySlot){
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
    
    protected final boolean validateSlot(Slot slot){
        Slot expected = createSlot(slot.getRole());
        if(expected == null){
            return true;
        } else {
            return expected.getTokenType() == slot.getTokenType() && expected.isRequired() == expected.isRequired();
        }
    }
    
    protected abstract boolean validate(Collection<Slot> slots, List<Token> tokens);
    
    protected Set<String> getPresentAndValidSlots(Collection<Slot> slots, List<Token> tokens) {
        final Set<String> present = new HashSet<>();
        slots.stream()
            .filter(this::validateSlot)
            .filter(slot -> validateToken(tokens, slot))
            .forEach(slot -> present.add(slot.getRole()));
        return present;
    }
    /**
     * Getter for any (valid) {@link Slot} part of the parsed {@link Template}
     * that has the parsed Role
     * @param role the role
     * @param template the query template
     * @return the Token or <code>null</code> if no valid QuerySlot is present
     */
    public final Slot getSlot(String role, Template template) {
        Slot expected = createSlot(role); //for known token also check the type
        final Optional<Slot> slot = template.getSlots().stream()
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
     * Getter for all (valid) {@link Slot}s part of the parsed {@link Template}
     * @param role the role
     * @param template the query template
     * @return all valid {@link Slot}s with the parsed role an empty List if none
     */
    public final List<Slot> getSlots(String role, Template template) {
        Slot expected = createSlot(role); //for known token also check the type
        return template.getSlots().stream()
                .filter(s -> StringUtils.equals(s.getRole(), role))
                .filter(s -> expected == null || expected.getTokenType() == s.getTokenType())
                .collect(Collectors.toList());
    }
    
}
