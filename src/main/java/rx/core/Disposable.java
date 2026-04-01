package rx.core;

/**
 * Интерфейс для отмены подписки
 */
public interface Disposable {

    /**
     * Отменяет подписку
     */
    void dispose();

    /**
     * Проверяет, отменена ли подписка
     * @return true если подписка отменена
     */
    boolean isDisposed();
}