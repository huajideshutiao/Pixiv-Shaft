package ceui.pixiv.ui.works

import android.os.Build
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import ceui.lisa.databinding.FragmentPixivListBinding
import ceui.lisa.utils.GlideUrlChild
import ceui.loxia.Illust
import ceui.loxia.ObjectPool
import ceui.pixiv.ui.task.NamedUrl
import ceui.pixiv.ui.task.TaskPool
import ceui.pixiv.utils.FastBlurTransformation
import ceui.pixiv.utils.applyBlur
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import kotlinx.coroutines.launch


fun getGalleryHolders(illust: Illust): List<GalleryHolder>? {
    // Helper function to create a GalleryHolder
    fun createGalleryHolder(index: Int, imageUrl: String?): GalleryHolder {
        val url = imageUrl.orEmpty()
        val task = TaskPool.getLoadTask(
            NamedUrl(url.substringAfterLast('/'), url),
            autoStart = false
        )
        return GalleryHolder(illust, index, task) {
            TaskPool.scope.launch { task.execute() }
        }
    }

    return when {
        illust.page_count == 1 -> {
            // Single page handling
            val imageUrl = illust.meta_single_page?.original_image_url
            listOf(createGalleryHolder(0, imageUrl))
        }
        !illust.meta_pages.isNullOrEmpty() -> {
            // Multiple pages handling
            illust.meta_pages.mapIndexed { index, metaPage ->
                createGalleryHolder(index, metaPage.image_urls?.original)
            }
        }
        else -> null
    }
}

fun Fragment.blurBackground(binding: FragmentPixivListBinding, illustId: Long) {
    val illust = ObjectPool.get<Illust>(illustId).value ?: return
    binding.dimmer.isVisible = true

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        binding.pageBackground.applyBlur(25f)
        Glide.with(this)
            .load(GlideUrlChild(illust.image_urls?.large))
            .override(200) // 限制加载尺寸以极大提升性能
            .transition(withCrossFade())
            .into(binding.pageBackground)
    } else {
        Glide.with(this)
            .load(GlideUrlChild(illust.image_urls?.large))
            .override(200)
            .apply(bitmapTransform(FastBlurTransformation(15)))
            .transition(withCrossFade())
            .into(binding.pageBackground)
    }
}
