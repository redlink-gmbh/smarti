package io.redlink.smarti.util;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;

import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Conversation;

public final class ConversationUtils {

    private ConversationUtils() {
        throw new IllegalStateException("do not use reflection to create instances of this class!");
    }

    public static void logConversation(Logger log, Conversation c, Analysis a) {
        logConversation(log, c);
        if(!log.isDebugEnabled()) return;
        if(a != null){
            if(a.getTokens() != null){
                log.debug(" > {} tokens:", a.getTokens().size());
                AtomicInteger count = new AtomicInteger(0);
                a.getTokens().forEach(t -> {
                    log.debug("    {}. {}",count.getAndIncrement(), t);
                });
            }
            if(a.getTemplates() != null){
                log.debug(" > {} templates:", a.getTemplates().size());
                AtomicInteger count = new AtomicInteger(0);
                a.getTemplates().forEach(t -> {
                    log.debug("    {}. {}", count.getAndIncrement(), t);
                    if (CollectionUtils.isNotEmpty(t.getQueries())) {
                        log.debug("    > with {} queries", t.getQueries().size());
                        t.getQueries().forEach(q -> log.debug("       - {}", q));
                    }
                });
            }
        }
    }
    public static void logConversation(Logger log, Conversation c) {
        if(!log.isDebugEnabled()) return;
        log.debug("Conversation[id:{} | owner: {} | modified: {}]", c.getId(), c.getOwner(),
                c.getLastModified() != null ? DateFormatUtils.ISO_DATETIME_FORMAT.format(c.getLastModified()) : "unknown");
        if(c.getUser() != null){
            log.debug(" > user[id: {}| name: {}] ", c.getUser().getId(), c.getUser().getDisplayName());
        }
        if(c.getMessages() != null){
            log.debug(" > {} messages:", c.getMessages().size());
            AtomicInteger count = new AtomicInteger(0);
            c.getMessages().forEach(m -> {
                log.debug("    {}. {} : {}",count.incrementAndGet(), m.getUser() == null ? m.getOrigin() : m.getUser().getDisplayName(), m.getContent());
            });
        }

    }
    
}
