package io.redlink.smarti.processor.keyword.intrestingterms;

import io.redlink.nlp.api.ProcessingData;
import io.redlink.nlp.api.Processor;
import io.redlink.nlp.api.model.Value;
import io.redlink.nlp.model.AnalyzedText;
import io.redlink.nlp.model.NlpAnnotations;
import io.redlink.nlp.model.Token;
import io.redlink.nlp.model.util.NlpUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.util.NamedList;

import javax.annotation.PreDestroy;

import static io.redlink.smarti.processor.keyword.intrestingterms.InterestingTermsConst.INTERESTING_TERM;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * To be sub-classed 
 * @author westei
 *
 */
public abstract class InterestingTermExtractor extends Processor implements Closeable {

    private SolrClient client;
    
    private ContentSectionProvider contentProvider = ContentSectionProvider.SECTIONS;

    private MltConfig mltConf;
    
    protected InterestingTermExtractor(String name, SolrClient client) {
        super("keyword.interestingterms." + name, "Interesting Term Extractor for Solr Core " + name, Phase.extraction);
        this.client = client;
        setMltConfig(null); //set the default config
    }

    /**
     * The SolrClient used by this linker
     * @return
     */
    public final SolrClient getClient() {
        return client;
    }


    /**
     * Setter for the Solr Client to be used by this linker.
     * MUST BE called before {@link #init()} is called if the client was not already parsed 
     * by the constructor
     * @param client the client
     */
    protected final void setClient(SolrClient client) {
        this.client = client;
    }

    /**
     * The similarity fields and MLT configuration
     */
    public final MltConfig getMltConf() {
        return mltConf;
    }

    public final void setMltConfig(MltConfig conf) {
        this.mltConf = conf == null ? MltConfig.getDefault() : conf;
        if(!this.mltConf.isInterstingTerms()){
            log.debug("set interstingTerms=true on parsed {}", conf);
            this.mltConf.setInterstingTerms(true);
        }
    }

    /**
     * Allows to set a custom {@link ContentSectionProvider} used to determine what
     * sections of the {@link AnalyzedText} to link with the {@link SolrClient}
     * @param contentProvider the contentProvider or <code>null</code> to rest to
     * the {@link ContentSectionProvider#DEFAULT}
     */
    public final void setContentProvider(ContentSectionProvider contentProvider) {
        this.contentProvider = contentProvider == null ? ContentSectionProvider.DEFAULT : contentProvider;
    }
    /**
     * Getter for the {@link ContentSectionProvider} used for linking parsed {@link AnalyzedText}s
     * @return
     */
    public final ContentSectionProvider getContentProvider() {
        return contentProvider;
    }

    @Override
    protected void init() throws Exception {
        //lets ping the solrServer on initialization
        log.debug("ping {}", client);
        SolrPingResponse ping = client.ping();
        log.debug("ping respone: {}", ping);
    }

    @Override
    protected final void doProcessing(ProcessingData processingData) {
        if(client == null){
            return;
        }
        final AnalyzedText at = NlpUtils.getOrInitAnalyzedText(processingData);
        if(at == null){ //no plain text  ... nothing to do
            return;
        }
        //TODO: Think about language support
        String language = processingData.getLanguage();
        Locale locale = language == null ? Locale.ROOT : Locale.forLanguageTag(language);
        SolrQuery mltQuery = mltConf.createMltQuery();
        MltRequest mltRequest = new MltRequest(mltQuery, at.getSpan());
        NamedList<Object> response;
        try {
            response = getClient().request(mltRequest);
        } catch (SolrServerException | IOException e) {
            log.warn("Unable to search for interesting terms for {} in {} ({}: {})", processingData, getClient(),
                    e.getClass().getSimpleName(), e.getMessage());
            log.debug("Stacktrace:", e);
            return;
        }
        
        NamedList<Object> interestingTermList = (NamedList<Object>)response.get("interestingTerms");
        if(interestingTermList.size() < 1) { //no interesting terms
            return;
        }
        Map<String,List<Token>> termMap = new HashMap<>();
        for(Iterator<Token> tokens = at.getTokens(); tokens.hasNext(); ){
            Token token = tokens.next();
            if(NlpUtils.isNoun(token) || NlpUtils.isAdjective(token)){
                //register for the span, stem and lemma
                new HashSet<>(Arrays.asList(token.getSpan(), NlpUtils.getStem(token), NlpUtils.getLemma(token)))
                    .forEach(key -> addTerm(termMap,key,locale, token));
            } //else ignore words with other POS tags
            
        }
        List<Entry<String,Float>> interestingTerms = new LinkedList<>();
        float maxBoost = 0; //search for the highest boost for normalization [0..1]
        log.debug("Solr MLT interesting Terms:");
        for(Iterator<Entry<String,Object>> terms = interestingTermList.iterator(); terms.hasNext();){
            Entry<String,Object> e = terms.next();
            String term = e.getKey();
            float boost = ((Number)e.getValue()).floatValue();
            if(boost > maxBoost){
                maxBoost = boost;
            }
            log.debug("  {}:{}", boost,term);
            interestingTerms.add(new ImmutablePair<String,Float>(term, boost));
        }
        for(Entry<String,Float> term : interestingTerms){
            String termKey = term.getKey();
            int fieldSepIdx = termKey.indexOf(':');
            String termName = fieldSepIdx > 0 ? termKey.substring(fieldSepIdx+1, termKey.length()) : termKey;
            List<Token> termTokens = termMap.get(termName);
            if(termTokens != null){
                for(Token token : termTokens){
                    Value<String> value = Value.value(termName, term.getValue()/maxBoost);
                    token.addValue(INTERESTING_TERM, value);
                    log.debug("mark {} as interesting Term {}", token, value);
                }
            }
        }
        

    }
    
    private void addTerm(Map<String, List<Token>> termMap, String stem, Locale locale, Token token) {
        if(stem == null){
            return;
        }
        List<Token> termTokens = termMap.get(stem);
        if(termTokens == null){
            termTokens = new LinkedList<>();
            termMap.put(stem, termTokens);
        }
        termTokens.add(token);
        //also add a lower case variant
        String lcStem = stem.toLowerCase(locale);
        if(!lcStem.equals(stem)){
            termTokens = termMap.get(lcStem);
            if(termTokens == null){
                termTokens = new LinkedList<>();
                termMap.put(lcStem, termTokens);
            }
            termTokens.add(token);
        }
    }
    
    protected Set<String> getLanguages(final AnalyzedText at) {
        List<Value<String>> langAnnos = at.getValues(NlpAnnotations.LANGUAGE_ANNOTATION);
        Set<String> languages = new HashSet<>();
        for(Value<String> langAnno : langAnnos){
            if(langAnno.probability() == Value.UNKNOWN_PROBABILITY || langAnno.probability() >= 0.33f){
                String lang = langAnno.value();
                if(lang.indexOf('-') > 0){
                    String baseLang = lang.substring(0, lang.indexOf('-'));
                    languages.add(baseLang);
                }
                languages.add(lang);
            }
        }
        return languages;
    }

    
    @PreDestroy
    @Override
    public void close() throws IOException {
        SolrClient client = this.client;
        this.client = null;
        client.close();
        
    }
    
}
