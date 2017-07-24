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

package io.redlink.smarti.query.dbsearch;

import io.redlink.smarti.api.QueryBuilder;
import io.redlink.smarti.model.*;
import io.redlink.smarti.model.Token.Type;
import io.redlink.smarti.model.result.Result;
import io.redlink.smarti.services.TemplateRegistry;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.redlink.smarti.query.dbsearch.DbSearchTemplateDefinition.DBSEARCH_TYPE;

/**
 */
@Component
@EnableConfigurationProperties(DbSearchEndpointConfiguration.class)
public class DbSearchQueryBuilder extends QueryBuilder<DbSearchEndpointConfiguration> {

    private final static float MIN_TOKEN_CONF = 0.1f;
    
    EnumSet<Token.Type> IGNORED_TOKEN_TYPES = EnumSet.of(Type.Date, Type.Attribute, Type.Other);

    private final String nameSuffix;
    
    private final DbSearchEndpointConfiguration defaultConfig;
    
    @Autowired
    public DbSearchQueryBuilder(DbSearchEndpointConfiguration dbSearchConfig, TemplateRegistry registry) {
        this(dbSearchConfig, registry, null);
    }
    
    protected DbSearchQueryBuilder(DbSearchEndpointConfiguration dbSearchConfig, TemplateRegistry registry, String nameSuffix) {
        super(DbSearchEndpointConfiguration.class, registry);
        //sub-classes MUST classes some name suffix!
        if(!getClass().equals(DbSearchQueryBuilder.class) && StringUtils.isBlank(nameSuffix)){
            throw new IllegalArgumentException("the parsed nameSuffix MUST NOT be NULL nor empty");
        }
        this.nameSuffix = StringUtils.trimToNull(nameSuffix);
        //Fixme: refactoring - we need to find an alternative to cloning
        this.defaultConfig = dbSearchConfig.clone(); //sub-classes must not override the single instance spring config
    }

    @Override
    public String getCreatorName() {
        StringBuilder name = new StringBuilder("query_")
                .append(DbSearchTemplateDefinition.DBSEARCH_TYPE);
        if(nameSuffix != null){
            name.append('_').append(nameSuffix);
        }
        return name.toString();
    }
    
    protected String getQueryTitle(){
        return "DB Search Related";
    }

    @Override
    public boolean acceptTemplate(Template template) {
        boolean state =  DBSEARCH_TYPE.equals(template.getType()) && 
                template.getSlots().stream() //at least a single filled slot
                    .filter(s -> s.getTokenIndex() >= 0)
                    .findAny().isPresent();
        log.trace("{} does {}accept {}", this, state ? "" : "not ", template);
        return state;
    }

    @Override
    protected final void doBuildQuery(DbSearchEndpointConfiguration config, Template template, Conversation conversation) {
        final Query query = buildQuery(config, template, conversation);
        if (query != null) {
            template.getQueries().add(query);
        }
    }

    @Override
    public final boolean isResultSupported() {
        return false;
    }

    @Override
    public final List<? extends Result> execute(DbSearchEndpointConfiguration conf, Template template, Conversation conversation) throws IOException {
        throw new UnsupportedOperationException("This QueryBuilder does not support inline results");
    }
    
    protected final Query buildQuery(DbSearchEndpointConfiguration config, Template template, Conversation conversation){
        DbSearchQuery query = new DbSearchQuery(getCreatorName());
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setFields("*","score");
        solrQuery.setRows(10);
        
        List<String> queryTerms = template.getSlots().stream()
            .filter(s -> validateSlot(s,conversation))
            .filter(s -> acceptSlot(s, conversation))
            .map(s -> conversation.getTokens().get(s.getTokenIndex()))
            .sorted((t1,t2) -> Float.compare(t2.getConfidence(),t1.getConfidence())) //sort by confidence
            .map(t -> new StringBuilder(ClientUtils.escapeQueryChars(t.getValue().toString())) //escape the term
                    .insert(0,'"').append('"') //quote the term in case of multiple words
                    .append('^').append(t.getConfidence()).toString()) //use the confidence as boost
            .collect(Collectors.toList());
        if(queryTerms.isEmpty()){ //no terms to build a query for
            return null;
        }
        query.setFullTextTerms(queryTerms);
        solrQuery.setQuery(StringUtils.join(queryTerms, " OR "));
       
        query.setUrl(config.getSolrEndpoint() + solrQuery.toQueryString());
        query.setDisplayTitle(getQueryTitle());
        query.setConfidence(0.8f);
        query.setInlineResultSupport(false); //we can not query DB Search directly
        
        return query;
    }
    
    /**
     * Allows sub-classes to filter slots based on custom rules. The default implementation returns <code>true</code>
     * @param slot a slot of the template that is already validated (meaning a valid slot refering a valid token)
     * @param conversation the conversation of the slot
     * @return this base implementation returns <code>true</code>
     */
    protected boolean acceptSlot(Slot slot, Conversation conversation){
        return true;
    }
    
    /**
     * Base implementation that checks that the slot is valid, and refers a valid {@link Token} with an
     * high enough confidence and an none empty value
     * @param slot the slot to validate
     * @param conversation the conversation of the parsed SLot
     * @return <code>true</code> if the slot can be accepted for building the query
     */
    private boolean validateSlot(Slot slot, Conversation conversation){
        if(slot.getTokenIndex() >= 0 && slot.getTokenIndex() < conversation.getTokens().size()){
            Token token = conversation.getTokens().get(slot.getTokenIndex());
            return token != null && token.getValue() != null && token.getConfidence() >= MIN_TOKEN_CONF
                    && StringUtils.isNoneBlank(token.getValue().toString());
        } else {
            return false;
        }
    }
    
    @Override
    public DbSearchEndpointConfiguration getDefaultConfiguration() {
        return defaultConfig;
    }
    
    @Override
    public boolean validate(DbSearchEndpointConfiguration configuration, Set<String> missing,
            Map<String, String> conflicting) {
        try {
            new URL(configuration.getSolrEndpoint());
        }catch (MalformedURLException e) {
            conflicting.put("solrEndpoint", e.getMessage());
            return false;
        }
        return true;
    }
}
