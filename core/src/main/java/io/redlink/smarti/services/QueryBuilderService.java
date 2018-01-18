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

package io.redlink.smarti.services;

import io.redlink.smarti.api.QueryBuilder;
import io.redlink.smarti.exception.NotFoundException;
import io.redlink.smarti.model.*;
import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.model.config.Configuration;
import io.redlink.smarti.model.result.Result;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
@Service
public class QueryBuilderService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ConfigurationService confService;
    
    private final Map<String, QueryBuilder<?>> builders;
    
    @Autowired
    public QueryBuilderService(ConfigurationService confService, Optional<List<QueryBuilder<?>>> builders) {
        this.confService = confService;
        log.debug("QueryBuilders: {}", builders);
        this.builders = new HashMap<>();

        builders.orElse(Collections.emptyList())
                .forEach(this::registerBuilder);
    }

    private void registerBuilder(QueryBuilder<?> queryBuilder) {
        if (this.builders.putIfAbsent(queryBuilder.getName(), queryBuilder) != null) {
            throw new IllegalArgumentException("QueryBuilder with name " + queryBuilder.getName() + " already registered!");
        }
    }

    public void buildQueries(Client client, Conversation conversation, Analysis analysis) {
        if(conversation == null){
            return;
        }
        
        if(!confService.isConfiguration(client)) return;

        Configuration clientConfig = confService.getClientConfiguration(client.getId());

        buildQueries(clientConfig, conversation, analysis);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void buildQueries(Configuration config, Conversation conversation, Analysis analysis) {
        if(config == null){
            throw new NullPointerException("parsed config MUST NOT be NULL!");
        }
        log.debug("Building queries for {}", conversation);
        //retrieve the states for the queries
        final Map<Integer,Map<String,State>> queryStates = new HashMap<>();
        final AtomicInteger idx = new AtomicInteger();
        analysis.getTemplates().forEach(t -> {
            final Map<String,State> templateQueryStates = new HashMap<>();
            t.getQueries().stream()
                .filter(q -> q.getCreator() != null)
                .filter(q -> q.getState() != null)
                .forEach(q -> templateQueryStates.put(q.getCreator(),q.getState()));
            queryStates.put(idx.getAndIncrement(), templateQueryStates);
            t.setQueries(new LinkedList<>()); //remove the current queries before they are rebuilt
        });


        //build the new queries
        //NOTE: I have no idea how to write this using generics. But the impl. checks for
        //      types safety
        for (QueryBuilder queryBuilder : builders.values()) {
            List<ComponentConfiguration> builderConfigs = (List<ComponentConfiguration>)config.getConfigurations(queryBuilder);
            for(ComponentConfiguration cc : builderConfigs){
                log.trace("build queries [{} | {} | {}]", queryBuilder, cc, conversation);
                try {
                    queryBuilder.buildQuery(conversation, analysis, cc);
                } catch (RuntimeException e) {
                    log.warn("Failed to build Queries using {} with {} for {} ({} - {})",
                            queryBuilder, cc, conversation, e.getClass().getSimpleName(), e.getMessage());
                    log.debug("Stacktrace:",e);
                }
            }
        }

        //recover the state of known queries
        idx.set(0); //rest the template index
        analysis.getTemplates().forEach(t -> {
            final Map<String,State> templateQueryStates = queryStates.get(Integer.valueOf(idx.getAndIncrement()));
            t.getQueries().stream().forEach(q -> {
                State state = templateQueryStates.get(q.getCreator());
                if(state != null){
                    q.setState(state);
                } //else looks like this is a new query as not previous state is available
            });
        });
    }

    public SearchResult<? extends Result> execute(Client client, String creator, Template template, Conversation conversation, Analysis analysis) throws IOException {
        return execute(client, creator, template, conversation, analysis, new LinkedMultiValueMap<>());
    }

    public SearchResult<? extends Result> execute(Client client, String creatorString, Template template, Conversation conversation, Analysis analysis, MultiValueMap<String, String> params) throws IOException {
        Configuration conf = confService.getClientConfiguration(client);
        if(conf == null){
            throw new IllegalStateException("The client '" + conversation.getOwner() + "' of the parsed conversation does not have a Configuration!");
        }
        final Entry<QueryBuilder<ComponentConfiguration>, ComponentConfiguration> creator = getQueryBuilder(creatorString, conf);
        if (creator != null) {
            return creator.getKey().execute(creator.getValue(), template, conversation, analysis, params);
        } else {
            throw new NotFoundException(QueryBuilder.class, creatorString, "QueryBuilder for creator '"+ creatorString +"' not present");
        }
    }



    /**
     * Getter for the QueryBuilder for the parsed creator string
     * @param creator the creator string formated as '<code>queryBuilder/{queryBuilder#getName()}/{config#getName()}</code>'
     * where '<code>{queryBuilder#getName()}</code>' is the same as '<code>{config#getType()}</code>'
     * @return the {@link QueryBuilder} or <code>null</code> if not present
     */
    public QueryBuilder<?> getQueryBuilder(String creator) {
        String[] creatorParts = StringUtils.split(creator, ':');
        if(creatorParts.length >= 2){
            return builders.get(creatorParts[1]);
        } else {
            return null;
        }
    }
    /**
     * Getter for the QueryBuilder for the parsed creator string
     * @param creator the creator string formated as '<code>queryBuilder/{queryBuilder#getName()}/{config#getName()}</code>'
     * where '<code>{queryBuilder#getName()}</code>' is the same as '<code>{config#getType()}</code>'
     * @return the {@link QueryBuilder} or <code>null</code> if not present
     */
    public <C extends ComponentConfiguration> Entry<QueryBuilder<C>,C> getQueryBuilder(String creator, Configuration conf) {
        String[] creatorParts = StringUtils.split(creator, ':');
        if(creatorParts.length >= 2){
            QueryBuilder<C> queryBuilder = (QueryBuilder<C>)builders.get(creatorParts[1]);
            if(queryBuilder == null){
                return null;
            }
            if(creatorParts.length >= 3){
                Optional<C> config = conf.getConfiguration(queryBuilder,creatorParts[2]);
                if(config.isPresent()){
                    return new ImmutablePair<>(queryBuilder, config.get());
                } else { //the referenced config was not found
                    return null;
                }
            } else { //no configuration in the creator string ... so a null configuration is OK
                return new ImmutablePair<>(queryBuilder, null);
            }
        } else {
            return null;
        }
    }

    public Map<String, QueryBuilder> getQueryBuilders() {
        return Collections.unmodifiableMap(builders);
    }

}
