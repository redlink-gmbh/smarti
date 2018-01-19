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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.redlink.smarti.model.values.DateValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * A token - this is what a {@link io.redlink.smarti.api.QueryPreparator} normally produces.
 */
@ApiModel
public class Token implements Comparable<Token>{

    public enum Type {
        /**
         * A date
         */
        Date(DateValue.class),
        /**
         * A Topic (typically the result of a classifier)
         */
        Topic(MessageTopic.class),
        /**
         * Any other type of Named Entities
         */
        Entity(String.class),
        /**
         * Entities that represent a Location
         */
        Place(String.class),
        /**
         * Entities that representing an Organization
         */
        Organization(String.class),
        /**
         * Entities that represent Persons
         */
        Person(String.class),
        /**
         * Entities that represent products or services
         */
        Product(String.class),
        /**
         * Attributes (typically adjectives)
         */
        Attribute(String.class),
        /**
         * Terms as contained in Terminologies with mixed types
         */
        Term(String.class),
        /**
         * Keywords (typically results of some keyword extraction/detection)
         */
        Keyword(String.class),
        /**
         * Any other type of an Entity
         */
        Other(Object.class),
        ;
        
        private Object valueClass;

        Type(Class<?> valueClass){
            this.valueClass = valueClass == null ? Object.class : valueClass;
        }
        
        /**
         * The expected type of the {@link Token#getValue()} of tokens using 
         * this type.
         * @return The class for token values with this type
         */
        public Object getValueClass() {
            return valueClass;
        }
    }
    /**
     * Enumeration with well known hints 
     *
     */
    public enum Hint {
        instant(Type.Date),
        start(Type.Date),
        end(Type.Date),
        from(Type.Place),
        to(Type.Place),
        via(Type.Place),
        at(Type.Place),
        depart(Type.Date),
        arrive(Type.Date),
//        hinfahrt(Type.Date), //Hinfahrt
//        rückfahrt(Type.Date), //Rückfahrt
        /**
         * Means that the Token is negated (e.g. "no Pizzaia", "nicht über München")
         */
        negated,
        ;
        
        Type type;
        /**
         * A Hint that can be used with all Token {@link Type}s
         */
        Hint() {
            this(null);
        }
        /**
         * A Hint that is only used for a specific Token {@link Type}s
         */
        Hint(Type type) {
            this.type = type;
        }

        public Type getType() {
            return type;
        }
        
    }

    /**
     * Who created the token?
     */
    public enum Origin {
        System,
        Agent,
        User
    }


    /**
     * Comparator that sorts {@link Token}s based on<ol>
     * <li> lower message index
     * <li> lower start char offset
     * <li> higher end char offset
     * </ol>
     * first
     */
    public static final Comparator<Token> IDX_START_END_COMPARATOR = new Comparator<Token>() {

        @Override
        public int compare(Token t1, Token t2) {
            int c = Integer.compare(t1.getMessageIdx(), t2.getMessageIdx());
            if(c == 0){ //lower start first
                c = Integer.compare(t1.getStart(), t2.getStart());
                if(c == 0){ //higher end first
                    c = Integer.compare(t2.getEnd(), t1.getEnd());
                }
            }
            return c;
        }
    };
    
    /**
     * Comparator that sorts the {@link Token} with the highest {@link Token#getConfidence()}
     * first.
     */
    public static final Comparator<Token> CONFIDENCE_COMPARATOR = new Comparator<Token>() {
        @Override
        public int compare(Token t1, Token t2) {
            return Float.compare(t2.getConfidence(), t1.getConfidence());
        }
    };


    /**
     * The index of the message this token was extracted from
     */
    @ApiModelProperty(value = "message-index", notes = "reference to the message index this token was found in", required = true)
    private int messageIdx = -1;
    /**
     * The start char offset of the mention for this token within the {@link #getMessageIdx()}
     */
    @ApiModelProperty(value = "start-position", notes = "start-position of the match", required = true)
    private int start = -1;
    /**
     * The end char offset of the mention for this token within the {@link #getMessageIdx()}
     */
    @ApiModelProperty(value = "end-position", notes = "end-position of the match", required = true)
    private int end = -1;

    /**
     * Who created the token
     */
    @ApiModelProperty(value = "origin", notes = "who created the token. Tokens created by Smarti will use 'System'. Tokens"
            + "create by the chat systems users should use 'User' or 'Agent'", 
            allowableValues="System, Agent, User", example="User", required=true, allowEmptyValue=false)
    private Origin origin = Origin.System;

    /**
     * The {@link State} of the token
     */
    @ApiModelProperty(notes="token state. Tokens created by Smarti will use 'Suggested'. Interactions with suggested tokens should"
            + "result in state updates to 'Confirmed' or 'Rejected'. Tokens created by chat system users should start with"
            + "the state 'Confiremd'",
            allowableValues="Suggested, Confirmed, Rejected", example="Confirmed", required=true, allowEmptyValue=false)
    private State state = State.Suggested;

    /**
     * The value of the token. The type of the value depends on the {@link Type} of the token
     */
    @ApiModelProperty(notes = "the actual value of the token. The type of the value depends on the type of the token", required = true)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", defaultImpl = String.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = DateValue.class, name = "Date"),
            @JsonSubTypes.Type(value = MessageTopic.class, name = "Topic"),
            @JsonSubTypes.Type(value = String.class)
    })
    private Object value;

    /**
     * The {@link Type} of the token
     */
    @ApiModelProperty(notes = "the type of the token", required = true, allowEmptyValue=false,
            allowableValues="Date, Topic, Entity, Place, Organization, Person, Product, Attribute, Term, Keyword, Other")
    private Token.Type type;

    @ApiModelProperty(notes = "collection of strings providing additional hints about the token. This allows any string, but "
            + "well known hints include: start, end, from, to, via, at, depart, arrive, negated, instant", required = false)
    private Set<String> hints = new HashSet<>();

    /**
     * The confidence of the token. Provided by the component that extracted the token
     * from the {@link #getMessageIdx() message}
     */
    @ApiModelProperty(notes="the confidence for the token [0..1]")
    private float confidence;

    /**
     * Getter for the index of the {@link Message} within the {@link Conversation#getMessages()}
     * this token was extracted from
     * @return the index of the {@link Message} within the {@link Conversation#getMessages()}
     * this token was extracted from
     */
    public int getMessageIdx() {
        return messageIdx;
    }

    /**
     * Setter for the index of the {@link Message} within the {@link Conversation#getMessages()}
     * this token was extracted from
     * @param messageIdx the index of the {@link Message} within the {@link Conversation#getMessages()}
     * this token was extracted from
     */
    public void setMessageIdx(int messageIdx) {
        this.messageIdx = messageIdx;
    }

    /**
     * Getter for the start char offset of the Token within the {@link Message#getContent()}
     * @return the start char offset
     */
    public int getStart() {
        return start;
    }

    /**
     * Setter for the start char offset of the Token within the {@link Message#getContent()}
     * @param start the start char offset
     */
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * Getter for the end char offset of the Token within the {@link Message#getContent()}
     * @return the end char offset (exclusive) (similar to {@link String#substring(int, int)})
     */
    public int getEnd() {
        return end;
    }

    /**
     * Setter for the end char offset of the Token within the {@link Message#getContent()}
     * @param end the end char offset (exclusive) (similar to {@link String#substring(int, int)})
     */
    public void setEnd(int end) {
        this.end = end;
    }

    /**
     * Getter for the state of this token.
     * @return the state
     */
    public State getState() {
        return state;
    }

    /**
     * Setter for the state of this token.
     * @param state the state
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * Getter for the value of the token. The actual class of the token depends on
     * the {@link #getType()}. Expected types are provided by {@link Type#getValueClass()}
     * @return the value of the token
     */
    public Object getValue() {
        return value;
    }

    /**
     * Setter for the value of the token. The parsed value MUST match
     * the {@link Type#getValueClass()} of the {@link #getType()} of the token.
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Getter for the Type of the Token. If no type is defined {@link Token.Type#Unknown}
     * is returned.
     * @return the type of the Token. Guaranteed to NOT <code>null</code>.
     */
    public Type getType() {
        return type == null ? Token.Type.Other : type;
    }

    /**
     * Setter for the Type of the Token
     * @param type the type of the Token
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Getter for the set of hints of this token
     * @return the set of hints assigned to this token
     */
    public Set<String> getHints() {
        return hints;
    }
    /**
     * Checks if the parsed {@link Hint} is present for this
     * Token
     */
    public boolean hasHint(Hint hint){
        return hints.contains(hint == null ? null : hint.name());
    }
    /**
     * Checks if the passed hint is present for this
     * Token
     */
    public boolean hasHint(String hint){
        return hints.contains(hint);
    }
    /**
     * Setter for the set of hints of this token
     * @param hints the set of hints to replace existing
     * hints. <code>null</code> to clear any existing hints
     */
    public void setHints(Set<String> hints) {
        this.hints = hints == null ? new HashSet<>() : hints;
    }
    /**
     * Adds a hint to this token
     * @param hint the hint
     * @return <code>true</code> if the hint was added.
     * <code>false</code> if the hint was null, blank or
     * already present.
     */
    public boolean addHint(String hint){
        if(StringUtils.isNotBlank(hint)){
            return this.hints.add(hint);
        } else {
            return false;
        }
    }
    /**
     * Adds a well known {@link Hint} to this token
     * @param hint the hint
     * @return <code>true</code> if the hint was added.
     * <code>false</code> if the hint was null or already present.
     */
    public boolean addHint(Hint hint){
        if(hint != null){
            return this.hints.add(hint.name());
        } else {
            return false;
        }
    }
    
    /**
     * Removes a hint from the token
     * @param hint the hint to remove
     * @return <code>true</code> if the hint was removed. 
     * <code>false</code> if the hint was not present.
     */
    public boolean removeHint(String hint){
        return this.hints.remove(hint);
    }
    
    /**
     * Getter for the <code>[0..1]</code> confidence of this token.
     * @return the confidence
     */
    public float getConfidence() {
        return confidence;
    }

    /**
     * Setter for the <code>[0..1]</code> confidence of this token.
     * @param confidence the confidence
     */
    public void setConfidence(float confidence) {
        this.confidence = confidence > 1f ? 1f : confidence < 0f ? 0f : confidence;
    }

    /**
     * Getter for the {@link Origin} of the Token
     * @return the origin
     */
    public Origin getOrigin() {
        return origin;
    }
    /**
     * Setter for the {@link Origin} of the {@link Origin#System} is the 
     * default.
     * @param origin the Origin
     */
    public void setOrigin(Origin origin) {
        this.origin = origin == null ? Origin.System : origin;
    }
    
    @Override
    public int compareTo(Token o) {
        int c = 0;
        c = Integer.compare(messageIdx, o.messageIdx);
        if(c == 0){
            c = Integer.compare(start, o.start);
            if(c == 0){
                c = Integer.compare(o.end, end);
                if(c == 0){
                    c = Integer.compare(type == null ? -1 : type.ordinal(), o.type == null ? -1 : o.type.ordinal());
                    if(c == 0){
                        //compare based on value
                        if(value != null && o.value != null){
                            c = value.getClass().getName().compareTo(o.value.getClass().getName());
                            if(c == 0){
                                if(value instanceof Comparable){
                                    c = ((Comparable)value).compareTo(o.value);
                                } else {
                                    c = value.toString().compareTo(o.value.toString());
                                }
                            }
                        } else if(value != null || o.value != null){
                            c = value == null ? -1 : 1;
                        } //else both are null -> return equals
                    }
                }
            }
        }
        return c;
    }
    
    @Override
    public String toString() {
        return "Token [msgIdx=" + messageIdx + ", start=" + start + ", end=" + end + ", orign=" + origin 
                + ", type=" + type + ", value=" + value + ", conf=" + confidence + "]";
    }
    
    
}
