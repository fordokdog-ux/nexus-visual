package dev.simplevisuals.client.util.async;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * Shared lightweight CPU pool for background computations.
 * Strict rule: no Minecraft or OpenGL API calls off-thread.
 */
public final class Async {
    private Async() {}

    private static final ExecutorService CPU_POOL;
    private static final java.util.concurrent.atomic.AtomicInteger THREAD_ID = new java.util.concurrent.atomic.AtomicInteger(1);

    static {
        int cores = Math.max(2, Runtime.getRuntime().availableProcessors());
        int threads = Math.max(1, cores - 1); // leave room for the main thread
        CPU_POOL = new ThreadPoolExecutor(
                threads,
                threads,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1024),
                r -> {
                    Thread t = new Thread(r, "SV-CPU-" + THREAD_ID.getAndIncrement());
                    t.setDaemon(true);
                    t.setPriority(Thread.NORM_PRIORITY);
                    return t;
                },
                new ThreadPoolExecutor.DiscardOldestPolicy()
        );
    }

    public static <T> Future<T> submit(Callable<T> task) {
        Objects.requireNonNull(task, "task");
        return CPU_POOL.submit(task);
    }

    public static Future<?> run(Runnable task) {
        Objects.requireNonNull(task, "task");
        return CPU_POOL.submit(task);
    }

    public static <T> List<T> awaitAll(List<Future<T>> futures, long timeout, TimeUnit unit) {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        List<T> results = new ArrayList<>(futures.size());
        for (Future<T> f : futures) {
            long left = deadline - System.nanoTime();
            if (left <= 0) break;
            try {
                results.add(f.get(left, TimeUnit.NANOSECONDS));
            } catch (TimeoutException te) {
                break; // partial results
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException ee) {
                // skip failed tasks, continue
            }
        }
        return results;
    }

    public static int cpuCount() { return Math.max(1, Runtime.getRuntime().availableProcessors()); }
}
