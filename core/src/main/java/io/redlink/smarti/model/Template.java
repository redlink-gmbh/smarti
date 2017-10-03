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
public class Template implements Comparable<Template> {

    @JsonProperty("type")
    @ApiModelProperty(notes = "type of the template that can be build from this template", required = true)
    private String type;
    @ApiModelProperty(notes = "probability that this template is the right one")
    private float probability;
    @ApiModelProperty(notes = "state of this template")
    private State state = State.Suggested;

    @JsonProperty("slots")
    @ApiModelProperty(notes = "slots to fill with tokens", required = true)
    private Collection<Slot> slots;

    @ApiModelProperty(value = "Queries suggested/executed")
    private List<Query> queries = new ArrayList<>();

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Template(@JsonProperty("type") String type, @JsonProperty("slots") Collection<Slot> slots) {
        this.type = type;
        this.slots = slots;
    }

    /**
     * Comparator that sorts the {@link Template} with the highest {@link Template#getProbability()}
     * first.
     */
    public static final Comparator<Template> CONFIDENCE_COMPARATOR = (t1, t2) -> Float.compare(t2.getProbability(), t1.getProbability());

    /**
     * Getter for the type of this template
     * @return
     */
    public String getType() {
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

    public Collection<Slot> getSlots() {
        return slots;
    }

    public void setSlots(Collection<Slot> slots) {
        this.slots = slots;
    }

    public List<Query> getQueries() {
        return queries;
    }

    public void setQueries(List<Query> queries) {
        this.queries = queries;
    }

    @Override
    public int compareTo(Template o) {
        return Float.compare(o.probability, probability);
    }
    
    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append("[type: ").append(type)
                .append(", slots: ").append(slots).append(", state: ").append(state)
                .append(", probability: ").append(probability).append(']').toString();
    }
}
