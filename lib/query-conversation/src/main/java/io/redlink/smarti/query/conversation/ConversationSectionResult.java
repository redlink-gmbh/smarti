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
package io.redlink.smarti.query.conversation;

import io.redlink.smarti.model.result.Result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Jakob Frank
 * @author Rupert Westenthaler
 * @since 10.02.17
 */
public class ConversationSectionResult extends Result {

    private double score;
    private String conversationId; 
    private List<String> messageIds;
    private List<Long> messageIdxs;
    private String userName;
    private int votes;
    private Date timestamp;
    private List<SectionMessage> section = new ArrayList<>();

    public ConversationSectionResult(String creator) {
        super(creator);
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }


    public List<String> getMessageIds() {
        return messageIds;
    }
    
    public void setMessageIds(List<String> messageIds) {
        this.messageIds = messageIds;
    }
    
    public List<Long> getMessageIdxs() {
        return messageIdxs;
    }
    
    public void setMessageIdxs(List<Long> messageIdxs) {
        this.messageIdxs = messageIdxs;
    }
    
    public int getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public List<SectionMessage> getSection() {
        return section;
    }
    
    public void setSection(List<SectionMessage> section) {
        this.section = section;
    }
    
    public static class SectionMessage {
        
        private String content;
        private Collection<String> messageIds;
        private Collection<Long> messageIdxs;
        private String userName;
        private int votes;
        private Date timestamp;
        
        public String getContent() {
            return content;
        }
        public void setContent(String content) {
            this.content = content;
        }
        public Collection<String> getMessageIds() {
            return messageIds;
        }
        public void setMessageIds(Collection<String> messageIds) {
            this.messageIds = messageIds;
        }
        public Collection<Long> getMessageIdxs() {
            return messageIdxs;
        }
        public void setMessageIdxs(Collection<Long> messageIdxs) {
            this.messageIdxs = messageIdxs;
        }
        public String getUserName() {
            return userName;
        }
        public void setUserName(String userName) {
            this.userName = userName;
        }
        public int getVotes() {
            return votes;
        }
        public void setVotes(int votes) {
            this.votes = votes;
        }
        public Date getTimestamp() {
            return timestamp;
        }
        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }
        
        
    }
}
