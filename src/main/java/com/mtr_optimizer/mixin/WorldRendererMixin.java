package com.mtr_optimizer.mixin;

import com.mtr_optimizer.render.*;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    /**
     * 帧开始：更新视锥体，准备批处理器
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void onFrameStart(MatrixStack matrices, float tickDelta,
            long limitTime, boolean renderBlockOutline,
            Camera camera, GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f positionMatrix, Matrix4f projectionMatrix,
            CallbackInfo ci) {
        // 更新视锥体用于剔除
        AdvancedFrustumCuller.updateFrustum(positionMatrix,
                gameRenderer.getBasicProjectionMatrix(tickDelta));

        // 设置当前帧的渲染上下文
        RenderContext.beginFrame();
    }

    /**
     * 帧结束：批量渲染所有收集的 GPU Instancing 实例
     */
    @Inject(method = "render", at = @At("RETURN"))
    private void onFrameEnd(MatrixStack matrices, float tickDelta,
            long limitTime, boolean renderBlockOutline,
            Camera camera, GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f positionMatrix, Matrix4f projectionMatrix,
            CallbackInfo ci) {
        // 批量渲染所有列车实例
        TrainInstanceBatcher.getInstance().flushBatchedTrains(
                matrices,
                MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers());

        // 定期 GC 缓存（每 300 帧 ≈ 5 秒）
        if (RenderContext.getFrameCount() % 300 == 0) {
            VBOCacheManager.gc();
        }
    }
}