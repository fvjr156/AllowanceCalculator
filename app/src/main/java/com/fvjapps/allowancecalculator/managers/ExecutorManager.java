package com.fvjapps.allowancecalculator.managers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ExecutorManager {
    private static volatile ExecutorManager instance;
    private final ExecutorService dbExec;
    private final ExecutorService fileExec;

    private ExecutorManager() {
        dbExec = Executors.newFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "databaseopsthread");
                t.setDaemon(true);
                return t;
            }
        });
        fileExec = Executors.newFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "fileopsthread");
                t.setDaemon(true);
                return t;
            }
        });
    }

    public static ExecutorManager getInstance() {
        if (instance == null) {
            synchronized (ExecutorManager.class) {
                if (instance == null) {
                    instance = new ExecutorManager();
                }
            }
        }
        return instance;
    }

    public ExecutorService getDbExec() {
        return dbExec;
    }

    public ExecutorService getFileExec() {
        return fileExec;
    }

    public void shutdown() {
        dbExec.shutdown();
        fileExec.shutdown();
    }
}
