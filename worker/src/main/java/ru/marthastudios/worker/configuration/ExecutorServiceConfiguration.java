package ru.marthastudios.worker.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorServiceConfiguration {
    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(4);
    }
}
