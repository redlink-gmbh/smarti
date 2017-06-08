/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.smarti.services;

import io.redlink.smarti.model.IntendDefinition;
import io.redlink.smarti.model.Intent;
import io.redlink.smarti.model.MessageTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class IntendRegistry {

    private final Map<MessageTopic, IntendDefinition> templates;
    
    @Autowired
    public IntendRegistry(Collection<IntendDefinition> templateDefinitions) {
        templates = new EnumMap<>(MessageTopic.class);
        for(IntendDefinition template : templateDefinitions){
            IntendDefinition old = templates.put(template.getType(), template);
            if(old != null){
                throw new IllegalStateException("Multiple QueryTemplateDefinitions for Topic "
                        + template.getType() + "(" + old.getClass().getSimpleName()
                        + " and " +template.getClass().getSimpleName()+")!");
            }
        }
    }
    
    public IntendDefinition getTemplate(Intent qt){
        return getTemplate(qt.getType());
    }

    public IntendDefinition getTemplate(MessageTopic type){
        IntendDefinition def = templates.get(type);
        if(def == null){
            for(Iterator<MessageTopic> it = type.hierarchy().iterator(); it.hasNext() && def == null;){
                def = templates.get(it.next());
            }
        }
        return def;
    }
    
}
