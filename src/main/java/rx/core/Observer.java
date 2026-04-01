package rx.core;

/**
 * Наблюдатель, который получает события из Observable
 * @param <T> тип элементов потока
 */
public interface Observer<T> {

    /**
     * Получает следующий элемент потока
     * @param item элемент данных
     */
    void onNext(T item);

    /**
     * Обрабатывает ошибку в потоке
     * @param t ошибка
     */
    void onError(Throwable t);

    /**
     * Вызывается при успешном завершении потока
     */
    void onComplete();
}