package io.redlink.smarti.data.solrcore.crawl.systel;

import io.redlink.solrlib.SimpleCoreDescriptor;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class CrawlSystelIndexConfiguration {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public final static String CRAWL_SYSTEL = "crawl-systel";
    

    @Value("${solrcore.crawl.systel.resource}")
    private String crawlSystelResource;
    
    @Bean(name=CRAWL_SYSTEL)
    @ConditionalOnProperty(name="solrcore.crawl.systel.resource")
    protected SolrCoreDescriptor getWikipediaDeCoreDescriptor() throws IOException {
        log.info("init CoreDescriptor with resource '{}'", crawlSystelResource);
        SimpleCoreDescriptor cd = new SimpleCoreDescriptor(CRAWL_SYSTEL, Paths.get(crawlSystelResource)){
            @Override
            public void initCoreDirectory(Path coreDir, Path sharedLibDir) throws IOException {
                if(!Files.exists(coreDir.resolve("core.properties"))){
                    log.debug("copying {} solr core to {}", CRAWL_SYSTEL, coreDir);
                    super.initCoreDirectory(coreDir, sharedLibDir);
                } else { //TODO: check version based on version number in core.properties
                    log.info("using existing {} solr core (if you want to update delete '{}' and restart)", CRAWL_SYSTEL, coreDir);
                }
            }
        };
        return cd;
    }
    
    
}
