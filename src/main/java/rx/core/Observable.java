package rx.core;

import rx.functions.Function;
import rx.functions.Predicate;
import rx.operators.FilterOperator;
import rx.operators.FlatMapOperator;
import rx.operators.MapOperator;
import rx.schedulers.Scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Реактивный поток данных
 * @param <T> тип элементов потока
 */
public class Observable<T> {

    private final OnSubscribe<T> onSubscribe;

    /**
     * Функциональный интерфейс для подписки
     */
    public interface OnSubscribe<T> {
        void subscribe(Observer<T> observer);
    }

    /**
     * Конструктор Observable
     * @param onSubscribe функция подписки
     */
    private Observable(OnSubscribe<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }

    /**
     * Создает Observable из функции подписки
     * @param onSubscribe функция подписки
     * @param <T> тип элементов
     * @return новый Observable
     */
    public static <T> Observable<T> create(OnSubscribe<T> onSubscribe) {
        return new Observable<>(onSubscribe);
    }

    /**
     * Создает Observable из элементов
     * @param items элементы
     * @param <T> тип элементов
     * @return Observable
     */
    @SafeVarargs
    public static <T> Observable<T> just(T... items) {
        return create(observer -> {
            try {
                for (T item : items) {
                    observer.onNext(item);
                }
                observer.onComplete();
            } catch (Exception e) {
                observer.onError(e);
            }
        });
    }

    /**
     * Создает Observable из итерируемого объекта
     * @param iterable итерируемый объект
     * @param <T> тип элементов
     * @return Observable
     */
    public static <T> Observable<T> fromIterable(Iterable<T> iterable) {
        return create(observer -> {
            try {
                for (T item : iterable) {
                    observer.onNext(item);
                }
                observer.onComplete();
            } catch (Exception e) {
                observer.onError(e);
            }
        });
    }

    /**
     * Подписывает Observer на события
     * @param observer наблюдатель
     * @return Disposable для отмены подписки
     */
    public Disposable subscribe(Observer<T> observer) {
        Disposable disposable = new DefaultDisposable();
        try {
            onSubscribe.subscribe(new SafeObserver<>(observer, disposable));
        } catch (Exception e) {
            observer.onError(e);
        }
        return disposable;
    }

    /**
     * Упрощенная подписка с лямбдами
     * @param onNext обработчик элементов
     * @param onError обработчик ошибок
     * @param onComplete обработчик завершения
     * @return Disposable
     */
    public Disposable subscribe(
            java.util.function.Consumer<T> onNext,
            java.util.function.Consumer<Throwable> onError,
            Runnable onComplete) {
        return subscribe(new Observer<T>() {
            @Override
            public void onNext(T item) {
                onNext.accept(item);
            }

            @Override
            public void onError(Throwable t) {
                onError.accept(t);
            }

            @Override
            public void onComplete() {
                onComplete.run();
            }
        });
    }

    /**
     * Упрощенная подписка только с обработчиком элементов
     */
    public Disposable subscribe(java.util.function.Consumer<T> onNext) {
        return subscribe(onNext, Throwable::printStackTrace, () -> {});
    }

    /**
     * Оператор map - преобразование элементов
     * @param mapper функция преобразования
     * @param <R> тип результата
     * @return новый Observable
     */
    public <R> Observable<R> map(Function<T, R> mapper) {
        return new Observable<>(new MapOperator<>(this, mapper));
    }

    /**
     * Оператор filter - фильтрация элементов
     * @param predicate условие фильтрации
     * @return новый Observable
     */
    public Observable<T> filter(Predicate<T> predicate) {
        return new Observable<>(new FilterOperator<>(this, predicate));
    }

    /**
     * Оператор flatMap - преобразование в Observable
     * @param mapper функция преобразования в Observable
     * @param <R> тип результата
     * @return новый Observable
     */
    public <R> Observable<R> flatMap(Function<T, Observable<R>> mapper) {
        return new Observable<>(new FlatMapOperator<>(this, mapper));
    }

    /**
     * Указывает, в каком потоке будет выполняться подписка
     * @param scheduler планировщик
     * @return Observable
     */
    public Observable<T> subscribeOn(Scheduler scheduler) {
        return create(observer -> {
            scheduler.execute(() -> {
                onSubscribe.subscribe(observer);
            });
        });
    }

    /**
     * Указывает, в каком потоке будут обрабатываться элементы
     * @param scheduler планировщик
     * @return Observable
     */
    public Observable<T> observeOn(Scheduler scheduler) {
        return create(observer -> {
            onSubscribe.subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    scheduler.execute(() -> observer.onNext(item));
                }

                @Override
                public void onError(Throwable t) {
                    scheduler.execute(() -> observer.onError(t));
                }

                @Override
                public void onComplete() {
                    scheduler.execute(observer::onComplete);
                }
            });
        });
    }

    /**
     * Обработка ошибок
     * @param errorHandler обработчик ошибок
     * @return Observable
     */
    public Observable<T> onErrorResumeNext(java.util.function.Function<Throwable, Observable<T>> errorHandler) {
        return create(observer -> {
            onSubscribe.subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    observer.onNext(item);
                }

                @Override
                public void onError(Throwable t) {
                    try {
                        errorHandler.apply(t).subscribe(observer);
                    } catch (Exception e) {
                        observer.onError(e);
                    }
                }

                @Override
                public void onComplete() {
                    observer.onComplete();
                }
            });
        });
    }

    /**
     * Безопасный обертка Observer, проверяющая Disposable
     */
    private static class SafeObserver<T> implements Observer<T> {
        private final Observer<T> observer;
        private final Disposable disposable;

        public SafeObserver(Observer<T> observer, Disposable disposable) {
            this.observer = observer;
            this.disposable = disposable;
        }

        @Override
        public void onNext(T item) {
            if (!disposable.isDisposed()) {
                observer.onNext(item);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!disposable.isDisposed()) {
                observer.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!disposable.isDisposed()) {
                observer.onComplete();
            }
        }
    }

    /**
     * Реализация Disposable
     */
    private static class DefaultDisposable implements Disposable {
        private final AtomicBoolean disposed = new AtomicBoolean(false);

        @Override
        public void dispose() {
            disposed.set(true);
        }

        @Override
        public boolean isDisposed() {
            return disposed.get();
        }
    }
}