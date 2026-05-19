package ceui.pixiv.ui.common


import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import ceui.lisa.databinding.FragmentPixivListBinding
import ceui.lisa.databinding.LayoutToolbarBinding
import ceui.lisa.utils.Common
import ceui.lisa.utils.GlideUrlChild
import ceui.loxia.Illust
import ceui.loxia.ObjectPool
import ceui.loxia.getHumanReadableMessage
import ceui.pixiv.ui.task.NamedUrl
import ceui.pixiv.ui.task.TaskPool
import ceui.pixiv.ui.task.TaskStatus
import ceui.pixiv.ui.works.ToggleToolnarViewModel
import ceui.pixiv.utils.FastBlurTransformation
import ceui.pixiv.utils.animateFadeInQuickly
import ceui.pixiv.utils.animateFadeOutQuickly
import ceui.pixiv.utils.setOnClick
import ceui.pixiv.widgets.alertYesOrCancel
import ceui.lisa.utils.AppKit
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.github.panpf.sketch.loadImage
import com.github.panpf.zoomimage.SketchZoomImageView
import com.github.panpf.zoomimage.view.zoom.OnViewTapListener
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

abstract class ImgDisplayFragment(layoutId: Int) : PixivFragment(layoutId) {

    protected val viewModel by viewModels<ToggleToolnarViewModel>()

    abstract val downloadButton: View
    abstract val progressCircular: CircularProgressIndicator
    abstract val displayImg: SketchZoomImageView

    abstract fun displayName(): String
    abstract fun contentUrl(): String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressCircular.max = 100
        displayImg.onViewTapListener = OnViewTapListener { view, _ ->
            viewModel.toggleFullscreen()
        }
        val activity = requireActivity()
        val url = contentUrl()
        if (url.isEmpty()) {
            Log.d(TAG, "ImgDisplayFragment display img: empty")
            return
        }

        Log.d(TAG, "ImgDisplayFragment display img: ${url}")
        val namedUrl = NamedUrl(displayName(), url)
        val task = TaskPool.getLoadTask(namedUrl)
        task.result.observe(viewLifecycleOwner) { file ->
            displayImg.loadImage(file)
            downloadButton.setOnClick {
                performDownload(activity, file)
            }
            val resolution = getImageDimensions(file)
            Common.showLog("sadasd2 bb ${resolution}")
            Common.showLog("sadasd2 cc ${getFileSize(file)}")
        }
        progressCircular.setUpWithTaskStatus(task.status, viewLifecycleOwner)
    }

    private fun performDownload(activity: FragmentActivity, file: File) {
        lifecycleScope.launch {
            val name = displayName()
            val imageId = withContext(Dispatchers.IO) { getImageIdInGallery(activity, name) }
            if (imageId != null) {
                val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId.toString())
                val filePath = AppKit.uri2File(requireContext(), uri)
                if (alertYesOrCancel("图片已存在，确定覆盖下载吗? 文件路径: ${filePath?.path}")) {
                    withContext(Dispatchers.IO) {
                        deleteImageById(activity, imageId)
                        saveImageToGallery(activity, file, name)
                    }
                    recordIllustDownload(file)
                }
            } else {
                withContext(Dispatchers.IO) { saveImageToGallery(activity, file, name) }
                recordIllustDownload(file)
            }
        }
    }

    // 与 Manager.java 标准下载流程一致：成功保存到相册后写一条下载历史，
    // 这样从 PagedImgUrlFragment 触发的「保存」也能出现在「已下载」列表里。
    // 仅当所在 ViewPager 已绑定 illust（PagedImgUrlFragment 设置了 illustId）
    // 时才记录；novel 内嵌图等没有 illust 上下文的入口直接跳过。
    private fun recordIllustDownload(file: File) {
        // no-op: PagedImgUrlFragment removed, illust download recording
        // is handled by the old version's download flow
    }

    override fun onDestroyView() {
        if (viewModel.isFullscreenMode.value == true) {
            val windowInsetsController = WindowInsetsControllerCompat(
                requireActivity().window,
                requireActivity().window.decorView
            )
            // 重新显示系统的状态栏和导航栏
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "ImgDisplayFragment"
    }
}


fun Fragment.setUpFullScreen(
    viewModel: ToggleToolnarViewModel,
    infoItems: List<View>,
    binding: LayoutToolbarBinding
) {
    val windowInsetsController = WindowInsetsControllerCompat(
        requireActivity().window,
        requireActivity().window.decorView
    )
    windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    viewModel.isFullscreenMode.observe(viewLifecycleOwner) { isFullScreen ->
        if (isFullScreen) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
                WindowInsetsCompat.CONSUMED
            }
            infoItems.forEach {
                it.animateFadeOutQuickly()
            }
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                binding.toolbarLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = insets.top
                }
                WindowInsetsCompat.CONSUMED
            }
            infoItems.forEach {
                it.animateFadeInQuickly()
            }
        }
    }
    binding.naviBack.setOnClick {
        findNavController().popBackStack()
    }
}


fun getImageDimensions(file: File): Pair<Int, Int> {
    val options = BitmapFactory.Options().apply {
        // 设置为 true 只解析图片的宽高，不加载图片到内存中
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(file.absolutePath, options)
    return Pair(options.outWidth, options.outHeight)
}

fun getFileSize(file: File): String {
    val fileSizeInBytes = file.length()

    return when {
        fileSizeInBytes < 1000 -> "${fileSizeInBytes}B" // 小于 1KB
        fileSizeInBytes < 1000 * 1000 -> String.format(
            Locale.getDefault(),
            "%.2f KB",
            fileSizeInBytes / 1000f
        ) // 小于 1MB
        fileSizeInBytes < 1000 * 1000 * 1000 -> String.format(
            Locale.getDefault(),
            "%.2f MB",
            fileSizeInBytes / (1000f * 1000)
        ) // 小于 1GB
        else -> String.format(
            Locale.getDefault(),
            "%.2f GB",
            fileSizeInBytes / (1000f * 1000 * 1000)
        ) // 大于等于 1GB
    }
}

fun CircularProgressIndicator.setUpWithTaskStatus(
    taskStatus: LiveData<TaskStatus>,
    lifecycleOwner: LifecycleOwner
) {
    val progressCircular = this
    taskStatus.observe(lifecycleOwner) { status ->
        if (status is TaskStatus.NotStart) {
            progressCircular.isVisible = true
            progressCircular.progress = 0
        } else if (status is TaskStatus.Executing) {
            progressCircular.isVisible = true
            progressCircular.progress = status.percentage
        } else {
            progressCircular.isVisible = false
        }
    }
}

fun CircularProgressIndicator.setUpWithTaskStatus(
    taskStatus: LiveData<TaskStatus>,
    errorLayout: ViewGroup,
    errorTitle: TextView,
    retryButton: TextView,
    errorRetry: () -> Unit,
    lifecycleOwner: LifecycleOwner
) {
    val progressCircular = this
    taskStatus.observe(lifecycleOwner) { status ->
        if (status is TaskStatus.NotStart) {
            progressCircular.isVisible = true
            progressCircular.progress = 0
        } else if (status is TaskStatus.Executing) {
            progressCircular.isVisible = true
            progressCircular.progress = status.percentage
        } else {
            progressCircular.isVisible = false
        }
        errorLayout.isVisible = status is TaskStatus.Error
        retryButton.setOnClick {
            errorRetry()
        }
        if (status is TaskStatus.Error) {
            errorTitle.text = status.exception.getHumanReadableMessage(context)
        }
    }
}

fun Fragment.blurBackground(binding: FragmentPixivListBinding, illustId: Long) {
    val liveIllust = ObjectPool.get<Illust>(illustId)
    liveIllust.observe(viewLifecycleOwner) { illust ->
        Glide.with(this)
            .load(GlideUrlChild(illust.image_urls?.large))
            .apply(bitmapTransform(FastBlurTransformation(15)))
            .into(binding.pageBackground)
    }
}