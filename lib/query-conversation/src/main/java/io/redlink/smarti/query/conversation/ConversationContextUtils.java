package io.redlink.smarti.query.conversation;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.TreeSet;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;

public final class ConversationContextUtils {
    
    private static final long DEFAULT_GAP = 3*60*1000l;
    private static final Logger log = LoggerFactory.getLogger(ConversationContextUtils.class);
    
    private ConversationContextUtils(){
        throw new IllegalStateException("Do not use reflection to create instances of utility classes");
    }

    /**
     * Getter for the start index of the message context in cases where the current message is the
     * last message to be added to the conversation.
     * @param messages a list of messages typically coming from {@link Conversation#getMessages()}
     * @return the start index of the message considered to be within the context of the last message
     */
    public static int getContextStart(ConversationIndexerConfig config, List<Message> messages, int minContextLength, int contextLength, 
            int minInclMsgs, int maxInclMsgs, long minAge, long maxAge){
        if(messages.isEmpty()){
            return 0;
        }
        int inclMsgs = 0;
        Date contextDate = null;
        int contextSize = 0;
        for(ListIterator<Message> it = messages.listIterator(messages.size()); 
                it.hasPrevious();){
            int index = it.previousIndex();
            Message msg = it.previous();
            if(contextDate == null){
                contextDate = msg.getTime();
            }
            if(config.isMessageIndexed(msg)){
                if(contextSize < minContextLength || //force inclusion
                        inclMsgs < minInclMsgs || 
                        msg.getTime().getTime() >= contextDate.getTime() - minAge){
                    contextSize = contextSize + msg.getContent().length();
                    inclMsgs++;
                } else if(contextSize < contextLength && //allow include if more context is allowed
                        inclMsgs < maxInclMsgs && 
                        msg.getTime().getTime() >= contextDate.getTime() - maxAge){
                    contextSize = contextSize + msg.getContent().length();
                    inclMsgs++;
                } else {
                    return index; //we have enough content ... ignore previous messages
                }
            } //do not consider Messages skipped for analysis
        }
        return 0;
    }
    
    public static List<Integer> getContextSections(ConversationIndexerConfig config, Iterator<Message> messages, int offset, int minContextLength, int contextLength, int minMsgs, int maxInclMsgs) {
        int idx = offset;
        Queue<Long> gapsWindow = new CircularFifoQueue<Long>(10);
        Date lastDate = null;
        int contextSize = 0;
        int msgCount = 0;
        List<Integer> contextChnages = new LinkedList<>();
        Message prev = null;
        while(messages.hasNext()) {
            idx++;
            Message msg = messages.next();
            if(!config.isMessageIndexed(msg)){
                continue; //ignore messages that are not indexed
            }
            final long gap; 
            if(lastDate != null) {
                gap = msg.getTime().getTime() - lastDate.getTime();
            } else {
                gap = DEFAULT_GAP; //start with 3min
            }
            contextSize = contextSize + msg.getContent().length();
            if(!config.isMessageMerged(prev, msg)) {
                msgCount++;
            } //to not increase msg count for merged messages
            //TODO: test if merged messages should be also ignored for gapFactor calculation
            double meanGap = gapsWindow.isEmpty() ? gap : gapsWindow.stream().mapToLong(Long::longValue).sum()/(double)gapsWindow.size();
            double gapFactor = gap/meanGap;
            gapsWindow.add(gap);
            lastDate = msg.getTime();
            boolean clearGap = true;
            boolean cutSection = false;
            if(minMsgs > msgCount) {
                cutSection = false;
            } else if(minMsgs > msgCount && gapFactor > 10) {
                cutSection = true; 
            } else if(gapFactor > 5 && contextSize > minContextLength) {
                cutSection = true;
            } else if(gapFactor > 3 && contextSize > contextLength) {
                cutSection = true;
            } else if(msgCount >= maxInclMsgs) {
                clearGap = false;
                cutSection = true;
            }

            if(cutSection) {
                if(log.isDebugEnabled()) {
                    log.debug(" -> cut before idx: {} (after {} message and {} chars with gapFactor {})", idx, msgCount, contextSize, gapFactor);
                    log.debug("idx: {} - {} | gapFactor: {}, numMsg: {}, contextSize: {}", idx, StringUtils.abbreviate(msg.getContent(), 30), gapFactor, msgCount, contextSize);
                }
                contextChnages.add(idx);
                if(clearGap) {
                    gapsWindow.clear();
                    gapsWindow.add(DEFAULT_GAP); //init with the default Gap
                }
                msgCount = 0;
                contextSize = 0;
            } else if(log.isDebugEnabled()){
                log.debug("idx: {} - {} | gapFactor: {}, numMsg: {}, contextSize: {}", idx, StringUtils.abbreviate(msg.getContent(), 30), gapFactor, msgCount, contextSize);
            }
            prev = msg;
        }
        return contextChnages;
    }
    
    /**
     * Getter for the context of a message 
     * @param messages the messages
     * @param contextIdx the index of the message within messages the context needs to be determined
     * @param minContextLength the minimum desired length of the context (in chars)
     * @param contextLength the desired length of the context
     * @param minInclMsgs the minimum number of included messages
     * @param minInclBefore the minimum number of messages to be included before the current message
     * @param minInclAfter the minimum number of messages to be included after the current message
     * @param minAge the minimum age of messages to be included (in ms)
     * @param maxAge the maximum age of message to be included (in ms)
     * @return the <code>[{start},{end}]</code> (both end including) - for <code>messages.sublist(..)</code> use <code>{end} + 1</code>
     */
    public static int[] getMessageContext(List<Message> messages, int contextIdx, int minContextLength, int contextLength, 
            int minInclMsgs, int maxInclMsgs, int minInclBefore, int minInclAfter, long minAge, long maxAge){
        log.debug(" >> calc msgCtx: {} --", contextIdx);
        if(messages == null){
            return null;
        }
        //special case - very short message list
        if(messages.size() <= minInclMsgs){
            return new int[]{0,messages.size()};
        }
        //special case - contextIdx is the last message (or out of bounds)
        //if(contextIdx >= messages.size() -1){
        //    return new int[]{getContextStart(messages, minContextLength, contextLength, minInclMsgs, maxInclMsgs, minAge, maxAge), messages.size()};
        //}
        //general case
        Message context = messages.get(contextIdx);
        Date contextDate = context.getTime();
        Date followingDate = contextDate;
        Date previousDate = contextDate;
        int contextSize = 0;
        ListIterator<Message> pIt = messages.listIterator(contextIdx);
        ListIterator<Message> fIt = messages.listIterator(contextIdx);
        Message following = null;
        Message previous = null;
        NavigableSet<Integer> inclIdxs = new TreeSet<>();
        boolean backwardState = true;
        boolean forwardState = true;
        while((backwardState && pIt.hasPrevious()) || (forwardState && fIt.hasNext())){
            if(following == null){
                following = fIt.hasNext() && forwardState ? fIt.next() : null;
            }
            if(previous == null){
                previous = pIt.hasPrevious() && backwardState ? pIt.previous() : null;
            }
            long fAge = following == null ? Long.MAX_VALUE : following.getTime().getTime() - contextDate.getTime();
            long pAge = previous == null ? Long.MAX_VALUE : contextDate.getTime() - previous.getTime().getTime();
            boolean forward = fAge <= pAge;
            long delta = Math.min(fAge, pAge); //delta to the context date
            long dif; //delta to the prev message
            int idx;
            Message msg = null;
            if(forward){
                msg = following;
                dif = msg.getTime().getTime() - followingDate.getTime();
                followingDate = msg.getTime();
                following = null;
                idx = fIt.nextIndex() - 1;
            } else {
                msg = previous;
                dif = previousDate.getTime() - msg.getTime().getTime();
                previousDate = msg.getTime();
                followingDate = msg.getTime();
                previous = null;
                idx = pIt.previousIndex() + 1;
            }
            boolean mustState = contextSize < minContextLength || //force inclusion
                    inclIdxs.size() < minInclMsgs || 
                    delta <= minAge;
            
            float timeGapScore = delta == 0 ? 0 : (dif * (inclIdxs.size()+1)) / (float)delta;
            if(timeGapScore > (mustState ? 5 : 3)) {
                if(forward) {
                    log.debug("stop forward search because timeGapScore: {}", timeGapScore);
                    forwardState = false;
                } else {
                    log.debug("stop backward search because timeGapScore: {}", timeGapScore);
                    backwardState = false;
                }
            } else { //consider token
                if(mustState){
                    contextSize = contextSize + msg.getContent().length();
                    if(log.isDebugEnabled()) {
                        log.debug("{} add {} - {}", forward ? "forward" : "backward", idx, StringUtils.abbreviate(msg.getContent(), 20));
                    }
                    inclIdxs.add(idx);
                } else if(contextSize < contextLength && //allow include if more context is allowed
                        inclIdxs.size() < maxInclMsgs && 
                        delta <= maxAge){
                    if(log.isDebugEnabled()) {
                        log.debug("{} add {} - {}", forward ? "forward" : "backward" ,  idx, StringUtils.abbreviate(msg.getContent(), 20));
                    }
                    inclIdxs.add(idx);
                } else { //we are done!
                    if(log.isDebugEnabled()) {
                        log.debug("done [ctxSize: {}| msgCount: {} | age: {}]]",contextSize < contextLength, inclIdxs.size() < maxInclMsgs, delta <= maxAge);
                    }
                    break;
                }
            }
        }
        log.debug("<< idxs: {} --", inclIdxs);
        return new int[]{
                Math.min(inclIdxs.first(), Math.max(0, contextIdx - minInclBefore)),
                Math.max(inclIdxs.last(), Math.min(messages.size() - 1, contextIdx + minInclAfter))};
    };
    
    
}
