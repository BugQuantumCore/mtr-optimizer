use jni::objects::{JByteArray, JClass, JIntArray};
use jni::sys::{jboolean, jint, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use rectpack::{Packer, PackerConfig, Rect};

/// 将多个小纹理打包到一个大 Atlas 中
/// 返回每个小纹理在 Atlas 中的 (x, y) 坐标偏移量
#[no_mangle]
pub extern "C" fn Java_com_mtr_1optimizer_native_1bridge_NativeJSRunner_packTextures(
    mut env: JNIEnv,
    _class: JClass,
    widths: JIntArray,
    heights: JIntArray,
    atlas_width: jint,
    atlas_height: jint,
    out_coords: JIntArray, // 输出数组，长度为 count * 2 (x, y 交替)
) -> jboolean {
    let count = env.get_array_length(&widths).unwrap() as usize;

    let mut w_vec = vec![0; count];
    let mut h_vec = vec![0; count];
    env.get_int_array_region(&widths, 0, &mut w_vec).unwrap();
    env.get_int_array_region(&heights, 0, &mut h_vec).unwrap();

    // 配置矩形打包器
    let config = PackerConfig {
        width: atlas_width as u32,
        height: atlas_height as u32,
        ..Default::default()
    };
    let mut packer = Packer::new(config);

    let mut rects: Vec<Rect> = (0..count)
        .map(|i| Rect::new(w_vec[i] as u32, h_vec[i] as u32, i as u32))
        .collect();

    // 执行打包
    if packer.pack(&mut rects).is_err() {
        return JNI_FALSE; // 空间不足，打包失败
    }

    // 提取结果坐标
    let mut out_vec = vec![0; count * 2];
    for rect in rects {
        let idx = rect.id as usize;
        out_vec[idx * 2] = rect.x as i32;
        out_vec[idx * 2 + 1] = rect.y as i32;
    }

    env.set_int_array_region(&out_coords, 0, &out_vec).unwrap();
    JNI_TRUE
}
