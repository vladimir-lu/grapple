package org.halfway.grapple.util;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Iterators;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * A builder for different types of {@link java.util.concurrent.ExecutorService}
 * </p>
 * Note that this class is not thread-safe as it is mutable.
 */
public class ExecutorServiceBuilder {
    private static final Logger logger = Logger.getLogger(ExecutorServiceBuilder.class.getName());

    private Optional<Integer> poolSize = Optional.absent();
    private Optional<ThreadFactoryBuilder> threadFactoryBuilder = Optional.absent();

    public static <V> Iterator<ListenableFuture<V>> submitTasks(final ListeningExecutorService service, Iterator<Callable<V>> tasks) {
        return Iterators.transform(tasks, new Function<Callable<V>, ListenableFuture<V>>() {
            @Override
            public ListenableFuture<V> apply(Callable<V> task) {
                return service.submit(task);
            }
        });
    }

    public ExecutorServiceBuilder withPoolSize(final Optional<Integer> poolSize) {
        Verify.verify(poolSize.or(1) > 0, "pool size must be positive");
        this.poolSize = poolSize;
        return this;
    }

    public ExecutorServiceBuilder withThreadFactoryBuilder(final Optional<ThreadFactoryBuilder> threadFactoryBuilder) {
        this.threadFactoryBuilder = threadFactoryBuilder;
        return this;
    }

    private ExecutorService newCachedThreadPool() {
        logger.fine("creating new cached thread pool (unbounded)");
        if (!threadFactoryBuilder.isPresent()) {
            return Executors.newCachedThreadPool();
        } else {
            return Executors.newCachedThreadPool(threadFactoryBuilder.get().build());
        }
    }

    private ExecutorService newFixedThreadPool(final int nThreads) {
        logger.fine("creating new thread pool with " + nThreads + " threads");
        if (!threadFactoryBuilder.isPresent()) {
            return Executors.newFixedThreadPool(nThreads);
        } else {
            return Executors.newFixedThreadPool(nThreads, threadFactoryBuilder.get().build());
        }
    }

    public ListeningExecutorService newListeningExecutorService() {
        final ExecutorService service;
        if (!poolSize.isPresent()) {
            service = newCachedThreadPool();
        } else {
            service = newFixedThreadPool(poolSize.get());
        }
        return MoreExecutors.listeningDecorator(service);
    }
}
