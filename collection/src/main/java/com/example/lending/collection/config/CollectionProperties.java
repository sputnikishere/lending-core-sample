package com.example.lending.collection.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Settings for queuing delinquent loans into collections and assigning cases.
 *
 * @param overdueDaysThreshold days past due from which a loan is handed to collectors
 * @param minOverdueAmount     minimum overdue balance for a loan to be queued
 * @param ingestionBatchSize   how many loans the ingestion job processes per pass
 * @param collectorsCacheTtl   time-to-live of the collector roster cache, in seconds
 */
@ConfigurationProperties(prefix = "app.collection")
public record CollectionProperties(
        Integer overdueDaysThreshold,
        BigDecimal minOverdueAmount,
        Integer ingestionBatchSize,
        Long collectorsCacheTtl
) {
}
