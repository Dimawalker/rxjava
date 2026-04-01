package examples;

import rx.core.Observable;
import rx.core.Disposable;
import rx.schedulers.Scheduler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RxJavaDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== RxJava Custom Implementation Demo ===\n");

        // Пример 1: Базовый поток
        System.out.println("1. Basic Observable:");
        Observable.just("Hello", "RxJava", "World!")
                .map(String::toUpperCase)
                .subscribe(
                        item -> System.out.println("  Received: " + item),
                        error -> System.err.println("  Error: " + error),
                        () -> System.out.println("  Complete!\n")
                );

        // Пример 2: Фильтрация
        System.out.println("2. Filtering numbers:");
        Observable.just(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .filter(n -> n % 2 == 0)
                .map(n -> n * 10)
                .subscribe(
                        item -> System.out.println("  " + item),
                        error -> System.err.println("  Error: " + error),
                        () -> System.out.println("  Complete!\n")
                );

        // Пример 3: FlatMap
        System.out.println("3. FlatMap example:");
        Observable.just("A", "B", "C")
                .flatMap(letter -> Observable.just(letter + "1", letter + "2"))
                .subscribe(
                        item -> System.out.println("  " + item),
                        error -> System.err.println("  Error: " + error),
                        () -> System.out.println("  Complete!\n")
                );

        // Пример 4: Многопоточность
        System.out.println("4. Multi-threading with Schedulers:");
        CountDownLatch latch1 = new CountDownLatch(1);

        Observable.just(1, 2, 3, 4, 5)
                .subscribeOn(Scheduler.computation())
                .map(x -> {
                    String thread = Thread.currentThread().getName();
                    System.out.println("  Map on: " + thread);
                    return x * 10;
                })
                .observeOn(Scheduler.single())
                .subscribe(
                        item -> {
                            String thread = Thread.currentThread().getName();
                            System.out.println("  Received " + item + " on: " + thread);
                        },
                        error -> System.err.println("  Error: " + error),
                        latch1::countDown
                );

        latch1.await(2, TimeUnit.SECONDS);
        System.out.println();

        // Пример 5: Обработка ошибок с восстановлением
        System.out.println("5. Error handling with recovery:");
        Observable.<String>create(observer -> {
                    observer.onNext("First");
                    observer.onNext("Second");
                    observer.onError(new RuntimeException("Something went wrong!"));
                    // Эта строка не выполнится из-за ошибки
                    observer.onNext("Third");
                })
                .onErrorResumeNext(error -> Observable.just("Fallback", "Recovery"))
                .subscribe(
                        item -> System.out.println("  " + item),
                        error -> System.err.println("  Error: " + error),
                        () -> System.out.println("  Complete!\n")
                );

        // Пример 6: Отмена подписки
        System.out.println("6. Disposable - cancel subscription:");
        Disposable disposable = Observable.create(observer -> {
                    try {
                        for (int i = 1; i <= 10; i++) {
                            if (observer instanceof rx.core.Disposable && ((rx.core.Disposable) observer).isDisposed()) {
                                break;
                            }
                            observer.onNext(i);
                            Thread.sleep(500);
                        }
                        observer.onComplete();
                    } catch (InterruptedException e) {
                        observer.onError(e);
                        Thread.currentThread().interrupt();
                    }
                })
                .subscribe(
                        item -> System.out.println("  Received: " + item),
                        error -> System.err.println("  Error: " + error),
                        () -> System.out.println("  Complete!")
                );

        Thread.sleep(2000);
        disposable.dispose();
        System.out.println("  Subscription cancelled after 2 seconds!\n");

        // Пример 7: Сложная цепочка операторов
        System.out.println("7. Complex pipeline:");
        Observable.just("apple", "banana", "cherry", "date", "elderberry")
                .filter(fruit -> fruit.length() > 5)
                .map(String::toUpperCase)
                .flatMap(fruit -> Observable.just(fruit, fruit + "-juice"))
                .subscribe(
                        item -> System.out.println("  " + item),
                        error -> System.err.println("  Error: " + error),
                        () -> System.out.println("  Complete!\n")
                );

        // Пример 8: Работа с коллекциями
        System.out.println("8. Working with collections:");
        java.util.List<Integer> numbers = java.util.Arrays.asList(1, 2, 3, 4, 5);

        Observable.fromIterable(numbers)
                .map(n -> n * n)
                .filter(n -> n > 10)
                .subscribe(
                        item -> System.out.println("  Square > 10: " + item),
                        error -> System.err.println("  Error: " + error),
                        () -> System.out.println("  Complete!\n")
                );

        // Пример 9: Асинхронная обработка с несколькими планировщиками
        System.out.println("9. Async processing with multiple schedulers:");
        CountDownLatch latch2 = new CountDownLatch(1);

        Observable.just(100, 200, 300, 400, 500)
                .subscribeOn(Scheduler.io())
                .map(n -> {
                    try {
                        System.out.println("  Processing " + n + " on: " + Thread.currentThread().getName());
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return n / 100;
                })
                .observeOn(Scheduler.computation())
                .map(n -> n * 10)
                .observeOn(Scheduler.single())
                .subscribe(
                        item -> System.out.println("  Result: " + item + " on: " + Thread.currentThread().getName()),
                        error -> System.err.println("  Error: " + error),
                        latch2::countDown
                );

        latch2.await(3, TimeUnit.SECONDS);
        System.out.println();

        // Пример 10: Комплексная обработка ошибок
        System.out.println("10. Advanced error handling:");
        Observable.just(1, 2, 3, 4, 5, 0, 6, 7)
                .map(n -> {
                    if (n == 0) {
                        throw new ArithmeticException("Division by zero");
                    }
                    return 100 / n;
                })
                .onErrorResumeNext(error -> {
                    System.out.println("  Recovered from: " + error.getMessage());
                    return Observable.just(1000, 2000);
                })
                .subscribe(
                        item -> System.out.println("  Result: " + item),
                        error -> System.err.println("  Error: " + error),
                        () -> System.out.println("  Complete!\n")
                );

        // Пример 11: Эмуляция асинхронного API
        System.out.println("11. Async API emulation:");
        CountDownLatch latch3 = new CountDownLatch(1);

        Observable.<String>create(observer -> {
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            observer.onNext("Async result 1");
                            Thread.sleep(500);
                            observer.onNext("Async result 2");
                            observer.onComplete();
                        } catch (InterruptedException e) {
                            observer.onError(e);
                        }
                    }).start();
                })
                .subscribeOn(Scheduler.io())
                .map(result -> result.toUpperCase())
                .observeOn(Scheduler.single())
                .subscribe(
                        item -> System.out.println("  " + item),
                        error -> System.err.println("  Error: " + error),
                        latch3::countDown
                );

        latch3.await(2, TimeUnit.SECONDS);
        System.out.println();

        // Пример 12: Параллельная обработка
        System.out.println("12. Parallel processing:");
        CountDownLatch latch4 = new CountDownLatch(1);
        long startTime = System.currentTimeMillis();

        Observable.just(1, 2, 3, 4, 5)
                .flatMap(n ->
                        Observable.just(n)
                                .subscribeOn(Scheduler.computation())
                                .map(x -> {
                                    try {
                                        Thread.sleep(500); // Симулируем тяжелую операцию
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                    return x * x;
                                })
                )
                .observeOn(Scheduler.single())
                .subscribe(
                        item -> System.out.println("  Square: " + item),
                        error -> System.err.println("  Error: " + error),
                        () -> {
                            long duration = System.currentTimeMillis() - startTime;
                            System.out.println("  All tasks completed in " + duration + "ms");
                            latch4.countDown();
                        }
                );

        latch4.await(2, TimeUnit.SECONDS);
        System.out.println();

        // Пример 13: Демонстрация работы с разными планировщиками
        System.out.println("13. Schedulers comparison:");
        System.out.println("  IO Scheduler: " + Scheduler.io().getClass().getSimpleName());
        System.out.println("  Computation Scheduler: " + Scheduler.computation().getClass().getSimpleName());
        System.out.println("  Single Scheduler: " + Scheduler.single().getClass().getSimpleName() + "\n");

        // Пример 14: Цепочка трансформаций с обработкой ошибок
        System.out.println("14. Transformation chain with error handling:");
        Observable.just(1, 2, 3, 4, 5)
                .map(x -> {
                    if (x == 3) {
                        throw new RuntimeException("Error at value 3");
                    }
                    return x * 10;
                })
                .onErrorResumeNext(error -> {
                    System.out.println("  Error caught: " + error.getMessage());
                    return Observable.just(30, 40, 50);
                })
                .filter(x -> x > 25)
                .subscribe(
                        item -> System.out.println("  Filtered: " + item),
                        error -> System.err.println("  Error: " + error),
                        () -> System.out.println("  Chain complete!\n")
                );

        System.out.println("=== Demo completed ===");
    }
}