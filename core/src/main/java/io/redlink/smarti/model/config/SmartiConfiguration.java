package io.redlink.smarti.model.config;

import java.util.LinkedList;
import java.util.List;

public class SmartiConfiguration {
   
    private final List<ComponentConfiguration> queryBuilder = new LinkedList<>();
    
    public SmartiConfiguration() {
    }

    public List<ComponentConfiguration> getQueryBuilder() {
        return queryBuilder;
    }
    
}
