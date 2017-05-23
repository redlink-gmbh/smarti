/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.api;

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Intend;
import io.redlink.smarti.model.IntendDefinition;
import io.redlink.smarti.model.State;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.model.Token.Type;
import io.redlink.smarti.model.result.Result;
import io.redlink.smarti.services.IntendRegistry;
import io.redlink.smarti.util.QueryBuilderUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 */
public abstract class QueryBuilder {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private final IntendRegistry registry;

    protected QueryBuilder(IntendRegistry registry) {
        this.registry = registry;
    }

    public static boolean containsTokenWithType(List<Token> queryTokens, Type type) {
        return containsTokenWithType(queryTokens, type, false);
    }

    public static boolean containsTokenWithType(List<Token> queryTokens, Type type, boolean ignoreNegated) {
        return queryTokens.stream()
                .filter(t -> !(ignoreNegated && t.hasHint(Token.Hint.negated)))
                .filter(t -> t.getType() == type)
                .findFirst().isPresent();
    }


    public final void buildQuery(Conversation conversation) {
        conversation.getQueryTemplates().stream()
                .filter(t -> t.getState() != State.Rejected)
                .filter(t -> {
                    final IntendDefinition def = registry.getTemplate(t);
                    return def.isValid(t, conversation.getTokens());
                })
                .filter(this::acceptTemplate)
                .forEach(t -> doBuildQuery(t, conversation));
    }

    public abstract boolean acceptTemplate(Intend t);

    protected abstract void doBuildQuery(Intend template, Conversation conversation);
    
    
    
    /**
     * Getter for any (valid) {@link Token} referenced by the parsed
     * {@link Intend} with the parsed role
     * @param role the role
     * @param template the query template
     * @param conversation the conversation
     * @param tokenTypes the allowed types of the token
     * @return the Token or <code>null</code> if not preset or invalid
     */
    protected Token getToken(String role, Intend template, Conversation conversation, Token.Type...tokenTypes) {
        Set<Token.Type> types = toTypeSet(tokenTypes);
        final Optional<Token> token = template.getSlots().stream()
                .filter(s -> StringUtils.equals(s.getRole(), role))
                .filter(s -> s.getTokenIndex() >= 0)
                .map(s -> conversation.getTokens().get(s.getTokenIndex()))
                .filter(t -> types == null || types.contains(t.getType()))
                .findAny();
        if (token.isPresent()) {
            return token.get();
        } else {
            return null;
        }
    }

    /**
     * Getter for all (valid) {@link Token}s referenced by the parsed
     * {@link Intend} with the parsed role
     * @param role the role
     * @param template the query template
     * @param conversation the conversation
     * @param tokenTypes the allowed types of the tokens
     * @return the Token or <code>null</code> if not preset or invalid
     */
    protected List<Token> getTokens(String role, Intend template, Conversation conversation, Token.Type...tokenTypes) {
        final Set<Token.Type> types = toTypeSet(tokenTypes);
        return template.getSlots().stream()
                .filter(s -> StringUtils.equals(s.getRole(), role))
                .filter(s -> s.getTokenIndex() >= 0)
                .map(s -> conversation.getTokens().get(s.getTokenIndex()))
                .filter(t -> types == null || types.contains(t.getType()))
                .collect(Collectors.toList());
    }

    /**
     * Converts the parsed array of {@link Type} to an {@link EnumSet}.
     * @param tokenTypes the types to convert
     * @return the {@link EnumSet} with the parsed {@link Type} or <code>null</code> if no types where parsed
     */
    private Set<Token.Type> toTypeSet(Token.Type... tokenTypes) {
        if(tokenTypes == null || tokenTypes.length < 1) {
            return null;
        } else {
            Set<Token.Type> types = EnumSet.noneOf(Token.Type.class);
            for(Token.Type type : tokenTypes){
                if(type != null){
                    types.add(type);
                }
            }
            return types.isEmpty() ? null : types;
        }
    }

    /**
     * Check if the template has this role assigned to a (valid) token.
     * @param role the role
     * @param template the query template
     * @param conversation the conversation
     * @param tokenTypes the allowed token type
     * @return {@code true} if the template has a token assigned to the provided role
     */
    protected boolean hasToken(String role, Intend template, Conversation conversation, Token.Type...tokenTypes) {
        final Set<Token.Type> types = toTypeSet(tokenTypes);
        return template.getSlots().stream()
                .filter(s -> StringUtils.equals(s.getRole(), role))
                .filter(s -> s.getTokenIndex() >= 0)
                .map(s -> conversation.getTokens().get(s.getTokenIndex()))
                .filter(t -> types == null || types.contains(t.getType()))
                .findFirst().isPresent();
    }

    public List<? extends Result> execute(Intend template, Conversation conversation) throws IOException {
        return Collections.emptyList();
    }

    public boolean isResultSupported() {
        return false;
    }

    public String getCreatorName() {
        return QueryBuilderUtils.getQueryBuilderName(getClass());
    }

}
