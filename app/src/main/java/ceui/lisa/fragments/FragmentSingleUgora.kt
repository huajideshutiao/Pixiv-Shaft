package ceui.lisa.fragments

import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ceui.lisa.R
import ceui.lisa.activities.ContainerActivity
import ceui.lisa.activities.SearchActivity
import ceui.lisa.activities.Shaft
import ceui.lisa.activities.UActivity
import ceui.lisa.cache.Cache
import ceui.lisa.core.Manager
import ceui.lisa.databinding.FragmentUgoraBinding
import ceui.lisa.dialogs.MuteDialog
import ceui.lisa.download.IllustDownload
import ceui.lisa.file.LegacyFile
import ceui.lisa.file.OutPut
import ceui.lisa.http.ErrorCtrl
import ceui.lisa.models.GifResponse
import ceui.lisa.models.IllustsBean
import ceui.lisa.models.TagsBean
import ceui.lisa.notification.CallBackReceiver
import ceui.lisa.utils.Common
import ceui.lisa.utils.GlideUtil
import ceui.lisa.utils.Params
import ceui.lisa.utils.PixivOperate
import ceui.lisa.utils.SearchTypeUtil.SEARCH_TYPE_DB_KEYWORD
import ceui.lisa.utils.ShareIllust
import ceui.lisa.utils.toIllustsBean
import ceui.lisa.viewmodel.AppLevelViewModel
import ceui.loxia.ObjectPool
import ceui.pixiv.ui.task.NamedUrl
import ceui.pixiv.ui.task.TaskPool
import ceui.pixiv.utils.FastBlurTransformation
import ceui.pixiv.utils.populate
import com.blankj.utilcode.util.ColorUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.qmuiteam.qmui.skin.QMUISkinManager
import com.qmuiteam.qmui.widget.dialog.QMUIDialog
import com.scwang.smart.refresh.header.FalsifyFooter
import com.scwang.smart.refresh.header.FalsifyHeader
import java.io.File

class FragmentSingleUgora : BaseFragment<FragmentUgoraBinding>() {

    private var illust: IllustsBean? = null
    private var mReceiver: CallBackReceiver? = null
    private var mPlayReceiver: CallBackReceiver? = null
    private var frameHandler: Handler? = null
    private var frameRunnable: Runnable? = null
    private var pendingSave = false
    private var illustId: Long = 0

    override fun initBundle(bundle: Bundle) {
        illust = bundle.getSerializable(Params.CONTENT) as? IllustsBean
        illustId = bundle.getLong(Params.ILLUST_ID, 0)
        if (illust == null && illustId != 0L) {
            val poolIllust = ObjectPool.getKIllust(illustId).value
            if (poolIllust != null) {
                illust = poolIllust.toIllustsBean()
            }
        }
    }

    override fun initLayout() {
        mLayoutID = R.layout.fragment_ugora
    }

    private fun loadImage() {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                illust?.let {
                    Glide.with(mContext)
                        .load(GlideUtil.getSquare(it))
                        .override(200)
                        .apply(bitmapTransform(FastBlurTransformation(25)))
                        .transition(withCrossFade())
                        .into(baseBind.bgImage)
                }
            }
        }

        val imageSize = resources.displayMetrics.widthPixels -
            2 * resources.getDimensionPixelSize(R.dimen.twelve_dp)
        illust?.let {
            val params = baseBind.illustImage.layoutParams
            params.height = imageSize * it.height / it.width
            params.width = imageSize
            baseBind.illustImage.layoutParams = params

            Glide.with(mContext)
                .asDrawable()
                .load(GlideUtil.getLargeImage(it))
                .transition(withCrossFade())
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        baseBind.illustImage.setImageDrawable(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }
    }

    override fun initData() {
        if (illust != null) {
            loadImage()
        }

        run {
            val intentFilter = IntentFilter()
            mReceiver = CallBackReceiver { _, intent ->
                val bundle = intent.extras
                if (bundle != null) {
                    val id = bundle.getInt(Params.ID)
                    if (illust?.id == id) {
                        val isLiked = bundle.getBoolean(Params.IS_LIKED)
                        if (isLiked) {
                            illust?.is_bookmarked = true
                            baseBind.postLike.setImageResource(R.drawable.ic_favorite_red_24dp)
                        } else {
                            illust?.is_bookmarked = false
                            baseBind.postLike.setImageResource(R.drawable.ic_favorite_black_24dp)
                        }
                    }
                }
            }
            intentFilter.addAction(Params.LIKED_ILLUST)
            mReceiver?.let {
                LocalBroadcastManager.getInstance(mContext).registerReceiver(it, intentFilter)
            }
        }

        run {
            val intentFilter = IntentFilter()
            mPlayReceiver = CallBackReceiver { _, intent ->
                baseBind.progressLayout.donutProgress.visibility = View.GONE
                baseBind.gifStatusText.visibility = View.GONE
                val bundle = intent.extras
                if (bundle != null) {
                    val id = bundle.getInt(Params.ID)
                    val currentIllust = illust
                    if (currentIllust?.id == id) {
                        if (pendingSave) {
                            pendingSave = false
                            val gifFile = LegacyFile.gifResultFile(mContext, currentIllust)
                            if (gifFile.exists() && gifFile.length() > 1024) {
                                OutPut.outPutGif(mContext, gifFile, currentIllust)
                                if (Shaft.sSettings.isAutoPostLikeWhenDownload && !currentIllust.is_bookmarked) {
                                    PixivOperate.postLikeDefaultStarType(currentIllust)
                                }
                            }
                        }
                        nowPlayGif()
                    }
                }
            }
            intentFilter.addAction(Params.PLAY_GIF)
            mPlayReceiver?.let {
                LocalBroadcastManager.getInstance(mContext).registerReceiver(it, intentFilter)
            }
        }

        illust?.let {
            PixivOperate.setBack(it.id) { progress ->
                baseBind.progressLayout.donutProgress.progress = progress.toInt() * 100
            }
        }
    }

    override fun onDestroy() {
        stopFrameAnimation()
        try {
            mReceiver?.let { LocalBroadcastManager.getInstance(mContext).unregisterReceiver(it) }
            mPlayReceiver?.let {
                LocalBroadcastManager.getInstance(mContext).unregisterReceiver(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    fun nowPlayGif() {
        val illust = this.illust ?: return
        val gifFile = LegacyFile.gifResultFile(mContext, illust)
        PixivOperate.setBack(illust.id) { progress ->
            baseBind.progressLayout.donutProgress.progress = (progress * 100).toInt()
        }
        Common.showLog("nowPlayGif " + gifFile.path)
        if (gifFile.exists() && gifFile.length() > 1024) {
            Common.showLog("GIF文件已存在，直接播放")
            stopFrameAnimation()
            baseBind.playGif.visibility = View.INVISIBLE
            baseBind.progressLayout.donutProgress.visibility = View.INVISIBLE
            baseBind.gifStatusText.visibility = View.GONE
            Glide.with(mContext)
                .asGif()
                .load(gifFile)
                .placeholder(baseBind.illustImage.drawable)
                .into(baseBind.illustImage)
        } else {
            val unzipFolder = LegacyFile.gifUnzipFolder(mContext, illust)
            val frameFiles = unzipFolder.listFiles()
            if (frameFiles != null && frameFiles.isNotEmpty()) {
                if (frameHandler == null) {
                    startFrameAnimation(unzipFolder)
                }
                return
            }

            stopFrameAnimation()
            val hasDownload =
                Shaft.getDefaultPrefs().getBoolean(Params.ILLUST_ID + "_" + illust.id, false)
            val zipFile = LegacyFile.gifZipFile(mContext, illust)
            if (hasDownload && zipFile.exists() && zipFile.length() > 1024) {
                baseBind.playGif.visibility = View.INVISIBLE
                baseBind.progressLayout.donutProgress.visibility = View.VISIBLE
                PixivOperate.unzipAndPlay(mContext, illust)
            } else {
                baseBind.gifStatusText.text = getString(R.string.gif_info)
                baseBind.gifStatusText.visibility = View.VISIBLE
                baseBind.progress.visibility = View.VISIBLE
                PixivOperate.getGifInfo(illust, object : ErrorCtrl<GifResponse>() {
                    override fun next(gifResponse: GifResponse) {
                        baseBind.progress.visibility = View.INVISIBLE
                        Cache.get().saveModel(Params.ILLUST_ID + "_" + illust.id, gifResponse)
                        baseBind.gifStatusText.text = getString(R.string.gif_download)
                        val downloadItem = IllustDownload.downloadGif(gifResponse, illust)
                        Manager.get().setCallback(downloadItem.uuid) { t ->
                            try {
                                if (illust.id == Manager.get().currentIllustID) {
                                    baseBind.playGif.visibility = View.INVISIBLE
                                    baseBind.progressLayout.donutProgress.visibility = View.VISIBLE
                                    baseBind.progressLayout.donutProgress.progress = t.progress
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                })
            }
        }
    }

    private fun startFrameAnimation(unzipFolder: File) {
        stopFrameAnimation()

        val files = unzipFolder.listFiles()
        if (files == null || files.isEmpty()) return

        val sortedFrames = ArrayList(listOf(*files))
        sortedFrames.sortWith { o1, o2 ->
            try {
                val n1 = o1.name.substring(0, o1.name.length - 4).toInt()
                val n2 = o2.name.substring(0, o2.name.length - 4).toInt()
                n1.compareTo(n2)
            } catch (_: NumberFormatException) {
                o1.name.compareTo(o2.name)
            }
        }

        val delays = ArrayList<Int>()
        val illust = illust ?: return
        val gifResponse =
            Cache.get().getModel(Params.ILLUST_ID + "_" + illust.id, GifResponse::class.java)
        val metadata = gifResponse?.ugoira_metadata
        metadata?.frames?.let { frames ->
            for (frame in frames) {
                delays.add(frame.delay)
            }
        }

        baseBind.playGif.visibility = View.INVISIBLE
        baseBind.progressLayout.donutProgress.visibility = View.INVISIBLE
        baseBind.gifStatusText.visibility = View.GONE

        frameHandler = Handler(Looper.getMainLooper())
        val index = intArrayOf(0)

        frameRunnable = object : Runnable {
            override fun run() {
                if (frameHandler == null) return
                if (!isAdded || context == null) {
                    frameHandler?.postDelayed(this, 200)
                    return
                }
                val i = index[0] % sortedFrames.size
                val bitmap = BitmapFactory.decodeFile(sortedFrames[i].path)
                if (bitmap != null) {
                    baseBind.illustImage.setImageBitmap(bitmap)
                }
                val delay = if (i < delays.size) delays[i] else 60
                index[0]++
                frameHandler?.postDelayed(this, delay.toLong())
            }
        }
        frameHandler?.post(frameRunnable as Runnable)
    }

    private fun stopFrameAnimation() {
        frameHandler?.removeCallbacksAndMessages(null)
        frameHandler = null
        frameRunnable = null
    }

    override fun initView() {
        val illust = this.illust ?: return

        baseBind.toolbar.setNavigationOnClickListener { finish() }

        if (illust.id == 0) {
            baseBind.toolbar.setTitle(R.string.string_206)
            baseBind.refreshLayout.visibility = View.INVISIBLE
            return
        }

        if (illust.id == Manager.get().currentIllustID) {
            Manager.get().setCallback { t ->
                try {
                    if (illust.id == Manager.get().currentIllustID) {
                        baseBind.playGif.visibility = View.INVISIBLE
                        baseBind.progressLayout.donutProgress.visibility = View.VISIBLE
                        baseBind.progressLayout.donutProgress.progress = t.progress
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        baseBind.playGif.setOnClickListener { nowPlayGif() }

        baseBind.illustImage.transitionName = "image_0"
        baseBind.illustImage.setOnClickListener {
            val intent = Intent(mContext, ContainerActivity::class.java).apply {
                putExtra("illust", illust)
                putExtra(ContainerActivity.EXTRA_FRAGMENT, "图片详情")
                putExtra("index", 0)
            }
            val options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                mActivity,
                baseBind.illustImage,
                "image_0"
            )
            startActivity(intent, options.toBundle())
        }

        baseBind.refreshLayout.visibility = View.VISIBLE
        baseBind.refreshLayout.setEnableLoadMore(true)
        baseBind.refreshLayout.setRefreshHeader(FalsifyHeader(mContext))
        baseBind.refreshLayout.setRefreshFooter(FalsifyFooter(mContext))
        baseBind.title.text = illust.title
        baseBind.title.setOnLongClickListener {
            Common.copy(mContext, illust.title)
            true
        }

        baseBind.toolbar.inflateMenu(R.menu.share)
        baseBind.toolbar.menu.findItem(R.id.action_show_original).isVisible = false
        baseBind.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_share -> {
                    object : ShareIllust(mContext, illust) {
                        override fun onPrepare() {}
                    }.execute()
                    true
                }

                R.id.action_dislike -> {
                    val muteDialog = MuteDialog.newInstance(illust)
                    muteDialog.show(childFragmentManager, "MuteDialog")
                    true
                }

                R.id.action_copy_link -> {
                    val url = ShareIllust.URL_Head + illust.id
                    Common.copy(mContext, url)
                    true
                }

                R.id.action_mute_illust -> {
                    PixivOperate.muteIllust(illust)
                    true
                }

                R.id.action_share_image -> {
                    val imageUrl = IllustDownload.getUrl(illust, 0, Params.IMAGE_RESOLUTION_LARGE)
                    if (!imageUrl.isNullOrEmpty()) {
                        val cachedFile = TaskPool.peekCachedFile(imageUrl)
                        if (cachedFile != null && cachedFile.exists()) {
                            Common.shareImageFile(mActivity, cachedFile, "${illust.id}_p0.jpg")
                        } else {
                            val namedUrl = NamedUrl("", imageUrl)
                            val task = TaskPool.getLoadTask(namedUrl, true)
                            task.result.observe(viewLifecycleOwner) { file ->
                                if (file != null && file.exists()) {
                                    Common.shareImageFile(mActivity, file, "${illust.id}_p0.jpg")
                                }
                            }
                        }
                    }
                    true
                }

                else -> false
            }
        }

        baseBind.download.setOnClickListener {
            val gifFile = LegacyFile.gifResultFile(mContext, illust)
            if (gifFile.exists() && gifFile.length() > 1024) {
                OutPut.outPutGif(mContext, gifFile, illust)
                if (Shaft.sSettings.isAutoPostLikeWhenDownload && !illust.is_bookmarked) {
                    PixivOperate.postLikeDefaultStarType(illust)
                }
            } else {
                val unzipFolder = LegacyFile.gifUnzipFolder(mContext, illust)
                val frames = unzipFolder.listFiles()
                if (frames != null && frames.isNotEmpty()) {
                    pendingSave = true
                    baseBind.progressLayout.donutProgress.visibility = View.VISIBLE
                    baseBind.progressLayout.donutProgress.progress = 0
                    baseBind.gifStatusText.text = getString(R.string.gif_save)
                    baseBind.gifStatusText.visibility = View.VISIBLE
                    PixivOperate.setBack(illust.id) { progress ->
                        baseBind.progressLayout.donutProgress.progress = (progress * 100).toInt()
                    }
                    PixivOperate.encodeGifV2(mContext, unzipFolder, illust, true)
                } else {
                    IllustDownload.downloadGif(illust)
                    Common.showToast('1'.toString() + requireContext().getString(R.string.has_been_added))
                }
            }
        }
        baseBind.userName.setOnLongClickListener {
            Common.copy(mContext, illust.user?.name?.toString() ?: "")
            true
        }
        baseBind.related.setOnClickListener {
            val intent = Intent(mContext, ContainerActivity::class.java)
            intent.putExtra(ContainerActivity.EXTRA_FRAGMENT, "相关作品")
            intent.putExtra(Params.ILLUST_ID, illust.id)
            intent.putExtra(Params.ILLUST_TITLE, illust.title)
            startActivity(intent)
        }
        baseBind.comment.setOnClickListener {
            val intent = Intent(mContext, ContainerActivity::class.java)
            intent.putExtra(ContainerActivity.EXTRA_FRAGMENT, "相关评论")
            intent.putExtra(Params.ILLUST_ID, illust.id)
            intent.putExtra(Params.ILLUST_TITLE, illust.title)
            startActivity(intent)
        }
        baseBind.illustLike.setOnClickListener {
            val intent = Intent(mContext, ContainerActivity::class.java)
            intent.putExtra(Params.CONTENT, illust)
            intent.putExtra(ContainerActivity.EXTRA_FRAGMENT, "喜欢这个作品的用户")
            startActivity(intent)
        }
        if (illust.is_bookmarked) {
            baseBind.postLike.setImageResource(R.drawable.ic_favorite_red_24dp)
        } else {
            baseBind.postLike.setImageResource(R.drawable.ic_favorite_black_24dp)
        }
        baseBind.postLike.setOnClickListener {
            if (illust.is_bookmarked) {
                baseBind.postLike.setImageResource(R.drawable.ic_favorite_black_24dp)
            } else {
                baseBind.postLike.setImageResource(R.drawable.ic_favorite_red_24dp)
            }
            PixivOperate.postLikeDefaultStarType(illust)
        }
        baseBind.postLike.setOnLongClickListener {
            val intent = Intent(mContext, ContainerActivity::class.java)
            intent.putExtra(Params.ILLUST_ID, illust.id)
            intent.putExtra(Params.DATA_TYPE, Params.TYPE_ILLUST)
            intent.putExtra(Params.TAG_NAMES, illust.tagNames)
            intent.putExtra(Params.LAST_CLASS, javaClass.simpleName)
            intent.putExtra(ContainerActivity.EXTRA_FRAGMENT, "按标签收藏")
            startActivity(intent)
            true
        }
        baseBind.userHead.setOnClickListener {
            val intent = Intent(mContext, UActivity::class.java)
            intent.putExtra(Params.USER_ID, illust.user!!.id)
            startActivity(intent)
        }
        baseBind.userName.setOnClickListener {
            val intent = Intent(mContext, UActivity::class.java)
            intent.putExtra(Params.USER_ID, illust.user!!.id)
            startActivity(intent)
        }

        baseBind.follow.setOnClickListener {
            val integerValue = Shaft.appViewModel.getFollowUserLiveData(illust.user!!.id).value
            if (AppLevelViewModel.FollowUserStatus.isFollowed(integerValue ?: 0)) {
                PixivOperate.postUnFollowUser(illust.user!!.id)
                illust.user!!.is_followed = false
            } else {
                PixivOperate.postFollowUser(illust.user!!.id, Params.TYPE_PUBLIC)
                illust.user!!.is_followed = true
            }
        }

        baseBind.follow.setOnLongClickListener {
            val integerValue = Shaft.appViewModel.getFollowUserLiveData(illust.user!!.id).value
            if (!AppLevelViewModel.FollowUserStatus.isFollowed(integerValue ?: 0)) {
                illust.user!!.is_followed = true
            }
            PixivOperate.postFollowUser(illust.user!!.id, Params.TYPE_PRIVATE)
            true
        }

        Glide.with(mContext)
            .load(GlideUtil.getUrl(illust.user!!.profile_image_urls?.medium))
            .into(baseBind.userHead)

        baseBind.userName.text = illust.user!!.name

        val sizeString =
            SpannableString(getString(R.string.string_193, illust.width, illust.height))
        val currentPrimaryColorId = ColorUtils.getColor(R.color.page_default_background)
        sizeString.setSpan(
            ForegroundColorSpan(currentPrimaryColorId),
            sizeString.length - illust.getSize().length,
            sizeString.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        baseBind.illustPx.text = sizeString

        baseBind.illustTag.populate(
            illust.tags ?: emptyList(),
            R.layout.recy_single_line_text_new
        ) { view, tag ->
            val tv = view as TextView
            var text = tag.name
            if (!TextUtils.isEmpty(tag.translated_name)) {
                text = text + "/" + tag.translated_name
            }
            tv.text = text
            tv.setOnClickListener {
                val intent = Intent(mContext, SearchActivity::class.java)
                intent.putExtra(Params.KEY_WORD, tag.name)
                intent.putExtra(Params.INDEX, 0)
                startActivity(intent)
            }
            tv.setOnLongClickListener {
                val tagName = tag.name
                val searchEntity = PixivOperate.getSearchHistory(tagName, SEARCH_TYPE_DB_KEYWORD)
                val isPinned = searchEntity != null && searchEntity.isPinned
                QMUIDialog.MessageDialogBuilder(mContext)
                    .setTitle(tagName)
                    .setSkinManager(QMUISkinManager.defaultInstance(mContext))
                    .addAction(if (isPinned) getString(R.string.string_443) else getString(R.string.string_442)) { dialog, _ ->
                        PixivOperate.insertPinnedSearchHistory(
                            tagName,
                            SEARCH_TYPE_DB_KEYWORD,
                            !isPinned
                        )
                        Common.showToast(R.string.operate_success)
                        dialog.dismiss()
                    }
                    .addAction(getString(R.string.string_120)) { dialog, _ ->
                        Common.copy(mContext, tagName)
                        dialog.dismiss()
                    }
                    .create()
                    .show()
                true
            }
        }

        val caption = illust.caption
        if (!caption.isNullOrEmpty()) {
            baseBind.description.visibility = View.VISIBLE
            baseBind.description.setHtml(caption)
        } else {
            baseBind.description.visibility = View.GONE
        }
        baseBind.illustDate.text = Common.getLocalYYYYMMDDHHMMString(illust.create_date)
        baseBind.illustView.text = illust.total_view.toString()
        baseBind.illustLike.text = illust.total_bookmarks.toString()

        val user = illust.user!!
        val userString = SpannableString(getString(R.string.string_195, user.id))
        userString.setSpan(
            ForegroundColorSpan(currentPrimaryColorId),
            userString.length - user.id.toString().length,
            userString.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        baseBind.userId.text = userString
        baseBind.userId.setOnClickListener {
            Common.copy(mContext, user.id.toString())
        }
        val illustString = SpannableString(getString(R.string.string_194, illust.id))
        illustString.setSpan(
            ForegroundColorSpan(currentPrimaryColorId),
            illustString.length - illust.id.toString().length,
            illustString.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        baseBind.illustId.text = illustString
        baseBind.illustId.setOnClickListener {
            Common.copy(mContext, illust.id.toString())
        }

        Shaft.appViewModel.getFollowUserLiveData(illust.user!!.id).observe(this) { integer ->
            updateFollowUserUI(integer)
        }
    }

    override fun vertical() {
        val headParams = baseBind.head.layoutParams
        headParams.height = Shaft.statusHeight + Shaft.toolbarHeight
        baseBind.head.layoutParams = headParams
        baseBind.toolbar.setPadding(0, Shaft.statusHeight, 0, 0)
    }

    override fun horizon() {
        val headParams = baseBind.head.layoutParams
        headParams.height = Shaft.statusHeight * 3 / 5 + Shaft.toolbarHeight
        baseBind.head.layoutParams = headParams
    }

    private fun updateFollowUserUI(status: Int) {
        if (AppLevelViewModel.FollowUserStatus.isFollowed(status)) {
            baseBind.follow.setText(R.string.string_177)
        } else {
            baseBind.follow.setText(R.string.string_4)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(illust: IllustsBean): FragmentSingleUgora {
            val args = Bundle()
            args.putSerializable(Params.CONTENT, illust)
            val fragment = FragmentSingleUgora()
            fragment.arguments = args
            return fragment
        }

        @JvmStatic
        fun newInstance(id: Long): FragmentSingleUgora {
            val args = Bundle()
            args.putLong(Params.ILLUST_ID, id)
            val fragment = FragmentSingleUgora()
            fragment.arguments = args
            return fragment
        }
    }
}
