package net.nothingtv.ruva.client.tools;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Async {

    public static Async getInstance() {
        if (instance == null)
            init();
        return instance;
    }

    private static Async instance;
    private final ExecutorService executor;

    private Async() {
        executor = Executors.newCachedThreadPool(r -> {
            Thread thr = new Thread(r);
            thr.setDaemon(true);
            return thr;
        });
    }

    private void internalShutDown() {
        executor.shutdown();
    }

    public static void init() {
        if (instance == null) {
            instance = new Async();
        }
    }

    public static void shutDown() {
        if (instance != null)
            instance.internalShutDown();
    }

    public static void submit(Runnable runnable) {
        getInstance().executor.submit(runnable);
    }

    public static <T> Future<T> submit(Callable<T> callable) {
        return getInstance().executor.submit(callable);
    }
}