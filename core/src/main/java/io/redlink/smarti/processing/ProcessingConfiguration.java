package io.redlink.smarti.processing;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="smarti.processing")
public class ProcessingConfiguration {

    public  static final int DEFAULT_NUM_THREADS = 2;
    
    private int numThreads = DEFAULT_NUM_THREADS;
    
    private static final String THREAD_NAME = "smarti-processing-thread-%d";

    public final int getNumThreads() {
        return numThreads;
    }

    public final void setNumThreads(Integer numThreads) {
        this.numThreads = numThreads;
    }

    public ExecutorService createExecuterService(){
        return Executors.newFixedThreadPool(numThreads <= 0 ? DEFAULT_NUM_THREADS : numThreads, 
                new BasicThreadFactory.Builder()
                .daemon(true)
                .namingPattern(THREAD_NAME)
                .build());
    }
    
}
