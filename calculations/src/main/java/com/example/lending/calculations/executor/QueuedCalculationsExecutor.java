package com.example.lending.calculations.executor;

import com.example.lending.calculations.config.CalculationsProperties;
import com.example.lending.core.calculation.CalculationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Calculations executor with an internal queue and per-loan locking that prevents
 * concurrent changes to the same loan.
 * <p>
 * Locks are striped: a fixed array of locks indexed by loan id. Memory stays bounded no matter
 * how many loans pass through, while calculations for the same loan are always serialized.
 * Two different loans may occasionally share a stripe; that only costs a little parallelism.
 */
@Component
@Slf4j
public class QueuedCalculationsExecutor {

    private static final int LOCK_STRIPES = 1024;

    private final BlockingQueue<TaskData> asyncQueue = new LinkedBlockingQueue<>();
    private final ReentrantLock[] locks = new ReentrantLock[LOCK_STRIPES];
    private final ExecutorService asyncExecutor;
    private final int coreSize;
    private final long terminationTimeoutInSeconds;
    private final CalculationService calculationService;

    public QueuedCalculationsExecutor(CalculationService calculationService, CalculationsProperties calculationsProperties) {
        this.asyncExecutor = Executors.newFixedThreadPool(calculationsProperties.getCoreSize());
        this.coreSize = calculationsProperties.getCoreSize();
        this.terminationTimeoutInSeconds = calculationsProperties.getTerminationTimeoutInSeconds();
        this.calculationService = calculationService;
        for (int i = 0; i < LOCK_STRIPES; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    @PostConstruct
    public void init() {
        startAsyncWorkers();
    }

    @PreDestroy
    public void cleanup() {
        stopAsyncWorkers();
    }

    /**
     * Starts the pool's worker threads that drain the async task queue.
     */
    public void startAsyncWorkers() {
        for (int i = 0; i < coreSize; i++) {
            asyncExecutor.submit(this::processAsyncTasks);
        }
        log.info("Started {} async worker threads", coreSize);
    }

    /**
     * Initiates pool shutdown and waits for running tasks to complete.
     * Forces interruption once the timeout expires.
     */
    public void stopAsyncWorkers() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(terminationTimeoutInSeconds, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Adds a calculation task to the async queue.
     *
     * @param loanId loan identifier
     * @param date   calculation date
     */
    public void addToProcessQueue(Long loanId, LocalDate date) {
        asyncQueue.add(new TaskData(loanId, date));
    }

    /**
     * Runs the calculation in the calling thread under the loan's lock.
     *
     * @param loanId loan identifier
     * @param date   calculation date
     * @return {@code true} - calculation succeeded; {@code false} - an error occurred
     */
    public boolean executeUnderLock(Long loanId, LocalDate date) {
        ReentrantLock lock = lockFor(loanId);
        lock.lock();
        try {
            calculationService.executeCalculation(loanId, date);
            return true;
        } catch (Exception e) {
            log.error("Calculation failed for loan {}", loanId, e);
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return number of tasks waiting to be processed
     */
    public int getQueueSize() {
        return asyncQueue.size();
    }

    private ReentrantLock lockFor(Long loanId) {
        return locks[Math.floorMod(Long.hashCode(loanId), LOCK_STRIPES)];
    }

    private void processAsyncTasks() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                TaskData data = asyncQueue.take();
                executeUnderLock(data.loanId(), data.date());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Async worker error", e);
            }
        }
    }

    /**
     * A single calculation task.
     *
     * @param loanId loan identifier
     * @param date   calculation date
     */
    public record TaskData(Long loanId, LocalDate date) {}
}
