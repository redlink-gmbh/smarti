package io.redlink.smarti.query.solr;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.redlink.smarti.model.config.ComponentConfiguration;

@ConfigurationProperties(prefix="query.solr")
@JsonInclude(content=Include.ALWAYS) //the UI needs to know all possible properties
public class SolrEndpointConfiguration extends ComponentConfiguration implements Cloneable {

    
    private String solrEndpoint = "http://search.db.de/change/me";
    
    private SearchConfig search = new SearchConfig();
    private Map<String,Object> defaults = new HashMap<>();
    private ResultConfig result = new ResultConfig();
    
    public String getSolrEndpoint() {
        return solrEndpoint;
    }
    
    public void setSolrEndpoint(String solrEndpoint) {
        this.solrEndpoint = solrEndpoint;
    }

    public SearchConfig getSearch() {
        return search;
    }
    
    public void setSearch(SearchConfig search) {
        this.search = search;
    }
    
    /**
     * Defaults set for every Solr Query sent to this Solr endpoint.
     * Typically used to set parameters like <code>rows</code> but
     * can also be used to append a <code>fq</code> (FilterQuery) to
     * restrict results to a part of the index
     * @return
     */
    public Map<String, Object> getDefaults() {
        return defaults;
    }
    
    public ResultConfig getResult() {
        return result;
    }
    
    public void setResult(ResultConfig result) {
        this.result = result;
    }
    
    @JsonInclude(content=Include.ALWAYS) //the UI needs to know all possible properties
    public static class SearchConfig {
        
        private SingleFieldConfig title = new SingleFieldConfig();
        private SingleFieldConfig fullText = new SingleFieldConfig();
        private SpatialConfig spatial = new SpatialConfig();
        private TemporalConfig temporal = new TemporalConfig();
        private MultiFieldConfig related = new MultiFieldConfig();
        
        public void setTitle(SingleFieldConfig title) {
            this.title = title;
        }
        
        public SingleFieldConfig getTitle() {
            return title;
        }
        
        public void setFullText(SingleFieldConfig fullText) {
            this.fullText = fullText;
        }
        
        public SingleFieldConfig getFullText() {
            return fullText;
        }
        
        public void setSpatial(SpatialConfig spatial) {
            this.spatial = spatial;
        }
        
        public SpatialConfig getSpatial() {
            return spatial;
        }
        
        public void setTemporal(TemporalConfig temporal) {
            this.temporal = temporal;
        }
        
        public TemporalConfig getTemporal() {
            return temporal;
        }
        
        public void setRelated(MultiFieldConfig related) {
            this.related = related;
        }
        
        public MultiFieldConfig getRelated() {
            return related;
        }
    }
    
    @JsonInclude(content=Include.ALWAYS) //the UI needs to know all possible properties
    public static class SingleFieldConfig{
        
        private boolean enabled = true;
        
        private String field = "text";

        public final boolean isEnabled() {
            return enabled;
        }

        public final void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public final String getField() {
            return field;
        }

        public final void setField(String field) {
            this.field = field;
        }
        
        
    }
    
    @JsonInclude(content=Include.ALWAYS) //the UI needs to know all possible properties
    public static class TemporalConfig {
        private boolean enabled = false;
        private String timeRangeField;
        
        private String startTimeField;
        
        private String endTimeField;
        
        public final boolean isEnabled() {
            return enabled;
        }

        public final void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public final String getTimeRangeField() {
            return timeRangeField;
        }

        public final void setTimeRangeField(String timeRangeField) {
            this.timeRangeField = timeRangeField;
        }

        public final String getStartTimeField() {
            return startTimeField;
        }

        public final void setStartTimeField(String startTimeField) {
            this.startTimeField = startTimeField;
        }

        public final String getEndTimeField() {
            return endTimeField;
        }

        public final void setEndTimeField(String endTimeField) {
            this.endTimeField = endTimeField;
        }        
    }
    
    @JsonInclude(content=Include.ALWAYS) //the UI needs to know all possible properties
    public static class SpatialConfig {
        private boolean enabled = false;
        
        private String locationNameField;
        
        private String latLonPointSpatialField;
        
        private String rptField;
        
        private String bboxField;

        public final boolean isEnabled() {
            return enabled;
        }
        
        public final void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getLocationNameField() {
            return locationNameField;
        }
        
        public void setLocationNameField(String locationNameField) {
            this.locationNameField = locationNameField;
        }
        
        public final String getLatLonPointSpatialField() {
            return latLonPointSpatialField;
        }

        public final void setLatLonPointSpatialField(String latLonPointSpatialField) {
            this.latLonPointSpatialField = latLonPointSpatialField;
        }

        public final String getRptField() {
            return rptField;
        }

        public final void setRptField(String rptField) {
            this.rptField = rptField;
        }

        public final String getBboxField() {
            return bboxField;
        }

        public final void setBboxField(String bboxField) {
            this.bboxField = bboxField;
        }
    }
    /**
     * Allows to configure fields that can be used to search for related content (Solr MLT)
     */
    @JsonInclude(content=Include.ALWAYS) //the UI needs to know all possible properties
    public static class MultiFieldConfig {
        
        private boolean enabled = false;
        private final List<String> fields;

        public MultiFieldConfig(){
            this(null);
        }
        public MultiFieldConfig(Collection<String> fields){
            this.fields = fields == null ? new LinkedList<>() : new LinkedList<>(fields);
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public List<String> getFields() {
            return fields;
        }
    }
    
    /**
     * Allows to configure the mappings from SolrFields in the index
     * to fields shown by the templates presenting results
     * @author Rupert Westenthaler
     *
     */
    @JsonInclude(content=Include.ALWAYS) //the UI needs to know all possible properties
    public static class ResultConfig {
        
        private String title = "title";
        private String description = "description";
        private String icon = "icon";
        private String url = "url";
        private String keywords = "keywords";

        public final String getTitle() {
            return title;
        }
        public final void setTitle(String title) {
            this.title = title;
        }
        public final String getDescription() {
            return description;
        }
        public final void setDescription(String description) {
            this.description = description;
        }
        public final String getIcon() {
            return icon;
        }
        public final void setIcon(String icon) {
            this.icon = icon;
        }
        public final String getUrl() {
            return url;
        }
        public final void setUrl(String url) {
            this.url = url;
        }
        public final String getKeywords() {
            return keywords;
        }
        public final void setKeywords(String keywords) {
            this.keywords = keywords;
        }
        
    }
        
    @Override
    public SolrEndpointConfiguration clone() {
        SolrEndpointConfiguration clone = new SolrEndpointConfiguration();
        copyState(clone);
        clone.solrEndpoint = solrEndpoint;
        return clone;
    }
}
