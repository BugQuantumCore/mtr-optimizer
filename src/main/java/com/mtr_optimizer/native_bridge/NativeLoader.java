package com.mtr_optimizer.native_bridge;

import java.io.*;
import java.nio.file.*;
import java.util.Locale;

public class NativeLoader {

    private static boolean loaded = false;
    private static String errorMessage = "";

    public static void load() {
        if (loaded)
            return;

        try {
            // 1. 确定当前操作系统和架构
            String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);

            String targetTriple;
            String libFileName;

            if (os.contains("win")) {
                targetTriple = "x86_64-pc-windows-msvc";
                libFileName = "mtr_optimizer.dll";
            } else if (os.contains("mac") || os.contains("darwin")) {
                targetTriple = arch.contains("aarch64") || arch.contains("arm")
                        ? "aarch64-apple-darwin"
                        : "x86_64-apple-darwin";
                libFileName = "libmtr_optimizer.dylib";
            } else if (os.contains("linux")) {
                targetTriple = arch.contains("aarch64") || arch.contains("arm")
                        ? "aarch64-unknown-linux-gnu"
                        : "x86_64-unknown-linux-gnu";
                libFileName = "libmtr_optimizer.so";
            } else {
                throw new UnsupportedOperationException("Unsupported OS: " + os);
            }

            // 2. 从 Jar 包中提取原生库到临时目录
            String resourcePath = "/native/" + targetTriple + "/" + libFileName;
            InputStream is = NativeLoader.class.getResourceAsStream(resourcePath);

            if (is == null) {
                throw new FileNotFoundException("Native library not found in jar: " + resourcePath);
            }

            Path tempDir = Files.createTempDirectory("mtr_optimizer_native_");
            Path tempLib = tempDir.resolve(libFileName);
            Files.copy(is, tempLib, StandardCopyOption.REPLACE_EXISTING);
            is.close();

            // 3. 加载原生库
            System.load(tempLib.toAbsolutePath().toString());
            loaded = true;

            // 4. 注册退出清理钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.deleteIfExists(tempLib);
                    Files.deleteIfExists(tempDir);
                } catch (IOException ignored) {
                }
            }));

            System.out.println("[MTR-Optimizer] Native library loaded: " + targetTriple);

        } catch (Exception e) {
            errorMessage = e.getMessage();
            System.err.println("[MTR-Optimizer] Failed to load native library: " + errorMessage);
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static String getErrorMessage() {
        return errorMessage;
    }
}