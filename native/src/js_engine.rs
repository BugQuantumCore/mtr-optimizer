use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, JNI_TRUE, JNI_FALSE};
use boa_engine::{Context, Source, JsValue};

/// 执行 JS 脚本并返回结果
/// 每次调用创建独立 Context，确保绝对的线程安全，且对于低频更新的 PIDS 指示牌性能足够。
#[no_mangle]
pub extern "C" fn Java_com_mtr_1optimizer_native_1bridge_NativeJSRunner_executeScript(
    mut env: JNIEnv,
    _class: JClass,
    script: JString,
    params_json: JString,
) -> jboolean {
    let script_str: String = match env.get_string(&script) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };
    
    let params_str: String = match env.get_string(&params_json) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };

    let mut ctx = Context::default();

    // 注入 JSON 参数到 JS 上下文
    // 使用 JSON.parse 确保复杂对象被正确解析
    let params_injection = format!("var params = JSON.parse('{}');", params_str.replace('\'', "\\'"));
    if ctx.eval(Source::from_bytes(&params_injection)).is_err() {
        return JNI_FALSE;
    }

    // 执行主脚本
    match ctx.eval(Source::from_bytes(&script_str)) {
        Ok(_) => JNI_TRUE,
        Err(_) => JNI_FALSE,
    }
}

/// 从 JS 上下文中提取像素数据 (占位实现)
/// 实际项目中，JS 脚本会将渲染结果写入全局 ArrayBuffer，此处通过 JNI 读取
#[no_mangle]
pub extern "C" fn Java_com_mtr_1optimizer_native_1bridge_NativeJSRunner_extractPixels(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    // TODO: 实现从 Boa Context 中提取 Uint8ClampedArray 并转换为 byte[]
    JNI_TRUE
}