/*
 * Copyright (c) 2016 Redlink GmbH
 */

package io.redlink.smarti.processor.hasso;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.redlink.nlp.api.ProcessingException;
import io.redlink.nlp.model.NlpAnnotations;
import io.redlink.nlp.model.Section;
import io.redlink.nlp.model.util.NlpUtils;
import io.redlink.nlp.regex.ner.RegexNerProcessor;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.processing.ProcessingData;
import io.redlink.smarti.processor.hasso.QuestionIdentifierVocab;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import static io.redlink.nlp.api.ProcessingData.Configuration.LANGUAGE;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RegexNerProcessor.class, QuestionIdentifierVocab.class})
public class QuestionIdentifierVocabTest {

    @Autowired
    private RegexNerProcessor extractor;

    @Test
    public void testExtraction() throws ProcessingException {
        Conversation c = new Conversation();
        final Message m = new Message();
        m.setContent("Wo finde ich den tCode für GIS?");
        c.getMessages().add(m);
        ProcessingData data = ProcessingData.create(c);
        data.getConfiguration().put(LANGUAGE,"de"); //this test does not have a language detector
        extractor.process(data);
        
        List<Section> sections = data.getMessageSections();
        Assert.assertEquals(1, sections.size());

        Set<String> expected = new HashSet<>(Arrays.asList("Einstiegshilfe"));
        
        Section section = sections.get(0);
        StreamSupport.stream(Spliterators.spliteratorUnknownSize(section.getChunks(),Spliterator.ORDERED),false)
            .filter(cunk -> cunk.getAnnotation(NlpAnnotations.NER_ANNOTATION) != null)
            .forEach(cunk -> {
                String name = NlpUtils.getNormalized(cunk);
                Assert.assertTrue(expected.remove(name));
            });
        Assert.assertTrue(expected.isEmpty());
    }
}