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

import io.redlink.nlp.model.ner.NerTag;
import io.redlink.nlp.regex.ner.csv.CsvVocabularyNerDetector;
import io.redlink.smarti.model.Token;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Locale;

@Component
@ConditionalOnProperty("smarti.extractor.synonyms.sap")
public class SapKeywordsVocab extends CsvVocabularyNerDetector {
    public static final String HINT = "sap";

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final Resource dataFile;

    @Autowired
    public SapKeywordsVocab(@Value("${smarti.extractor.synonyms.sap}") Resource dataFile) {
        super("SAP Keywords", new NerTag("sap-entity",Token.Type.Term.name()), Locale.GERMAN, CaseSensitivity.smart);
        this.dataFile = dataFile;
    }

    //TODO: HINT functionality removed
    
    @Override
    protected Reader readFrom() {
        try {
            return new InputStreamReader(dataFile.getInputStream(), UTF8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load dataFile " + dataFile, e);
        }
    }
}
