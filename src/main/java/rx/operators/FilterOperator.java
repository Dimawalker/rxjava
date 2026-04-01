package rx.operators;

import rx.core.Observable;
import rx.core.Observer;
import rx.functions.Predicate;

/**
 * Оператор filter - фильтрует элементы потока
 */
public class FilterOperator<T> implements Observable.OnSubscribe<T> {
    private final Observable<T> source;
    private final Predicate<T> predicate;

    public FilterOperator(Observable<T> source, Predicate<T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    public void subscribe(Observer<T> observer) {
        source.subscribe(new Observer<T>() {
            @Override
            public void onNext(T item) {
                try {
                    if (predicate.test(item)) {
                        observer.onNext(item);
                    }
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
                observer.onComplete();
            }
        });
    }
}