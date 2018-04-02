package io.redlink.smarti.processing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;

//TODO: we should add code that allows to determine if the content-type of the message is Markdown!
@Component
public class MarkdownContentProcessor implements MessageContentProcessor {

    private final Pattern CODEBLOCK = Pattern.compile("[\\s]*[`]{3,}[\\s]*");
    
    @Override
    public String processMessageContent(ObjectId clientId, Conversation conversation, Message message) {
        String content = message.getContent();
        if(StringUtils.isBlank(content)){
            return content;
        }
        Matcher m = CODEBLOCK.matcher(content);
        StringBuilder processed = new StringBuilder();
        int idx = 0;
        int codeBlockIdx = 0;
        boolean inCodeBlock = false;
        while(m.find(codeBlockIdx)){
            if(inCodeBlock){
                idx = m.end();
                if(processed.length() > 0){
                    processed.append("\n\n");
                }
            } else {
                processed.append(content.substring(idx, m.start()));
                if(m.start() > 0 && Character.isAlphabetic(content.codePointAt(m.start()-1))){
                    processed.append('.'); //this helps NLP Processing to close the previouse sentence
                }
            }
            inCodeBlock = !inCodeBlock;
            codeBlockIdx = m.end();
        }
        if(!inCodeBlock){
            processed.append(content.substring(idx,content.length()));
        }
        return processed == null ? content : processed.toString();
    }

}
