/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.services;

import io.redlink.smarti.api.QueryBuilder;
import io.redlink.smarti.api.QueryBuilderContainer;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.State;
import io.redlink.smarti.model.Template;
import io.redlink.smarti.model.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
@Service
public class QueryBuilderService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Map<String, QueryBuilder> builders;

    public QueryBuilderService() {
        this.builders = Collections.emptyMap();
    }

    @Autowired(required = false)
    public QueryBuilderService(Optional<List<QueryBuilder>> builders, Optional<List<QueryBuilderContainer>> builderContainers) {
        log.debug("QueryBuilders: {}, QueryBuilderContainer: {}", builders, builderContainers);
        this.builders = new HashMap<>();

        builders.orElse(Collections.emptyList())
                .forEach(this::registerBuilder);
        builderContainers.orElse(Collections.emptyList()).stream()
                .flatMap(c -> c.getQueryBuilders().stream())
                .forEach(this::registerBuilder);
    }

    private void registerBuilder(QueryBuilder queryBuilder) {
        if (this.builders.putIfAbsent(queryBuilder.getCreatorName(), queryBuilder) != null) {
            throw new IllegalArgumentException("QueryBuilder with name " + queryBuilder.getCreatorName() + " already registered!");
        }
    }

    public void buildQueries(Conversation conversation) {
        log.debug("Building queries for {}", conversation);
        //retrieve the states for the queries
        final Map<Integer,Map<String,State>> queryStates = new HashMap<>();
        final AtomicInteger idx = new AtomicInteger();
        conversation.getTemplates().forEach(t -> {
            final Map<String,State> templateQueryStates = new HashMap<>();
            t.getQueries().stream()
                .filter(q -> q.getCreator() != null)
                .filter(q -> q.getState() != null)
                .forEach(q -> templateQueryStates.put(q.getCreator(),q.getState()));
            queryStates.put(idx.getAndIncrement(), templateQueryStates);
            t.setQueries(new LinkedList<>()); //remove the current queries before they are rebuilt
        });

        //build the new  queries
        for (QueryBuilder queryBuilder : builders.values()) {
            queryBuilder.buildQuery(conversation);
        }
        
        //recover the state of known queries
        idx.set(0); //rest the template index
        conversation.getTemplates().forEach(t -> {
            final Map<String,State> templateQueryStates = queryStates.get(Integer.valueOf(idx.getAndIncrement()));
            t.getQueries().stream().forEach(q -> {
                State state = templateQueryStates.get(q.getCreator());
                if(state != null){
                    q.setState(state);
                } //else looks like this is a new query as not previous state is available
            });
        });
    }

    public List<? extends Result> execute(String creator, Template template, Conversation conversation) throws IOException {
        final QueryBuilder queryBuilder = getQueryBuilder(creator);
        if (queryBuilder != null) {
            return queryBuilder.execute(template, conversation);
        } else {
            return Collections.emptyList();
        }
    }

    public QueryBuilder getQueryBuilder(String builder) {
        return builders.get(builder);
    }

    public Map<String, QueryBuilder> getQueryBuilders() {
        return Collections.unmodifiableMap(builders);
    }
}
