/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.processor.hasso;

import io.redlink.nlp.model.ner.NerTag;
import io.redlink.nlp.regex.ner.csv.CsvVocabularyNerDetector;
import org.apache.commons.csv.CSVFormat;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Locale;

@Component
public class DbKonzernSynonymVocab extends CsvVocabularyNerDetector {
    public static final String HINT = "synonym";

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String VOCAB = "wordlists/DB-Konzern-Synonyme.csv";

    public DbKonzernSynonymVocab() {
        super("DB-Konzern Synonyme", new NerTag("Keyword"), Locale.GERMAN, true, CSVFormat.DEFAULT.withDelimiter(';'));
    }

    //TODO: removed HINT functionality
    
    @Override
    protected Reader readFrom() {
        return new InputStreamReader(getClass().getClassLoader().getResourceAsStream(VOCAB), UTF8);
    }
}
