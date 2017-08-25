package io.redlink.smarti.processor.token.filter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.google.common.base.Objects;

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.processor.token.TokenFilter;


@Component
@EnableConfigurationProperties(StopwordlistConfiguration.class)
public class StopwordlistTokenFilter implements TokenFilter {

    private final StopwordlistConfiguration config;
    
    public StopwordlistTokenFilter(StopwordlistConfiguration config) {
        this.config = config;
    }

    @Override
    public boolean filter(Token token, String lang, Conversation c) {
        if(token.getValue() == null){
            return false;
        } else if(!(token.getValue() instanceof CharSequence)){
            return false;
        }
        String value = String.valueOf(token.getValue());
        if(StringUtils.isBlank(value)){
            return false;
        }
        Locale locale = lang == null ? null : Locale.forLanguageTag(lang);
        //the key is the lower case stop word the values are case sensitive variants
        Map<String, Collection<String>> langStopwords = config.getStopwords(locale);
        Map<String, Collection<String>> defaultStopwords = config.getStopwords(null);
        Collection<String> defLangVariants = defaultStopwords != null ? defaultStopwords.get(value.toLowerCase(Locale.ROOT)) : null;
        Collection<String> langVariants = langStopwords != null ? langStopwords.get(value.toLowerCase(Locale.ROOT)) : null;
        if(defLangVariants == null && langVariants == null){
            return false;
        } else if(!isAllAlphaUpperCase(value)){ //TODO: make case sensitivity configurable (now SMART)
            return true;
        } else { //require an exact match with one of the variants
            return (defLangVariants == null ? langVariants : langVariants == null ? 
                    defLangVariants : CollectionUtils.union(langVariants, defLangVariants)).stream()
                    .filter(sw -> Objects.equal(value, sw)).findAny().isPresent();
            
        }
        
    }
    
    /**
     * Checks if all {@link Character#isAlphabetic(char)} are also
     * {@link Character#isUpperCase(char)}.
     * @param cs
     * @return the state. <code>false</code> if the parsed sequence does not
     * contain a single alphabetic char
     */
    private static boolean isAllAlphaUpperCase(final CharSequence cs) {
        if(StringUtils.isBlank(cs)){
            return false;
        }
        final int sz = cs.length();
        boolean hasAlpha = false;
        for (int i = 0; i < sz; i++) {
            char c = cs.charAt(i);
            if(Character.isAlphabetic(c)){
                if(!Character.isUpperCase(c)){
                    return false;
                }
                hasAlpha = true;
            }
        }
        return hasAlpha; //return false if no alpha char is present
    }

}
