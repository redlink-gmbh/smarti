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

package io.redlink.smarti.processor.pos;

import io.redlink.nlp.api.ProcessingData.Configuration;
import io.redlink.nlp.api.ProcessingException;
import io.redlink.nlp.api.Processor;
import io.redlink.nlp.model.pos.PosSet;
import io.redlink.nlp.negation.NegationProcessor;
import io.redlink.nlp.negation.de.GermanNegationRule;
import io.redlink.nlp.opennlp.de.LanguageGerman;
import io.redlink.nlp.opennlp.pos.OpenNlpPosProcessor;
import io.redlink.smarti.model.*;
import io.redlink.smarti.model.Message.Origin;
import io.redlink.smarti.model.Token.Hint;
import io.redlink.smarti.processing.AnalysisData;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * This test two {@link QueryPreparator}s<ol>
 * <li> {@link PosCollector}: that creates tokens for {@link PosSet#ADJECTIVES}
 * <li> {@link NegatedTokenMarker}: that adds the {@link Hint#negated} to all Tokens
 * that are within a section marked as {@link AnalysisData#NEGATION_ANNOTATION}
 * </ol>
 */
public class PosCollectorAndNegationHandlerTest {
    
    private static final Logger log = LoggerFactory.getLogger(PosCollectorAndNegationHandlerTest.class);

    private static List<Triple<MsgData[],String[], Hint[]>> CONTENTS = new ArrayList<>();

    private static List<Processor> REQUIRED_PREPERATORS;

    private PosCollector posCollector;
    private NegatedTokenMarker negatedTokenMarker;
    
    
    @BeforeClass
    public static void initClass() throws IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        CONTENTS.add(new ImmutableTriple<MsgData[], String[], Hint[]>(new MsgData[]{
                new MsgData(Origin.User, "Ich suche ein ruhiges und romantisches Restaurant in Berlin für heute Abend.")},
                new String[]{"ruhiges", "romantisches"},
                new Hint[]{}));
        CONTENTS.add(new ImmutableTriple<MsgData[], String[], Hint[]>(new MsgData[]{
                new MsgData(Origin.User, "Ich suche ein nettes Restaurant in Berlin für den kommenden Montag.")},
                new String[]{"nettes"}, //NOTE: not kommenden, because this is in the ignored list
                new Hint[]{}));
        CONTENTS.add(new ImmutableTriple<MsgData[], String[], Hint[]>(new MsgData[]{
                new MsgData(Origin.User, "Bitte kein chinesisches oder indisches Restaurant.")},
                new String[]{"chinesisches","indisches"},
                new Hint[]{Hint.negated})); //those attributes are negated
        
        OpenNlpPosProcessor pos = new OpenNlpPosProcessor(Collections.singleton(new LanguageGerman()));
        NegationProcessor negation = new NegationProcessor(Arrays.asList(new GermanNegationRule()));
        
        REQUIRED_PREPERATORS = Arrays.asList(pos, negation);
        
    }
    
    private static final Conversation initConversation(int index) {
        Conversation c = new Conversation(new ObjectId(), new ObjectId());
        c.setLastModified(new Date());
        c.setMeta(new ConversationMeta());
        c.getMeta().setStatus(ConversationMeta.Status.New);
        log.trace("Conversation: ");
        for(MsgData md : CONTENTS.get(index).getLeft()){
            log.trace("    {}", md);
            c.getMessages().add(md.toMessage());
        }
        c.setUser(new User());
        c.getUser().setDisplayName("Test Dummy");
        c.getUser().setPhoneNumber("+43210123456");
        return c;
    }

    @Before
    public void init(){
        posCollector = new PosCollector();
        negatedTokenMarker = new NegatedTokenMarker();
    }
    
    private static final void preprocessConversation(AnalysisData pd) throws ProcessingException{
        for(Processor processor : REQUIRED_PREPERATORS){
            processor.process(pd);
        }
    }

    
    @Test
    public void testAll() throws ProcessingException{
        for(int idx = 0 ; idx < CONTENTS.size(); idx++){
            AnalysisData processingData = processConversation(AnalysisData.create(initConversation(idx), new Client(), null));
            assertPosProcessingResults(processingData, CONTENTS.get(idx).getMiddle(),CONTENTS.get(idx).getRight());
        }
    }

    AnalysisData processConversation(AnalysisData processingData) throws ProcessingException {
        //NOTE: we statically set the lanugage to "de" here as we do not have a language detection processor in this test
        processingData.getConfiguration().put(Configuration.LANGUAGE, "de");
        log.trace(" - preprocess conversation {}", processingData.getConversation());
        preprocessConversation(processingData);
        log.trace(" - start processing");
        long start = System.currentTimeMillis();
        posCollector.process(processingData);
        negatedTokenMarker.process(processingData);
        log.trace(" - processing time: {}",System.currentTimeMillis()-start);
        return processingData;
    }

    
    private void assertPosProcessingResults(AnalysisData processingData, String[] expected, Hint[] hints) {
        Set<String> expectedSet = new HashSet<>(Arrays.asList(expected)); 
        Conversation conv = processingData.getConversation();
        Analysis analysis = processingData.getAnalysis();
        Assert.assertNotNull(analysis);
        Assert.assertFalse(analysis.getTokens().isEmpty());
        for(Token token : analysis.getTokens()){
            log.debug("Token(idx: {}, span[{},{}], type: {}): {}", token.getMessageIdx(), token.getStart(), token.getEnd(), token.getType(), token.getValue());
            Assert.assertNotNull(token.getType());
            Assert.assertTrue(conv.getMessages().size() > token.getMessageIdx());
            Message message = conv.getMessages().get(token.getMessageIdx());
            Assert.assertTrue(message.getOrigin() == Origin.User);
            Assert.assertEquals(Token.Type.Attribute, token.getType());
            Assert.assertTrue(token.getStart() >= 0);
            Assert.assertTrue(token.getEnd() > token.getStart());
            Assert.assertTrue(token.getEnd() <= message.getContent().length());
            Assert.assertEquals(message.getContent().substring(token.getStart(), token.getEnd()), String.valueOf(token.getValue()));
            Assert.assertTrue(token.getConfidence() > 0f);
            Assert.assertTrue(token.getConfidence() <= 1f);
            Assert.assertTrue(expectedSet.remove(token.getValue()));
            for(Hint hint : hints){
                Assert.assertTrue("missing expected Hint "+ hint, token.hasHint(hint));
            }
        }
        Assert.assertTrue(expectedSet.isEmpty());
    }
    
    
    private static class  MsgData {
        
        final Origin origin;
        final String msg;
        final Date date;

        MsgData(Message.Origin origin, String msg){
            this(origin,msg,new Date());
        }
        MsgData(Message.Origin origin, String msg, Date date){
            this.origin = origin;
            this.msg = msg;
            this.date = date;
        }
        
        Message toMessage(){
            Message message = new Message();
            message.setContent(msg);
            message.setOrigin(origin);
            message.setTime(date);
            return message;
        }
        
        @Override
        public String toString() {
            return origin + ": "+ msg;
        }
        
    }
    
}
