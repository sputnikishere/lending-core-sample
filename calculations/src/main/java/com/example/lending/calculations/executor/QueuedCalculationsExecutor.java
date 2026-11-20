package com.example.lending.calculations.executor;

import com.example.lending.calculations.config.CalculationsProperties;
import com.example.lending.core.calculation.ICalculationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Calculations executor with an internal queue and a per-loan lock that prevents concurrent changes to the same loan.
 */
@Component
@Slf4j
public class QueuedCalculationsExecutor {

	private final BlockingQueue<TaskData>        asyncQueue = new LinkedBlockingQueue<>();
	private final Map<Long, ReentrantLock>       locks      = new ConcurrentHashMap<>();
	private final ExecutorService                asyncExecutor;
	private final int                            coreSize;
	private final long                           terminationTimeoutInSeconds;
	private final ICalculationService 			 calculationService;

	public QueuedCalculationsExecutor(ICalculationService calculationService, CalculationsProperties calculationsProperties) {
		this.asyncExecutor = Executors.newFixedThreadPool(calculationsProperties.getCoreSize());
		this.coreSize = calculationsProperties.getCoreSize();
		this.terminationTimeoutInSeconds = calculationsProperties.getTerminationTimeoutInSeconds();
		this.calculationService = calculationService;
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
	 * @param creditId loan identifier
	 * @param date     calculation date
	 */
	public void addToProcessQueue(Long creditId, LocalDate date) {
		asyncQueue.add(new TaskData(creditId, date));
	}

	/**
	 * Runs the calculation in the calling thread under a per-loan {@link ReentrantLock}.
	 *
	 * @param creditId loan identifier
	 * @param date     calculation date
	 * @return {@code true} - calculation succeeded; {@code false} - an error occurred
	 */
	public boolean executeUnderLock(Long creditId, LocalDate date) {
		ReentrantLock lock = locks.computeIfAbsent(creditId, id -> new ReentrantLock());
		lock.lock();
		try {
            calculationService.executeCalculation(creditId, date);
			return true;
		} catch (Exception e) {
			log.error("Calculation failed for id {}", creditId, e);
			return false;
		} finally {
			lock.unlock();
		}
	}

    /**
     * Returns the current size of the async task queue.
     *
     * @return number of tasks waiting to be processed
     */
    public Long getQueueSize() {
        return (long) asyncQueue.size();
    }

	private void processAsyncTasks() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				TaskData data = asyncQueue.take();
				executeUnderLock(data.creditId, data.date);
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
	 * @param creditId loan identifier
	 * @param date     calculation date
	 */
	public record TaskData(Long creditId, LocalDate date) {}
}
