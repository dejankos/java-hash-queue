import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.lang.Runtime.getRuntime;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LinkedBlockingHashQueueConcurrentTest {

    @Test
    @DisplayName("should wait until an element becomes available to take")
    public void waitForTake() throws InterruptedException {
        var queue = new LinkedBlockingHashQueue<Integer>(1);
        var singleThread = Executors.newSingleThreadExecutor();

        AtomicInteger i = new AtomicInteger(0);
        AtomicBoolean flag = new AtomicBoolean(false);
        CompletableFuture.runAsync(
                () -> {
                    try {
                        flag.set(true);
                        var e = queue.take();
                        i.set(e);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                },
                singleThread
        );

        await()
                .atMost(ofSeconds(2L))
                .until(flag::get);

        queue.put(42);

        await()
                .atMost(ofSeconds(2L))
                .until(() -> i.get() == 42);

        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("should wait until an element can be put")
    public void waitForPut() throws InterruptedException {
        var queue = new LinkedBlockingHashQueue<Integer>(1);
        var singleThread = Executors.newSingleThreadExecutor();

        // max capacity reached
        queue.put(1);

        AtomicInteger i = new AtomicInteger(0);
        AtomicBoolean flag = new AtomicBoolean(false);
        CompletableFuture.runAsync(
                () -> {
                    try {
                        flag.set(true);
                        queue.put(42);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                },
                singleThread
        );

        await()
                .atMost(ofSeconds(2L))
                .until(flag::get);

        assertEquals(1, queue.take());

        await()
                .atMost(ofSeconds(2L))
                .until(() -> queue.size() == 1);

        assertEquals(42, queue.take());
        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("should concurrently produce to queue while consuming in single thread")
    public void consumerProducer() throws InterruptedException {
        var count = 10_000;
        var queue = new LinkedBlockingHashQueue<Integer>(count);
        var fkPool = new ForkJoinPool(threadCount());

        CompletableFuture.runAsync(() -> {
            IntStream.range(0, count)
                    .boxed()
                    .parallel()
                    .forEach(i -> {
                        var offer = queue.offer(i);
                        assertTrue(offer);
                    });
        }, fkPool);

        var collect = new LinkedList<Integer>();
        var consumed = 0;
        while (consumed < count) {
            Integer e = queue.take();
            collect.add(e);
            consumed++;
        }
        collect.sort(Integer::compareTo);

        assertTrue(queue.isEmpty());
        for (int i = 0; i < count; i++) {
            assertEquals(i, collect.get(i));
        }
    }

    @Test
    @DisplayName("should concurrently produce to queue and consume from queue")
    public void multiThreadConsumerProducer() {
        var count = 10_000;
        var consumers = 4;

        var queue = new LinkedBlockingHashQueue<Integer>(count);
        var es = newFixedThreadPool(threadCount());
        final List<Integer> collect = new CopyOnWriteArrayList<>();

        IntStream.range(0, consumers + 1)
                .forEach(i -> {
                    es.submit(() -> {
                        var consumed = 0;
                        while (consumed <= count / consumers) {
                            try {
                                var e = queue.take();
                                collect.add(e);
                                consumed++;
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    });
                });


        var fkPool = new ForkJoinPool(threadCount());
        CompletableFuture.runAsync(() -> {
            IntStream.range(0, count)
                    .boxed()
                    .parallel()
                    .forEach(i -> {
                        var offer = queue.offer(i);
                        assertTrue(offer);
                    });
        }, fkPool);


        await()
                .atMost(ofSeconds(5L))
                .until(() -> collect.size() == count);

        collect.sort(Integer::compareTo);
        assertTrue(queue.isEmpty());

        for (int i = 0; i < count; i++) {
            assertEquals(i, collect.get(i));
        }
    }

    private int threadCount() {
        return Math.max(1, getRuntime().availableProcessors() / 2);
    }
}
