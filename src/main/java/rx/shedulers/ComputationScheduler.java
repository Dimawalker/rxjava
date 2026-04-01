package rx.schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Computation планировщик - использует FixedThreadPool
 * Для CPU-интенсивных операций
 */
public class ComputationScheduler implements Scheduler {
    private static final ComputationScheduler INSTANCE = new ComputationScheduler();
    private final ExecutorService executor;

    private ComputationScheduler() {
        int processors = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(processors, r -> {
            Thread t = new Thread(r, "Computation-Thread-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
    }

    public static ComputationScheduler getInstance() {
        return INSTANCE;
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }
}