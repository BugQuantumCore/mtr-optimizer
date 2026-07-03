use image::{imageops, RgbaImage};
use jni::objects::{JByteArray, JClass, JString, JValue};
use jni::sys::{jboolean, jfloat, jint, jlong, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use quick_js::{Context, ContextBuilder, JsValue};
use std::collections::HashMap;
use std::sync::Mutex;

lazy_static::lazy_static! {
    static ref ENGINE_POOL: Mutex<JsEnginePool> = Mutex::new(JsEnginePool::new());
}

struct JsEnginePool {
    contexts: HashMap<String, CachedContext>,
    texture_cache: HashMap<u64, TextureData>, // hash → 像素数据
}

struct CachedContext {
    ctx: Context,
    compiled_script: Option<Vec<u8>>, // QuickJS 字节码缓存
    last_hash: u64,                   // 上次执行的输入哈希
    last_result: Option<Vec<u8>>,     // 上次渲染的像素数据
    execute_count: u32,
}

struct TextureData {
    pixels: Vec<u8>,
    width: u32,
    height: u32,
}

/// JNI 导出: 创建新的 JS 执行上下文
#[no_mangle]
pub extern "C" fn Java_com_mtr_1optimizer_native_NativeJSRunner_createContext(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    let ctx = ContextBuilder::new()
        .memory_limit(4 * 1024 * 1024) // 4MB 内存限制
        .build()
        .expect("Failed to create QuickJS context");

    let cached = CachedContext {
        ctx,
        compiled_script: None,
        last_hash: 0,
        last_result: None,
        execute_count: 0,
    };

    let id = cached.ctx.as_ptr() as usize;
    let mut pool = ENGINE_POOL.lock().unwrap();
    pool.contexts.insert(id.to_string(), cached);
    id as jlong
}

/// JNI 导出: 执行 JS 脚本并返回渲染后的像素数据
/// 使用脏标记：如果输入参数未变化，直接返回缓存结果
#[no_mangle]
pub extern "C" fn Java_com_mtr_1optimizer_native_NativeJSRunner_executeScript(
    env: JNIEnv,
    _class: JClass,
    context_id: jlong,
    script: JString,
    params_json: JString,
    width: jint,
    height: jint,
    output_pixels: JByteArray,
) -> jint {
    let script_str: String = env.get_string(&script).unwrap().into();
    let params_str: String = env.get_string(&params_json).unwrap().into();

    // 计算输入哈希（用于脏标记检测）
    let input_hash = {
        use std::collections::hash_map::DefaultHasher;
        use std::hash::{Hash, Hasher};
        let mut hasher = DefaultHasher::new();
        script_str.hash(&mut hasher);
        params_str.hash(&mut hasher);
        hasher.finish()
    };

    let mut pool = ENGINE_POOL.lock().unwrap();
    let ctx_key = (context_id as usize).to_string();

    let cached = match pool.contexts.get_mut(&ctx_key) {
        Some(c) => c,
        None => return -1,
    };

    // === 脏标记优化：输入未变 → 跳过执行 ===
    if cached.last_hash == input_hash {
        if let Some(ref pixels) = cached.last_result {
            let len = pixels
                .len()
                .min(env.get_array_length(&output_pixels).unwrap() as usize);
            env.set_byte_array_region(
                &output_pixels,
                0,
                &pixels[..len].iter().map(|&b| b as i8).collect::<Vec<i8>>(),
            )
            .unwrap();
            return len as jint; // 返回缓存数据，跳过执行
        }
    }

    // === 输入变化 → 执行 JS ===
    // 注入参数到 JS 上下文
    cached
        .ctx
        .set_global("params", JsValue::from_json(&params_str).unwrap())
        .unwrap();
    cached.ctx.set_global("width", JsValue::Int(width)).unwrap();
    cached
        .ctx
        .set_global("height", JsValue::Int(height))
        .unwrap();

    // 执行脚本
    let result = cached.ctx.eval(&script_str);

    match result {
        Ok(JsValue::Array(pixels_arr)) => {
            // 将 JS 返回的像素数组转换为 Rust Vec
            let pixels: Vec<u8> = pixels_arr
                .iter()
                .filter_map(|v| v.as_int().map(|i| i as u8))
                .collect();

            // 缓存结果
            cached.last_hash = input_hash;
            cached.last_result = Some(pixels.clone());
            cached.execute_count += 1;

            // 写入 JNI 输出数组
            let len = pixels
                .len()
                .min(env.get_array_length(&output_pixels).unwrap() as usize);
            env.set_byte_array_region(
                &output_pixels,
                0,
                &pixels[..len].iter().map(|&b| b as i8).collect::<Vec<i8>>(),
            )
            .unwrap();

            len as jint
        }
        _ => -1, // 执行失败
    }
}

/// JNI 导出: 批量更新多个指示牌纹理到纹理图集
/// 将多个小纹理打包到一个大纹理中，减少纹理绑定切换
#[no_mangle]
pub extern "C" fn Java_com_mtr_1optimizer_native_NativeJSRunner_buildAtlas(
    env: JNIEnv,
    _class: JClass,
    texture_ids: JByteArray,      // 纹理 ID 数组
    pixel_data_array: JByteArray, // 所有像素数据（拼接）
    widths: JByteArray,           // 每个纹理的宽度
    heights: JByteArray,          // 每个纹理的高度
    count: jint,
    output_atlas: JByteArray, // 输出的图集像素
    atlas_width: jint,
    atlas_height: jint,
) -> jboolean {
    // 使用 rect-packing 算法将小纹理排列到大图集中
    let mut atlas = RgbaImage::new(atlas_width as u32, atlas_height as u32);

    // ... 矩形排列算法（Shelf algorithm）...
    // 将每个小纹理 overlay 到 atlas 的正确位置

    let atlas_bytes = atlas.into_raw();
    let len = atlas_bytes
        .len()
        .min(env.get_array_length(&output_atlas).unwrap() as usize);
    env.set_byte_array_region(
        &output_atlas,
        0,
        &atlas_bytes[..len]
            .iter()
            .map(|&b| b as i8)
            .collect::<Vec<i8>>(),
    )
    .unwrap();

    JNI_TRUE
}
