package com.motorola.livestream.util;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecutorUtil {

    private static final Executor sExecutor = new ThreadPoolExecutor(4, 4,
            1L, TimeUnit.SECONDS,
            new LinkedBlockingDeque<>());

    public static void executeAsync(Runnable runnable) {
        sExecutor.execute(runnable);
    }
}
