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

package io.redlink.smarti.processor.hasso;

import io.redlink.nlp.api.ProcessingException;
import io.redlink.nlp.model.NlpAnnotations;
import io.redlink.nlp.model.Section;
import io.redlink.nlp.model.util.NlpUtils;
import io.redlink.nlp.regex.ner.RegexNerProcessor;
import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.processing.AnalysisData;

import org.bson.types.ObjectId;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.stream.StreamSupport;

import static io.redlink.nlp.api.ProcessingData.Configuration.LANGUAGE;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RegexNerProcessor.class, SapKeywordsVocab.class},
        initializers = ConfigFileApplicationContextInitializer.class)
public class SapKeywordsVocabTest {

    @Autowired
    private RegexNerProcessor extractor;

    @Test
    public void testExtraction() throws ProcessingException {
        Conversation c = new Conversation(new ObjectId(), new ObjectId());
        c.setLastModified(new Date());
        final Message m = new Message();
        m.setContent("Was ist der tCode für GIS?");
        c.getMessages().add(m);
        AnalysisData data = AnalysisData.create(c, new Client());
        data.getConfiguration().put(LANGUAGE,"de"); //this test does not have a language detector
        extractor.process(data);
        
        List<Section> sections = data.getMessageSections();
        Assert.assertEquals(1, sections.size());
        
        Section section = sections.get(0);
        Collection<String> expected = new HashSet<>(Arrays.asList("Transaktion", "GeoSAP"));
        StreamSupport.stream(Spliterators.spliteratorUnknownSize(section.getChunks(),Spliterator.ORDERED),false)
            .filter(chunk -> chunk.getAnnotation(NlpAnnotations.NER_ANNOTATION) != null)
            .forEach(chunk -> {
                String name = NlpUtils.getNormalized(chunk);
                Assert.assertTrue(expected.remove(name));
            });
        Assert.assertThat(expected, Matchers.emptyIterable());
        
    }
}