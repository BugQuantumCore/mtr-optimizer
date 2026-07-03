package com.mtr_optimizer.render;

import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.VertexFormat;

public class CachedVBO implements AutoCloseable {
    private final VertexBuffer vbo;
    private final int vertexCount;
    private final VertexFormat.DrawMode drawMode;
    private final VertexFormat format;
    private final long createdTime;
    private long lastUsedTime;
    private boolean closed = false;

    public CachedVBO(VertexBuffer vbo, int vertexCount, VertexFormat.DrawMode drawMode, VertexFormat format,
            long createdTime) {
        this.vbo = vbo;
        this.vertexCount = vertexCount;
        this.drawMode = drawMode;
        this.format = format;
        this.createdTime = createdTime;
        this.lastUsedTime = createdTime;
    }

    /**
     * 绘制缓存的 VBO
     */
    public void draw() {
        if (closed) {
            return;
        }
        this.lastUsedTime = System.currentTimeMillis();
        vbo.bind();
        vbo.draw();
        VertexBuffer.unbind();
    }

    public long getLastUsedTime() {
        return lastUsedTime;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    @Override
    public void close() {
        if (!closed) {
            vbo.close();
            closed = true;
        }
    }
}