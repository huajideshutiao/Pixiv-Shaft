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

        // 1. 原图 TaskPool 缓存 (本地文件，最快)
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

        // 3. 尝试从 Glide 内存缓存中直接获取 (同步且安全)
        // 注意：这里由于我们修复了 GlideUrlChild 的 cacheKey，命中率会大幅提升
        if (!largeUrl.isNullOrEmpty()) {
            val imageSize = context.resources.displayMetrics.widthPixels -
                2 * context.resources.getDimensionPixelSize(R.dimen.twelve_dp)
            val overrideHeight = if (index == 0) {
                imageSize * illust.height / illust.width
            } else {
                imageSize
            }

            try {
                // 仅同步获取。如果命中内存缓存会很快。
                // 如果需要从磁盘读取，submit().get() 会阻塞，但在 transition 期间我们希望尽量快。
                return Glide.with(context.applicationContext)
                    .asBitmap()
                    .load(GlideUrlChild(largeUrl))
                    .override(imageSize, overrideHeight)
                    .transform(LargeBitmapScaleTransformer())
                    .onlyRetrieveFromCache(true)
                    .submit()
                    .get()
            } catch (e: Exception) {
                // ignore
            }
        }

        return null
    }
}
