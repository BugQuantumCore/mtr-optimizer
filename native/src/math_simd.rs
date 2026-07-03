use jni::objects::{JClass, JFloatArray};
use jni::sys::{jfloat, jint};
use jni::JNIEnv;
use wide::*;

/// 批量计算 4x4 矩阵乘法: C[i] = A[i] * B[i]
/// 用于 MTR 列车多节车厢的变换矩阵链式计算
///
/// matrices_a: 连续存储的 N 个 4x4 矩阵 (N * 16 floats)
/// matrices_b: 连续存储的 N 个 4x4 矩阵 (N * 16 floats)
/// output:     输出 N 个 4x4 矩阵
#[no_mangle]
pub extern "C" fn Java_com_mtr_1optimizer_native_NativeMathLib_batchMat4Multiply(
    env: JNIEnv,
    _class: JClass,
    matrices_a: JFloatArray,
    matrices_b: JFloatArray,
    output: JFloatArray,
    count: jint,
) {
    let n = count as usize;
    let len_a = env.get_array_length(&matrices_a).unwrap() as usize;

    // 获取输入数据
    let mut a = vec![0f32; len_a];
    let mut b = vec![0f32; len_a];
    let mut c = vec![0f32; n * 16];

    env.get_float_array_region(&matrices_a, 0, &mut a).unwrap();
    env.get_float_array_region(&matrices_b, 0, &mut b).unwrap();

    // === SIMD 优化的批量矩阵乘法 ===
    for i in 0..n {
        let offset = i * 16;

        // 加载矩阵 A 的行（使用 SIMD 4-wide 向量）
        let a_row0 = f32x4::from([a[offset], a[offset + 1], a[offset + 2], a[offset + 3]]);
        let a_row1 = f32x4::from([a[offset + 4], a[offset + 5], a[offset + 6], a[offset + 7]]);
        let a_row2 = f32x4::from([a[offset + 8], a[offset + 9], a[offset + 10], a[offset + 11]]);
        let a_row3 = f32x4::from([
            a[offset + 12],
            a[offset + 13],
            a[offset + 14],
            a[offset + 15],
        ]);

        // 加载矩阵 B 的列
        let b_col0 = f32x4::from([b[offset], b[offset + 4], b[offset + 8], b[offset + 12]]);
        let b_col1 = f32x4::from([b[offset + 1], b[offset + 5], b[offset + 9], b[offset + 13]]);
        let b_col2 = f32x4::from([b[offset + 2], b[offset + 6], b[offset + 10], b[offset + 14]]);
        let b_col3 = f32x4::from([b[offset + 3], b[offset + 7], b[offset + 11], b[offset + 15]]);

        // 计算结果矩阵（利用 SIMD 并行计算 4 个分量）
        // C[row][col] = dot(A_row, B_col)
        let c00 = (a_row0 * b_col0).reduce_add();
        let c01 = (a_row0 * b_col1).reduce_add();
        let c02 = (a_row0 * b_col2).reduce_add();
        let c03 = (a_row0 * b_col3).reduce_add();

        let c10 = (a_row1 * b_col0).reduce_add();
        let c11 = (a_row1 * b_col1).reduce_add();
        let c12 = (a_row1 * b_col2).reduce_add();
        let c13 = (a_row1 * b_col3).reduce_add();

        let c20 = (a_row2 * b_col0).reduce_add();
        let c21 = (a_row2 * b_col1).reduce_add();
        let c22 = (a_row2 * b_col2).reduce_add();
        let c23 = (a_row2 * b_col3).reduce_add();

        let c30 = (a_row3 * b_col0).reduce_add();
        let c31 = (a_row3 * b_col1).reduce_add();
        let c32 = (a_row3 * b_col2).reduce_add();
        let c33 = (a_row3 * b_col3).reduce_add();

        c[offset] = c00;
        c[offset + 1] = c01;
        c[offset + 2] = c02;
        c[offset + 3] = c03;
        c[offset + 4] = c10;
        c[offset + 5] = c11;
        c[offset + 6] = c12;
        c[offset + 7] = c13;
        c[offset + 8] = c20;
        c[offset + 9] = c21;
        c[offset + 10] = c22;
        c[offset + 11] = c23;
        c[offset + 12] = c30;
        c[offset + 13] = c31;
        c[offset + 14] = c32;
        c[offset + 15] = c33;
    }

    // 写回结果
    env.set_float_array_region(&output, 0, &c).unwrap();
}

/// 批量计算贝塞尔曲线上的点（用于 MTR 轨道曲线）
/// 使用 SIMD 同时计算 4 条曲线的采样点
#[no_mangle]
pub extern "C" fn Java_com_mtr_1optimizer_native_NativeMathLib_batchBezierSample(
    env: JNIEnv,
    _class: JClass,
    control_points: JFloatArray, // N 组控制点，每组 4 个 3D 点 (N * 4 * 3 floats)
    t_values: JFloatArray,       // 每组的采样 t 值数组
    samples_per_curve: jint,
    num_curves: jint,
    output: JFloatArray, // 输出采样点 (N * samples * 3 floats)
) {
    let n = num_curves as usize;
    let spc = samples_per_curve as usize;

    let cp_len = env.get_array_length(&control_points).unwrap() as usize;
    let tv_len = env.get_array_length(&t_values).unwrap() as usize;

    let mut cp = vec![0f32; cp_len];
    let mut tv = vec![0f32; tv_len];
    let mut out = vec![0f32; n * spc * 3];

    env.get_float_array_region(&control_points, 0, &mut cp)
        .unwrap();
    env.get_float_array_region(&t_values, 0, &mut tv).unwrap();

    for i in 0..n {
        let cp_offset = i * 12; // 4 个控制点 * 3 分量

        let p0x = cp[cp_offset];
        let p0y = cp[cp_offset + 1];
        let p0z = cp[cp_offset + 2];
        let p1x = cp[cp_offset + 3];
        let p1y = cp[cp_offset + 4];
        let p1z = cp[cp_offset + 5];
        let p2x = cp[cp_offset + 6];
        let p2y = cp[cp_offset + 7];
        let p2z = cp[cp_offset + 8];
        let p3x = cp[cp_offset + 9];
        let p3y = cp[cp_offset + 10];
        let p3z = cp[cp_offset + 11];

        let tv_offset = i * spc;

        for j in 0..spc {
            let t = tv[tv_offset + j];
            let t2 = t * t;
            let t3 = t2 * t;
            let mt = 1.0 - t;
            let mt2 = mt * mt;
            let mt3 = mt2 * mt;

            // 三次贝塞尔: B(t) = (1-t)³P₀ + 3(1-t)²tP₁ + 3(1-t)t²P₂ + t³P₃
            let b0 = mt3;
            let b1 = 3.0 * mt2 * t;
            let b2 = 3.0 * mt * t2;
            let b3 = t3;

            let out_offset = (i * spc + j) * 3;
            out[out_offset] = b0 * p0x + b1 * p1x + b2 * p2x + b3 * p3x;
            out[out_offset + 1] = b0 * p0y + b1 * p1y + b2 * p2y + b3 * p3y;
            out[out_offset + 2] = b0 * p0z + b1 * p1z + b2 * p2z + b3 * p3z;
        }
    }

    env.set_float_array_region(&output, 0, &out).unwrap();
}
