package com.mtr_optimizer.native;

import java.io.*;
import java.nio.file.*;

public class NativeJSRunner {

    private static boolean loaded = false;

    static {
        try {
            // 从 jar 包中提取原生库到临时目录
            String libName = System.mapLibraryName("mtr_optimizer");
            Path tempDir = Files.createTempDirectory("mtr_optimizer_native");
            Path tempLib = tempDir.resolve(libName);

            try (InputStream is = NativeJSRunner.class.getResourceAsStream("/native/" + libName)) {
                if (is != null) {
                    Files.copy(is, tempLib, StandardCopyOption.REPLACE_EXISTING);
                    System.load(tempLib.toAbsolutePath().toString());
                    loaded = true;
                }
            }

            // 注册 JVM 退出时清理临时文件
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.deleteIfExists(tempLib);
                    Files.deleteIfExists(tempDir);
                } catch (IOException ignored) {}
            }));
        } catch (Exception e) {
            System.err.println("[MTR-Optimizer] Failed to load native library: " + e.getMessage());
        }
    }

    public static boolean isAvailable() { return loaded; }

    // 原生方法声明
    public static native long createContext();
    public static native int executeScript(long contextId, String script, String paramsJson, int width, int height, byte[] outputPixels);
    public static native boolean buildAtlas(byte[] textureIds, byte[] pixelData, byte[] widths, byte[] heights, int count, byte[] outputAtlas, int atlasWidth, int atlasHeight);
}