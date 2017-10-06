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


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Useful metadata about a conversation
 */
@ApiModel
public class ConversationMeta {

    public enum Status {
        New,
        Ongoing,
        Complete
    }

    public enum AgentFeedback {
        Perfect,
        Helpful,
        NotUsed,
        Useless,
        Unknown
    }

    @ApiModelProperty("conversation status")
    private Status status = Status.New;

    @ApiModelProperty(value = "message offset", notes = "offset for the next analysis iteration")
    private int lastMessageAnalyzed = -1;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @ApiModelProperty(value = "conversation tags", notes = "tags added by the agent when completing the conversation")
    @Indexed
    private Set<String> tags = new HashSet<>();

    @ApiModelProperty(value = "could the knowledgebase help in this conversation?", notes = "feedback by the agent if the knowledgebase was helpful to serve the user. Only valid if status == Complete")
    private AgentFeedback feedback = null;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getLastMessageAnalyzed() {
        return lastMessageAnalyzed;
    }

    public void setLastMessageAnalyzed(int lastMessageAnalyzed) {
        this.lastMessageAnalyzed = lastMessageAnalyzed;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(String... tags) {
        setTags(Arrays.asList(tags));
    }

    @JsonSetter
    public void setTags(Collection<String> tags) {
        this.tags = new HashSet<>(tags);
    }

    public void addTag(String tag) {
        this.tags.add(tag);
    }

    public void clearTags() {
        this.tags.clear();
    }

    public AgentFeedback getFeedback() {
        return feedback;
    }

    public void setFeedback(AgentFeedback feedback) {
        this.feedback = feedback;
    }
}
