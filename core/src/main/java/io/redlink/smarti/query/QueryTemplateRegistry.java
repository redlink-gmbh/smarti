/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.smarti.query;

import io.redlink.smarti.model.MessageTopic;
import io.redlink.smarti.model.QueryTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class QueryTemplateRegistry {

    private final Map<MessageTopic, QueryTemplateDefinition> templates;
    
    @Autowired
    public QueryTemplateRegistry(Collection<QueryTemplateDefinition> templateDefinitions) {
        templates = new EnumMap<>(MessageTopic.class);
        for(QueryTemplateDefinition template : templateDefinitions){
            QueryTemplateDefinition old = templates.put(template.getType(), template);
            if(old != null){
                throw new IllegalStateException("Multiple QueryTemplateDefinitions for Topic "
                        + template.getType() + "(" + old.getClass().getSimpleName()
                        + " and " +template.getClass().getSimpleName()+")!");
            }
        }
    }
    
    public QueryTemplateDefinition getTemplate(QueryTemplate qt){
        return getTemplate(qt.getType());
    }

    public QueryTemplateDefinition getTemplate(MessageTopic type){
        QueryTemplateDefinition def = templates.get(type);
        if(def == null){
            for(Iterator<MessageTopic> it = type.hierarchy().iterator(); it.hasNext() && def == null;){
                def = templates.get(it.next());
            }
        }
        return def;
    }
    
}
