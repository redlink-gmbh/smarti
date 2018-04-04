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

package io.redlink.smarti.processor.ner;

import io.redlink.nlp.api.ProcessingData.Configuration;
import io.redlink.nlp.api.ProcessingException;
import io.redlink.nlp.api.Processor;
import io.redlink.nlp.regex.ner.BahnhofDetector;
import io.redlink.nlp.regex.ner.RegexNerProcessor;
import io.redlink.smarti.model.*;
import io.redlink.smarti.model.Message.Origin;
import io.redlink.smarti.model.Token.Hint;
import io.redlink.smarti.processing.AnalysisData;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.Precision;
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
import java.util.concurrent.*;

/**
 * Test for the {@link LocationTypeAppender}
 * 
 * @author Rupert Westenthaler
 *
 */
public class LocationTypeAppenderTest {
    
    private static final Logger log = LoggerFactory.getLogger(LocationTypeAppenderTest.class);

    private static List<Pair<MsgData[], List<Pair<String, Hint[]>>>> CONTENTS = new ArrayList<>();

    private static List<Processor> REQUIRED_PRE_PREPERATORS;
    private LocationTypeAppender locTypeAppender;
    private static List<Processor> REQUIRED_POST_PREPERATORS;
    
    
    @BeforeClass
    public static void initClass() throws IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        CONTENTS.add(new ImmutablePair<LocationTypeAppenderTest.MsgData[], List<Pair<String,Hint[]>>>(new MsgData[]{
                new MsgData(Origin.User, "Ist der ICE 1526 von München Hbf ist heute verspätet?")},
                Arrays.asList(
                    //München Hauptbahnhof is detected as both location and organization
                    new ImmutablePair<String, Hint[]>("München Hauptbahnhof", new Hint[]{}),
                    new ImmutablePair<String, Hint[]>("München Hauptbahnhof", new Hint[]{}))));
        CONTENTS.add(new ImmutablePair<LocationTypeAppenderTest.MsgData[], List<Pair<String,Hint[]>>>(new MsgData[]{
                new MsgData(Origin.User, "Ist der ICE 1526 von München Hauptbahnhof ist heute verspätet?")},
                Arrays.asList(
                    new ImmutablePair<String, Hint[]>("München Hauptbahnhof", new Hint[]{}))));
        CONTENTS.add(new ImmutablePair<LocationTypeAppenderTest.MsgData[], List<Pair<String,Hint[]>>>(new MsgData[]{
                new MsgData(Origin.User, "Brauche einen Zug von Darmstadt Hbf nach Ilmenau Ostbf. Muss um 16:00 in Ilmenau sein.")},
                Arrays.asList(
                        new ImmutablePair<String, Hint[]>("Darmstadt Hauptbahnhof", new Hint[]{}),
                        new ImmutablePair<String, Hint[]>("Ilmenau Ostbahnhof", new Hint[]{}),
                        new ImmutablePair<String, Hint[]>("Ilmenau", new Hint[]{}))));
        CONTENTS.add(new ImmutablePair<LocationTypeAppenderTest.MsgData[], List<Pair<String,Hint[]>>>(new MsgData[]{
                new MsgData(Origin.User, "Hi, welche italienischen Restaurants könnt ihr denn in Berlin Hbf/Prenzlberg empfehlen? Danke, Hannes")},
                Arrays.asList(
                    new ImmutablePair<String, Hint[]>("Berlin Hauptbahnhof", new Hint[]{}),
                    //new ImmutablePair<String, Hint[]>("Prenzlberg", new Hint[]{}), not detected by OpenNLP NER
                    new ImmutablePair<String, Hint[]>("Hannes", new Hint[]{}))));
        
        RegexNerProcessor bhDetect = new RegexNerProcessor(Collections.singletonList(new BahnhofDetector()));
        GermanTestSetup germanNlp = GermanTestSetup.getInstance();
        REQUIRED_PRE_PREPERATORS = Arrays.asList(germanNlp.getPosProcessor(), germanNlp.getNerProcessor(), bhDetect);
        
        REQUIRED_POST_PREPERATORS = Arrays.asList(new NamedEntityCollector());
        
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
        locTypeAppender = new LocationTypeAppender();
    }
    
    
    @Test
    public void testSingle() throws ProcessingException{
        int idx = Math.round((float)Math.random()*(CONTENTS.size()-1));
        Conversation conversation = initConversation(idx);
        AnalysisData processingData = AnalysisData.create(conversation, new Client(), null);
        processingData.getConfiguration().put(Configuration.LANGUAGE, "de");
        processConversation(processingData);
        assertNerProcessingResults(processingData, CONTENTS.get(idx).getRight());
    }

    void processConversation(AnalysisData processingData) throws ProcessingException {
        log.trace(" - preprocess conversation {}", processingData.getConversation());
        for(Processor processor : REQUIRED_PRE_PREPERATORS){
            processor.process(processingData);
        }
        log.trace(" - start processing");
        long start = System.currentTimeMillis();
        locTypeAppender.process(processingData);
        log.trace(" - processing time: {}",System.currentTimeMillis()-start);
        for(Processor qp : REQUIRED_POST_PREPERATORS){
            qp.process(processingData);
        }
        
    }

    
    @Test
    public void testMultiple() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        int numDoc = 100;
        int numWarmup = Math.max(CONTENTS.size(), 20);
        log.info("> warnup ({} calls + assertion of results)", numWarmup);
        List<Future<ConversationProcessor>> tasks = new LinkedList<>();
        for(int i = 0; i < numWarmup; i++){
            int idx = i%CONTENTS.size();
            tasks.add(executor.submit(new ConversationProcessor(idx,"de")));
        }
        while(!tasks.isEmpty()){ //wait for all the tasks to complete
            //during warmup we assert the NLP results
            ConversationProcessor cp = tasks.remove(0).get();
            assertNerProcessingResults(cp.getProcessingData(),CONTENTS.get(cp.getIdx()).getRight()); 
        }
        log.info("   ... done");
        log.info("> processing {} documents ...", numDoc);
        long min = Integer.MAX_VALUE;
        long max = Integer.MIN_VALUE;
        long sum = 0;
        for(int i = 0; i < numDoc; i++){
            int idx = i%CONTENTS.size();
            tasks.add(executor.submit(new ConversationProcessor(idx,"de")));
        }
        int i = 0;
        while(!tasks.isEmpty()){ //wait for all the tasks to complete
            ConversationProcessor completed = tasks.remove(0).get();
            i++;
            if(i%10 == 0){
                log.info(" ... {} documents processed",i);
            }
            int dur = completed.getDuration();
            if(dur > max){
                max = dur;
            }
            if(dur < min){
                min = dur;
            }
            sum = sum + dur;
        }
        log.info("Processing Times after {} documents",numDoc);
        log.info(" - average: {}ms",Precision.round(sum/(double)numDoc, 2));
        log.info(" - max: {}ms",max);
        log.info(" - min: {}ms",min);
        executor.shutdown();
    }
    
    private void assertNerProcessingResults(AnalysisData processingData, List<Pair<String,Hint[]>> expected) {
        expected = new LinkedList<>(expected); //copy so we can remove
        Conversation conv = processingData.getConversation();
        Analysis analysis = processingData.getAnalysis();
        Assert.assertFalse(analysis.getTokens().isEmpty());
        for(Token token : analysis.getTokens()){
            log.debug("Token(idx: {}, span[{},{}], type: {}): {}", token.getMessageIdx(), token.getStart(), token.getEnd(), token.getType(), token.getValue());
            Assert.assertNotNull(token.getType());
            Assert.assertTrue(conv.getMessages().size() > token.getMessageIdx());
            Message message = conv.getMessages().get(token.getMessageIdx());
            Assert.assertTrue(message.getOrigin() == Origin.User);
            Assert.assertTrue(token.getStart() >= 0);
            Assert.assertTrue(token.getEnd() > token.getStart());
            Assert.assertTrue(token.getEnd() <= message.getContent().length());
            //the next assert is no longer true as the token.getValue() is the Lemma if present
            //Assert.assertEquals(message.getContent().substring(token.getStart(), token.getEnd()), String.valueOf(token.getValue()));
            Pair<String,Hint[]> p = expected.remove(0);
            Assert.assertEquals("Wrong Named Entity", p.getKey(), token.getValue());
            for(Hint hint : p.getValue()){
                Assert.assertTrue("Missing expected hint " + hint,token.hasHint(hint));
            }
        }
    }
    
    
    private class ConversationProcessor implements Callable<ConversationProcessor> {

        private final int idx;
        private final AnalysisData processingData;
        private int duration;

        ConversationProcessor(int idx, String lang){
            this.idx = idx;
            this.processingData = AnalysisData.create(initConversation(idx), new Client(), null);
            this.processingData.getConfiguration().put(Configuration.LANGUAGE, lang);
        }
        

        @Override
        public ConversationProcessor call() throws Exception {
            long start = System.currentTimeMillis();
            processConversation(processingData);
            duration = (int)(System.currentTimeMillis() - start);
            return this;
        }

        public int getIdx() {
            return idx;
        }
        
        public AnalysisData getProcessingData() {
            return processingData;
        }
        
        public int getDuration() {
            return duration;
        }
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
