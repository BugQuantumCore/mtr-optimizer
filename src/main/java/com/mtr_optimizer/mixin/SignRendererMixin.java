package com.mtr_optimizer.mixin;

import com.mtr_optimizer.nativebridge.NativeJSRunner;
import com.mtr_optimizer.render.SignTextureCache;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截 MTR 的 SignRenderer / RouteSignRenderer
 * 在 MTR 4.0.5 中，指示牌渲染器位于:
 * mtr.client.SignRenderer 或 mtr.client.StationNameSignRenderer
 */
@Pseudo
@Mixin(targets = "mtr.client.StationNameSignRenderer", remap = false)
public abstract class SignRendererMixin {

    @Unique
    private long nativeJsContext = -1;

    @Unique
    private int tickCounter = 0;

    /**
     * 在指示牌渲染前拦截
     * 实现:
     * 1. 脏标记：非关键帧跳过重绘
     * 2. 节流：每 N tick 才允许执行 JS
     * 3. 使用原生 QuickJS 替代 Java ScriptEngine
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onSignRender(Entity entity, float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light, CallbackInfo ci) {
        if (!NativeJSRunner.isAvailable())
            return;

        // 节流控制：每 5 tick（0.25秒）才允许重新渲染
        tickCounter++;
        boolean isThrottled = (tickCounter % 5) != 0;

        // 计算指示牌内容的哈希值（用于脏标记）
        String signContent = getSignContent(entity); // 反射获取
        int contentHash = signContent.hashCode();

        // 检查缓存：内容未变 + 在节流周期内 → 使用缓存纹理
        if (SignTextureCache.hasValidCache(entity.getUuid(), contentHash)) {
            // 直接使用缓存的纹理渲染，跳过 JS 执行
            SignTextureCache.renderCached(entity.getUuid(), matrices,
                    vertexConsumers, light);
            ci.cancel(); // 取消原版渲染
            return;
        }

        if (isThrottled) {
            // 在节流期间，如果缓存失效，渲染上一帧的旧纹理
            if (SignTextureCache.hasAnyCache(entity.getUuid())) {
                SignTextureCache.renderCached(entity.getUuid(), matrices,
                        vertexConsumers, light);
                ci.cancel();
            }
            return; // 否则让原版渲染（降级方案）
        }

        // === 缓存失效 + 不在节流周期 → 使用原生引擎重新渲染 ===
        if (nativeJsContext == -1) {
            nativeJsContext = NativeJSRunner.createContext();
        }

        int width = getSignWidth(entity);
        int height = getSignHeight(entity);
        byte[] pixels = new byte[width * height * 4]; // RGBA

        String jsScript = getSignScript(entity);
        String paramsJson = buildParamsJson(entity, tickDelta);

        int result = NativeJSRunner.executeScript(
                nativeJsContext, jsScript, paramsJson, width, height, pixels);

        if (result > 0) {
            // 将像素数据上传为纹理并缓存
            SignTextureCache.updateCache(entity.getUuid(), contentHash,
                    pixels, width, height);
            SignTextureCache.renderCached(entity.getUuid(), matrices,
                    vertexConsumers, light);
            ci.cancel(); // 取消原版渲染
        }
    }
}