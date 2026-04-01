package rx.schedulers;

/**
 * Интерфейс планировщика потоков
 */
public interface Scheduler {

    /**
     * Выполняет задачу в потоке планировщика
     * @param task задача
     */
    void execute(Runnable task);

    /**
     * Создает планировщик для IO операций
     * @return IO Scheduler
     */
    static Scheduler io() {
        return IOScheduler.getInstance();
    }

    /**
     * Создает планировщик для вычислений
     * @return Computation Scheduler
     */
    static Scheduler computation() {
        return ComputationScheduler.getInstance();
    }

    /**
     * Создает планировщик с одним потоком
     * @return Single Scheduler
     */
    static Scheduler single() {
        return SingleScheduler.getInstance();
    }
}