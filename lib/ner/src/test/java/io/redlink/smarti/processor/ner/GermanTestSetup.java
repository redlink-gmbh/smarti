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

import io.redlink.nlp.opennlp.OpenNlpNerModel;
import io.redlink.nlp.opennlp.OpenNlpNerProcessor;
import io.redlink.nlp.opennlp.de.LanguageGerman;
import io.redlink.nlp.opennlp.de.NerGerman;
import io.redlink.nlp.opennlp.pos.OpenNlpPosProcessor;

import java.util.Collections;

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
