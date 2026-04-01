package rx.operators;

import rx.core.Disposable;
import rx.core.Observable;
import rx.core.Observer;
import rx.functions.Function;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Оператор flatMap - преобразует элементы в Observable и сливает их
 */
public class FlatMapOperator<T, R> implements Observable.OnSubscribe<R> {
    private final Observable<T> source;
    private final Function<T, Observable<R>> mapper;

    public FlatMapOperator(Observable<T> source, Function<T, Observable<R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public void subscribe(Observer<R> observer) {
        AtomicInteger pending = new AtomicInteger(1);

        source.subscribe(new Observer<T>() {
            @Override
            public void onNext(T item) {
                pending.incrementAndGet();
                try {
                    Observable<R> observable = mapper.apply(item);
                    observable.subscribe(new Observer<R>() {
                        @Override
                        public void onNext(R result) {
                            observer.onNext(result);
                        }

                        @Override
                        public void onError(Throwable t) {
                            observer.onError(t);
                        }

                        @Override
                        public void onComplete() {
                            if (pending.decrementAndGet() == 0) {
                                observer.onComplete();
                            }
                        }
                    });
                } catch (Exception e) {
                    observer.onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                observer.onError(t);
            }

            @Override
            public void onComplete() {
                if (pending.decrementAndGet() == 0) {
                    observer.onComplete();
                }
            }
        });
    }
}