package com.mtr_optimizer.render;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TrainInstanceBatcher {

    private static final TrainInstanceBatcher INSTANCE = new TrainInstanceBatcher();

    // 列车模型ID → 该帧收集到的所有渲染实例
    private final Map<String, List<TrainInstance>> frameInstances = new Object2ObjectOpenHashMap<>();

    // 模型ID → 已编译的 VBO（首次渲染时编译）
    private final Map<String, CompiledTrainModel> compiledModels = new ConcurrentHashMap<>();

    // 实例数据缓冲区（复用，避免每帧分配）
    private ByteBuffer instanceBuffer = MemoryUtil.memAlloc(1024 * 64); // 初始64KB

    public static TrainInstanceBatcher getInstance() {
        return INSTANCE;
    }

    /**
     * 收集一列列车的渲染信息（由 Mixin 调用）
     */
    public void collectTrainRender(Entity entity, float yaw, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light) {
        // 通过反射获取列车模型 ID（MTR 的列车有 modelResource 字段）
        String modelId = getModelIdViaReflection(entity);
        if (modelId == null) {
            return;
        }

        // 计算实体的插值位置
        double x = entity.prevX + (entity.getX() - entity.prevX) * tickDelta;
        double y = entity.prevY + (entity.getY() - entity.prevY) * tickDelta;
        double z = entity.prevZ + (entity.getZ() - entity.prevZ) * tickDelta;

        TrainInstance instance = new TrainInstance(
                x, y, z, yaw, light,
                getModelColor(entity), // 列车涂装颜色
                getDoorProgress(entity), // 开门动画进度
                getAnimationTick(entity) // 其他动画 tick
        );

        frameInstances.computeIfAbsent(modelId, k -> new ArrayList<>()).add(instance);
    }

    /**
     * 在帧结束时批量渲染所有收集的列车实例
     * 由 WorldRendererMixin 的 render 方法末尾调用
     */
    public void flushBatchedTrains(MatrixStack matrices, VertexConsumerProvider.Immediate immediate) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        for (Map.Entry<String, List<TrainInstance>> entry : frameInstances.entrySet()) {
            String modelId = entry.getKey();
            List<TrainInstance> instances = entry.getValue();

            if (instances.isEmpty()) {
                continue;
            }

            // 获取或编译模型
            CompiledTrainModel model = compiledModels.computeIfAbsent(modelId, id -> compileModel(id));
            if (model == null) {
                continue;
            }

            // === 准备实例数据 ===
            // 每个实例需要:
            // - 4x4 变换矩阵 (16 floats = 64 bytes)
            // - 颜色 RGB (3 floats = 12 bytes)
            // - 光照 (1 int = 4 bytes)
            // - 动画参数 (2 floats = 8 bytes)
            // 每个实例共 88 bytes
            int instanceCount = instances.size();
            int stride = 88; // bytes per instance
            int totalBytes = instanceCount * stride;

            // 确保缓冲区足够大
            if (instanceBuffer.capacity() < totalBytes) {
                MemoryUtil.memFree(instanceBuffer);
                instanceBuffer = MemoryUtil.memAlloc(Math.max(totalBytes, totalBytes * 2));
            }

            FloatBuffer floatBuf = instanceBuffer.asFloatBuffer();

            Matrix4f modelMatrix = new Matrix4f();
            int floatOffset = 0;

            for (TrainInstance inst : instances) {
                // 计算相对于摄像机的位置（避免浮点精度问题）
                float relX = (float) (inst.x - camPos.x);
                float relY = (float) (inst.y - camPos.y);
                float relZ = (float) (inst.z - camPos.z);

                // 距离剔除：超过渲染距离的实例跳过
                float distSq = relX * relX + relY * relY + relZ * relZ;
                if (distSq > 256 * 256)
                    continue; // 256格以外跳过

                // 构建模型矩阵
                modelMatrix.identity()
                        .translate(relX, relY, relZ)
                        .rotateY((float) Math.toRadians(-inst.yaw))
                        .scale(inst.scaleX, inst.scaleY, inst.scaleZ);

                // 写入 4x4 矩阵 (16 floats)
                modelMatrix.get(floatOffset, floatBuf);
                floatOffset += 16;

                // 写入颜色 (3 floats)
                floatBuf.put(floatOffset++, inst.colorR);
                floatBuf.put(floatOffset++, inst.colorG);
                floatBuf.put(floatOffset++, inst.colorB);

                // 写入光照 (packed as float)
                floatBuf.put(floatOffset++, Float.intBitsToFloat(inst.light));

                // 写入动画参数 (2 floats)
                floatBuf.put(floatOffset++, inst.doorProgress);
                floatBuf.put(floatOffset++, inst.animTick);
            }

            int actualInstanceCount = floatOffset / (stride / 4);
            if (actualInstanceCount == 0) {
                continue;
            }

            // === GPU Instancing 渲染 ===
            // 1. 绑定模型 VBO（共享几何体）
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, model.getGeometryVboId());
            model.setupVertexAttributes(); // 设置顶点属性指针

            // 2. 绑定实例数据 VBO
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, model.getInstanceVboId());
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (ByteBuffer) instanceBuffer.position(0).limit(totalBytes),
                    GL15.GL_STREAM_DRAW);
            model.setupInstanceAttributes(); // 设置实例属性指针

            // 3. 绑定纹理
            model.bindTexture();

            // 4. 使用 instanced draw call
            GL33.glDrawArraysInstanced(
                    GL11.GL_TRIANGLES, // 绘制模式
                    0, // 起始顶点
                    model.getVertexCount(), // 每个模型的顶点数
                    actualInstanceCount // 实例数量
            );

            // 5. 清理状态
            model.unbind();
        }

        // 清空本帧收集的实例
        frameInstances.clear();
    }

    /**
     * 编译列车模型：首次遇到某型号列车时，将其几何体编译到 VBO
     */
    private CompiledTrainModel compileModel(String modelId) {
        // 通过 MTR 的模型注册表获取原始模型数据
        // MTR 使用 bbmodel/obj 格式，在 TrainResourcePack 中加载
        // 这里需要通过反射获取模型顶点数据

        try {
            Object modelResource = getTrainModelResource(modelId);
            if (modelResource == null)
                return null;

            // 提取顶点数据
            float[] positions = getModelPositions(modelResource);
            float[] normals = getModelNormals(modelResource);
            float[] uvs = getModelUVs(modelResource);
            int vertexCount = positions.length / 3;

            // 构建交错的顶点缓冲区
            // 格式: [pos.x, pos.y, pos.z, normal.x, normal.y, normal.z, u, v]
            // 每个顶点 8 floats = 32 bytes
            ByteBuffer vertexData = MemoryUtil.memAlloc(vertexCount * 32);
            FloatBuffer vb = vertexData.asFloatBuffer();

            for (int i = 0; i < vertexCount; i++) {
                vb.put(positions[i * 3]);
                vb.put(positions[i * 3 + 1]);
                vb.put(positions[i * 3 + 2]);
                vb.put(normals[i * 3]);
                vb.put(normals[i * 3 + 1]);
                vb.put(normals[i * 3 + 2]);
                vb.put(uvs[i * 2]);
                vb.put(uvs[i * 2 + 1]);
            }

            // 上传到 VBO
            int geoVbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, geoVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexData, GL15.GL_STATIC_DRAW);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

            // 创建实例数据 VBO（预留空间）
            int instVbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 88 * 64, GL15.GL_STREAM_DRAW);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

            MemoryUtil.memFree(vertexData);

            return new CompiledTrainModel(
                    geoVbo, instVbo, vertexCount,
                    getTrainTextureId(modelId) // 获取 OpenGL 纹理 ID
            );

        } catch (Exception e) {
            return null;
        }
    }

    // ... 反射辅助方法省略（getModelIdViaReflection, getTrainModelResource 等）
}

/**
 * 单个列车实例的渲染数据
 */
class TrainInstance {
    double x, y, z;
    float yaw;
    int light;
    float colorR, colorG, colorB;
    float doorProgress;
    float animTick;
    float scaleX = 1f, scaleY = 1f, scaleZ = 1f;

    TrainInstance(double x, double y, double z, float yaw, int light, int color, float doorProgress, float animTick) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.light = light;
        this.colorR = ((color >> 16) & 0xFF) / 255f;
        this.colorG = ((color >> 8) & 0xFF) / 255f;
        this.colorB = (color & 0xFF) / 255f;
        this.doorProgress = doorProgress;
        this.animTick = animTick;
    }
}