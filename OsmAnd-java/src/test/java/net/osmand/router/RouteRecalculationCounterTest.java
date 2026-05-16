package net.osmand.router;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests the lock-free counter pattern used in
 * RouteRecalculationHelper.isRouteBeingCalculated().
 *
 * Contract:
 *   - increment the counter on executor.submit(task)
 *   - decrement it in afterExecute() once the task completes
 *   - isRouteBeingCalculated() reads the counter without any lock
 */
public class RouteRecalculationCounterTest {

    private static class CountingExecutor extends ThreadPoolExecutor {
        final AtomicInteger active = new AtomicInteger(0);

        CountingExecutor() {
            super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        }

        @Override
        public Future<?> submit(Runnable task) {
            Future<?> f = super.submit(task);
            active.incrementAndGet();
            return f;
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            active.decrementAndGet();
        }

        boolean isBusy() { return active.get() > 0; }
    }

    @Test
    public void counterReturnsZeroBeforeAnyTaskSubmitted() {
        CountingExecutor ex = new CountingExecutor();
        Assert.assertFalse(ex.isBusy());
        Assert.assertEquals(0, ex.active.get());
        ex.shutdownNow();
    }

    @Test
    public void counterIncrementsOnSubmitAndDecrementsOnComplete() throws Exception {
        CountingExecutor ex = new CountingExecutor();
        CountDownLatch hold = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);

        Future<?> f = ex.submit(() -> {
            started.countDown();
            try { hold.await(); } catch (InterruptedException ignored) {}
        });

        Assert.assertTrue("worker must start", started.await(2, TimeUnit.SECONDS));
        Assert.assertTrue("isBusy must be true while task runs", ex.isBusy());
        Assert.assertEquals(1, ex.active.get());

        hold.countDown();
        f.get(2, TimeUnit.SECONDS);
        // afterExecute runs on the worker thread; give it a moment
        for (int i = 0; i < 100 && ex.isBusy(); i++) Thread.sleep(5);
        Assert.assertFalse("isBusy must be false after task completes", ex.isBusy());
        Assert.assertEquals(0, ex.active.get());
        ex.shutdownNow();
    }

    @Test
    public void counterTracksMultipleSerializedTasks() throws Exception {
        CountingExecutor ex = new CountingExecutor();
        int N = 5;
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            futures.add(ex.submit(() -> {
                try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            }));
        }
        // Right after submitting, we should see at least one active (could be N if queue holds them)
        Assert.assertTrue(ex.active.get() >= 1);
        Assert.assertTrue(ex.active.get() <= N);

        for (Future<?> f : futures) f.get(5, TimeUnit.SECONDS);
        for (int i = 0; i < 200 && ex.isBusy(); i++) Thread.sleep(5);
        Assert.assertFalse("all tasks done -> not busy", ex.isBusy());
        Assert.assertEquals(0, ex.active.get());
        ex.shutdownNow();
    }

    @Test
    public void counterReadIsLockFreeUnderConcurrentReaders() throws Exception {
        CountingExecutor ex = new CountingExecutor();
        CountDownLatch hold = new CountDownLatch(1);
        ex.submit(() -> { try { hold.await(); } catch (InterruptedException ignored) {} });

        // 8 reader threads polling isBusy() — must never block, must always see true while task running
        ExecutorService readers = Executors.newFixedThreadPool(8);
        AtomicInteger trueReads = new AtomicInteger(0);
        AtomicInteger totalReads = new AtomicInteger(0);
        long endNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(200);
        List<Future<?>> readerFutures = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            readerFutures.add(readers.submit(() -> {
                while (System.nanoTime() < endNanos) {
                    if (ex.isBusy()) trueReads.incrementAndGet();
                    totalReads.incrementAndGet();
                }
            }));
        }
        for (Future<?> f : readerFutures) f.get(5, TimeUnit.SECONDS);

        Assert.assertTrue("readers must complete (no deadlock)", totalReads.get() > 1000);
        Assert.assertEquals("all reads during running task must see isBusy=true",
                totalReads.get(), trueReads.get());

        hold.countDown();
        ex.shutdown();
        ex.awaitTermination(5, TimeUnit.SECONDS);
        readers.shutdownNow();
    }
}
