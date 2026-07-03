package com.mtr_optimizer.render;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.UUID;

public class SignTextureCache {

    private static final Map<UUID, CachedSignTexture> cache = new Object2ObjectOpenHashMap<>();

    public static boolean hasValidCache(UUID signId, int contentHash) {
        CachedSignTexture cached = cache.get(signId);
        return cached != null && cached.contentHash == contentHash;
    }

    public static boolean hasAnyCache(UUID signId) {
        return cache.containsKey(signId);
    }

    public static void updateCache(UUID signId, int contentHash, byte[] pixels, int width, int height) {
        // 释放旧纹理
        CachedSignTexture old = cache.remove(signId);
        if (old != null) {
            MinecraftClient.getInstance().getTextureManager().unbindTexture(old.identifier);
            // 实际项目中应通过 TextureManager 正确注册和销毁
        }

        NativeImage image = new NativeImage(width, height, false);
        // 将 byte[] (RGBA) 写入 NativeImage
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = (y * width + x) * 4;
                int r = pixels[i] & 0xFF;
                int g = pixels[i + 1] & 0xFF;
                int b = pixels[i + 2] & 0xFF;
                int a = pixels[i + 3] & 0xFF;
                image.setColor(x, y, (a << 24) | (b << 16) | (g << 8) | r); // ABGR format for MC
            }
        }

        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        Identifier id = new Identifier("mtr-optimizer", "sign_" + signId.toString());
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);

        cache.put(signId, new CachedSignTexture(id, contentHash, width, height));
    }

    public static void renderCached(UUID signId, MatrixStack matrices, Object vertexConsumers, int light) {
        CachedSignTexture cached = cache.get(signId);
        if (cached == null)
            return;

        // 绑定纹理并渲染四边形 (具体渲染逻辑需配合 MTR 的 VertexConsumer)
        RenderSystem.setShaderTexture(0, cached.identifier);
        // ... 绘制 Quad ...
    }

    private static class CachedSignTexture {
        final Identifier identifier;
        final int contentHash;
        final int width, height;

        CachedSignTexture(Identifier identifier, int contentHash, int width, int height) {
            this.identifier = identifier;
            this.contentHash = contentHash;
            this.width = width;
            this.height = height;
        }
    }
}