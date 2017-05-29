/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.reisebuddy.extractor.token.regex;

import io.redlink.reisebuddy.extractor.token.TokenProcessingRuleset;
import io.redlink.reisebuddy.model.Message;
import io.redlink.reisebuddy.model.MessageTopic;
import io.redlink.reisebuddy.model.Token;
import io.redlink.reisebuddy.model.Token.Hint;
import io.redlink.reisebuddy.model.Token.Type;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link TokenProcessingRuleset} implementation that works with Regex pattern.
 * <p>
 * Tokens in the original texts are replaced with 
 * <code><{Token.Type.name()}></code>. So Regex patterns of rules can
 * match against the type instead of the actual value.
 * <p>
 * Intended usage:<pre>
 *     public MyGermanTokenMatcher(){
 *         super("de", EnumSet.of(MessageTopic.Reiseplanung), EnumSet.of(Type.Date, Type.Place));
 *         addRule(String.format("vo(?:n|m) (<%s>)", Type.Place), Hint.from);
 *         addRule(String.format("nach (<%s>)", Type.Place), Hint.to);
 *     }
 * </pre>
 * 
 * @author Rupert Westenthaler
 *
 */
public abstract class RegexTokenProcessingRuleset implements TokenProcessingRuleset {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private List<Pair<Pattern,List<Hint>>> rules = new LinkedList<>();
    
    private final String lang;
    private final Set<MessageTopic> topics;
    private final Set<Token.Type> tokenTypes;

    /**
     * Creates a regex {@link TokenProcessingRuleset} for the parsed language, called
     * for messages with one of the parsed message topics and called with a
     * list of tokens having one of the parsed {@link Type Token.Type}s
     * @param lang the language
     * @param topics
     * @param tokenTypes
     */
    protected RegexTokenProcessingRuleset(String lang, Set<MessageTopic> topics,Set<Token.Type> tokenTypes) {
        assert StringUtils.isNotBlank(lang);
        this.lang = lang.toLowerCase(Locale.ROOT);
        assert topics != null && !topics.isEmpty();
        this.topics = topics;
        assert tokenTypes != null && !tokenTypes.isEmpty();
        this.tokenTypes = tokenTypes;
    }
    /**
     * Adds a rule the array of {@link Hint}s needs to
     * correspond to capturing groups of the regex. 
     * Use <code>null</code> if a capturing group does not
     * correspond to a token
     * @param regex the regex
     * @param hints the {@link Hint}s to be added to Tokens
     * selected by capturing groups
     */
    protected void addRule(String regex, Hint...hints) {
        rules.add(new ImmutablePair<Pattern, List<Hint>>(Pattern.compile(regex),Arrays.asList(hints)));
    }

    @Override
    public String getLanguage() {
        return lang;
    }

    @Override
    public Set<MessageTopic> topics() {
        return topics;
    }

    @Override
    public Set<Type> tokenTypes() {
        return tokenTypes;
    }

    @Override
    public void apply(Message message, List<Token> tokens) {
        if(tokens == null || tokens.isEmpty()){
            return; //nothing to do
        }
        //we create a pseudo message where tokens are replaced with
        // '<{type}>' so that the patterns do not need to match
        // against the actual value but instead on the type
        String orig = message.getContent();
        tokens = new ArrayList<>(tokens); //we need a copy so we can sort
        Collections.sort(tokens, Token.IDX_START_END_COMPARATOR);
        StringBuilder sb = new StringBuilder(message.getContent().length());
        NavigableMap<Integer,List<Token>> tokenOffsets = new TreeMap<>();
        int cIdx = 0;
        for(Token token : tokens){
            if(token.getStart() < cIdx){ //multiple overlapping tokens
                //add to the current entry in the tokenOffset map
                List<Token> existing = tokenOffsets.lastEntry().getValue();
                existing.add(token);
                if(existing.get(0).getStart() != token.getStart() ||
                        existing.get(0).getEnd() != token.getEnd() ||
                        existing.get(0).getType() != token.getType()){
                    
                    log.warn("Unsupported overlapping Tokens 1: {} | 2: {}", existing.get(0), token);
                }// else overlapping tokens do have the same start/end and type
                if(cIdx < token.getEnd()){ //partly overlapping :(
                    //we do not support this, but at least update the end to the longest token
                    cIdx = token.getEnd();
                }
            } else {
                sb.append(orig.substring(cIdx, token.getStart()));
                tokenOffsets.put(Integer.valueOf(sb.length()),new ArrayList<Token>(Arrays.asList(token)));
                sb.append('<').append(token.getType()).append('>');
                cIdx = token.getEnd();
            }
        }
        sb.append(orig.substring(cIdx));
    
        //now start to match the configured patterns
        int matchedTokens = 0;
        Iterator<Pair<Pattern,List<Hint>>> ruleIt = rules.iterator();
        while(ruleIt.hasNext() && matchedTokens < tokens.size()){
            Pair<Pattern,List<Hint>> rule = ruleIt.next();
            Matcher matcher = rule.getLeft().matcher(sb);
            while(matcher.find()){
                for(int g = 0; g < matcher.groupCount(); g++){
                    Integer start = Integer.valueOf(matcher.start(g+1));
                    List<Token> offsetTokens = tokenOffsets.get(start);
                    if(offsetTokens != null){
                        final Hint hint = rule.getValue().get(g);
                        if(hint != null){
                            offsetTokens.forEach(t -> {
                                t.addHint(hint);
                                if(log.isDebugEnabled()){
                                    log.debug(" - add Hint {} to token[{}|{},{}|type:{}]'{}'",
                                            hint,t.getMessageIdx(),t.getStart(),t.getEnd(),t.getType(),
                                            orig.substring(t.getStart(),t.getEnd()));
                                }
                            });
                            offsetTokens.clear(); //consume the tokens
                        }
                    } else {
                        log.warn("no token with startOffset {} (rule: {} group: {})",
                                matcher.start(g+1), rule, g+1);
                    }
                    
                }
            }
        }
    }

}