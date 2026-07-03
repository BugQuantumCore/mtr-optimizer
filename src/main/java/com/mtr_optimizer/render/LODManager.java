package com.mtr_optimizer.render;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class LODManager {

    public enum LODLevel {
        FULL(0, 48), // 0~48 格：完整模型
        MEDIUM(48, 96), // 48~96 格：中等精度（50% 顶点）
        LOW(96, 192), // 96~192 格：低精度（25% 顶点）
        BILLBOARD(192, 256); // 192~256 格：十字面片（2 个四边形）

        public final double minDist;
        public final double maxDist;

        LODLevel(double min, double max) {
            this.minDist = min;
            this.maxDist = max;
        }
    }

    /**
     * 根据实体与摄像机的距离决定 LOD 等级
     */
    public static LODLevel getLODForEntity(Entity entity, Vec3d cameraPos) {
        double dist = Math.sqrt(entity.squaredDistanceTo(cameraPos));

        for (LODLevel level : LODLevel.values()) {
            if (dist >= level.minDist && dist < level.maxDist) {
                return level;
            }
        }
        return null; // 超出渲染距离
    }

    /**
     * 获取指定 LOD 等级的模型数据
     * 首次请求时自动生成简化模型（使用原生库的网格简化算法）
     */
    public static LODModelData getLODModel(String baseModelId, LODLevel level) {
        String lodKey = baseModelId + "_lod_" + level.ordinal();
        return LODModelCache.getOrGenerate(lodKey, baseModelId, level);
    }

    /**
     * 根据 LOD 等级决定动态贴图的更新频率
     * 远距离的指示牌不需要高频更新
     */
    public static int getUpdateIntervalForLOD(LODLevel level) {
        switch (level) {
            case FULL:
                return 5; // 每 0.25 秒更新
            case MEDIUM:
                return 20; // 每 1 秒更新
            case LOW:
                return 60; // 每 3 秒更新
            case BILLBOARD:
                return 120; // 每 6 秒更新（几乎静态）
            default:
                return 20;
        }
    }
}