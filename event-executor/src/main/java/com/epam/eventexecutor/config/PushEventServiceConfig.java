package com.epam.eventexecutor.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class PushEventServiceConfig {

    private final int NUMBER_OF_EXECUTOR_THREADS = 8;

    @Bean(destroyMethod = "shutdownNow")
    @Qualifier("participantEventThreadExecutor")
    ExecutorService clientCachedThreadExecutor() {
        return Executors.newFixedThreadPool(NUMBER_OF_EXECUTOR_THREADS);
    }

}
