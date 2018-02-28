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

package io.redlink.smarti.query.solr;

import io.redlink.smarti.api.QueryBuilder;
import io.redlink.smarti.intend.IrLatchTemplate;
import io.redlink.smarti.model.*;
import io.redlink.smarti.model.Token.Type;
import io.redlink.smarti.model.result.Result;
import io.redlink.smarti.query.solr.SolrEndpointConfiguration.SingleFieldConfig;
import io.redlink.smarti.query.solr.SolrEndpointConfiguration.SpatialConfig;
import io.redlink.smarti.services.TemplateRegistry;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.redlink.smarti.intend.IrLatchTemplate.IR_LATCH;

/**
 */
@Component
@EnableConfigurationProperties(SolrEndpointConfiguration.class)
public final class SolrSearchQueryBuilder extends QueryBuilder<SolrEndpointConfiguration> {

    private final static float MIN_TOKEN_CONF = 0.1f;
    
    EnumSet<Token.Type> IGNORED_TOKEN_TYPES = EnumSet.of(Type.Attribute, Type.Other);

    private final SolrEndpointConfiguration defaultConfig;
    
    @Autowired
    public SolrSearchQueryBuilder(SolrEndpointConfiguration searchConfig, TemplateRegistry registry) {
        this(searchConfig, registry, null);
    }
    
    protected SolrSearchQueryBuilder(SolrEndpointConfiguration searchConfig, TemplateRegistry registry, String nameSuffix) {
        super(SolrEndpointConfiguration.class, registry);
        //sub-classes MUST classes some name suffix!
        if(!getClass().equals(SolrSearchQueryBuilder.class) && StringUtils.isBlank(nameSuffix)){
            throw new IllegalArgumentException("the parsed nameSuffix MUST NOT be NULL nor empty");
        }
        this.defaultConfig = searchConfig;
    }

    protected String getQueryTitle(){
        return "Solr Search";
    }

    @Override
    public boolean acceptTemplate(Template template) {
        boolean state =  IR_LATCH.equals(template.getType()); //&& 
        //with #200 queries should be build even if no slot is set
//                template.getSlots().stream() //at least a single filled slot
//                    .filter(s -> s.getTokenIndex() >= 0)
//                    .findAny().isPresent();
        log.trace("{} does {} accept {}", this, state ? "" : "not ", template);
        return state;
    }

    @Override
    protected final void doBuildQuery(SolrEndpointConfiguration config, Template template, Conversation conversation, Analysis analysis) {
        final SolrSearchQuery query = buildQuery(config, template, conversation, analysis);
        if (query != null) {
            template.getQueries().add(query);
        }
    }

    @Override
    public final boolean isResultSupported() {
        return false;
    }

    @Override
    public final SearchResult<? extends Result> execute(SolrEndpointConfiguration conf, Template template, Conversation conversation, Analysis analysis, MultiValueMap<String, String> params) throws IOException {
        throw new UnsupportedOperationException("This QueryBuilder does not support inline results");
    }
    
    protected final SolrSearchQuery buildQuery(SolrEndpointConfiguration config, Template template, Conversation conversation, Analysis analysis){
        SolrSearchQuery query = new SolrSearchQuery(getCreatorName(config),config.getResult(),config.getDefaults());
        SolrQuery solrQuery = new SolrQuery();
        //apply the default params parsed with the configuration to the query
        addDefaultParams(solrQuery, config);

        List<String> queryTerms = template.getSlots().stream()
            .filter(s -> validateSlot(s,analysis, MIN_TOKEN_CONF))
            .filter(s -> acceptSlot(s, analysis))
            .map(s -> new ImmutablePair<Slot,Token>(s, analysis.getTokens().get(s.getTokenIndex())))
            .sorted((e1,e2) -> Float.compare(e2.getValue().getConfidence(),e1.getValue().getConfidence())) //sort by confidence
            .map(e -> {
                final Slot s = e.getKey();
                final Token t = e.getValue();
                final SingleFieldConfig titleText = config.getSearch().getTitle();
                final String titleField = titleText.isEnabled() && StringUtils.isNoneBlank(titleText.getField()) ? titleText.getField() : null;
                final float titleBoost = 3f; //TODO: add boost for titles
                final Collection<String> queryParams = new LinkedList<>();
                switch (s.getRole()) {
                case IrLatchTemplate.ROLE_LOCATION:
                    SpatialConfig spatial = config.getSearch().getSpatial();
                    queryParams.add(spatial.isEnabled() ?  createQueryParam(t, spatial.getLocationNameField(), 1f) : null);
                    //also search for locations in the
                    queryParams.add(spatial.isEnabled() && titleField != null ? createQueryParam(t, titleField, titleBoost) : null);
                    break;
                case IrLatchTemplate.ROLE_ALPHABET:
                    SingleFieldConfig fullText = config.getSearch().getFullText();
                    queryParams.add(fullText.isEnabled() ? createQueryParam(t, fullText.getField(), 1f) : null);
                    //also search for such tokens in the title
                    queryParams.add(titleField != null ? createQueryParam(t, titleText.getField(), titleBoost) : null);
                    break;
                case IrLatchTemplate.ROLE_CATEGORY:
                    //TODO support category filter to SolrConfig
                    break;
                case IrLatchTemplate.ROLE_HIERARCHY:
                    //TODO: support hierarchy tokens (check if already supported by LatchTemplateBuilder
                    break;
                default:
                    log.warn("IGNORE Slot with unsupported Role {} ({} of {} in {})", s.getRole(), s, template, conversation);
                    break;
                }
                return queryParams;
            })
            .filter(Objects::nonNull) //filter null query parameter lists
            .flatMap(c -> c.stream()) //faltten query parameter lsits
            .filter(Objects::nonNull) //filter null query parameters
            .collect(Collectors.toList()); //collect all valid query parameters
        
        //#200: we want queries to be present even if we do not have any terms
        //if(queryTerms.isEmpty()){ //no terms to build a query for
        //    return null;
        //}
        query.setQueryParams(queryTerms);
        solrQuery.setQuery(StringUtils.join(queryTerms, " OR "));
       
        query.setUrl(config.getSolrEndpoint() + solrQuery.toQueryString());
        query.setDisplayTitle(config.getDisplayName());
        query.setConfidence(0.8f);
        query.setInlineResultSupport(false); //not yet implemented
        
        return query;
    }

    private void addDefaultParams(SolrQuery solrQuery, SolrEndpointConfiguration config) {
        if(MapUtils.isNotEmpty(config.getDefaults())){
            config.getDefaults().entrySet().stream()
            .filter(e -> StringUtils.isNoneBlank(e.getKey()) && e.getValue() != null)
            .forEach(e -> {
                String param = e.getKey();
                Collection<?> values;
                if(e.getValue() instanceof Collection){
                    values = (Collection<?>)e.getValue();
                } else if(e.getValue().getClass().isArray()) {
                     values = Arrays.asList((Object[])e.getValue());
                } else {
                    values = Collections.singleton(e.getValue());
                }
                Collection<String> strValues = StreamSupport.stream(values.spliterator(), false)
                        .map(Objects::toString) //convert values to strings
                        .filter(StringUtils::isNoneBlank) //filter blank values
                        .collect(Collectors.toList());
                if(!strValues.isEmpty()){
                    solrQuery.add(param, strValues.toArray(new String[strValues.size()]));
                }
            });
        }
    }
    
    /**
     * Creates a Solr query parameter for the parsed values
     * @param token
     * @param field
     * @param boost
     * @return
     */
    private String createQueryParam(Token token, String field, float boost) {
        String value = token.getValue().toString();
        if(StringUtils.isBlank(value)){
            return null;
        }
        StringBuilder param = new StringBuilder();
        if(StringUtils.isNoneBlank(field)){
            param.append(field).append(':');
        } //else use default field
        param.append('"').append(ClientUtils.escapeQueryChars(value)).append('"');
        param.append('^').append(token.getConfidence()*boost);
        return param.toString();
    }
    
    /**
     * Allows sub-classes to filter slots based on custom rules. The default implementation returns <code>true</code>
     * @param slot a slot of the template that is already validated (meaning a valid slot refering a valid token)
     * @param analysis the conversation of the slot
     * @return this base implementation returns <code>true</code>
     */
    protected boolean acceptSlot(Slot slot, Analysis analysis){
        return true;
    }
    
    /**
     * Base implementation that checks that the slot is valid, and refers a valid {@link Token} with an
     * high enough confidence and an none empty value
     * @param slot the slot to validate
     * @param analysis the analysis of the parsed SLot
     * @return <code>true</code> if the slot can be accepted for building the query
     */
    private boolean validateSlot(Slot slot, Analysis analysis, Float minConf){
        if(slot.getTokenIndex() >= 0 && slot.getTokenIndex() < analysis.getTokens().size()){
            Token token = analysis.getTokens().get(slot.getTokenIndex());
            return token != null && token.getValue() != null && (minConf == null || token.getConfidence() >= minConf);
        } else {
            return false;
        }
    }
    
    @Override
    public SolrEndpointConfiguration getDefaultConfiguration() {
        return defaultConfig;
    }
    
    @Override
    public boolean validate(SolrEndpointConfiguration configuration, Set<String> missing,
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
