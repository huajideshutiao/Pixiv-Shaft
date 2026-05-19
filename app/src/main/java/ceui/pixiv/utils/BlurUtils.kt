package ceui.pixiv.utils

import android.graphics.Bitmap
import ceui.lisa.utils.AppKit
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

/**
 * Glide BitmapTransformation 包装，用于在 Glide 加载流程中应用高斯模糊
 * 
 * 底层使用 RenderScript ScriptIntrinsicBlur，GPU 加速，性能优秀
 * 
 * @param radius 模糊半径，范围 0~25
 */
class FastBlurTransformation(private val radius: Int = 15) : BitmapTransformation() {
    
    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        // 直接调用 AppKit.fastBlur，使用 RenderScript 进行高斯模糊
        return AppKit.fastBlur(toTransform, radius.toFloat())
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        // 用于 Glide 缓存 key，不同 radius 产生不同的缓存
        messageDigest.update("ceui.pixiv.utils.FastBlurTransformation(radius=$radius)".toByteArray())
    }

    override fun equals(other: Any?): Boolean {
        return other is FastBlurTransformation && other.radius == radius
    }

    override fun hashCode(): Int {
        return radius
    }
}