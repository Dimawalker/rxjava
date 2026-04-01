package rx.functions;

/**
 * Функциональный интерфейс для фильтрации
 * @param <T> тип элемента
 */
@FunctionalInterface
public interface Predicate<T> {
    boolean test(T t);
}