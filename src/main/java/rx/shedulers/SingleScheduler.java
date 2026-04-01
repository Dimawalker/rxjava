package rx.schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single планировщик - использует один поток
 * Для последовательных операций
 */
public class SingleScheduler implements Scheduler {
    private static final SingleScheduler INSTANCE = new SingleScheduler();
    private final ExecutorService executor;

    private SingleScheduler() {
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Single-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    public static SingleScheduler getInstance() {
        return INSTANCE;
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }
}