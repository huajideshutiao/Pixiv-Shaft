package ceui.pixiv.utils

import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.widget.ImageView
import com.blankj.utilcode.util.ImageUtils
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

/**
 * 高性能模糊方案
 * 方案A: 针对旧版本，通过降采样 + FastBlur 实现
 * 方案B: 针对 Android 12+，使用 RenderEffect 实现硬件加速模糊
 */

class FastBlurTransformation(private val radius: Int = 15) : BitmapTransformation() {
    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        return ImageUtils.fastBlur(toTransform, 1f, radius.toFloat(), true)
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("ceui.pixiv.utils.FastBlurTransformation(radius=$radius)".toByteArray())
    }

    override fun equals(other: Any?): Boolean {
        return other is FastBlurTransformation && other.radius == radius
    }

    override fun hashCode(): Int {
        return radius
    }
}

fun ImageView.applyBlur(radius: Float = 25f) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP))
    }
}
