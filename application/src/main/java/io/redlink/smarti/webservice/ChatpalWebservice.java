package io.redlink.smarti.webservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import io.redlink.smarti.chatpal.index.ChatpalIndexConfiguration;
import io.redlink.smarti.chatpal.service.ChatpalMessageServcie;
import io.redlink.smarti.model.AuthToken;
import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.SmartiUser;
import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.model.config.Configuration;
import io.redlink.smarti.processor.keyword.intrestingterms.InterestingTermExtractor;
import io.redlink.smarti.services.*;
import io.redlink.smarti.utils.ResponseEntities;
import io.redlink.smarti.webservice.pojo.AuthContext;
import io.redlink.smarti.webservice.pojo.ConversationData;
import io.redlink.smarti.webservice.pojo.SmartiUserData;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import static io.redlink.smarti.chatpal.index.ChatpalIndexConfiguration.CHATPAL_INDEX;
import static io.redlink.smarti.chatpal.index.ChatpalIndexConfiguration.FIELD_CLIENT;
import static io.redlink.smarti.chatpal.index.ChatpalIndexConfiguration.FIELD_ID;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Server side services required by Chatpal
 * 
 * @author Rupert Westenthaler
 *
 */
@CrossOrigin
@RestController
@RequestMapping(value = "/chatpal")
//@Api(hidden=true) //this SHOULD NOT be used by other clients as Chatpal anyways!
public class ChatpalWebservice {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ChatpalMessageServcie chatpalService;

    private final SolrCoreContainer solrServer;

    private final SolrCoreDescriptor chatpalCore;

    private final AuthenticationService authenticationService;

    @Autowired
    public ChatpalWebservice(ChatpalMessageServcie chatpalService, SolrCoreContainer solrServer,
            @Qualifier(CHATPAL_INDEX) SolrCoreDescriptor chatpalCore, AuthenticationService authenticationService) {
        this.chatpalService = chatpalService;
        this.solrServer = solrServer;
        this.chatpalCore = chatpalCore;
        this.authenticationService = authenticationService;
    }

    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    public ResponseEntity<?> ping(AuthContext authContext) {
        Set<ObjectId> clients = authenticationService.getClientIds(authContext);
        final ObjectId client = clients.size() == 1 ? clients.iterator().next() : null;
        if (client != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntities.badRequest("Unable to determine client based on request");
        }
    }

    @RequestMapping(value = { "/update",
            "/update/json/docs" }, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> indexMessage(AuthContext authContext, @RequestBody Object data) {
        Set<ObjectId> clients = authenticationService.getClientIds(authContext);
        final ObjectId client = clients.size() == 1 ? clients.iterator().next() : null;
        if (client == null) {
            return ResponseEntities.badRequest("Unable to determine client based on request");
        }
        // we want to support a single element and also a list of elements
        List<Map<String, Object>> dataElements = new LinkedList<>();
        if (data instanceof Collection) {
            for (Object elem : (Collection<Object>) data) {
                if (elem instanceof Map) {
                    dataElements.add((Map<String, Object>) elem); // this is save as this originates from JSON
                } else {
                    return ResponseEntities
                            .badRequest("Unexpected data format (supported: objects and list of objects)!");
                }
            }
        } else if (data instanceof Map) {
            dataElements.add((Map<String, Object>) data); // this is save as this originates from JSON
        } else {
            return ResponseEntities.badRequest("Unexpected data format (supported: objects and list of objects)!");
        }
        dataElements.forEach(d -> chatpalService.store(client, d));
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/search", method = { RequestMethod.POST,
            RequestMethod.GET }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> search(AuthContext authContext, @RequestParam MultiValueMap<String, String> params)
            throws IOException, SolrServerException {
        Set<ObjectId> clients = authenticationService.getClientIds(authContext);
        final ObjectId client = clients.size() == 1 ? clients.iterator().next() : null;
        if (client == null) {
            return ResponseEntities.badRequest("Unable to determine client based on request");
        }
        log.debug("exec /search for params: {}", params);
        try (SolrClient solr = solrServer.getSolrClient(chatpalCore)) {
            SolrQuery query = new SolrQuery();
            params.entrySet().stream().forEach(e -> query.add(e.getKey(), e.getValue().toArray(new String[0])));
            query.set("qt","/chatpal/search"); //select the correct request handler config
            //#209: filter for the authenticated client
            query.addFilterQuery(String.format("%s:%s", FIELD_CLIENT, client.toHexString()));
            query.addField(FIELD_ID); //and make sure we have the internal ID field
            QueryResponse result = solr.query(query);
            log.trace("results: {}", result.getResponse());
            result.getResponse().remove("immutableCopy");
            return ResponseEntity.ok(asMap(result.getResponse(), idMappings(result), Integer.MAX_VALUE));
        }
    }

    private Map<String, String> idMappings(QueryResponse result) {
        Map<String, String> idMappings = new HashMap<>();
        idMappings(result.getResponse(), idMappings, Integer.MAX_VALUE);
        return idMappings;
    }

    private void idMappings(NamedList<?> nl, Map<String, String> mappings, int maxDepth) {
        int size = nl.size();
        for (int i = 0; i < size; i++) {
            Object val = nl.getVal(i);
            if (val instanceof NamedList && maxDepth > 0) {
                idMappings((NamedList) val, mappings, maxDepth - 1);
            } else if (val instanceof Collection) {
                ((Collection<?>) val).forEach(v -> {
                        if(v instanceof NamedList){
                            idMappings((NamedList)v, mappings, maxDepth);
                        } else if(v instanceof SolrDocument){
                            SolrDocument doc = (SolrDocument) v;
                            String id = (String)doc.getFieldValue(ChatpalIndexConfiguration.FIELD_ID);
                            String chatpalId = (String)doc.getFieldValue("id");
                            if(id != null && chatpalId != null){
                                mappings.put(id, chatpalId);
                            }
                        }
                    });
            } else if(val instanceof SolrDocument){
                SolrDocument doc = (SolrDocument) val;
                String id = (String)doc.getFieldValue(ChatpalIndexConfiguration.FIELD_ID);
                String chatpalId = (String)doc.getFieldValue("id");
                if(id != null && chatpalId != null){
                    mappings.put(id, chatpalId);
                }
            }
        }
    }

    /**
     * Converts a {@link NamedList} to a {@link Map} suitable for JSON
     * serialization by fixing an issue in the native
     * {@link NamedList#asMap(int)} implementation that does not process values
     * of {@link Collection}s used as values.
     * 
     * @param nl
     * @param maxDepth
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Map<String, Object> asMap(NamedList<?> nl, Map<String,String> idMappings, int maxDepth) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        int size = nl.size();
        for (int i = 0; i < size; i++) {
            Object val = nl.getVal(i);
            if (val instanceof NamedList && maxDepth > 0) {
                // the maxDepth check is to avoid stack overflow due to infinite
                // recursion
                val = asMap((NamedList) val, idMappings, maxDepth - 1);
            } else if (val instanceof SolrDocumentList) { // this is also a
                                                          // collection
                SolrDocumentList docList = (SolrDocumentList) val;
                Map<String, Object> docMap = new HashMap<>();
                docMap.put("numFound", docList.getNumFound());
                if (docList.getMaxScore() != null) {
                    docMap.put("maxScore", docList.getMaxScore());
                }
                docMap.put("start", docList.getStart());
                //filter also smarti specific fields for solr documents to NOT leak internal
                //information
                docList.forEach(doc -> {
                    doc.getFieldNames().stream()
                        .filter(name -> name.startsWith(ChatpalIndexConfiguration.SMARTI_FIELD_PREFIX))
                        // NOTE: collect first to avoid concurrent
                        // modification exceptions!
                        .collect(Collectors.toSet()).forEach(field -> doc.removeFields(field));
                });
                docMap.put("docs", docList);
                val = docMap;
            } else if (val instanceof Collection) {
                val = ((Collection<?>) val).stream()
                        .map(v -> v instanceof NamedList ? asMap((NamedList) v, idMappings, maxDepth) : v)
                        .collect(Collectors.toList());
            }
            String name = nl.getName(i);
            if(idMappings.containsKey(name)){
                name = idMappings.get(name);
            }
            Object old = result.put(name, val);
            if (old != null) {
                if (old instanceof List) {
                    List list = (List) old;
                    list.add(val);
                    result.put(nl.getName(i), old);
                } else {
                    ArrayList l = new ArrayList();
                    l.add(old);
                    l.add(val);
                    result.put(nl.getName(i), l);
                }
            }
        }
        return result;
    }

    @RequestMapping(value = "/clear", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> clear(AuthContext authContext, @RequestBody Map<String, Object> data) {
        Set<ObjectId> clients = authenticationService.getClientIds(authContext);
        final ObjectId client = clients.size() == 1 ? clients.iterator().next() : null;
        if (client == null) {
            return ResponseEntities.badRequest("Unable to determine client based on request");
        }
        if (data.containsKey("delete")) {
            Object cmd = data.get("delete");
            if (cmd instanceof String) {
                chatpalService.delete(client, (String) cmd);
                return ResponseEntity.ok().build();
            } else if (cmd instanceof Map && ((Map<String, Object>) cmd).containsKey("query")) {
                Object query = ((Map<String, Object>) cmd).get("query");
                if ("*:*".equals(query)) {
                    chatpalService.deleteClientMessages(client);
                    return ResponseEntity.ok().build();
                } else {
                    log.warn("Unsupported delete query '{}'", query);
                    return ResponseEntities
                            .badRequest("Only '*:*' query for delete requests supported (was: " + query + ")!");
                }
            } else {
                log.warn("Unsupported delete command '{}'", cmd);
                return ResponseEntities.badRequest(
                        "Only 'delete: { query: \"*:*\"}' and 'delete: \"<id>\"' supported (was: " + cmd + ")");
            }
        } else {
            log.warn("received 'POST /clear' request without 'delete' command (req data: {})", data);
            return ResponseEntities.badRequest("requests are expected to contain a 'delete command "
                    + "(supported: 'delete: { query: \"*:*\"}' and 'delete: \"<id>\"'");
        }
    }

}
