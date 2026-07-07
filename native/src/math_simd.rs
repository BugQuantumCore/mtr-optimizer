use jni::objects::{JClass, JFloatArray};
use jni::sys::jfloat;
use jni::JNIEnv;

// 注意函数名变更：native -> nativebridge，且类名对应 NativeMathLib
#[no_mangle]
pub extern "C" fn Java_com_mtr_1optimizer_nativebridge_NativeMathLib_simdBezier(
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

    if env.get_float_array_region(&p0, 0, &mut v0).is_err()
        || env.get_float_array_region(&p1, 0, &mut v1).is_err()
        || env.get_float_array_region(&p2, 0, &mut v2).is_err()
        || env.get_float_array_region(&p3, 0, &mut v3).is_err()
    {
        return;
    }

    let mut result = [0.0f32; 3];
    let u = 1.0 - t;
    let uu = u * u;
    let uuu = uu * u;
    let tt = t * t;
    let ttt = tt * t;

    for i in 0..3 {
        result[i] = uuu * v0[i] + 3.0 * uu * t * v1[i] + 3.0 * u * tt * v2[i] + ttt * v3[i];
    }

    let _ = env.set_float_array_region(&out, 0, &result);
}
