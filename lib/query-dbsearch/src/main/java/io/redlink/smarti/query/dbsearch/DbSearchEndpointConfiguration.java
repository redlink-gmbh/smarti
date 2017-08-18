package io.redlink.smarti.query.dbsearch;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.redlink.smarti.model.config.ComponentConfiguration;

@ConfigurationProperties(prefix="dbsearch")
public class DbSearchEndpointConfiguration extends ComponentConfiguration implements Cloneable {

    
    private String solrEndpoint = "http://search.db.de/change/me";
    
    @JsonIgnore
    @Deprecated
    public String getSolr() {
        return solrEndpoint;
    }
    
    @JsonIgnore
    @Deprecated
    public void setSolr(String solr) {
        this.solrEndpoint = solr;
    }
    
    public String getSolrEndpoint() {
        return solrEndpoint;
    }
    
    public void setSolrEndpoint(String solrEndpoint) {
        this.solrEndpoint = solrEndpoint;
    }
    
    @Override
    public DbSearchEndpointConfiguration clone() {
        DbSearchEndpointConfiguration clone = new DbSearchEndpointConfiguration();
        copyState(clone);
        clone.solrEndpoint = solrEndpoint;
        return clone;
    }
}
