package rx.operators;

import rx.core.Observable;
import rx.core.Observer;
import rx.functions.Function;

/**
 * Оператор map - преобразует элементы потока
 */
public class MapOperator<T, R> implements Observable.OnSubscribe<R> {
    private final Observable<T> source;
    private final Function<T, R> mapper;

    public MapOperator(Observable<T> source, Function<T, R> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public void subscribe(Observer<R> observer) {
        source.subscribe(new Observer<T>() {
            @Override
            public void onNext(T item) {
                try {
                    R result = mapper.apply(item);
                    observer.onNext(result);
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