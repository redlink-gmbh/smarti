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

package io.redlink.smarti.query.conversation;

import io.redlink.smarti.model.*;
import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.services.TemplateRegistry;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.FieldAnalysisRequest;
import org.apache.solr.client.solrj.response.FieldAnalysisResponse;
import org.apache.solr.client.solrj.response.AnalysisResponseBase.AnalysisPhase;
import org.apache.solr.client.solrj.response.AnalysisResponseBase.TokenInfo;
import org.apache.solr.client.solrj.response.DocumentAnalysisResponse.FieldAnalysis;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.MoreLikeThisParams;
import org.apache.solr.common.util.NamedList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.AtomicDouble;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.FIELD_MLT_CONTEXT;
import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.FIELD_TYPE;
import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.TYPE_CONVERSATION;
import static io.redlink.smarti.query.conversation.RelatedConversationTemplateDefinition.ROLE_KEYWORD;
import static io.redlink.smarti.query.conversation.RelatedConversationTemplateDefinition.ROLE_TERM;

/**
 */
@Component
public class RocketChatSearchQueryBuilder extends ConversationQueryBuilder {


    public static final String CONFIG_KEY_PAYLOAD = "payload";
    
    public static final String PARAM_PAYLOAD_ROWS = "rows";
    public static final Integer DEFAULT_PLAYLOAD_ROWS = 10;
    
    public static final String CONFIG_KEY_EXCLUDE_CURRENT = "excludeCurrentChannel";
    public static final Boolean DEFAULT_EXCLUDE_CURRENT = Boolean.FALSE;
    
    public static final String CREATOR_NAME = "query_rocketchat_search";

    @Autowired
    public RocketChatSearchQueryBuilder(SolrCoreContainer solrServer, 
            @Qualifier(ConversationIndexConfiguration.CONVERSATION_INDEX) SolrCoreDescriptor conversationCore,
            TemplateRegistry registry) {
        super(CREATOR_NAME, solrServer, conversationCore, registry);
    }

    @Override
    protected RocketChatSearchQuery buildQuery(ComponentConfiguration conf, Template intent, Conversation conversation, Analysis analysis) {
        List<Token> keywords = getTokens(ROLE_KEYWORD, intent, analysis);
        List<Token> terms = getTokens(ROLE_TERM, intent, analysis);
        int contextStart = ConversationContextUtils.getContextStart(conversation.getMessages(),
                MIN_CONTEXT_LENGTH, CONTEXT_LENGTH, MIN_INCL_MSGS, MAX_INCL_MSGS, MIN_AGE, MAX_AGE);

        final RocketChatSearchQuery query = new RocketChatSearchQuery(getCreatorName(conf));

        final String displayTitle = StringUtils.defaultIfBlank(conf.getDisplayName(), conf.getName());

        query.setInlineResultSupport(isResultSupported())
                .setState(State.Suggested)
                .setConfidence(.6f)
                .setDisplayTitle(displayTitle);
        
        query.setUrl(null);
        
        query.setUsers(conversation.getMessages().subList(contextStart, conversation.getMessages().size()).stream()
                .map(Message::getUser).filter(Objects::nonNull)
                .map(User::getId).filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        
        keywords.stream()
                .filter(t -> t.getMessageIdx() >= contextStart)
                .map(Token::getValue)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toCollection(() -> query.getKeywords()));
        
        terms.stream()
                .filter(t -> t.getMessageIdx() >= contextStart)
                .map(Token::getValue)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toCollection(() -> query.getTerms()));
        
        //apply the payload from the configuration
        Object value = conf.getConfiguration(CONFIG_KEY_PAYLOAD);
        if(value instanceof Map){
            query.getPayload().putAll((Map<String,Object>)value);;
        } else if(value instanceof Collection){
            ((Collection<Object>)value).stream()
                .filter(v -> v instanceof Map)
                .map(v -> Map.class.cast(v))
                .filter(m -> m.containsKey("key") && m.containsKey("value"))
                .forEach(m -> query.getPayload().put(String.valueOf(m.get("key")), m.get("value")));
        }
        //apply the ROE
        //NOTE: do not override if row is not set but rows is set as default 
        if(conf.isConfiguration(CONFIG_KEY_PAGE_SIZE) || !query.getPayload().containsKey("rows")){
            query.getPayload().put("rows", conf.getConfiguration(CONFIG_KEY_PAGE_SIZE, DEFAULT_PAGE_SIZE));
        }
        //copy defined parameters 
        query.setParam(CONFIG_KEY_EXCLUDE_CURRENT, conf.getConfiguration(CONFIG_KEY_EXCLUDE_CURRENT, DEFAULT_EXCLUDE_CURRENT));

        //TODO: maybe we want Filter support as for other releated conversations Query Builder
        //query.getFilters().addAll(getPropertyFilters(conversation, conf));
        
        try {
            buildContextQuery(conversation, conf, query);
        } catch (IOException | SolrServerException e) {
            if(log.isDebugEnabled()){
                log.warn("Unable to build ContextQuery for {}",conversation, e);
            } else {
                log.warn("Unable to build ContextQuery for {} ({} - {})", conversation, e.getClass().getSimpleName(), e.getMessage());
            }
        }
        return query;
    }
    
    @Override
    public ComponentConfiguration getDefaultConfiguration() {
        //ComponentConfiguration cc = super.getDefaultConfiguration();
        //if(cc == null){
        //    cc = new ComponentConfiguration();
        //}
        ComponentConfiguration cc = new ComponentConfiguration();
        cc.setConfiguration(CONFIG_KEY_EXCLUDE_CURRENT, DEFAULT_EXCLUDE_CURRENT);
        //add defaults
        Map<String,Object> payload = new HashMap<>();
        payload.put(PARAM_PAYLOAD_ROWS, DEFAULT_PLAYLOAD_ROWS);
        cc.setConfiguration(CONFIG_KEY_PAYLOAD, payload);
        return cc;
    }
    
    private void buildContextQuery(Conversation conv, ComponentConfiguration conf, RocketChatSearchQuery query) throws IOException, SolrServerException{
        int cxtStart = ConversationContextUtils.getContextStart(conv.getMessages(), 
                MIN_CONTEXT_LENGTH, CONTEXT_LENGTH, MIN_INCL_MSGS, MAX_INCL_MSGS, MIN_AGE, MAX_AGE);
        List<Message> ctxMsgs = conv.getMessages().subList(cxtStart, conv.getMessages().size());
        //add the context messages to Ids of the Messages
        ctxMsgs.forEach(msg -> query.addContextMsg(msg.getId()));
        String context = ctxMsgs.stream()
            .filter(m -> !MapUtils.getBoolean(m.getMetadata(), Message.Metadata.SKIP_ANALYSIS, false))
            .map(Message::getContent)
            .reduce(null, (s, e) -> {
                if (s == null) return e;
                return s + "\n\n" + e;
            });
        log.trace("SimilarityContext: {}", context);
        if(StringUtils.isBlank(context)){ //fix for #2258
            return; //for an empty context use an empty query
        }

        //we make a MLT query to get the interesting terms
        final SolrQuery solrQuery = new SolrQuery();
        //search interesting terms in the conversations (as those do not have overlapping
        //contexts as messages)
        solrQuery.addFilterQuery(String.format("%s:%s", FIELD_TYPE, TYPE_CONVERSATION));
        //respect client filters
        addClientFilter(solrQuery, conv);
        //and also property related filters
        addPropertyFilters(solrQuery, conv, conf);
        //and completed query
        if(conf.getConfiguration(CONFIG_KEY_COMPLETED_ONLY, DEFAULT_COMPLETED_ONLY)){
            addCompletedFilter(solrQuery);
        }
        //we do not want any results
        solrQuery.setRows(0);
        //we search for interesting terms in the MLT Context field
        solrQuery.add(MoreLikeThisParams.SIMILARITY_FIELDS, FIELD_MLT_CONTEXT);
        solrQuery.add(MoreLikeThisParams.BOOST,String.valueOf(true));
        solrQuery.add(MoreLikeThisParams.INTERESTING_TERMS,"details");
        solrQuery.add(MoreLikeThisParams.MAX_QUERY_TERMS, String.valueOf(10));
        solrQuery.add(MoreLikeThisParams.MIN_WORD_LEN, String.valueOf(3));
        
        log.trace("InterestingTerms QueryParams: {}", solrQuery);
        
        try (SolrClient solrClient = solrServer.getSolrClient(conversationCore)) {
            NamedList<Object> response = solrClient.request(new ConversationMltRequest(solrQuery, context));
            NamedList<Object> interestingTermList = (NamedList<Object>)response.get("interestingTerms");
            if(interestingTermList != null && interestingTermList.size() > 0) { //interesting terms present
                //Do make it easier to combine context params with other ensure that the maximum boost is 1.0
                AtomicDouble norm = new AtomicDouble(-1);
                List<ContextTerm> ctxTerms = StreamSupport.stream(interestingTermList.spliterator(), false)
                    .sorted((a,b) -> Double.compare(((Number)b.getValue()).doubleValue(), ((Number)a.getValue()).doubleValue()))
                    .map(e -> {
                        //NOTE: we need to query escape the value of the term as the returned
                        // interesting terms are not!
                        final String term;
                        final String field;
                        int fieldSepIdx = e.getKey().indexOf(':'); //we need to strip the solr field from the term 
                        //NOTE: we do not Solr query escape as we do not want to make assumptions about the
                        //      implementation of the Rocket.Chat search
                        if(fieldSepIdx >= 0){
                            term = e.getKey().substring(fieldSepIdx + 1);
                            field = e.getKey().substring(0,fieldSepIdx);
                        } else {
                            term = e.getKey();
                            field = null;
                        }
                        //NOTE that those terms are Solr Analyzed (lower case, stemmed, ...)
                        //We could try to map them with words in the context or extracted Entities and Keywords
                        final float relevance;
                        if(norm.doubleValue() < 0) {
                            norm.set(1d/((Number)e.getValue()).doubleValue());
                            relevance = 1f;
                        } else {
                            relevance = (((Number)e.getValue()).floatValue() * norm.floatValue());
                        }
                        return new ContextTerm(e.getKey(), relevance);
                    })
                    .collect(Collectors.toList());
                //build an Index of {field,term} -> [<AnalysisInfo>]
                final WordAnalysisIdx waIdx = new WordAnalysisIdx();
                analyseTerm(solrClient, 
                        ctxTerms.stream().map(ContextTerm::getField).collect(Collectors.toSet()), 
                        context).forEach((field,terms) -> waIdx.add(field,terms));
                //now we can get the actual words for the Terms returned by the Solr MLT interesting Terms element
                log.debug("write contextQueryTerms");
                ctxTerms.forEach(ctxTerm -> {
                    Set<String> words = waIdx.getWords(ctxTerm.field,ctxTerm.term);
                    if(CollectionUtils.isNotEmpty(words)){
                        log.debug(" term: {} -> words: {}", ctxTerm.term, words);
                        query.addContextQueryTerm(words.iterator().next(), ctxTerm.getRelevance());
                    } else {
                        log.debug(" term: {} -> no words found - ignored", ctxTerm.term);
                    }
                });
            }
        } catch (SolrException solrEx) { //related to #2258 - make code more robust to unexpected errors
            if(log.isDebugEnabled()){
                log.warn("Unable to build ContextQuery using solrQuery: {} and context: '{}'", 
                        solrQuery, context, solrEx);
            } else {
                log.warn("Unable to build ContextQuery using solrQuery: {} and context: '{}' ({} - {})", 
                        solrQuery, context, solrEx.getClass().getSimpleName(), solrEx.getMessage());
            }
        }
    }
    
    /**
     * This method allows to analyse the parsed text with the Solr Analyser of the parsed field name.
     * This is useful when one needs to match Solr terms (from the inverted index) with sections of
     * the text or Terms and Keywords extracted from that text
     * @param client the SolrClient used to execute the requests
     * @param field the Solr field
     * @param text the text to analyse
     * @throws SolrException
     * @throws IOException
     * @throws SolrServerException
     */
    private Map<String,List<WordAnalysis>> analyseTerm(SolrClient client, Set<String> fields, String text) throws SolrException, IOException, SolrServerException {
        FieldAnalysisRequest request = new FixedFieldAnalysisRequest();
        request.setFieldNames(new ArrayList<>(fields));
        request.setFieldValue(text);
        FieldAnalysisResponse respone = request.process(client);
        Map<String,List<WordAnalysis>> fieldAnalysis = new HashMap<>();
        respone.getAllFieldNameAnalysis().forEach(e -> {
            org.apache.solr.client.solrj.response.FieldAnalysisResponse.Analysis analysis = e.getValue();
            final AnalysisPhase result; //the analysis result is the output of the last AnalysisPhase
            if(analysis.getIndexPhases() instanceof List){ //in current SolrJ this is a list 
                result = ((List<AnalysisPhase>)analysis.getIndexPhases()).get(analysis.getIndexPhasesCount() - 1);
            } else { //but provide a fallback if not ...
                AnalysisPhase phase = null;
                for(Iterator<AnalysisPhase> it = analysis.getIndexPhases().iterator(); it.hasNext(); phase = it.next());
                result = phase;
            }
            fieldAnalysis.put(e.getKey(), result.getTokens().stream().map(t -> new WordAnalysis(text, t)).collect(Collectors.toList()));
        });
        return fieldAnalysis;
    }
    
    private static class WordAnalysis {
        
        public final String word;
        public final String term;
        public final int start;
        public final int end;
        public final int pos;
        
        WordAnalysis(String text, TokenInfo token){
            start = token.getStart();
            end = token.getEnd();
            pos = token.getPosition();
            this.term = token.getText();
            word = text.substring(start, end);
        }
        
        public String getWord() {
            return word;
        }
        
        public String getTerm() {
            return term;
        }
        
        public int getStart() {
            return start;
        }
        
        public int getEnd() {
            return end;
        }
        
        public int getPos() {
            return pos;
        }
    }
    
    private static class ContextTerm {
        
        final String field;
        final String term;
        final float relevance;
        
        ContextTerm(String solrTerm, float relevance){
            int fieldSepIdx = solrTerm.indexOf(':'); //we need to strip the solr field from the term 
            //NOTE: we do not Solr query escape as we do not want to make assumptions about the
            //      implementation of the Rocket.Chat search
            if(fieldSepIdx >= 0){
                term = solrTerm.substring(fieldSepIdx + 1);
                field = solrTerm.substring(0,fieldSepIdx);
            } else {
                term = solrTerm;
                field = null;
            }
            this.relevance = relevance;
        }
        
        public String getField() {
            return field;
        }
        
        public String getTerm() {
            return term;
        }
        
        public float getRelevance() {
            return relevance;
        }
    }
    
    static class WordAnalysisIdx {
        
        private final Map<Pair<String,String>, Collection<WordAnalysis>> tokenIndex = new HashMap<>();
        
        public void add(String field, Iterable<WordAnalysis> words){
            words.forEach(wa -> tokenIndex.computeIfAbsent(
                    new ImmutablePair<String, String>(field, wa.term), k -> new LinkedList<>()).add(wa));
        }
        
        public Collection<WordAnalysis> getWordAnalysis(String field, String term){
            return tokenIndex.get(new ImmutablePair<String,String>(field, term));
        }
        public Set<String> getWords(String field, String term){
            return tokenIndex.getOrDefault(new ImmutablePair<String,String>(field, term), Collections.emptySet()).stream()
                .map(WordAnalysis::getWord)
                .collect(Collectors.toSet());
        }
    }
    
    /*
     * Workaround for a Bug in SolrJ FixedFieldAnalysisResponse
     * 
     * The FixedFieldAnalysisResponse#buildPhases(..) can not handle situations where
     * a CharFilter is part of the AnalysisPipeline.
     * In those cases the phaseNL does simple contain the parsed Text and not a
     * further NamedList. So the buildPhrase runs into an ClassCastException.
     * 
     * This implementation performs an instanceof check and simple ignores CharFilter
     * information present in the Response
     * 
     * To make this work it is required to call the default visible constructor of the
     * AnalysisPhase via reflection :(
     * 
     */
    private final static class FixedFieldAnalysisRequest extends FieldAnalysisRequest {
        @Override
        protected FieldAnalysisResponse createResponse(SolrClient client) {
            if (getFieldTypes() == null && getFieldNames() == null) {
                throw new IllegalStateException("At least one field type or field name need to be specified");
            }
            if (getFieldValue() == null) {
                throw new IllegalStateException("The field value must be set");
            }
            return new FixedFieldAnalysisResponse();
        }
    }
    private final static class FixedFieldAnalysisResponse extends FieldAnalysisResponse {
        @Override
        protected List<AnalysisPhase> buildPhases(NamedList<List<NamedList<Object>>> phaseNL) {
            List<AnalysisPhase> phases = new ArrayList<>(phaseNL.size());
            for (Map.Entry<String, List<NamedList<Object>>> phaseEntry : phaseNL) {
                if(phaseEntry.getValue() instanceof List){
                    final AnalysisPhase phase;
                    try {
                        Constructor<AnalysisPhase> constructor = AnalysisPhase.class.getDeclaredConstructor(String.class);
                        constructor.setAccessible(true);
                        phase = constructor.newInstance(phaseEntry.getKey());
                    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | 
                            IllegalArgumentException | InvocationTargetException e) {
                        throw new SolrException(ErrorCode.UNKNOWN, "Unable to instanciate AnalysisPhrase using reflection ("
                                + e.getClass().getSimpleName() + " - " + e.getMessage() + ")!", e);
                    }
                    List<NamedList<Object>> tokens = phaseEntry.getValue();
                    for (NamedList<Object> token : tokens) {
                        TokenInfo tokenInfo = buildTokenInfo(token);
                        phase.getTokens().add(tokenInfo);
                    }
                    phases.add(phase);
                } // CharFilter do not define the information needed to construct AnalysisPhrases ... as we do not need those anyway we just ignore them
            }
            return phases;
        }
    }
    
}
