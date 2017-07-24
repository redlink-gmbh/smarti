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

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Slot;
import io.redlink.smarti.services.TemplateRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Variant of the {@link DbSearchQueryBuilder} that builds a
 * {@link DbSearchQuery} that only includes {@link Slot}s with
 * the Role {@link DbSearchTemplateDefinition#ROLE_KEYWORD}
 * @author Rupert Westenthaler
 *
 */
@Component
public class DbSearchKeywordQueryBuilder extends DbSearchQueryBuilder {

    public DbSearchKeywordQueryBuilder(DbSearchEndpointConfiguration defConf, TemplateRegistry registry) {
        super(defConf, registry, "keyword");
    }

    @Override
    protected boolean acceptSlot(Slot slot, Conversation conversation) {
        return DbSearchTemplateDefinition.ROLE_KEYWORD.equals(slot.getRole()); 
    }
    
    @Override
    protected String getQueryTitle(){
        return "DB Search Keyword Search";
    }

    
}
