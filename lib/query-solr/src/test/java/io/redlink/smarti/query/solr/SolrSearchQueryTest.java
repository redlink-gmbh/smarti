package io.redlink.smarti.query.solr;

import java.io.InputStream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.redlink.smarti.model.Query;
import org.junit.Assert;

public class SolrSearchQueryTest {
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testDeserialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream in = getClass().getClassLoader().getResourceAsStream("solrsearchquery.json");
        Assert.assertNotNull(in);
        Query query = mapper.readValue(in, Query.class);
        Assert.assertNotNull(query);
        log.debug("parsed (as Query): {}", query);
        Assert.assertTrue(query instanceof SolrSearchQuery);
    }
    
    
}
