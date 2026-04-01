package rx;

import rx.core.Disposable;
import rx.core.Observable;
import rx.core.Observer;
import rx.schedulers.Scheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class RxJavaTest {

    private List<Integer> results;
    private List<Throwable> errors;
    private AtomicInteger completeCount;

    @BeforeEach
    void setUp() {
        results = new ArrayList<>();
        errors = new ArrayList<>();
        completeCount = new AtomicInteger(0);
    }

    // ==================== Базовые компоненты ====================

    @Test
    void testObservableJust() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Observable.just(1, 2, 3, 4, 5)
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        results.add(item);
                    }

                    @Override
                    public void onError(Throwable t) {
                        errors.add(t);
                    }

                    @Override
                    public void onComplete() {
                        completeCount.incrementAndGet();
                        latch.countDown();
                    }
                });

        latch.await(1, TimeUnit.SECONDS);

        assertEquals(List.of(1, 2, 3, 4, 5), results);
        assertEquals(0, errors.size());
        assertEquals(1, completeCount.get());
    }

    @Test
    void testObservableFromIterable() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> source = List.of("a", "b", "c");

        Observable.fromIterable(source)
                .subscribe(s -> results.add(s.length()),
                        Throwable::printStackTrace,
                        latch::countDown);

        latch.await(1, TimeUnit.SECONDS);

        assertEquals(List.of(1, 1, 1), results);
    }

    // ==================== Операторы ====================

    @Test
    void testMapOperator() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Observable.just(1, 2, 3, 4, 5)
                .map(x -> x * 2)
                .subscribe(x -> results.add(x),
                        Throwable::printStackTrace,
                        latch::countDown);

        latch.await(1, TimeUnit.SECONDS);

        assertEquals(List.of(2, 4, 6, 8, 10), results);
    }

    @Test
    void testFilterOperator() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Observable.just(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .filter(x -> x % 2 == 0)
                .subscribe(x -> results.add(x),
                        Throwable::printStackTrace,
                        latch::countDown);

        latch.await(1, TimeUnit.SECONDS);

        assertEquals(List.of(2, 4, 6, 8, 10), results);
    }

    @Test
    void testMapAndFilterChain() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Observable.just(1, 2, 3, 4, 5)
                .map(x -> x * 10)
                .filter(x -> x > 20)
                .subscribe(x -> results.add(x),
                        Throwable::printStackTrace,
                        latch::countDown);

        latch.await(1, TimeUnit.SECONDS);

        assertEquals(List.of(30, 40, 50), results);
    }

    @Test
    void testFlatMapOperator() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Observable.just(1, 2, 3)
                .flatMap(x -> Observable.just(x, x * 10))
                .subscribe(x -> results.add(x),
                        Throwable::printStackTrace,
                        latch::countDown);

        latch.await(1, TimeUnit.SECONDS);

        // Порядок может быть разным из-за асинхронности
        assertEquals(6, results.size());
        assertTrue(results.containsAll(List.of(1, 10, 2, 20, 3, 30)));
    }

    // ==================== Планировщики ====================

    @Test
    void testSubscribeOnIOScheduler() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        Observable.just(1, 2, 3)
                .subscribeOn(Scheduler.io())
                .map(x -> {
                    threadName.set(Thread.currentThread().getName());
                    return x;
                })
                .subscribe(x -> results.add(x),
                        Throwable::printStackTrace,
                        latch::countDown);

        latch.await(2, TimeUnit.SECONDS);

        assertTrue(threadName.get().contains("IO-Thread"));
        assertEquals(3, results.size());
    }

    @Test
    void testObserveOnScheduler() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> computeThread = new AtomicReference<>();
        AtomicReference<String> observeThread = new AtomicReference<>();

        Observable.just(1, 2, 3)
                .map(x -> {
                    computeThread.set(Thread.currentThread().getName());
                    return x;
                })
                .observeOn(Scheduler.single())
                .subscribe(x -> {
                    observeThread.set(Thread.currentThread().getName());
                    results.add(x);
                }, Throwable::printStackTrace, latch::countDown);

        latch.await(2, TimeUnit.SECONDS);

        assertTrue(computeThread.get().contains("main"));
        assertTrue(observeThread.get().contains("Single"));
        assertEquals(3, results.size());
    }

    @Test
    void testSubscribeOnAndObserveOn() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> computeThread = new AtomicReference<>();
        AtomicReference<String> observeThread = new AtomicReference<>();

        Observable.just(1, 2, 3)
                .subscribeOn(Scheduler.computation())
                .map(x -> {
                    computeThread.set(Thread.currentThread().getName());
                    return x * 2;
                })
                .observeOn(Scheduler.single())
                .subscribe(x -> {
                    observeThread.set(Thread.currentThread().getName());
                    results.add(x);
                }, Throwable::printStackTrace, latch::countDown);

        latch.await(2, TimeUnit.SECONDS);

        assertTrue(computeThread.get().contains("Computation"));
        assertTrue(observeThread.get().contains("Single"));
        assertEquals(List.of(2, 4, 6), results);
    }

    // ==================== Обработка ошибок ====================

    @Test
    void testErrorHandling() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable.<Integer>create(observer -> {
                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onError(new RuntimeException("Test error"));
                })
                .subscribe(x -> results.add(x),
                        error::set,
                        latch::countDown);

        latch.await(1, TimeUnit.SECONDS);

        assertEquals(List.of(1, 2), results);
        assertNotNull(error.get());
        assertEquals("Test error", error.get().getMessage());
    }

    @Test
    void testOnErrorResumeNext() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Observable.<Integer>create(observer -> {
                    observer.onNext(1);
                    observer.onError(new RuntimeException("Error"));
                })
                .onErrorResumeNext(e -> Observable.just(2, 3))
                .subscribe(x -> results.add(x),
                        Throwable::printStackTrace,
                        latch::countDown);

        latch.await(1, TimeUnit.SECONDS);

        assertEquals(List.of(1, 2, 3), results);
    }

    // ==================== Disposable ====================

    @Test
    void testDisposable() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger(0);

        Disposable disposable = Observable.create(observer -> {
                    for (int i = 0; i < 100; i++) {
                        observer.onNext(i);
                        Thread.sleep(10);
                    }
                    observer.onComplete();
                })
                .subscribe(x -> {
                    counter.incrementAndGet();
                    if (counter.get() >= 5) {
                        Thread.currentThread().interrupt();
                    }
                }, Throwable::printStackTrace, latch::countDown);

        Thread.sleep(200);
        disposable.dispose();

        assertTrue(disposable.isDisposed());
        assertTrue(counter.get() < 100);
    }

    // ==================== Многопоточность ====================

    @Test
    void testConcurrentSubscriptions() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger sum = new AtomicInteger(0);

        Runnable task = () -> {
            Observable.just(1, 2, 3, 4, 5)
                    .subscribeOn(Scheduler.io())
                    .map(x -> x * 10)
                    .subscribe(x -> sum.addAndGet(x),
                            Throwable::printStackTrace,
                            latch::countDown);
        };

        new Thread(task).start();
        new Thread(task).start();
        new Thread(task).start();

        latch.await(5, TimeUnit.SECONDS);

        assertEquals(450, sum.get()); // (10+20+30+40+50) * 3 = 450
    }

    @Test
    void testPerformanceWithManyOperations() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger count = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        Observable.just(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .subscribeOn(Scheduler.computation())
                .map(x -> x * 2)
                .filter(x -> x > 10)
                .flatMap(x -> Observable.just(x, x * 10))
                .observeOn(Scheduler.io())
                .subscribe(x -> count.incrementAndGet(),
                        Throwable::printStackTrace,
                        latch::countDown);

        latch.await(5, TimeUnit.SECONDS);

        long duration = System.currentTimeMillis() - startTime;

        assertEquals(10, count.get()); // 5 элементов * 2 = 10
        assertTrue(duration < 3000);
    }
}