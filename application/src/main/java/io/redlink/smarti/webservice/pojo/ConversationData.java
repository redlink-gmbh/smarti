package io.redlink.smarti.webservice.pojo;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Context;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.ConversationMeta;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.User;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Wraps a {@link Conversation} and optionally a {@link Analysis} for
 * JsonSerialization
 * @author Rupert Westenthaler
 *
 */
@ApiModel
public class ConversationData {

    @ApiModelProperty
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @ApiModelProperty(value = "metadata")
    private ConversationMeta meta = new ConversationMeta();

    @JsonProperty(required = true)
    @ApiModelProperty(required = true)
    private User user = new User();

    @ApiModelProperty(required = true, value = "List of Messages")
    private final List<Message> messages = new LinkedList<>();

    @ApiModelProperty(value = "conversation context")
    private Context context = new Context();

    @JsonInclude(Include.NON_NULL)
    private Analysis analysis;
    
    private Date lastModified = null;

    public static ConversationData fromModel(Conversation c){
        ConversationData cd = new ConversationData();
        cd.setContext(c.getContext());
        cd.setId(c.getId());
        cd.setLastModified(c.getLastModified());
        cd.setMeta(c.getMeta());
        cd.setUser(c.getUser());
        cd.getMessages().addAll(c.getMessages());
        //cd.setOwner(c.getOwner()) - owner is not sent to the client
        return cd;
    }
    
    public static Conversation toModel(ConversationData cd){
        Conversation c = new Conversation();
        c.setContext(cd.getContext());
        c.setId(cd.getId());
        c.setLastModified(cd.getLastModified());
        c.setMeta(cd.getMeta());
        c.setUser(cd.getUser());
        c.getMessages().addAll(cd.getMessages());
        return c;
    }
    
    public ObjectId getId() {
        return id;
    }
    
    public void setId(ObjectId id) {
        this.id = id;
    }
    
    public ConversationMeta getMeta() {
        return meta;
    }

    public void setMeta(ConversationMeta meta) {
        this.meta = meta;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Read-/writeable list of {@link Message}s
     * @return the messages of this conversation
     */
    public List<Message> getMessages() {
        return messages;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
    
    public Analysis getAnalysis() {
        return analysis;
    }
    
    public void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }

    @Override
    public String toString() {
        return "ConversationData [id=" + id + ",  user=" + user + ", lastModified=" + lastModified + ", " + messages.size() + " messages]";
    }
}
