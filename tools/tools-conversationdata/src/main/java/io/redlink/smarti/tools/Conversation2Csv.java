package io.redlink.smarti.tools;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Conversation2Csv {

    private static final Logger log = LoggerFactory.getLogger(Conversation2Csv.class);
    
    private static final String DEFAULT_CHARSET = "utf-8";
    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT
            .withFirstRecordAsHeader()
            .withDelimiter(',')
            .withNullString("")
            .withTrim()
            .withIgnoreEmptyLines();

    private static final Options options = new Options();

    private static final String DEFAULT_VERSION = "0.6.*";

    private static final String COLUMN_ID = "ID";
    private static final String COLUMN_USER = "User";
    private static final String COLUMN_STATUS = "Status";
    private static final String COLUMN_TIME = "Date";
    private static final String COLUMN_CHANNEL = "Channel";
    private static final String COLUMN_DOMAIN = "Domain";
    private static final String COLUMN_MSG_CONTENT = "Content";
    private static final String COLUMN_MSG_IDX = "Msg Idx";
    private static final String COLUMN_MSG_USER = "Sender";
    private static final String COLUMN_MSG_TIME = "Sent";
    private static final String COLUMN_MSG_ID = "Msg Id";
    
    static {
        
        options.addOption(Option.builder("v")
                .longOpt("version")
                .desc("The Smarti version of the export to be processed (default: "+DEFAULT_VERSION+")")
                .hasArg(true)
                .build());
        options.addOption(Option.builder("c")
                .longOpt("source-encoding")
                .desc("The charset used by the surce file (default: " + DEFAULT_CHARSET + ")")
                .hasArg(true)
                .build());
        options.addOption(Option.builder("i")
                .longOpt("incl-invalid-msg")
                .desc("if invalid messages should be included in the output")
                .hasArg(true)
                .build());
        options.addOption(Option.builder("n")
                .longOpt("incl-none-completed-conv")
                .desc("if none completed conversations should be included in the output")
                .hasArg(true)
                .build());
        
    }
    
    
    public static void main(String[] args) throws ParseException, IOException {
        CommandLineParser parser = new DefaultParser();
        // parse the command line arguments
        CommandLine line = parser.parse(options, args);
        
        if(line.getArgList().size() != 2){
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Conversation2Csv [options] <source> <target>", options );
            System.exit(1);
        }
        Path source = Paths.get(line.getArgList().get(0));
        if(!Files.exists(source)){
            log.error("parsed source file '{}' does not exist",source);
            System.exit(1);
        }
        Path target = Paths.get(line.getArgList().get(1));
        if(Files.exists(target)){
            Files.delete(target);
        }
        Files.createFile(target);
        ObjectMapper mapper = new ObjectMapper();
        
        JsonFactory jfactory = new JsonFactory();
        try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(target, Charset.forName(DEFAULT_CHARSET)),CSV_FORMAT)){
            printer.printRecord(COLUMN_ID, COLUMN_STATUS, COLUMN_USER, COLUMN_CHANNEL, COLUMN_DOMAIN, COLUMN_TIME, 
                    COLUMN_MSG_IDX, COLUMN_MSG_ID, COLUMN_MSG_USER, COLUMN_MSG_TIME, COLUMN_MSG_CONTENT);
            try (JsonParser jParser = jfactory.createParser(Files.newBufferedReader(source, Charset.forName(line.getOptionValue('c',DEFAULT_CHARSET))))){
                if(jParser.nextToken() != JsonToken.START_ARRAY){
                    throw new IOException("Source data are expected to use an Array as ROOT element (expected: " 
                            + JsonToken.START_ARRAY + ", found: " + jParser.getCurrentToken() + ")");
                }
                long exported = 0;
                long processed = 0;
                while (jParser.nextToken() != null) {
                    if(jParser.getCurrentToken() == JsonToken.END_ARRAY){
                        log.info("exported {}/{} conversations", exported, processed);
                    } else if(jParser.getCurrentToken() == JsonToken.START_OBJECT) {
                        processed++;
                        if(processConversation(line, mapper.readTree(jParser), printer, mapper)){
                            exported++;
                        }
                    } else {
                        log.error("Source data are expected to contain an Array containing only Conversations (expected: "
                                + JsonToken.START_OBJECT + ", found: " + jParser.getCurrentToken() + ")");
                        System.exit(2);
                    }
                }
            }
        } catch (final Throwable t) {
            try { //Delete incomplete target in case if an error
                Files.deleteIfExists(target);
            } catch (IOException e) { /* ignore */}
            throw t;
        }
    }
    
    private static boolean processConversation(CommandLine line, JsonNode conv, CSVPrinter printer, ObjectMapper mapper) throws IOException{
        String version = line.getOptionValue('v', DEFAULT_VERSION);
        boolean writeInvalid = line.hasOption('i');
        boolean writeNoneCompleted = line.hasOption('n');
        JsonNode idNode = conv.at("/id");
        if(idNode == null || idNode.isNull()){
            log.warn("skip conversation because 'id' node is missing or NULL");
            return false;
        }
        String cId = idNode.asText();
        log.trace("process conversation[id:{}]", cId);
        JsonNode messages = conv.get("messages");
        if(!messages.isArray()){
            log.warn("skip conversation because 'messages' node is missing or not an array (node: {})", messages);
            return false;
        }
        final String status = conv.at("/meta/status").asText(null);
        final String userId = conv.at("/user/id").asText(null);
        final String channelId = conv.at("/meta/channel_id/0").asText(null);
        final String domain = conv.at("/context/domain").asText(null);

        if(writeNoneCompleted || "Complete".equals(status)){
            int mIdx = 0;
            final List<MessageData> msgData = new LinkedList<>();
            //Collect the messages
            for(JsonNode message : messages){
                if(!message.isObject()){
                    log.warn("skip conversation[id: {}] because the {} message is not an object (node: {})", cId, mIdx, message);
                    return false;
                }
                msgData.add(parseMessage(version, mIdx++, message, mapper));
            }
            //find the time of the first message in the conversation (for easy sorting by date)
            Instant firstMessageTime = msgData.stream()
                    .filter(Objects::nonNull)
                    .map(md -> md.date)
                    .filter(Objects::nonNull)
                    .map(Date::toInstant)
                    .findFirst().orElse(null);
            //write information
            boolean messagePresent = false;
            for(MessageData msg : msgData){
                if(writeInvalid || msg.isValid()){
                    messagePresent = true;
                    //---- Conversation related columns (repeated for every messsage)
                    printer.print(cId); //conv id
                    printer.print(status); //status
                    printer.print(userId); //user of the conversation
                    printer.print(channelId); //channel id
                    printer.print(domain); // the domain
                    printer.print(firstMessageTime); //the date for the conversation
                    // ---- Message related columns
                    printer.print(msg.idx); //index of the message
                    printer.print(msg.id); //the id of the message
                    printer.print(msg.user); //the user that sent the message
                    printer.print(msg.date == null ? null : msg.date.toInstant().toString()); //the sent date
                    printer.print(msg.content); //the content
                    printer.println();
                }
            }
            if(!messagePresent){ //add conversation metadata for those without an message
                printer.print(cId); //conv id
                printer.print(status); //status
                printer.print(userId); //user of the conversation
                printer.print(channelId); //channel id
                printer.print(domain); // the domain
                printer.print(firstMessageTime); //the date for the conversation
                // ---- no message
                printer.print(null); //index of the message
                printer.print(null); //the id of the message
                printer.print(null); //the user that sent the message
                printer.print(null); //the sent date
                printer.print(null); //the content
                printer.println();

            }
            return true;
        } else { //ignored none completed conversation
            return false;
        }

    }
    
    private static MessageData parseMessage(String version, int idx, JsonNode message, ObjectMapper mapper) throws IOException{
        MessageData msg = new MessageData();
        msg.idx = idx;
        msg.content = message.at("/content").asText(null);
        msg.id = message.at("/id").asText(null);
        msg.user = message.at("/user/id").asText(null);
        JsonNode time = message.at("/time");
        if(time.isValueNode()){
            try {
                msg.date = mapper.readerFor(Date.class).readValue(time);
            } catch (JsonProcessingException e) {
                log.debug("Unable to parse Date from '" + time + "'", e);
            }
        }
        return msg;
    }
    
    private static class MessageData {
        int idx;
        String id;
        Date date;
        String content;
        String user;
        
        public boolean isValid(){
            return idx >= 0 && id != null && date != null && content != null && user != null;
        }
    }

}
