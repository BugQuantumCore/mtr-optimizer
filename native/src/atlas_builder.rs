use jni::objects::{JClass, JIntArray};
use jni::sys::{jboolean, jint, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;

// 注意函数名变更：native -> nativebridge
#[no_mangle]
pub extern "C" fn Java_com_mtr_1optimizer_nativebridge_NativeJSRunner_packTextures(
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

    if env.get_int_array_region(&widths, 0, &mut w_vec).is_err()
        || env.get_int_array_region(&heights, 0, &mut h_vec).is_err()
    {
        return JNI_FALSE;
    }

    let max_w = atlas_width as u32;
    let max_h = atlas_height as u32;

    let mut cursor_x: u32 = 0;
    let mut cursor_y: u32 = 0;
    let mut shelf_height: u32 = 0;
    let mut out_vec = vec![0; count * 2];

    for i in 0..count {
        let w = w_vec[i] as u32;
        let h = h_vec[i] as u32;

        if cursor_x + w > max_w {
            cursor_x = 0;
            cursor_y += shelf_height;
            shelf_height = 0;
        }

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
