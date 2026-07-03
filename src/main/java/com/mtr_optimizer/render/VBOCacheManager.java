package com.mtr_optimizer.render;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VBOCacheManager {

    // 缓存键 → 缓存的 VBO 数据
    private static final Map<String, CachedVBO> cache = new ConcurrentHashMap<>(256);

    // 不可缓存的渲染上下文（如动态变化的实体）
    private static final ThreadLocal<java.util.Set<String>> dynamicContexts = ThreadLocal
            .withInitial(java.util.HashSet::new);

    // MTR 相关的可缓存前缀（轨道、接触网、装饰方块等）
    private static final String[] CACHEABLE_PREFIXES = {
            "mtr_rail_", // 轨道
            "mtr_catenary_", // 接触网
            "mtr_signal_", // 信号机
            "mtr_psd_", // 屏蔽门 (Platform Screen Door)
            "mtr_decoration_", // 车站装饰
            "joban_signal_", // Joban 信号机
            "joban_catenary_", // Joban 接触网
            "tianjin_deco_", // 天津地铁装饰
    };

    /**
     * 判断给定的渲染上下文是否可以缓存
     */
    public static boolean isCacheable(String contextKey) {
        for (String prefix : CACHEABLE_PREFIXES) {
            if (contextKey.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 尝试获取缓存的 VBO
     *
     * @return 如果缓存命中，返回 CachedVBO；否则返回 null
     */
    public static CachedVBO getCached(String contextKey) {
        return cache.get(contextKey);
    }

    /**
     * 将 BufferBuilder 构建的顶点数据上传到 GPU 并缓存
     */
    public static void uploadAndCache(String contextKey,
            BufferBuilder.BuiltBuffer builtBuffer) {
        // 如果已存在缓存，先释放旧的
        CachedVBO old = cache.get(contextKey);
        if (old != null) {
            old.close();
        }

        // 获取原始字节数据
        ByteBuffer vertexData = builtBuffer.getBuffer();
        int vertexCount = builtBuffer.getBufferCount();

        // 分配直接内存（off-heap）并拷贝数据
        ByteBuffer directBuffer = MemoryUtil.memAlloc(vertexData.remaining());
        directBuffer.put(vertexData);
        directBuffer.flip();

        // 创建并上传 VBO
        VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);

        RenderSystem.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo.getId());
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, directBuffer, GL15.GL_STATIC_DRAW);
        RenderSystem.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        // 缓存
        CachedVBO cached = new CachedVBO(
                vbo,
                vertexCount,
                builtBuffer.getDrawMode(),
                builtBuffer.getFormat(),
                System.currentTimeMillis());
        cache.put(contextKey, cached);

        // 释放直接内存（数据已上传到 GPU）
        MemoryUtil.memFree(directBuffer);
    }

    /**
     * 使特定缓存失效（如轨道被破坏时）
     */
    public static void invalidate(String contextKey) {
        CachedVBO removed = cache.remove(contextKey);
        if (removed != null) {
            removed.close();
        }
    }

    /**
     * 使所有缓存失效（如切换世界/维度时）
     */
    public static void invalidateAll() {
        cache.values().forEach(CachedVBO::close);
        cache.clear();
    }

    /**
     * 定期清理长时间未使用的缓存（每5分钟清理超过10分钟未使用的）
     */
    public static void gc() {
        long threshold = System.currentTimeMillis() - 600_000; // 10分钟
        cache.entrySet().removeIf(entry -> {
            if (entry.getValue().getLastUsedTime() < threshold) {
                entry.getValue().close();
                return true;
            }
            return false;
        });
    }
}