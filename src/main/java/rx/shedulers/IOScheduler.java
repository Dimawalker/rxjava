package rx.schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * IO планировщик - использует CachedThreadPool
 * Для операций ввода-вывода, сетевых запросов
 */
public class IOScheduler implements Scheduler {
    private static final IOScheduler INSTANCE = new IOScheduler();
    private final ExecutorService executor;

    private IOScheduler() {
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "IO-Thread-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
    }

    public static IOScheduler getInstance() {
        return INSTANCE;
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }
}