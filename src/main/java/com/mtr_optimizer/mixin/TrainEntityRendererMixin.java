package com.mtr_optimizer.mixin;

import com.mtr_optimizer.render.TrainInstanceBatcher;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 使用 @Pseudo 注解，因为 MTR 类不在编译期可见
 * 运行时由 Mixin 框架通过类名匹配注入
 *
 * 目标类: mtr.client.TrainClientRenderer (或类似路径)
 * MTR 4.0.5 中列车渲染器的实际类名需要通过反编译确认
 */
@Pseudo
@Mixin(targets = "mtr.client.TrainClientRenderer", remap = false)
public abstract class TrainEntityRendererMixin extends EntityRenderer {

    protected TrainEntityRendererMixin() {
        super(null);
    }

    /**
     * 拦截列车渲染的 render 方法
     * 不取消原版渲染（避免破坏兼容性），而是在渲染前收集信息
     * 在帧结束时由 TrainInstanceBatcher 统一处理
     */
    @Inject(method = "render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void onTrainRender(Entity entity, float yaw, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        // 收集渲染信息到批处理器
        TrainInstanceBatcher.getInstance().collectTrainRender(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}