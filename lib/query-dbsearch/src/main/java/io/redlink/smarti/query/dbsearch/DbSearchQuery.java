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

package io.redlink.smarti.query.dbsearch;


import io.redlink.smarti.model.Query;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 */
public class DbSearchQuery extends Query {

    private Set<String> fullTextTerms = new LinkedHashSet<>();

    public DbSearchQuery() {
        super();
    }

    public DbSearchQuery(String creator) {
        super(creator);
    }

    public void setFullTextTerm(String keyword) {
        this.fullTextTerms.clear();
        this.fullTextTerms.add(keyword);
    }
    public void addFullTextTerms(String term){
        this.fullTextTerms.add(term);
    }

    public Collection<String> getFullTextTerms() {
        return fullTextTerms;
    }

    public void setFullTextTerms(Collection<String> strings) {
        fullTextTerms.clear();
        fullTextTerms.addAll(strings);
    }

    @Override
    public String toString() {
        return "DbSearchQuery [title=" + getDisplayTitle() + ", creator=" + getCreator() + ",fullTextTerms=" + fullTextTerms + "]";
    }
    
    
}

