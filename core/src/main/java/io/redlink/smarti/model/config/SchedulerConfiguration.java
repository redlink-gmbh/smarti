/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.model.config;

import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@ConfigurationProperties(prefix = "scheduler")
public class SchedulerConfiguration {

    private int poolSize = 1;

    @Bean
    public TaskScheduler taskScheduler() {
        LoggerFactory.getLogger(SchedulerConfiguration.class).debug("Create TaskScheduler with poolSize {}", poolSize);

        final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

        taskScheduler.setPoolSize(poolSize);

        return taskScheduler;
    }

}
