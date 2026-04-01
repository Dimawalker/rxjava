package rx.functions;

/**
 * Функциональный интерфейс для преобразования
 * @param <T> входной тип
 * @param <R> выходной тип
 */
@FunctionalInterface
public interface Function<T, R> {
    R apply(T t);
}