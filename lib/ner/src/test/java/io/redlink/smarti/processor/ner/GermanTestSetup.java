package io.redlink.smarti.processor.ner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.redlink.nlp.api.Processor;
import io.redlink.nlp.opennlp.OpenNlpNerModel;
import io.redlink.nlp.opennlp.OpenNlpNerProcessor;
import io.redlink.nlp.opennlp.de.LanguageGerman;
import io.redlink.nlp.opennlp.de.NerGerman;
import io.redlink.nlp.opennlp.pos.OpenNlpPosProcessor;

/**
 * We only want to load the memory intensive NLP models once!
 * @author Rupert Westenthaler
 *
 */
public class GermanTestSetup {

    private static GermanTestSetup INSTANCE;
    
    private LanguageGerman nlpModel;
    private OpenNlpNerModel nerModel;
    
    private OpenNlpNerProcessor nerProcessor;
    private OpenNlpPosProcessor posProcessor;
    
    private GermanTestSetup(){
        nlpModel = new LanguageGerman();
        nerModel = new NerGerman();
        
        posProcessor = new OpenNlpPosProcessor(Collections.singleton(nlpModel));
        nerProcessor = new OpenNlpNerProcessor(Collections.singletonList(nerModel));
    }
    
    public OpenNlpNerProcessor getNerProcessor() {
        return nerProcessor;
    }

    public OpenNlpPosProcessor getPosProcessor() {
        return posProcessor;
    }
    
    public static GermanTestSetup getInstance(){
        if(INSTANCE == null){
            synchronized (GermanTestSetup.class) {
                if(INSTANCE == null){
                    INSTANCE = new GermanTestSetup();
                }
            }
        }
        return INSTANCE;
    }
    
}
