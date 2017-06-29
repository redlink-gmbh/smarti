/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.processor.hasso;

import org.springframework.stereotype.Component;

import io.redlink.nlp.model.ner.NerTag;
import io.redlink.nlp.regex.ner.csv.CsvVocabularyNerDetector;
import io.redlink.smarti.model.Token;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Locale;

@Component
public class SapKeywordsVocab extends CsvVocabularyNerDetector {
    public static final String HINT = "sap";

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String VOCAB = "wordlists/SapKeywords.csv";

    public SapKeywordsVocab() {
        super("SAP Keywords", new NerTag("sap-entity",Token.Type.Term.name()), Locale.GERMAN, CaseSensitivity.smart);
    }

    //TODO: HINT functionality removed
    
    @Override
    protected Reader readFrom() {
        return new InputStreamReader(getClass().getClassLoader().getResourceAsStream(VOCAB), UTF8);
    }
}
