package com.mtr_optimizer.mixin;

import com.mtr_optimizer.render.VBOCacheManager;
import com.mtr_optimizer.render.CachedVBO;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferPool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin {

    @Shadow
    private int vertexCount;
    @Shadow
    private boolean building;

    @Unique
    private String mtrOptimizer$cacheKey = null;

    /**
     * 在 BufferBuilder 开始构建时，检查是否有可用的缓存
     * 通过 ThreadLocal 传递当前渲染上下文（由 EntityRenderer Mixin 设置）
     */
    @Inject(method = "begin", at = @At("HEAD"))
    private void onBegin(VertexFormat.DrawMode drawMode, VertexFormat format, CallbackInfo ci) {
        // 从 ThreadLocal 获取当前渲染上下文标识
        String contextKey = RenderContext.getContextKey();
        if (contextKey != null && VBOCacheManager.isCacheable(contextKey)) {
            this.mtrOptimizer$cacheKey = contextKey;
        }
    }

    /**
     * 拦截 BufferBuilder.end() —— 当顶点数据构建完成时
     * 如果是可缓存的 MTR 几何体，将数据上传到 VBO 并缓存
     */
    @Inject(method = "end", at = @At("RETURN"))
    private void onEnd(CallbackInfoReturnable<BufferBuilder.BuiltBuffer> cir) {
        if (this.mtrOptimizer$cacheKey != null) {
            BufferBuilder.BuiltBuffer builtBuffer = cir.getReturnValue();
            if (builtBuffer != null && builtBuffer.getBufferCount() > 0) {
                // 将顶点数据上传到 GPU VBO 并缓存
                VBOCacheManager.uploadAndCache(this.mtrOptimizer$cacheKey, builtBuffer);
            }
            this.mtrOptimizer$cacheKey = null;
        }
    }
}