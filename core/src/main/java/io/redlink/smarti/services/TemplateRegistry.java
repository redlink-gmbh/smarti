/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.smarti.services;

import io.redlink.smarti.model.TemplateDefinition;
import io.redlink.smarti.model.Template;
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
