package io.redlink.smarti.cloudsync;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="smarti.index.sync")
public class ConversationCloudSyncConfiguration {

    public static final int DEFAULT_EPOCH_SIZE = 10000;
    public static final int MIN_EPOCH_SIZE = 100;
    public static final int DEFALUT_BATCH_SIZE = 1;
    
    private int epochSize = DEFAULT_EPOCH_SIZE;
    private int batchSize = DEFALUT_BATCH_SIZE;
    
    public int getEpochSize() {
        return epochSize;
    }
    public void setEpochSize(int epochSize) {
        this.epochSize = epochSize <= 0 ? DEFAULT_EPOCH_SIZE : 
                epochSize < MIN_EPOCH_SIZE ? MIN_EPOCH_SIZE : epochSize;
    }
    public int getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize <= 0 ? DEFALUT_BATCH_SIZE : batchSize;
    }
    
    @Override
    public String toString() {
        return "ConversationCloudSyncConfiguration [smarti.index.sync.epochSize=" + epochSize 
                + ", smarti.index.sync.batchSize=" + batchSize + "]";
    }
    
}
