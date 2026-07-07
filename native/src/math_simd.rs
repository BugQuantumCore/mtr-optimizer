use jni::JNIEnv;
use jni::objects::{JClass, JFloatArray};
use jni::sys::jfloat;

/// 计算三次贝塞尔曲线上的点 (纯 Rust 实现，后续可替换为 std::simd)
/// P(t) = (1-t)^3*P0 + 3(1-t)^2*t*P1 + 3(1-t)*t^2*P2 + t^3*P3
#[no_mangle]
pub extern "C" fn Java_com_mtr_1optimizer_native_1bridge_NativeMath_simdBezier(
    mut env: JNIEnv,
    _class: JClass,
    t: jfloat,
    p0: JFloatArray,
    p1: JFloatArray,
    p2: JFloatArray,
    p3: JFloatArray,
    out: JFloatArray,
) {
    let mut v0 = [0.0f32; 3];
    let mut v1 = [0.0f32; 3];
    let mut v2 = [0.0f32; 3];
    let mut v3 = [0.0f32; 3];

    // 安全读取 JNI 数组
    if env.get_float_array_region(&p0, 0, &mut v0).is_err() ||
       env.get_float_array_region(&p1, 0, &mut v1).is_err() ||
       env.get_float_array_region(&p2, 0, &mut v2).is_err() ||
       env.get_float_array_region(&p3, 0, &mut v3).is_err() {
        return;
    }

    let mut result = [0.0f32; 3];
    let u = 1.0 - t;
    let uu = u * u;
    let uuu = uu * u;
    let tt = t * t;
    let ttt = tt * t;

    for i in 0..3 {
        result[i] = uuu * v0[i] 
                  + 3.0 * uu * t * v1[i] 
                  + 3.0 * u * tt * v2[i] 
                  + ttt * v3[i];
    }

    // 写回结果
    let _ = env.set_float_array_region(&out, 0, &result);
}