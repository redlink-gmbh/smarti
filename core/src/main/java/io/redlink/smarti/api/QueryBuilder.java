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

package io.redlink.smarti.api;

import io.redlink.smarti.api.config.Configurable;
import io.redlink.smarti.model.*;
import io.redlink.smarti.model.Token.Type;
import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.model.result.Result;
import io.redlink.smarti.services.TemplateRegistry;
import io.redlink.smarti.util.QueryBuilderUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A QueryBuilder is expected to build a {@link Query} for some external (or internal) service based on
 * information consumed from a {@link Template}. Optionally it can support server side execution of
 * built queries.
 */
public abstract class QueryBuilder<C extends ComponentConfiguration> implements Configurable<C>{

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private final TemplateRegistry registry;
    private final Class<C> configType;
    private String _creatorName;

    protected QueryBuilder(Class<C> configType, TemplateRegistry registry) {
        this.configType = configType;
        this.registry = registry;
    }

    @Override
    public final String getComponentCategory() {
        return "queryBuilder";
    }
    
    @Override
    public final Class<C> getConfigurationType() {
        return configType;
    }
    
    @Override
    public final String getComponentName() {
        if(_creatorName == null){
            //Note: us a different variable to build to avoid concurrency issues
            String name = getName();
            if(name == null){
                name = getClass().getName();
            }
            _creatorName = name.replace('.', '_'); //mongo does not like '.' in field names
        }
        return _creatorName;
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


    public final void buildQuery(Conversation conversation, Analysis analysis, C configuration) {
        if(conversation == null || configuration == null){
            throw new NullPointerException();
        }
        analysis.getTemplates().stream()
                .filter(t -> t.getState() != State.Rejected)
                .filter(t -> {
                    final TemplateDefinition def = registry.getTemplate(t);
                    if(def != null){
                        return def.isValid(t, analysis.getTokens());
                    } else {
                        log.warn("Missing TemplateDefinition for type '{}' (Template: {})", t.getType(), t.getClass().getName());
                        return false;
                    }
                })
                .filter(this::acceptTemplate)
                .forEach(t -> {
                    log.trace("build query for {} and {} with {}", t , conversation, this);
                    doBuildQuery(configuration, t, conversation, analysis);
                });
    }

    public abstract boolean acceptTemplate(Template intent);

    /**
     * Builds the query for a template part of a conversation by using the parsed configuration
     * @param config the configuration to use. If a query is built it shall be added to
     * {@link Template#getQueries()}.
     * @param intent the template 
     * @param conversation the conversation
     */
    protected abstract void doBuildQuery(C config, Template intent, Conversation conversation, Analysis analysis);
    
    /**
     * Getter for the name of this QueryBuilder implementation (MUST BE a slug name) and unique to all
     * query builder components
     * @return the name (default: {@link QueryBuilderUtils#getQueryBuilderName(Class)})
     */
    public String getName(){
        return QueryBuilderUtils.getQueryBuilderName(getClass());
    }
    
    /**
     * Builds the creator name for this query builder and the parsed configuration (may be <code>null</code> if this
     * {@link QueryBuilder} does not use configurations).
     * @param config
     * @return
     */
    public final String getCreatorName(C config) {
        StringBuilder creator = new StringBuilder("queryBuilder:");
        if(config == null){
            creator.append(getName());
        } else {
            creator.append(config.getType()).append(':').append(config.getName());
        }
        return creator.toString();
            
    }
    
    /**
     * Getter for any (valid) {@link Token} referenced by the parsed
     * {@link Template} with the parsed role
     * @param role the role
     * @param template the query template
     * @param analysis the analysis results for the current conversation
     * @param tokenTypes the allowed types of the token
     * @return the Token or <code>null</code> if not preset or invalid
     */
    protected Token getToken(String role, Template template, Analysis analysis, Token.Type...tokenTypes) {
        Set<Token.Type> types = toTypeSet(tokenTypes);
        final Optional<Token> token = template.getSlots().stream()
                .filter(s -> StringUtils.equals(s.getRole(), role))
                .filter(s -> s.getTokenIndex() >= 0)
                .map(s -> analysis.getTokens().get(s.getTokenIndex()))
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
     * {@link Template} with the parsed role
     * @param role the role
     * @param template the query template
     * @param analysis the analysis results of the current conversation
     * @param tokenTypes the allowed types of the tokens
     * @return the Token or <code>null</code> if not preset or invalid
     */
    protected List<Token> getTokens(String role, Template template, Analysis analysis, Token.Type...tokenTypes) {
        final Set<Token.Type> types = toTypeSet(tokenTypes);
        return template.getSlots().stream()
                .filter(s -> StringUtils.equals(s.getRole(), role))
                .filter(s -> s.getTokenIndex() >= 0)
                .map(s -> analysis.getTokens().get(s.getTokenIndex()))
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
     * @param analysis the analysis results of the current conversation
     * @param tokenTypes the allowed token type
     * @return {@code true} if the template has a token assigned to the provided role
     */
    protected boolean hasToken(String role, Template template, Analysis analysis, Token.Type...tokenTypes) {
        final Set<Token.Type> types = toTypeSet(tokenTypes);
        return template.getSlots().stream()
                .filter(s -> StringUtils.equals(s.getRole(), role))
                .filter(s -> s.getTokenIndex() >= 0)
                .map(s -> analysis.getTokens().get(s.getTokenIndex()))
                .filter(t -> types == null || types.contains(t.getType()))
                .findFirst().isPresent();
    }

    public final SearchResult<? extends Result> execute(C config, Template template, Conversation conversation, Analysis analysis) throws IOException {
        return execute(config, template, conversation, analysis, new LinkedMultiValueMap<>());
    }
    public SearchResult<? extends Result> execute(C config, Template template, Conversation conversation, Analysis analysis, MultiValueMap<String, String> params) throws IOException {
        return new SearchResult<>();
    }

    public boolean isResultSupported() {
        return false;
    }

    @Override
    public String toString() {
        return getName();
    }

}
