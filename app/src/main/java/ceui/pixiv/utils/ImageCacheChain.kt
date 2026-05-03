package ceui.pixiv.utils

import android.content.Context
import ceui.lisa.R
import ceui.lisa.download.IllustDownload
import ceui.lisa.models.IllustsBean
import ceui.lisa.transformer.LargeBitmapScaleTransformer
import ceui.lisa.utils.GlideUrlChild
import ceui.lisa.utils.Params
import ceui.pixiv.ui.task.TaskPool
import com.bumptech.glide.Glide

object ImageCacheChain {

    @JvmStatic
    @JvmOverloads
    fun peek(context: Context, illust: IllustsBean?, index: Int, url: String? = null): Any? {
        val originalUrl = if (illust != null) {
            IllustDownload.getUrl(illust, index, Params.IMAGE_RESOLUTION_ORIGINAL)
        } else {
            url
        }

        if (originalUrl.isNullOrEmpty()) {
            return null
        }

        // 1. 原图 TaskPool 缓存
        val originalFile = TaskPool.peekCachedFile(originalUrl)
        if (originalFile != null && originalFile.exists()) {
            return originalFile
        }

        if (illust == null) {
            return null
        }

        // 2. 大图 TaskPool 缓存
        val largeUrl = IllustDownload.getUrl(illust, index, Params.IMAGE_RESOLUTION_LARGE)
        if (!largeUrl.isNullOrEmpty()) {
            val largeFile = TaskPool.peekCachedFile(largeUrl)
            if (largeFile != null && largeFile.exists()) {
                return largeFile
            }
        }

        // 3. 大图 Glide 缓存
        if (!largeUrl.isNullOrEmpty()) {
            val resources = context.resources
            val imageSize = resources.displayMetrics.widthPixels -
                2 * resources.getDimensionPixelSize(R.dimen.twelve_dp)
            val overrideHeight = if (index == 0) {
                imageSize * illust.height / illust.width
            } else {
                imageSize
            }

            try {
                // Glide 的 onlyRetrieveFromCache(true) 会尝试从磁盘或内存缓存读取。
                // 如果是在主线程调用，submit().get() 可能会阻塞 IO。
                // 但由于 FragmentImageDetail 原本就在主线程这样做，这里保持逻辑一致。
                val bitmap = Glide.with(context)
                    .asBitmap()
                    .load(GlideUrlChild(largeUrl))
                    .override(imageSize, overrideHeight)
                    .transform(LargeBitmapScaleTransformer())
                    .onlyRetrieveFromCache(true)
                    .submit()
                    .get()
                if (bitmap != null) {
                    return bitmap
                }
            } catch (e: Exception) {
                // Cache miss
            }
        }

        return null
    }
}
