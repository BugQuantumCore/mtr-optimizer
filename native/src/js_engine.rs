use boa_engine::{Context, Source};
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;

// 注意函数名变更：native -> nativebridge
#[no_mangle]
pub extern "C" fn Java_com_mtr_1optimizer_nativebridge_NativeJSRunner_executeScript(
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

    let params_injection = format!(
        "var params = JSON.parse('{}');",
        params_str.replace('\'', "\\'")
    );
    if ctx.eval(Source::from_bytes(&params_injection)).is_err() {
        return JNI_FALSE;
    }

    match ctx.eval(Source::from_bytes(&script_str)) {
        Ok(_) => JNI_TRUE,
        Err(_) => JNI_FALSE,
    }
}

#[no_mangle]
pub extern "C" fn Java_com_mtr_1optimizer_nativebridge_NativeJSRunner_extractPixels(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    JNI_TRUE
}
