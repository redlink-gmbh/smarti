package io.redlink.smarti.query.conversation;

import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableSet;
import java.util.TreeSet;

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;

public final class ConversationContextUtils {
    
    private ConversationContextUtils(){
        throw new IllegalStateException("Do not use reflection to create instances of utility classes");
    }

    /**
     * Getter for the start index of the message context in cases where the current message is the
     * last message to be added to the conversation.
     * @param messages a list of messages typically coming from {@link Conversation#getMessages()}
     * @return the start index of the message considered to be within the context of the last message
     */
    public static int getContextStart(List<Message> messages, int minContextLength, int contextLength, 
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
            if(contextSize < minContextLength || //force inclusion
                    inclMsgs < minInclMsgs || 
                    msg.getTime().getTime() >= contextDate.getTime() - minAge){
                contextSize = contextSize + msg.getContent().length();
            } else if(contextSize < contextLength && //allow include if more context is allowed
                    inclMsgs < maxInclMsgs && 
                    msg.getTime().getTime() >= contextDate.getTime() - maxAge){
                contextSize = contextSize + msg.getContent().length();
            } else {
                return index; //we have enough content ... ignore previous messages
            }
        }
        return 0;
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
        if(messages == null){
            return null;
        }
        //special case - very short message list
        if(messages.size() <= minInclMsgs){
            return new int[]{0,messages.size()};
        }
        //special case - contextIdx is the last message (or out of bounds)
        if(contextIdx >= messages.size() -1){
            return new int[]{getContextStart(messages, minContextLength, contextLength, minInclMsgs, maxInclMsgs, minAge, maxAge), messages.size()};
        }
        //general case
        Message context = messages.get(contextIdx);
        Date contextDate = context.getTime();
        int contextSize = 0;
        ListIterator<Message> pIt = messages.listIterator(contextIdx);
        ListIterator<Message> fIt = messages.listIterator(contextIdx);
        Message following = null;
        Message previouse = null;
        NavigableSet<Integer> inclIdxs = new TreeSet<>();
        while(pIt.hasPrevious() || fIt.hasNext()){
            if(following == null){
                following = fIt.hasNext() ? fIt.next() : null;
            }
            if(previouse == null){
                previouse = pIt.hasPrevious() ? pIt.previous() : null;
            }
            long fAge = following == null ? Long.MAX_VALUE : following.getTime().getTime() - contextDate.getTime();
            long pAge = previouse == null ? Long.MAX_VALUE : contextDate.getTime() - previouse.getTime().getTime();
            boolean forward = fAge <= pAge;
            long delta = Math.min(fAge, pAge);
            int idx;
            Message msg = null;
            if(forward){
                msg = following;
                following = null;
                idx = fIt.nextIndex() - 1;
            } else {
                msg = previouse;
                previouse = null;
                idx = pIt.previousIndex() + 1;
            }
            if(contextSize < minContextLength || //force inclusion
                    inclIdxs.size() < minInclMsgs || 
                    delta <= minAge){
                contextSize = contextSize + msg.getContent().length();
                inclIdxs.add(idx);
            } else if(contextSize < contextLength && //allow include if more context is allowed
                    inclIdxs.size() < maxInclMsgs && 
                    delta <= maxAge){
                inclIdxs.add(idx);
            } else { //we are done!
                break;
            }
        }
        return new int[]{
                Math.min(inclIdxs.first(), Math.max(0, contextIdx - minInclBefore)),
                Math.max(inclIdxs.last(), Math.min(messages.size() - 1, contextIdx + minInclAfter))};
    };
    
    
}
