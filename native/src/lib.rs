mod js_engine;
mod math_simd;
mod atlas_builder;

use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::jstring;

/// JNI 初始化钩子
#[no_mangle]
pub extern "C" fn JNI_OnLoad(
    _vm: jni::JavaVM, 
    _reserved: *mut std::ffi::c_void
) -> jni::sys::jint {
    jni::sys::JNI_VERSION_1_8
}

/// 获取原生库版本信息
#[no_mangle]
pub extern "C" fn Java_com_mtr_1optimizer_native_1bridge_NativeLoader_getNativeVersion(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let version = env!("CARGO_PKG_VERSION");
    env.new_string(version)
        .expect("Failed to create JNI string")
        .into_raw()
}