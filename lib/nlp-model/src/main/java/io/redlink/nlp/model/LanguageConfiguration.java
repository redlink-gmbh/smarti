/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.redlink.nlp.model;

import org.apache.commons.collections.map.CompositeMap;
import org.apache.commons.collections.map.CompositeMap.MapMutator;

import javax.naming.ConfigurationException;
import java.util.*;

/**
 * Utility that supports the configuration of languages and language
 * specific parameters.
 * <h3>Language configuration</h3>
 * Languages are configured as follows:
 * <pre>
 *     de,en </pre>
 * or 
 * <pre>
 *     !fr,!cn,*</pre>
 * The '<code>!{lang}</code>' is used to {@link #getExplicitlyExcluded() 
 * explicitly exclude} an language. '<code>*</code>' can be used to
 * specify that all languages are allowed. '<code>{lang}</code>'
 * {@link #getExplicitlyIncluded() explicitly includes} a language.
 * '<code>,</code>' is used as separator between multiple configurations
 * however this class also supports the usage of  <code>String[]</code> and
 * {@link Collection} (in case of Collections the
 * {@link Object#toString()} method is used to obtain the configuration).
 * If an array or a collection is used for the configuration, than comma
 * is NOT used as separator!
 * <p>
 * <h3>Parameter Support</h3>
 * This class supports the parsing of language specific parameters by
 * the followng syntax
 * <pre>
 *    {language};{param-name}={param-value};{param-name}={param-value}</pre>
 * Parameters that apply to all {languages} with no configuration can be
 * either set for the '<code>*</code>' or an empty language tag. Here
 * is an example
 * <pre>
 *     *;myParam=myValue
 *     ;myParam=myValue</pre>
 * Multiple default configurations will cause a {@link ConfigurationException}.
 * <p>
 * The {@link #getParameters(String)} and {@link #getParameter(String, String)}
 * will return values of the {@link #getDefaultParameters()} if no
 * language specific parameters are present for the requested language. However
 * the default configuration is not merged but replaced by language specific
 * parameter declarations. Applications that want to use the default configuration
 * as fallback to language specific settings can implement this by
 * using the properties provided by {@link #getDefaultParameters()}.
 * <p>
 * <b>Notes</b> <ul>
 * <li>only the first occurrence of '<code>=</code>' within an
 * parameter is used as separator between the param name and value. This
 * means that the {param-name} is allowed to contain '='.
 * <li>in case a comma separated string is used for the lanugage
 * configuration parameter declaration MUST NOT contain 
 * '<code>,</code>' (comma) values. In case a <code>String[]</code> or an
 * {@link Collection} is used this is not the case.
 * </ul>
 * <p>
 * <b>NOTE:</b> 
 * This class originates from the Apache Stanbol LanguageConfiguration utility.
 * 
 * @author Rupert Westenthaler
 */
public class LanguageConfiguration {

    private static final Map<String,String> EMPTY_PARAMS = Collections.emptyMap();
    
    private static MapMutator CONFIGURATION_MERGER = new MapMutator() {
        
        @Override
        @SuppressWarnings("rawtypes")
        public void resolveCollision(CompositeMap composite, Map existing, Map added, Collection intersect) {
            //nothing to do as we want the value of the first map
        }
        
        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void putAll(CompositeMap map, Map[] composited, Map mapToAdd) {
            //add to the first
            composited[0].putAll(mapToAdd);
        }
        
        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public Object put(CompositeMap map, Map[] composited, Object key, Object value) {
            Object prevResult = map.get(key);
            Object result = composited[0].put(key,value);
            return result == null ? prevResult : result;
        }
    };
    
    private final Collection<String> parsedConfig;
    //Langauge configuration
    private Map<String,Map<String,String>> configuredLanguages = new HashMap<String,Map<String,String>>();
    private Set<String> excludedLanguages = new HashSet<String>();
    private boolean allowAll;
    private Map<String,String> defaultParameters = EMPTY_PARAMS;

    /**
     * The language configuration. Supports multiple configurations using varargs
     * as well as a single String value using comma as separator. See class
     * documentation for information about the configuration syntax
     * @param config the configuration or <code>null</code> to create an empty
     * configuration
     */
    public LanguageConfiguration(String...config){
        if(config == null || config.length == 1 && config[0] == null){
            this.parsedConfig = Collections.emptyList();
        } else if(config.length == 1 && config[0].indexOf(',') >= 0){
            this.parsedConfig = Arrays.asList(config[0].split(","));
        } else {
            this.parsedConfig = Arrays.asList(config);
            if(parsedConfig.contains(null)){
                throw new IllegalArgumentException("The parsed config MUST NOT "
                        + "contain a NULL element (parsed: " + Arrays.toString(config) + ")");
            }
        }
        parseConfiguration(parsedConfig);
    }

    /**
     * 
     * @param config
     * @throws IllegalArgumentException if the parsed configuration could not be parsed
     */
    private void parseConfiguration(Collection<?> config) {
        if(config == null){
            config = parsedConfig;
        }
        //rest values
        configuredLanguages.clear();
        excludedLanguages.clear();
        defaultParameters = EMPTY_PARAMS; //do not change values in multi threaded environments
        for(Object value : config) {
            if(value == null){
                continue; //ignore null values
            }
            String line = value.toString().trim();
            int sepIndex = line.indexOf(';');
            String lang = sepIndex < 0 ? line : line.substring(0, sepIndex).trim();
            //lang = lang.toLowerCase(); //country codes are upper case
            if(lang.length() > 0 && lang.charAt(0) == '!'){ //exclude
                lang = lang.substring(1);
                if(configuredLanguages.containsKey(lang)){
                    throw new IllegalArgumentException( 
                        "Langauge '"+lang+"' is both included and excluded (config: "
                        + config+")");
                }
                if(sepIndex >= 0){
                    throw new IllegalArgumentException( 
                        "The excluded Langauge '"+lang+"' MUST NOT define parameters (config: "
                        + config+")");
                }
                excludedLanguages.add(lang);
            } else if("*".equals(lang)){
                allowAll = true;
                parsedDefaultParameters(line, sepIndex+1);
            } else if(!lang.isEmpty()){
                if(excludedLanguages.contains(lang)){
                    throw new IllegalArgumentException( 
                        "Langauge '"+lang+"' is both included and excluded (config: "
                        + config+")");
                }
                configuredLanguages.put(lang,sepIndex >= 0 && sepIndex < line.length()-2 ? 
                        parseParameters(line.substring(sepIndex+1, line.length()).trim()) :
                            EMPTY_PARAMS);
            } else { //language tag is empty (line starts with an ';'
                //this indicates that this is used to configure the default parameters
                parsedDefaultParameters(line, sepIndex+1);
            }
        }
    }
    /**
     * Parsed the {@link #defaultParameters} and also checks that not multiple 
     * (non empty) of such configurations are present
     * @param line the current line
     * @param sepIndex the index of first ';' in the configuration line
     * @throws IllegalArgumentException if multiple default configurations are present or
     * if the parameters are illegal formatted.
     */
    private void parsedDefaultParameters(String line, int sepIndex) {
        if(!defaultParameters.isEmpty()){
            throw new IllegalArgumentException("Language Configuration MUST NOT "
                + "contain multiple default property configurations. This are configurations "
                + "of properties for the wildcard '*;{properties}' or the empty language "
                + "';{properties}'.");
        }
        defaultParameters = sepIndex >= 0 && sepIndex < line.length()-2 ? 
                parseParameters(line.substring(sepIndex, line.length()).trim()) :
                    EMPTY_PARAMS;
    }
    /**
     * Parses optional parameters <code>{key}[={value}];{key2}[={value2}]</code>. Using
     * the same key multiple times will override the previous value
     * @param paramString
     * @return
     * @throws IllegalArgumentException if the parsed paramString can not be parsed
     */
    private Map<String,String> parseParameters(String paramString) {
        Map<String,String> params = new HashMap<String,String>();
        for(String param : paramString.split(";")){
            param = param.trim();
            int equalsPos = param.indexOf('=');
            if(equalsPos == 0){
                throw new IllegalArgumentException("Parameter '"+param+"' has empty key!");
            }
            String key = equalsPos > 0 ? param.substring(0, equalsPos).trim() : param;
            String value;
            if(equalsPos > 0){
                if(equalsPos < param.length()-2) {
                    value = param.substring(equalsPos+1).trim(); 
                } else {
                    value = "";
                }
            } else {
                value = null;
            }
            params.put(key, value);
        }
        return params.isEmpty() ? EMPTY_PARAMS : Collections.unmodifiableMap(params);
    }

    private class LangState{
        
        protected final boolean state;
        protected final String lang;
        protected LangState(boolean state, String lang){
            this.state = state;
            this.lang = lang;
        }
    }
    
    private LangState getLanguageState(String language){
        int countrySepPos = language == null ? -1 : language.indexOf('-');
        boolean excluded = excludedLanguages.contains(language);
        boolean included = configuredLanguages.containsKey(language);
        if(countrySepPos >= 2 && !excluded && ! included){
            //search without language specific part
            String baseLang = language.substring(0, countrySepPos);
            return new LangState(allowAll ? !excludedLanguages.contains(baseLang) :
                configuredLanguages.containsKey(baseLang), baseLang);
        } else {
            return new LangState(allowAll ? !excluded : included,language);
        }
    }
    
    /**
     * Checks if the parsed language is included in the configuration
     * @param language the language
     * @return the state
     */
    public boolean isLanguage(String language){
        return getLanguageState(language).state;
    }
    /**
     * The explicitly configured languages
     * @return
     */
    public Set<String> getExplicitlyIncluded(){
        return configuredLanguages.keySet();
    }
    /**
     * The explicitly excluded (e.g. !de) languages
     * @return
     */
    public Set<String> getExplicitlyExcluded(){
        return excludedLanguages;
    }
    /**
     * If the '*' was used in the configuration to allow
     * all lanugages. 
     * @return
     */
    public boolean useWildcard(){
        return allowAll;
    }
    
    /**
     * Returns configured parameters if <code>{@link #isLanguage(String)} == true</code>.
     * The returned map contains {@link #getLanguageParams(String) language specific parameters} 
     * merged with {@link #getDefaultParameters()}
     * @param parsedLang the language
     * @return the parameters or <code>null</code> if none or the parsed language
     * is not active.
     */
    @SuppressWarnings("unchecked")
    public Map<String,String> getParameters(String parsedLang){
        LangState ls = getLanguageState(parsedLang);
        if(ls.state){
            Map<String,String> params = configuredLanguages.get(ls.lang);
            if(params != null){
                params = new CompositeMap(params,defaultParameters,CONFIGURATION_MERGER);
            } else {
                params = defaultParameters;
            }
            return params;
        } else {
            return null; //to indicate the parsed language is not active
        }
    }
    /**
     * Getter for the language specific parameters. This does NOT include
     * default parameters.
     * @param parsedLang the language
     * @return the language specific parameters or <code>null</code> if no
     * parameters are configured.
     */
    public Map<String,String> getLanguageParams(String parsedLang){
        LangState ls = getLanguageState(parsedLang);
        return ls.state ? configuredLanguages.get(ls.lang) : null;
    }
    /**
     * Getter for the default parameters
     * @return the default parameters, an empty map if none.
     */
    public Map<String,String> getDefaultParameters() {
        return defaultParameters;
    }
    
    /**
     * Returns the value of the parameter for the language (if present and the
     * langage is active). This merges language specific parameters with
     * default parameters.
     * @param language the language
     * @param paramName the name of the param
     * @return the param or <code>null</code> if not present OR the language
     * is not active.
     */
    public String getParameter(String language, String paramName) {
        Map<String,String> params = getParameters(language);
        int countrySepPos = language == null ? -1 : language.indexOf('-');
        //we need to fallback to the language specific config if
        // * there is a country code
        // * no country specific params OR
        // * param not present in country specific config
        if(countrySepPos >= 2 && (params == null || !params.containsKey(paramName))) {
            params = getParameters(language.substring(0,countrySepPos));
        }
        return params == null ? null : params.get(paramName);
    }
        
}
