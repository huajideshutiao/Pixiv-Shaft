package ceui.lisa.fragments


import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import ceui.lisa.R
import ceui.lisa.activities.ImageDetailActivity
import ceui.lisa.activities.Shaft
import ceui.lisa.databinding.FragmentImageDetailBinding
import ceui.lisa.download.IllustDownload
import ceui.lisa.models.IllustsBean
import ceui.lisa.utils.Common
import ceui.lisa.utils.GlideUrlChild
import ceui.lisa.utils.Params
import ceui.lisa.utils.ShareIllust
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
import com.github.panpf.zoomimage.view.zoom.OnViewTapListener
import com.github.panpf.zoomimage.zoom.ReadMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FragmentImageDetail : BaseFragment<FragmentImageDetailBinding?>() {
    private var index = 0
    private var url: String? = null
    private var saveName: String? = null
    private var currentImageFile: File? = null
    private val viewModel by viewModels<ToggleToolnarViewModel>(ownerProducer = { requireActivity() })

    // 不再放进 arguments / savedInstanceState，避免每个 Fragment 重复持久化 80KB IllustsBean
    // 导致 TransactionTooLargeException。统一向 ImageDetailActivity 取。
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
        baseBind.emptyActionButton.setOnClickListener { v: View? -> loadImage() }
        //插画二级详情保持屏幕常亮
        if (Shaft.sSettings.isIllustDetailKeepScreenOn) {
            baseBind.root.keepScreenOn = true
        }
        baseBind.image.onViewTapListener = OnViewTapListener { _, _ ->
            viewModel.toggleFullscreen()
        }
        baseBind.image.setOnLongClickListener {
            val file = currentImageFile
            if (file != null && file.exists()) {
                val illust = mIllustsBean
                val shareText = illust?.let {
                    getString(
                        R.string.share_illust,
                        it.title,
                        it.user?.name,
                        ShareIllust.URL_Head + it.id
                    )
                }
                Common.shareImageFile(
                    requireContext(),
                    file,
                    "${illust?.id ?: "image"}_p$index.jpg",
                    shareText
                )
            }
            true
        }
        // 长图阅读模式：自动填满宽度、��顶部开始，无需手动双击放大再滑动
        baseBind.image.zoomable.setReadMode(ReadMode.Default)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadImage()
    }

    private fun loadImage() {
        baseBind.emptyFrame.visibility = View.GONE
        val isUrlMode = mIllustsBean == null && !TextUtils.isEmpty(url)
        val imageUrl: String? = if (isUrlMode) {
            url
        } else {
            IllustDownload.getUrl(mIllustsBean, index, Params.IMAGE_RESOLUTION_ORIGINAL)
        }

        val shortUrl = imageUrl?.substringAfterLast('/') ?: "null"
        Log.d(TAG, "[ImageDetail] loadImage index=$index, isUrlMode=$isUrlMode, url=$shortUrl")

        if (imageUrl?.isNotEmpty() == true) {
            // content:// URI（来自下载完成页的 SAF 路径）直接用 Sketch 加载，
            // 不走 TaskPool/Glide，因为 Glide 没有 SAF URI 的访问权限。
            if (imageUrl.startsWith("content://")) {
                baseBind.image.loadImage(Uri.parse(imageUrl))
                return
            }

            val task = TaskPool.getLoadTask(NamedUrl("", imageUrl))
            Log.d(
                TAG,
                "[ImageDetail] task acquired. taskId=${task.taskId}, status=${task.status.value}, hasResult=${task.result.value != null}, url=$shortUrl"
            )

            // 原图尚未加载完时，若一级详情页的大图已在缓存，先用大图占位
            if (mIllustsBean != null && task.result.value == null) {
                val largeUrl = IllustDownload.getUrl(
                    mIllustsBean, index, Params.IMAGE_RESOLUTION_LARGE
                )
                if (!largeUrl.isNullOrEmpty() && largeUrl != imageUrl) {
                    val largeFile = TaskPool.peekCachedFile(largeUrl)
                    if (largeFile != null) {
                        Log.d(
                            TAG,
                            "[ImageDetail] placeholder HIT (TaskPool) path=${largeFile.absolutePath} size=${largeFile.length()}"
                        )
                        baseBind.image.loadImage(largeFile)
                        currentImageFile = largeFile
                    } else {
                        Log.d(
                            TAG,
                            "[ImageDetail] placeholder MISS (TaskPool), trying Glide cache largeUrl=${
                                largeUrl.substringAfterLast('/')
                            }"
                        )
                        tryLoadFromGlideCache(largeUrl)
                    }
                }
            }

            task.result.observe(viewLifecycleOwner) { file ->
                Log.d(
                    TAG,
                    "[ImageDetail] result callback. file=${file?.absolutePath}, exists=${file?.exists()}, size=${file?.length() ?: -1}, url=$shortUrl"
                )
                baseBind.image.loadImage(file)
                currentImageFile = file
                if (isUrlMode) {
                    baseBind.downloadButton.visibility = View.VISIBLE
                    baseBind.downloadButton.setOnClick {
                        val ext = imageUrl.substringAfterLast('.', "jpg")
                        val displayName = if (!saveName.isNullOrEmpty()) {
                            "$saveName.$ext"
                        } else {
                            imageUrl.substringAfterLast('/')
                        }
                        val ctx = requireActivity()
                        viewLifecycleOwner.lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                val imageId = getImageIdInGallery(ctx, displayName)
                                if (imageId != null) {
                                    deleteImageById(ctx, imageId)
                                }
                                saveImageToGallery(ctx, file, displayName)
                            }
                        }
                    }
                }
            }
            baseBind.progressCircular.setUpWithTaskStatus(task.status, viewLifecycleOwner)
        }
    }

    private fun tryLoadFromGlideCache(url: String) {
        Glide.with(this)
            .asFile()
            .load(GlideUrlChild(url))
            .onlyRetrieveFromCache(true)
            .into(object : CustomTarget<File>() {
                override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                    if (currentImageFile == null) {
                        Log.d(
                            TAG,
                            "[ImageDetail] placeholder HIT (Glide) path=${resource.absolutePath} size=${resource.length()}"
                        )
                        baseBind.image.loadImage(resource)
                        currentImageFile = resource
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    Log.d(
                        TAG,
                        "[ImageDetail] placeholder MISS (Glide) url=${url.substringAfterLast('/')}"
                    )
                }
            })
    }

    companion object {
        private const val TAG = "FragmentImageDetail"
        // IllustsBean 由 ImageDetailActivity 持有，Fragment 运行时读取，避免放进 Bundle
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
