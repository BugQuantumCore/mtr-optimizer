package com.mtr_optimizer.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.stream.Collectors;

public class AdvancedFrustumCuller {

    private static FrustumIntersection frustumIntersection;
    private static final Vector3f tempVec = new Vector3f();

    /**
     * 更新视锥体（每帧调用一次）
     */
    public static void updateFrustum(Matrix4f projMatrix, Matrix4f viewMatrix) {
        Matrix4f vpMatrix = new Matrix4f(projMatrix).mul(viewMatrix);
        frustumIntersection = new FrustumIntersection(vpMatrix);
    }

    /**
     * 判断一个 MTR 实体是否应该被渲染
     * 综合使用: 视锥剔除 + 距离剔除 + 遮挡剔除
     */
    public static boolean shouldRender(Entity entity, Camera camera) {
        if (frustumIntersection == null)
            return true;

        // 1. 距离剔除
        Vec3d camPos = camera.getPos();
        double distSq = entity.squaredDistanceTo(camPos);
        if (distSq > 256 * 256) {
            return false; // 超过 256 格
        }

        // 2. 视锥剔除（使用实体的 AABB 包围盒）
        Box box = entity.getBoundingBox();
        float minX = (float) (box.minX - camPos.x);
        float minY = (float) (box.minY - camPos.y);
        float minZ = (float) (box.minZ - camPos.z);
        float maxX = (float) (box.maxX - camPos.x);
        float maxY = (float) (box.maxY - camPos.y);
        float maxZ = (float) (box.maxZ - camPos.z);

        // JOML 的 FrustumIntersection 测试 AABB 是否在视锥内
        boolean inFrustum = frustumIntersection.testAab(
                new Vector3f(minX, minY, minZ),
                new Vector3f(maxX, maxY, maxZ));

        if (!inFrustum) {
            return false;
        }

        // 3. 简单遮挡剔除：检查实体位置是否在不透明方块内部
        // （地铁站内场景中，大量列车可能在墙壁后面）
        if (distSq > 16 * 16) { // 16格以外才做遮挡检测（避免开销）
            World world = entity.getWorld();
            Vec3d entityEyePos = entity.getEyePos();
            Vec3d direction = entityEyePos.subtract(camPos).normalize();

            // 射线检测：从摄像机到实体，检查是否有不透明方块
            if (isOccludedByBlocks(world, camPos, entityEyePos, direction, distSq)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 简单的射线遮挡检测
     * 使用 Bresenham 3D 射线步进，检查沿途是否有不透明方块
     */
    private static boolean isOccludedByBlocks(World world, Vec3d start, Vec3d end,
            Vec3d direction, double distSq) {
        double distance = Math.sqrt(distSq);
        double stepSize = 1.0; // 每 1 格检查一次
        int steps = (int) (distance / stepSize);

        Vec3d current = start;
        for (int i = 1; i < steps - 1; i++) { // 不检查起点和终点
            current = start.add(direction.multiply(i * stepSize));

            // 检查该位置是否有不透明方块
            if (world.getBlockState(
                    net.minecraft.util.math.BlockPos.ofFloored(current)).isOpaqueFullCube(world,
                            net.minecraft.util.math.BlockPos.ofFloored(current))) {
                return true; // 被遮挡
            }
        }
        return false;
    }

    /**
     * 过滤可渲染的实体列表（在 WorldRenderer Mixin 中调用）
     */
    public static List<Entity> filterVisibleEntities(List<Entity> entities, Camera camera) {
        return entities.stream()
                .filter(e -> shouldRender(e, camera))
                .collect(Collectors.toList());
    }
}