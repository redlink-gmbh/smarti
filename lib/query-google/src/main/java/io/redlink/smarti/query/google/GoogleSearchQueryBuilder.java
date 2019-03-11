/*
 * Copyright 2019 DB Systel GmbH
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
 */

package io.redlink.smarti.query.google;

import io.redlink.smarti.api.QueryBuilder;
import io.redlink.smarti.intend.IrLatchTemplate;
import io.redlink.smarti.model.*;
import io.redlink.smarti.model.Token.Type;
import io.redlink.smarti.model.result.Result;
import io.redlink.smarti.services.TemplateRegistry;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
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
 * 
 */
@Component
@EnableConfigurationProperties(GoogleSearchConfiguration.class)
public final class GoogleSearchQueryBuilder extends QueryBuilder<GoogleSearchConfiguration> {

    EnumSet<Token.Type> IGNORED_TOKEN_TYPES = EnumSet.of(Type.Attribute, Type.Other);
    private final static float MIN_TOKEN_CONF = 0.1f;
    private final GoogleSearchConfiguration defaultConfig;
    
    @Autowired
    public GoogleSearchQueryBuilder(GoogleSearchConfiguration searchConfig, TemplateRegistry registry) {
        this(searchConfig, registry, null);
    }
    
    protected GoogleSearchQueryBuilder(GoogleSearchConfiguration searchConfig, TemplateRegistry registry, String nameSuffix) {
        super(GoogleSearchConfiguration.class, registry);
        //sub-classes MUST classes some name suffix!
        if(!getClass().equals(GoogleSearchQueryBuilder.class) && StringUtils.isBlank(nameSuffix)){
            throw new IllegalArgumentException("the parsed nameSuffix MUST NOT be NULL nor empty");
        }
        this.defaultConfig = searchConfig;
    }

    @Override
    protected final void doBuildQuery(GoogleSearchConfiguration config, Template template, Conversation conversation, Analysis analysis) {
        final GoogleSearchQuery query = buildQuery(config, template, conversation, analysis);
        if (query != null) {
            template.getQueries().add(query);
        }
    }

    protected final GoogleSearchQuery buildQuery(GoogleSearchConfiguration config, Template template, Conversation conversation, Analysis analysis){

        GoogleSearchQuery query = new GoogleSearchQuery(getCreatorName(config), config.getResult(), config.getDefaults());

        List<String> queryTerms = template.getSlots().stream()
            .filter(s -> validateSlot(s,analysis, MIN_TOKEN_CONF))
            .filter(s -> acceptSlot(s, analysis))
            .map(s -> new ImmutablePair<Slot,Token>(s, analysis.getTokens().get(s.getTokenIndex())))
            .sorted((e1,e2) -> Float.compare(e2.getValue().getConfidence(),e1.getValue().getConfidence())) //sort by confidence
            .map(e -> {
                final Slot s = e.getKey();
                final Token t = e.getValue();
                final Collection<String> queryParams = new LinkedList<>();
                switch (s.getRole()) {
                //we add Location and Alphabet tokens for now
                case IrLatchTemplate.ROLE_LOCATION: 
                case IrLatchTemplate.ROLE_ALPHABET:
                    queryParams.add(t.getValue().toString());
                    break;
                case IrLatchTemplate.ROLE_CATEGORY:
                    //TODO: add support category filter
                    break;
                case IrLatchTemplate.ROLE_HIERARCHY:
                    //TODO: add support hierarchy filter
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

        query.setQueryParams(queryTerms);
        query.setUrl(config.getGoogleEndpoint() + query.toQueryString());
        query.setDisplayTitle(config.getDisplayName());
        query.setConfidence(0.8f);
        query.setInlineResultSupport(false); //not yet implemented

        return query;
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

    /**
     * Creates a Solr query parameter for the parsed values
     * @param token
     * @param field
     * @param boost
     * @return
     */
    private String createQueryParam(Token token, String field, float boost) {

        return token.getValue().toString();
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

    protected String getQueryTitle(){
        return "Google Search";
    }

    @Override
    public final boolean isResultSupported() {
        return false;
    }

    @Override
    public final SearchResult<? extends Result> execute(GoogleSearchConfiguration conf, Template template, Conversation conversation, Analysis analysis, MultiValueMap<String, String> params) throws IOException {
        throw new UnsupportedOperationException("This QueryBuilder does not support inline results");
    }

    @Override
    public boolean acceptTemplate(Template template) {
        boolean state =  IR_LATCH.equals(template.getType());
        log.trace("{} does {} accept {}", this, state ? "" : "not ", template);
        return state;
    }

    @Override
    public GoogleSearchConfiguration getDefaultConfiguration() {
        return defaultConfig;
    }

    @Override
    public boolean validate(GoogleSearchConfiguration configuration, Set<String> missing,
            Map<String, String> conflicting) {
        try {
            new URL(configuration.getGoogleEndpoint());
        }catch (MalformedURLException e) {
            conflicting.put("googleEndpoint", e.getMessage());
            return false;
        }
        return true;
    }
}
