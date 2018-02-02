package io.redlink.smarti;

import java.io.InputStream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Query;
import org.junit.Assert;

public class AnalysisDeserializationTest {
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testDeserialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream in = getClass().getClassLoader().getResourceAsStream("analysis.json");
        Assert.assertNotNull(in);
        Analysis analysis = mapper.readValue(in, Analysis.class);
        Assert.assertNotNull(analysis);
        log.debug("parsed : {}", analysis);
    }
    
    
}
