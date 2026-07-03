package com.mtr_optimizer.render;

import java.util.concurrent.atomic.AtomicLong;

public class RenderContext {

    private static final ThreadLocal<String> currentContextKey = new ThreadLocal<>();
    private static final AtomicLong frameCount = new AtomicLong(0);

    public static void beginFrame() {
        frameCount.incrementAndGet();
    }

    public static void setContextKey(String key) {
        currentContextKey.set(key);
    }

    public static String getContextKey() {
        return currentContextKey.get();
    }

    public static void clearContext() {
        currentContextKey.remove();
    }

    public static long getFrameCount() {
        return frameCount.get();
    }
}