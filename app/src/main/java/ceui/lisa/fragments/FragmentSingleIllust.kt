package ceui.lisa.fragments

import android.content.ClipData
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ceui.lisa.R
import ceui.lisa.activities.BaseActivity
import ceui.lisa.activities.SearchActivity
import ceui.lisa.activities.Shaft
import ceui.lisa.activities.UActivity
import ceui.lisa.adapters.IllustDetailAdapter
import ceui.lisa.databinding.FragmentSingleIllustBinding
import ceui.lisa.dialogs.MuteDialog
import ceui.lisa.download.FileCreator
import ceui.lisa.download.IllustDownload
import ceui.lisa.models.IllustsBean
import ceui.lisa.models.TagsBean
import ceui.lisa.notification.CallBackReceiver
import ceui.lisa.utils.Common
import ceui.lisa.utils.DensityUtil
import ceui.lisa.utils.GlideUtil
import ceui.lisa.utils.Params
import ceui.lisa.utils.PixivOperate
import ceui.lisa.utils.SearchTypeUtil.SEARCH_TYPE_DB_KEYWORD
import ceui.lisa.utils.ShareIllust
import ceui.lisa.utils.toIllustsBean
import ceui.pixiv.route.AppRoute
import ceui.lisa.view.LinearItemDecorationNoLRTB
import ceui.lisa.view.ScrollChange
import ceui.lisa.viewmodel.AppLevelViewModel
import ceui.loxia.ObjectPool
import ceui.pixiv.utils.FastBlurTransformation
import ceui.pixiv.utils.applyBlur
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
import java.util.Locale

class FragmentSingleIllust : BaseFragment<FragmentSingleIllustBinding>() {

    private var illust: IllustsBean? = null
    private var mReceiver: CallBackReceiver? = null
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
        mLayoutID = R.layout.fragment_single_illust
    }

    private fun loadImage() {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    illust?.let {
                        baseBind.bgImage.applyBlur(25f)
                        Glide.with(mContext)
                            .load(GlideUtil.getSquare(it))
                            .override(200)
                            .transition(withCrossFade())
                            .into(baseBind.bgImage)
                    }
                } else {
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
        }

        baseBind.recyclerView.adapter = IllustDetailAdapter(this, illust)
    }

    override fun initData() {
        if (illust != null) {
            loadImage()
        }

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
                        baseBind.postLike.setImageResource(R.drawable.ic_favorite_grey_24dp)
                    }
                }
            }
        }
        intentFilter.addAction(Params.LIKED_ILLUST)
        mReceiver?.let {
            LocalBroadcastManager.getInstance(mContext).registerReceiver(it, intentFilter)
        }
    }

    override fun onDestroy() {
        mReceiver?.let {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(it)
        }
        super.onDestroy()
    }

    override fun onDestroyView() {
        try {
            baseBind.recyclerView.adapter = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroyView()
    }

    override fun initView() {
        val illust = this.illust ?: return

        baseBind.toolbar.setNavigationOnClickListener { finish() }

        if (illust.id == 0 || !illust.visible) {
            Common.showToast(R.string.string_206)
            baseBind.refreshLayout.visibility = View.INVISIBLE
            finish()
            return
        }

        baseBind.refreshLayout.visibility = View.VISIBLE
        baseBind.refreshLayout.setEnableLoadMore(true)
        baseBind.refreshLayout.setRefreshHeader(FalsifyHeader(mContext))
        baseBind.refreshLayout.setRefreshFooter(FalsifyFooter(mContext))

        val series = illust.series
        if (series != null && !TextUtils.isEmpty(series.title)) {
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    AppRoute.MangaSeriesDetail(series.id).start(requireContext())
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.color = Common.resolveThemeAttribute(
                        mContext,
                        androidx.appcompat.R.attr.colorPrimary
                    )
                }
            }
            val seriesString = getString(R.string.string_229)
            val spannableString = SpannableString("@$seriesString ${illust.title}")
            spannableString.setSpan(
                clickableSpan, 0, seriesString.length + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            baseBind.title.movementMethod = LinkMovementMethod.getInstance()
            baseBind.title.text = spannableString
        } else {
            baseBind.title.text = illust.title
        }
        baseBind.title.setOnLongClickListener {
            Common.copy(mContext, illust.title)
            true
        }

        baseBind.toolbar.inflateMenu(R.menu.share)
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

                R.id.action_show_original -> {
                    baseBind.recyclerView.adapter = IllustDetailAdapter(
                        this@FragmentSingleIllust,
                        illust,
                        true
                    )
                    true
                }

                R.id.action_mute_illust -> {
                    PixivOperate.muteIllust(illust)
                    true
                }

                R.id.action_share_image -> {
                    val glideUrl = if (Shaft.sSettings.isShowOriginalPreviewImage) {
                        GlideUtil.getOriginalImage(illust, 0)
                    } else {
                        GlideUtil.getLargeImage(illust, 0)
                    }
                    Glide.with(this@FragmentSingleIllust)
                        .asBitmap()
                        .load(glideUrl)
                        .onlyRetrieveFromCache(true)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                val uri = Common.copyBitmapToImageCacheFolder(
                                    resource,
                                    "${illust.id}_p0.jpg"
                                )
                                if (uri != null) {
                                    val shareIntent = Intent(Intent.ACTION_SEND)
                                    shareIntent.type = "image/*"
                                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                                    shareIntent.clipData = ClipData.newRawUri(null, uri)
                                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    val chooser = Intent.createChooser(
                                        shareIntent,
                                        getString(R.string.share)
                                    )
                                    chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    startActivity(chooser)
                                } else {
                                    Common.showToast(R.string.msg_load_fail)
                                }
                            }

                            override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}

                            override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                                Common.showToast(R.string.msg_load_fail)
                            }
                        })
                    true
                }

                else -> false
            }
        }

        baseBind.download.setOnClickListener {
            if (illust.page_count == 1) {
                IllustDownload.downloadIllustFirstPage(illust, mContext as BaseActivity<*>)
            } else {
                IllustDownload.downloadIllustAllPages(illust, mContext as BaseActivity<*>)
            }
            if (Shaft.sSettings.isAutoPostLikeWhenDownload && !illust.is_bookmarked) {
                PixivOperate.postLikeDefaultStarType(illust)
            }
        }
        baseBind.userName.setOnLongClickListener {
            Common.copy(mContext, illust.user!!.name.toString())
            true
        }
        baseBind.related.setOnClickListener {
            AppRoute.RelatedIllust(illust.id, illust.title).start(requireContext())
        }
        baseBind.comment.setOnClickListener {
            AppRoute.Comments(illust.id).start(requireContext())
        }
        baseBind.illustLike.setOnClickListener {
            AppRoute.LikeUsers(illust).start(requireContext())
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
            AppRoute.SbTag(illust.id, Params.TYPE_ILLUST, illust.tagNames).start(requireContext())
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
            val integerValue =
                Shaft.appViewModel.getFollowUserLiveData(illust.user!!.id).value
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

        baseBind.illustTag.populate<TagsBean>(
            illust.tags ?: emptyList(),
            R.layout.recy_single_line_text_new
        ) { view, s ->
            val tv = view as TextView
            var tag = s.name
            if (!TextUtils.isEmpty(s.translated_name)) {
                tag = "$tag/${s.translated_name}"
            }
            tv.text = tag
            tv.setOnClickListener {
                val intent = Intent(mContext, SearchActivity::class.java)
                intent.putExtra(Params.KEY_WORD, s.name)
                intent.putExtra(Params.INDEX, 0)
                startActivity(intent)
            }
            tv.setOnLongClickListener {
                val tagName = s.name
                val searchEntity = PixivOperate.getSearchHistory(tagName, SEARCH_TYPE_DB_KEYWORD)
                val isPinned = searchEntity != null && searchEntity.isPinned
                QMUIDialog.MessageDialogBuilder(mContext)
                    .setTitle(tagName)
                    .setSkinManager(QMUISkinManager.defaultInstance(mContext))
                    .addAction(
                        if (isPinned) getString(R.string.string_443) else getString(R.string.string_442)
                    ) { dialog, _ ->
                        PixivOperate.insertPinnedSearchHistory(
                            tagName,
                            SEARCH_TYPE_DB_KEYWORD,
                            !isPinned
                        )
                        Common.showToast(R.string.operate_success)
                        dialog.dismiss()
                    }
                    .addAction(
                        getString(R.string.string_120)
                    ) { dialog, _ ->
                        Common.copy(mContext, tagName)
                        dialog.dismiss()
                    }
                    .create()
                    .show()
                true
            }
        }

        if (!TextUtils.isEmpty(illust.caption)) {
            baseBind.description.visibility = View.VISIBLE
            baseBind.description.setHtml(illust.caption ?: "")
        } else {
            baseBind.description.visibility = View.GONE
        }
        baseBind.illustDate.text = Common.getLocalYYYYMMDDHHMMString(illust.create_date)
        baseBind.illustView.text = illust.total_view.toString()
        baseBind.illustLike.text = illust.total_bookmarks.toString()

        val layoutManager = ScrollChange(mContext)
        baseBind.recyclerView.layoutManager = layoutManager
        baseBind.recyclerView.isNestedScrollingEnabled = true
        baseBind.recyclerView.addItemDecoration(LinearItemDecorationNoLRTB(DensityUtil.dp2px(1.0f)))

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
            Common.copy(
                mContext,
                user.id.toString()
            )
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
            Common.copy(
                mContext,
                illust.id.toString()
            )
        }
        if (illust.page_count == 1) {
            baseBind.pSize.visibility = View.GONE
            baseBind.darkBlank.visibility = View.GONE
            baseBind.seeAll.visibility = View.GONE
            baseBind.illustList.open()
        } else {
            baseBind.pSize.visibility = View.VISIBLE
            baseBind.pSize.text = String.format(Locale.getDefault(), "%dP", illust.page_count)

            // 先默认展开，以便测量内容的真实高度
            baseBind.illustList.open()
            baseBind.recyclerView.post {
                if (baseBind.recyclerView.measuredHeight <= baseBind.illustList.maxHeight) {
                    // 如果内容总高度还没有达到最大限制高度，则不需要展开/折叠功能
                    baseBind.darkBlank.visibility = View.GONE
                    baseBind.seeAll.visibility = View.GONE
                } else {
                    // 超过最大高度，执行折叠并显示按钮
                    baseBind.darkBlank.visibility = View.VISIBLE
                    baseBind.seeAll.visibility = View.VISIBLE
                    baseBind.illustList.close()
                    baseBind.seeAll.text = "点击展开"
                }
            }

            baseBind.seeAll.setOnClickListener {
                if (baseBind.illustList.isExpand) {
                    baseBind.illustList.close()
                    baseBind.seeAll.text = "点击展开"
                } else {
                    baseBind.illustList.open()
                    baseBind.seeAll.text = "点击折叠"
                }
            }
        }

        Shaft.appViewModel.getFollowUserLiveData(illust.user!!.id).observe(
            this
        ) { integer -> updateFollowUserUI(integer ?: 0) }
    }

    override fun onResume() {
        super.onResume()
        checkDownload()
    }

    private fun checkDownload() {
        val illust = this.illust ?: return
        if (illust.page_count == 1) {
            Thread {
                val isExist = FileCreator.isExist(illust, 0)
                activity?.runOnUiThread {
                    if (isExist) {
                        baseBind.download.setImageResource(R.drawable.ic_has_download)
                    } else {
                        baseBind.download.setImageResource(R.drawable.ic_file_download_black_24dp)
                    }
                }
            }.start()
        }
    }

    override fun vertical() {
        //竖屏
        val headParams = baseBind.head.layoutParams
        headParams.height = Shaft.statusHeight + Shaft.toolbarHeight
        baseBind.head.layoutParams = headParams
        baseBind.toolbar.setPadding(0, Shaft.statusHeight, 0, 0)
    }

    override fun horizon() {
        //横屏
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

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        attachToParent: Boolean
    ): FragmentSingleIllustBinding {
        return FragmentSingleIllustBinding.inflate(inflater, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance(illust: IllustsBean): FragmentSingleIllust {
            val args = Bundle()
            args.putSerializable(Params.CONTENT, illust)
            val fragment = FragmentSingleIllust()
            fragment.arguments = args
            return fragment
        }

        @JvmStatic
        fun newInstance(id: Long): FragmentSingleIllust {
            val args = Bundle()
            args.putLong(Params.ILLUST_ID, id)
            val fragment = FragmentSingleIllust()
            fragment.arguments = args
            return fragment
        }
    }
}
