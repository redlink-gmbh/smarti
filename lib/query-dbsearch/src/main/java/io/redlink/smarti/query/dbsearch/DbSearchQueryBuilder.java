/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.query.dbsearch;

import static io.redlink.smarti.query.dbsearch.DbSearchTemplateDefinition.DBSEARCH_TYPE;
import static io.redlink.smarti.query.dbsearch.DbSearchTemplateDefinition.ROLE_TERM;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.redlink.smarti.api.QueryBuilder;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Query;
import io.redlink.smarti.model.Template;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.model.Token.Type;
import io.redlink.smarti.model.result.Result;
import io.redlink.smarti.services.TemplateRegistry;

/**
 */
@Component
@ConditionalOnProperty("dbsearch.solr")
public class DbSearchQueryBuilder extends QueryBuilder {

    @Value("${dbsearch.solr}") //required
    private String solrEndpoint;

    private final static float MIN_TERM_CONF = 0.1f;
    
    EnumSet<Token.Type> IGNORED_TOKEN_TYPES = EnumSet.of(Type.Date);
    
    public DbSearchQueryBuilder(TemplateRegistry registry) {
        super(registry);
    }

    @Override
    public String getCreatorName() {
        return "query." + DbSearchTemplateDefinition.DBSEARCH_TYPE;
    }

    @Override
    public boolean acceptTemplate(Template template) {
        return DBSEARCH_TYPE.equals(template.getType()) && 
                template.getSlots().stream() //at least a single filled slot
                    .filter(s -> s.getRole().equals(ROLE_TERM))
                    .filter(s -> s.getTokenIndex() >= 0)
                    .findAny().isPresent();
    }

    @Override
    protected void doBuildQuery(Template template, Conversation conversation) {
        final Query query = buildQuery(template, conversation);
        if (query != null) {
            template.getQueries().add(query);
        }
    }

    @Override
    public boolean isResultSupported() {
        return false;
    }

    @Override
    public List<? extends Result> execute(Template template, Conversation conversation) throws IOException {
        throw new UnsupportedOperationException("This QueryBuilder does not support inline results");
    }
    protected Query buildQuery(Template template, Conversation conversation){
        DbSearchQuery query = new DbSearchQuery();
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setFields("*","score");
        solrQuery.setRows(10);
        
        List<String> queryTerms = template.getSlots().stream()
            .filter(s -> ROLE_TERM.equals(s.getRole()))
            .filter(s -> s.getTokenIndex() >= 0 && s.getTokenIndex() < conversation.getTokens().size())
            .map(s -> conversation.getTokens().get(s.getTokenIndex()))
            .filter(Objects::nonNull) //ignore null tokens
            .filter(t -> !IGNORED_TOKEN_TYPES.contains(t.getType())) //token of some types might be ignored
            .filter(t -> t.getValue() != null) //the value MUST NOT be NULL
            .filter(t -> StringUtils.isNoneBlank(t.getValue().toString())) //and tokens with empty value
            .filter(t -> t.getConfidence() >= MIN_TERM_CONF) //and low confidence
            .sorted((t1,t2) -> Float.compare(t2.getConfidence(),t1.getConfidence())) //sort by confidence
            .map(t -> new StringBuilder(ClientUtils.escapeQueryChars(t.getValue().toString())) //escape the term
                    .insert(0,'"').append('"') //quote the term in case of multiple words
                    .append('^').append(t.getConfidence()).toString()) //use the confidence as boost
            .collect(Collectors.toList());
        
        query.setFullTextTerms(queryTerms);
        solrQuery.setQuery(StringUtils.join(queryTerms, " OR "));
       
        query.setUrl(solrEndpoint + solrQuery.toQueryString());
        query.setDisplayTitle("DB Search Related");
        query.setConfidence(0.8f);
        query.setInlineResultSupport(false); //we can not query DB Search directly
        
        return query;
    }
}
