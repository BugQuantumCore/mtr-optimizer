mod atlas_builder;
mod js_engine;
mod math_simd;

use jni::objects::JClass;
use jni::sys::jstring;
use jni::JNIEnv;

/// JNI 初始化钩子，当 Java 端调用 System.loadLibrary 时自动执行
#[no_mangle]
pub extern "C" fn JNI_OnLoad(_vm: jni::JavaVM, _reserved: *mut std::ffi::c_void) -> jni::sys::jint {
    // 返回 JNI 版本号
    jni::sys::JNI_VERSION_1_8
}

/// 获取原生库版本信息（用于 Java 端校验兼容性）
#[no_mangle]
pub extern "C" fn Java_com_mtr_1optimizer_native_1bridge_NativeLoader_getNativeVersion(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let version = env!("CARGO_PKG_VERSION");
    env.new_string(version)
        .expect("Failed to create JNI string")
        .into_raw()
}
