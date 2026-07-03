package com.mtr_optimizer.native;

public class NativeMathLib {

    /**
     * 批量 4x4 矩阵乘法（SIMD 加速）
     * @param matricesA 输入矩阵 A（连续存储，每矩阵16个float）
     * @param matricesB 输入矩阵 B
     * @param output 输出矩阵 C = A * B
     * @param count 矩阵数量
     */
    public static native void batchMat4Multiply(
        float[] matricesA, float[] matricesB, float[] output, int count
    );

    /**
     * 批量三次贝塞尔曲线采样（SIMD 加速）
     * 用于 MTR 轨道曲线顶点生成
     * @param controlPoints 控制点 (N * 4 * 3 floats)
     * @param tValues 采样参数 t
     * @param samplesPerCurve 每条曲线的采样数
     * @param numCurves 曲线数量
     * @param output 输出采样点坐标
     */
    public static native void batchBezierSample(
        float[] controlPoints, float[] tValues,
        int samplesPerCurve, int numCurves, float[] output
    );
}