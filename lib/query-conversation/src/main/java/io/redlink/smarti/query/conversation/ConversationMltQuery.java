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

package io.redlink.smarti.query.conversation;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.redlink.smarti.model.Query;
import io.redlink.smarti.model.State;

/**
 */
public class ConversationMltQuery extends Query {

    private String content;

    @JsonProperty("defaults")
    private final Map<String,Object> defaults = new HashMap<>();

    public ConversationMltQuery() {
        super();
    }

    public ConversationMltQuery(String creator) {
        super(creator);
    }

    @Override
    public ConversationMltQuery setConfidence(float confidence) {
        super.setConfidence(confidence);
        return this;
    }

    @Override
    public ConversationMltQuery setDisplayTitle(String displayTitle) {
        super.setDisplayTitle(displayTitle);
        return this;
    }

    @Override
    public ConversationMltQuery setUrl(String url) {
        super.setUrl(url);
        return this;
    }

    @Override
    public ConversationMltQuery setCreator(String creator) {
        super.setCreator(creator);
        return this;
    }

    @Override
    public ConversationMltQuery setInlineResultSupport(boolean inlineResultSupport) {
        super.setInlineResultSupport(inlineResultSupport);
        return this;
    }

    @Override
    public ConversationMltQuery setState(State state) {
        super.setState(state);
        return this;
    }

    public ConversationMltQuery setContent(String content) {
        this.content = content;
        return this;
    }

    public String getContent() {
        return content;
    }

    public ConversationMltQuery setDefaults(Map<String, Object> defaults) {
        this.defaults.clear();
        if(defaults != null){
            this.defaults.putAll(defaults);
        }
        return this;
    }

    
    public Map<String, Object> getDefaults() {
        return defaults;
    }
    
    
}
