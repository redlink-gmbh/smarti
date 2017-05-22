/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 */
@ApiModel
public class QueryTemplate implements Comparable<QueryTemplate> {

    @JsonProperty("queryType")
    @ApiModelProperty(notes = "type of the query that can be build from this template", required = true)
    private MessageTopic type;
    @ApiModelProperty(notes = "probability that this template is the right one")
    private float probability;
    @ApiModelProperty(notes = "state of this template")
    private State state = State.Suggested;

    @JsonProperty("querySlots")
    @ApiModelProperty(notes = "slots to fill with tokens", required = true)
    private Collection<QuerySlot> slots;

    @ApiModelProperty(position = 5, value = "Queries suggested/executed")
    private List<Query> queries = new ArrayList<>();

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public QueryTemplate(@JsonProperty("queryType") MessageTopic type, @JsonProperty("querySlots") Collection<QuerySlot> slots) {
        this.type = type;
        this.slots = slots;
    }

    /**
     * Comparator that sorts the {@link QueryTemplate} with the highest {@link QueryTemplate#getProbability()}
     * first.
     */
    public static final Comparator<QueryTemplate> CONFIDENCE_COMPARATOR = (t1, t2) -> Float.compare(t2.getProbability(), t1.getProbability());

    public MessageTopic getType() {
        return type;
    }
    
    public float getProbability() {
        return probability;
    }

    public void setProbability(float probability) {
        this.probability = probability;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Collection<QuerySlot> getSlots() {
        return slots;
    }

    public List<Query> getQueries() {
        return queries;
    }

    public void setQueries(List<Query> queries) {
        this.queries = queries;
    }

    @Override
    public int compareTo(QueryTemplate o) {
        return Float.compare(o.probability, probability);
    }
}
