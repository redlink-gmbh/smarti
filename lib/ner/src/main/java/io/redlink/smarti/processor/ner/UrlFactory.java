package io.redlink.smarti.processor.ner;

import io.redlink.nlp.model.SpanCollection;
import io.redlink.nlp.model.ner.NerTag;
import io.redlink.nlp.regex.ner.RegexNamedEntityFactory;
import io.redlink.nlp.regex.ner.RegexNerProcessor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 26.07.17.
 */
@Component
public class UrlFactory extends RegexNamedEntityFactory {

    private static final String URL = "url";

    private static final NerTag URL_TAG = new NerTag(URL, NerTag.NAMED_ENTITY_MISC);

    //url pattern taken from https://mathiasbynens.be/demo/url-regex @stephenhay (38 chars)
    private static final Pattern URL_PATTERN = Pattern.compile("\\b(?:https?|ftp)://(?:-\\.)?(?:[^\\s/?\\.#-]+\\.?)+(?:/[^\\s]*)?", Pattern.CASE_INSENSITIVE);

    @Override
    protected RegexNerProcessor.NamedEntity createNamedEntity(String pattern_name, MatchResult matchResult) {
        return new RegexNerProcessor.NamedEntity(
                matchResult.start(),
                matchResult.end(),
                URL_TAG
        );
    }

    @Override
    protected List<NamedPattern> getRegexes(SpanCollection spanCollection, String lang) {
        return Collections.singletonList(new NamedPattern(URL, URL_PATTERN));
    }
}
