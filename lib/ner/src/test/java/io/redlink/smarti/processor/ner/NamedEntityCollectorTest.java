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
import io.redlink.nlp.model.AnalyzedText;
import io.redlink.nlp.negation.NegationProcessor;
import io.redlink.nlp.negation.de.GermanNegationRule;
import io.redlink.smarti.model.*;
import io.redlink.smarti.model.Message.Origin;
import io.redlink.smarti.model.Token.Hint;
import io.redlink.smarti.processing.AnalysisData;
import io.redlink.smarti.processor.pos.NegatedTokenMarker;
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
 * Tests collecting {@link Token}s for {@link Annotations#NER_ANNOTATION}s
 * present in the {@link AnalyzedText}. In addition this also tests that the
 * {@link NegationHandler} also marks Named Entity Tokens as {@link Hint#negated}
 * if they are in a text section that is marked as {@link AnalysisData#NEGATION_ANNOTATION}.
 * 
 * @author Rupert Westenthaler
 *
 */
public class NamedEntityCollectorTest {
    
    private static final Logger log = LoggerFactory.getLogger(NamedEntityCollectorTest.class);

    private static List<Pair<MsgData[], List<Pair<String, Hint[]>>>> CONTENTS = new ArrayList<>();

    private static List<Processor> REQUIRED_PREPERATORS;

    private NamedEntityCollector nerCollector;
    private NegatedTokenMarker negationHandler;
    
    
    @BeforeClass
    public static void initClass() throws IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        CONTENTS.add(new ImmutablePair<NamedEntityCollectorTest.MsgData[], List<Pair<String,Hint[]>>>(new MsgData[]{
                new MsgData(Origin.User, "Ist der ICE 1526 von München heute verspätet?")},
                Arrays.asList(
                    new ImmutablePair<String, Hint[]>("München", new Hint[]{}))));
        CONTENTS.add(new ImmutablePair<NamedEntityCollectorTest.MsgData[], List<Pair<String,Hint[]>>>(new MsgData[]{
                new MsgData(Origin.User, "Brauche einen Zug von Darmstadt nach Ilmenau. Muss um 16:00 in Ilmenau sein.")},
                Arrays.asList(
                        new ImmutablePair<String, Hint[]>("Darmstadt", new Hint[]{}),
                        new ImmutablePair<String, Hint[]>("Ilmenau", new Hint[]{}),
                        new ImmutablePair<String, Hint[]>("Ilmenau", new Hint[]{}))));
        CONTENTS.add(new ImmutablePair<NamedEntityCollectorTest.MsgData[], List<Pair<String,Hint[]>>>(new MsgData[]{
                new MsgData(Origin.User, "Brauche einen Zug nach Ilmenau. Muss vor 16:00 ankommen!"),
                new MsgData(Origin.Agent, "Von wo willst Du wegfahren?"),
                new MsgData(Origin.User, "Darmstadt.")},
                Arrays.asList(
                        new ImmutablePair<String, Hint[]>("Ilmenau", new Hint[]{}),
                        new ImmutablePair<String, Hint[]>("Darmstadt", new Hint[]{}))));
        CONTENTS.add(new ImmutablePair<NamedEntityCollectorTest.MsgData[], List<Pair<String,Hint[]>>>(new MsgData[]{
                new MsgData(Origin.User, "Brauche für morgen Nachmittag einen Zug von Hamburg nach Berlin"),
                new MsgData(Origin.Agent, "Ich würde Dir den ICE 1234 um 14:35 empfehlen. Brauchst Du ein Hotel in Berlin?"),
                new MsgData(Origin.User, "Ja. Bitte eines in der nähe des Messezentrum. Wenn möglich um weniger als 150 Euro.")},
                Arrays.asList(
                        new ImmutablePair<String, Hint[]>("Hamburg", new Hint[]{}),
                        new ImmutablePair<String, Hint[]>("Berlin", new Hint[]{}),
                        //new ImmutablePair<String, Hint[]>("Messezentrum", new Hint[]{}), not detected by OpenNLP Ner
                        new ImmutablePair<String, Hint[]>("Euro", new Hint[]{}))));
        CONTENTS.add(new ImmutablePair<NamedEntityCollectorTest.MsgData[], List<Pair<String,Hint[]>>>(new MsgData[]{
                new MsgData(Origin.User, "Warte schon 10 Minuten auf the ICE 1234. Keine Information am Bahnhof!"),
                new MsgData(Origin.Agent, "der ICE 1234 ist im Moment 25 Minuten verspätet. Tut mir leid, dass am Bahnhof keine Informationen ausgerufen werden."),
                new MsgData(Origin.User, "Kannst Du mir auch noch sagen ob ich den ICE 2345 in Hamburg erreiche.")},
                Arrays.asList(
                        new ImmutablePair<String, Hint[]>("ICE", new Hint[]{}),
                        new ImmutablePair<String, Hint[]>("Hamburg", new Hint[]{})//,
                        //new ImmutablePair<String, Hint[]>("ICE", new Hint[]{}), not detected on the 2nd mention (in message 3) by OpenNLP NER
                        )));
        CONTENTS.add(new ImmutablePair<NamedEntityCollectorTest.MsgData[], List<Pair<String,Hint[]>>>(new MsgData[]{
                new MsgData(Origin.User, "Brauche einen Zug von München nach Hamburg, aber bitte nicht über Nürnberg.")},
                Arrays.asList(
                        new ImmutablePair<String, Hint[]>("München", new Hint[]{}),
                        new ImmutablePair<String, Hint[]>("Hamburg", new Hint[]{}),
                        new ImmutablePair<String, Hint[]>("Nürnberg", new Hint[]{Hint.negated}))));
        
        GermanTestSetup germanNlp = GermanTestSetup.getInstance();
        NegationProcessor negation = new NegationProcessor(Collections.singleton(new GermanNegationRule()));
        REQUIRED_PREPERATORS = Arrays.asList(germanNlp.getPosProcessor(), germanNlp.getNerProcessor(), negation);
        
    }
    
    private static final Conversation initConversation(int index) {
        Conversation c = new Conversation(new ObjectId(), new ObjectId());
        c.setLastModified(new Date());
        c.setMeta(new ConversationMeta());
        c.getMeta().setStatus(ConversationMeta.Status.New);
        List<Message> messages = new ArrayList<>();
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
        nerCollector = new NamedEntityCollector();
        negationHandler = new NegatedTokenMarker();
    }
    
    private static final void preprocessConversation(AnalysisData pd) throws ProcessingException{
        for(Processor processor : REQUIRED_PREPERATORS){
            processor.process(pd);
        }
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
        preprocessConversation(processingData);
        log.trace(" - start processing");
        long start = System.currentTimeMillis();
        nerCollector.process(processingData);
        negationHandler.process(processingData);
        log.trace(" - processing time: {}",System.currentTimeMillis()-start);
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
            tasks.add(executor.submit(new ConversationProcessor(idx, "de")));
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
            tasks.add(executor.submit(new ConversationProcessor(idx, "de")));
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
            Assert.assertEquals(message.getContent().substring(token.getStart(), token.getEnd()), String.valueOf(token.getValue()));
            Pair<String,Hint[]> p = expected.remove(0);
            Assert.assertEquals("Wrong Named Entity",p.getKey(), token.getValue());
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
            this.processingData.getConfiguration().put(Configuration.LANGUAGE, "de");
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
