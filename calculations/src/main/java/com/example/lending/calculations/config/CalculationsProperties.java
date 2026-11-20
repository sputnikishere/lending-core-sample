package com.example.lending.calculations.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Thread pool settings of the queued calculations executor.
 */
@Configuration
@ConfigurationProperties(prefix = "app.queued-calculations-worker")
@Getter
@Setter
public class CalculationsProperties {

    /** Number of worker threads. */
    private int coreSize;

    /** How long to wait for in-flight tasks on shutdown (seconds). */
    private int terminationTimeoutInSeconds;
}
