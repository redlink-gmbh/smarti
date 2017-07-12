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


import io.redlink.smarti.model.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 */
public class ConversationSearchQuery extends Query {

    private List<String> keywords = new ArrayList<>();

    public ConversationSearchQuery() {
        super();
    }

    public ConversationSearchQuery(String creator) {
        super(creator);
    }

    public void setKeyword(String keyword) {
        this.keywords.clear();
        this.keywords.add(keyword);
    }

    public List<String> getKeyword() {
        return Collections.unmodifiableList(keywords);
    }

    public void setKeywords(Collection<String> strings) {
        keywords.clear();
        keywords.addAll(strings);
    }
}
