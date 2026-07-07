use jni::JNIEnv;
use jni::objects::{JClass, JIntArray};
use jni::sys::{jint, jboolean, JNI_TRUE, JNI_FALSE};

#[no_mangle]
pub extern "C" fn Java_com_mtr_1optimizer_native_1bridge_NativeJSRunner_packTextures(
    mut env: JNIEnv,
    _class: JClass,
    widths: JIntArray,
    heights: JIntArray,
    atlas_width: jint,
    atlas_height: jint,
    out_coords: JIntArray,
) -> jboolean {
    let count = env.get_array_length(&widths).unwrap() as usize;
    if count == 0 {
        return JNI_TRUE;
    }
    
    let mut w_vec = vec![0; count];
    let mut h_vec = vec![0; count];
    
    if env.get_int_array_region(&widths, 0, &mut w_vec).is_err() ||
       env.get_int_array_region(&heights, 0, &mut h_vec).is_err() {
        return JNI_FALSE;
    }

    let max_w = atlas_width as u32;
    let max_h = atlas_height as u32;

    // 简单的 Shelf (货架) 矩形打包算法
    let mut cursor_x: u32 = 0;
    let mut cursor_y: u32 = 0;
    let mut shelf_height: u32 = 0;
    let mut out_vec = vec![0; count * 2];

    for i in 0..count {
        let w = w_vec[i] as u32;
        let h = h_vec[i] as u32;

        // 如果当前行放不下，换行
        if cursor_x + w > max_w {
            cursor_x = 0;
            cursor_y += shelf_height;
            shelf_height = 0;
        }

        // 如果垂直方向也超出了，说明 Atlas 空间不足
        if cursor_y + h > max_h {
            return JNI_FALSE; 
        }

        out_vec[i * 2] = cursor_x as i32;
        out_vec[i * 2 + 1] = cursor_y as i32;

        cursor_x += w;
        if h > shelf_height {
            shelf_height = h;
        }
    }

    if env.set_int_array_region(&out_coords, 0, &out_vec).is_err() {
        return JNI_FALSE;
    }
    
    JNI_TRUE
}