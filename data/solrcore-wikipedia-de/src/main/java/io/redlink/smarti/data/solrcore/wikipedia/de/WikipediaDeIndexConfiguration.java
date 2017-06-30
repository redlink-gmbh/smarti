package io.redlink.smarti.data.solrcore.wikipedia.de;

import io.redlink.solrlib.SimpleCoreDescriptor;
import io.redlink.solrlib.SolrCoreDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@ConditionalOnProperty(name="solrcore.wikipedia.de.resource")
public class WikipediaDeIndexConfiguration {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public final static String WIKIPEDIA_DE = "wikipedia-de";
    

    @Value("${solrcore.wikipedia.de.resource}")
    private String wikipediaDeResource;
    
    @Bean(name=WIKIPEDIA_DE)
    protected SolrCoreDescriptor getWikipediaDeCoreDescriptor() throws IOException {
        log.info("init CoreDescriptor with resource '{}'", wikipediaDeResource);
        SimpleCoreDescriptor cd = new SimpleCoreDescriptor(WIKIPEDIA_DE, Paths.get(wikipediaDeResource)){
            @Override
            public void initCoreDirectory(Path coreDir, Path sharedLibDir) throws IOException {
                if(!Files.exists(coreDir.resolve("core.properties"))){
                    log.debug("copying wikipedia-de solr core to {}", coreDir);
                    super.initCoreDirectory(coreDir, sharedLibDir);
                } else { //TODO: check version based on version number in core.properties
                    log.info("using existing wikipedia-de solr core (if you want to update delete '{}' and restart)", coreDir);
                }
            }
        };
        return cd;
    }
    
    
}
