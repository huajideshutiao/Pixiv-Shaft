package ceui.lisa.fragments


import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import ceui.lisa.R
import ceui.lisa.activities.ImageDetailActivity
import ceui.lisa.activities.Shaft
import ceui.lisa.databinding.FragmentImageDetailBinding
import ceui.lisa.download.IllustDownload
import ceui.lisa.models.IllustsBean
import ceui.lisa.transformer.LargeBitmapScaleTransformer
import ceui.lisa.utils.Common
import ceui.lisa.utils.GlideUrlChild
import ceui.lisa.utils.Params
import ceui.pixiv.ui.common.deleteImageById
import ceui.pixiv.ui.common.getImageIdInGallery
import ceui.pixiv.ui.common.saveImageToGallery
import ceui.pixiv.ui.common.setUpWithTaskStatus
import ceui.pixiv.ui.task.NamedUrl
import ceui.pixiv.ui.task.TaskPool
import ceui.pixiv.ui.works.ToggleToolnarViewModel
import ceui.pixiv.utils.setOnClick
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.panpf.sketch.loadImage
import com.github.panpf.zoomimage.view.zoom.OnViewLongPressListener
import com.github.panpf.zoomimage.view.zoom.OnViewTapListener
import com.github.panpf.zoomimage.zoom.ReadMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class FragmentImageDetail : BaseFragment<FragmentImageDetailBinding?>() {
    private var index = 0
    private var url: String? = null
    private var saveName: String? = null
    private var currentImageFile: File? = null
    private val viewModel by viewModels<ToggleToolnarViewModel>(ownerProducer = { requireActivity() })

    private val mIllustsBean: IllustsBean?
        get() = (activity as? ImageDetailActivity)?.mIllustsBean

    public override fun initBundle(bundle: Bundle) {
        url = bundle.getString(Params.URL)
        index = bundle.getInt(Params.INDEX)
        saveName = bundle.getString(Params.TITLE)
    }

    public override fun initLayout() {
        mLayoutID = R.layout.fragment_image_detail
    }

    override fun initView() {
        baseBind.emptyActionButton.setOnClickListener { loadImage() }
        if (Shaft.sSettings.isIllustDetailKeepScreenOn) {
            baseBind.root.keepScreenOn = true
        }
        baseBind.image.onViewTapListener = OnViewTapListener { _, _ ->
            viewModel.toggleFullscreen()
        }
        baseBind.image.onViewLongPressListener = OnViewLongPressListener { _, _ ->
            val file = currentImageFile
            if (file != null && file.exists()) {
                val bean = mIllustsBean
                val fileName = if (bean != null) {
                    "${bean.id}_p${index}.jpg"
                } else {
                    file.name
                }
                Common.shareImageFile(requireActivity(), file, fileName)
            }
        }
        baseBind.image.transitionName = "image_$index"
        baseBind.image.zoomable.setReadMode(ReadMode.Default)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadImage()
    }

    private fun startTransition() {
        val initialIndex = (activity as? ImageDetailActivity)?.initialIndex ?: 0
        if (index == initialIndex) {
            activity?.startPostponedEnterTransition()
        }
    }

    val detailImage: ImageView
        get() = baseBind.image

    /**
     * 捕获完整图片的物理状态。
     * 终极修复：绕过布局测量，直接进行原子级 Bounds 锁定。
     */
    fun prepareForExit() {
        try {
            val zoomable = baseBind.image.zoomable

            // 0. 强制制动缩放引擎的所有内部动画（如手势回弹、进入缩放动画等）
            // 解决“快速进出导致异常拉伸”的竞态问题。
            try {
                // 由于方法为 internal suspend，此处通过反射绕过访问限制。
                zoomable.javaClass.getMethod("stopAllAnimation", String::class.java)
                    .invoke(zoomable, "prepareForExit")
            } catch (e: Exception) {
            }

            // 1. 提取当前图片内容相对于 View 容器的物理矩形。
            val contentRect = zoomable.contentDisplayRectState.value
            if (contentRect.isEmpty || contentRect.width <= 0 || contentRect.height <= 0) return

            // 2. 获取当前 View 在窗口中的位置。
            val loc = IntArray(2)
            baseBind.image.getLocationInWindow(loc)

            // 3. 获取父容器在窗口中的位置。
            val parent = baseBind.image.parent as? View ?: return
            val pLoc = IntArray(2)
            parent.getLocationInWindow(pLoc)

            // 4. [原子级操作] 锁定物理属性并重置变换，绕过 RelativeLayout 的 Double Measure Pass。
            // 使用 FIT_XY 确保图片撑满我们手动设置的物理 Bounds。
            // 直接调用 layout() 方法修改 mLeft, mTop, mRight, mBottom，Transition 引擎捕获时能立即读取到最终态。
            baseBind.image.apply {
                scaleType = ImageView.ScaleType.FIT_XY
                imageMatrix = Matrix()
                scaleX = 1.0f
                scaleY = 1.0f
                translationX = 0f
                translationY = 0f
                layout(
                    (loc[0] + contentRect.left - pLoc[0]).toInt(),
                    (loc[1] + contentRect.top - pLoc[1]).toInt(),
                    (loc[0] + contentRect.right - pLoc[0]).toInt(),
                    (loc[1] + contentRect.bottom - pLoc[1]).toInt()
                )
            }

            // 5. 递归禁用裁剪，保证在回归过程中可见。
            var p: Any? = baseBind.image.parent
            while (p is ViewGroup) {
                p.clipChildren = false
                p.clipToPadding = false
                p = p.parent
            }

            baseBind.downloadButton.visibility = View.GONE
            baseBind.progressCircular.visibility = View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "prepareForExit failed", e)
        }
    }

    private fun loadImage() {
        baseBind.emptyFrame.visibility = View.GONE
        val isUrlMode = mIllustsBean == null && !url.isNullOrEmpty()
        val imageUrl: String? = if (isUrlMode) {
            url
        } else {
            IllustDownload.getUrl(mIllustsBean, index, Params.IMAGE_RESOLUTION_ORIGINAL)
        }

        if (imageUrl.isNullOrEmpty()) return

        if (imageUrl.startsWith("content://")) {
            baseBind.image.loadImage(Uri.parse(imageUrl))
            startTransition()
            return
        }

        // 1. 优先使用本地缓存
        TaskPool.peekCachedFile(imageUrl)?.takeIf { it.exists() }?.let {
            baseBind.image.loadImage(it)
            currentImageFile = it
            startTransition()
        }

        // 2. 异步加载大图作为占位（仅在本地无缓存时）
        if (currentImageFile == null) {
            val largeUrl = if (isUrlMode) url else IllustDownload.getUrl(
                mIllustsBean,
                index,
                Params.IMAGE_RESOLUTION_LARGE
            )
            if (!largeUrl.isNullOrEmpty()) {
                val ctx = requireContext().applicationContext
                val imageSize = ctx.resources.displayMetrics.widthPixels -
                    2 * ctx.resources.getDimensionPixelSize(R.dimen.twelve_dp)
                val overrideHeight = if (index == 0 && mIllustsBean != null) {
                    imageSize * mIllustsBean!!.height / mIllustsBean!!.width
                } else {
                    imageSize
                }

                Glide.with(this)
                    .asBitmap()
                    .load(GlideUrlChild(largeUrl))
                    .override(imageSize, overrideHeight)
                    .transform(LargeBitmapScaleTransformer())
                    .onlyRetrieveFromCache(true)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            if (currentImageFile == null) {
                                baseBind.image.setImageBitmap(resource)
                                startTransition()
                            }
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            if (currentImageFile == null) startTransition()
                        }
                    })
            } else {
                startTransition()
            }
        }

        // 3. 加载/监听原图任务
        val task = TaskPool.getLoadTask(NamedUrl("", imageUrl))
        task.result.observe(viewLifecycleOwner) { file ->
            if (file != null && file.exists()) {
                if (currentImageFile != file) {
                    baseBind.image.loadImage(file)
                    currentImageFile = file
                }
                startTransition()

                if (isUrlMode) {
                    baseBind.downloadButton.visibility = View.VISIBLE
                    baseBind.downloadButton.setOnClick {
                        val ext = imageUrl.substringAfterLast('.', "jpg")
                        val displayName =
                            if (!saveName.isNullOrEmpty()) "$saveName.$ext" else imageUrl.substringAfterLast(
                                '/'
                            )
                        val appContext = requireContext().applicationContext
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            val imageId = getImageIdInGallery(appContext, displayName)
                            if (imageId != null) deleteImageById(appContext, imageId)
                            saveImageToGallery(appContext, file, displayName)
                        }
                    }
                }
            }
        }
        baseBind.progressCircular.setUpWithTaskStatus(task.status, viewLifecycleOwner)
    }

    companion object {
        private const val TAG = "FragmentImageDetail"
        @JvmStatic
        fun newInstance(index: Int): FragmentImageDetail {
            val args = Bundle()
            args.putInt(Params.INDEX, index)
            val fragment = FragmentImageDetail()
            fragment.arguments = args
            return fragment
        }

        @JvmStatic
        @JvmOverloads
        fun newInstance(pUrl: String?, pSaveName: String? = null): FragmentImageDetail {
            val args = Bundle()
            args.putString(Params.URL, pUrl)
            if (pSaveName != null) {
                args.putString(Params.TITLE, pSaveName)
            }
            val fragment = FragmentImageDetail()
            fragment.arguments = args
            return fragment
        }
    }
}
