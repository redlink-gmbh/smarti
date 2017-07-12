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

package io.redlink.smarti.services;

import io.redlink.smarti.model.Template;
import io.redlink.smarti.model.TemplateDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple Service that tracks all {@link TemplateDefinition}s and allows for {@link Template#getType()}
 * lookups for templates
 * 
 * @author Rupert Westenthaler
 */
@Service
public class TemplateRegistry {

    private final Map<String, TemplateDefinition> templates;
    
    @Autowired
    public TemplateRegistry(Collection<TemplateDefinition> templateDefinitions) {
        templates = new HashMap<>();
        for(TemplateDefinition template : templateDefinitions){
            TemplateDefinition old = templates.put(template.getType(), template);
            if(old != null){
                throw new IllegalStateException("Multiple QueryTemplateDefinitions for Topic "
                        + template.getType() + "(" + old.getClass().getSimpleName()
                        + " and " +template.getClass().getSimpleName()+")!");
            }
        }
    }
    
    public TemplateDefinition getTemplate(Template qt){
        return getTemplate(qt.getType());
    }

    public TemplateDefinition getTemplate(String type){
        return templates.get(type);
    }
    
}
