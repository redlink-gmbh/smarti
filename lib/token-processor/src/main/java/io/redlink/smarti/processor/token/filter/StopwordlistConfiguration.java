package io.redlink.smarti.processor.token.filter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.jni.Local;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Allows to configure resource with stopwords by using
 * <pre>
 *      processor.token.stopword.{lang}={resource}
 *      processor.token.stopword.default={resource}
 * </pre>
 * where <code>{lang}</code> is the two letter language code and <code>{resource}</code>
 * is an valid Spring {@link Resource} (classpath, file path ...).<p>
 * The key <code>default</code> can be used for Stopwords that are used regardles of
 * the language of the content. Default language stopwords are used in addition to language 
 * specific stopwords if such are defined <p>
 * 
 * @author Rupert Westenthaler
 */ 
@ConfigurationProperties(prefix="processor.token")
public class StopwordlistConfiguration {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final Charset UTF8 = Charset.forName("utf-8");
    
    public static final Map<String, String> DEFAULTS;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("default", "classpath:data/tokenfilter/stopwords.txt");
        m.put("de", "classpath:data/tokenfilter/stopwords-de.txt");
        DEFAULTS = Collections.unmodifiableMap(m);
    }

    private Map<String, String> stopword = new HashMap<>();
    
    private Map<String, Map<String, Collection<String>>> stopwordLists = new HashMap<>();
    
    private final ResourceLoader rLoader;
    
    public StopwordlistConfiguration(ResourceLoader rLoader){
        //set the defaults
        stopword.putAll(DEFAULTS);
        this.rLoader = rLoader;
    }
    
    public Map<String, String> getStopword() {
        return stopword;
    }
    
    public void setStopword(Map<String, String> stopword) {
        this.stopword = stopword;
    }

    /**
     * The stopwords for the parsed language or <code>null</code> if none are defined
     * @param lang the {@link Local} representing the language or <code>null</code> to get the default stopwords
     * @return the stopwords or <code>null</code> if none are configured for this language
     */
    public Map<String,Collection<String>> getStopwords(Locale lang){
        final String key = lang == null ? "default" : lang.getLanguage();
        final Map<String,Collection<String>> stopwords;
        if(stopwordLists.containsKey(key)){
            stopwords = stopwordLists.get(key);
        } else {
            stopwords = parseStopwords(stopword.get(key), lang);
            stopwordLists.put(key, stopwords); //no list for this language present
        }
        return stopwords;
    }

    private Map<String,Collection<String>> parseStopwords(String location, Locale lang) {
        if(location == null) return null;
        Resource resource = rLoader.getResource(location);
        if(resource == null){
            log.warn("unable to load resource from location '{}' as configured for language '{}'", location, lang);
            return null;
        }
        Locale locale = lang == null ? Locale.ROOT : lang;
        Map<String,Collection<String>> stopwords = new HashMap<>();
        try (InputStream in = resource.getInputStream()){
            IOUtils.lineIterator(in, UTF8).forEachRemaining(line -> {
                if(StringUtils.isNoneBlank(line)){
                    line = line.trim();
                    if(line.charAt(0) != '#') {
                        String key = line.toLowerCase(locale);
                        Collection<String> values = stopwords.computeIfAbsent(key, (k) -> new LinkedHashSet<String>());
                        values.add(line);
                    } //else comment
                }
            });
        } catch (IOException e) {
            log.warn("Unable to parsed stopwords for language '{}' from resource '{}' ({} - {})",
                    lang, location, e.getClass().getSimpleName(), e.getMessage());
            log.debug("STACKTRACE", e);
        }
        return stopwords;
    }
    
}
