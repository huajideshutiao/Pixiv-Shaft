package ceui.lisa.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.qmuiteam.qmui.util.QMUIDisplayHelper
import com.qmuiteam.qmui.widget.popup.QMUIPopup
import com.qmuiteam.qmui.widget.popup.QMUIPopups
import java.util.Locale
import ceui.lisa.R
import ceui.pixiv.route.AppRoute
import ceui.lisa.activities.Shaft
import ceui.lisa.core.Container
import ceui.lisa.core.PageData
import ceui.lisa.databinding.RecyIllustStaggerBinding
import ceui.lisa.dialogs.MuteDialog
import ceui.lisa.download.IllustDownload
import ceui.lisa.interfaces.MultiDownload
import ceui.lisa.interfaces.OnItemClickListener
import ceui.lisa.models.IllustsBean
import ceui.lisa.page.ScreenUtils
import ceui.lisa.utils.GlideUtil
import ceui.lisa.utils.Params
import ceui.lisa.utils.PixivOperate

open class IAdapter(
    targetList: List<IllustsBean>,
    context: Context
) : BaseAdapter<IllustsBean, RecyIllustStaggerBinding>(targetList, context), MultiDownload {

    private var showRelated = false

    init {
        handleClick()
        handleLongClick()
    }

    override fun initLayout() {
        mLayoutID = R.layout.recy_illust_stagger
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        attachToParent: Boolean
    ): RecyIllustStaggerBinding {
        return RecyIllustStaggerBinding.inflate(inflater, parent, attachToParent)
    }

    override fun bindData(
        target: IllustsBean,
        bindView: ViewHolder<RecyIllustStaggerBinding>,
        position: Int
    ) {
        val ratio = target.height.toFloat() / target.width.toFloat()
        val params = bindView.baseBind.illustImage.layoutParams
        val itemWidth = ScreenUtils.getDisplayMetrics().widthPixels / Shaft.sSettings.lineCount
        params.width = itemWidth
        params.height = when {
            ratio > MAX_HEIGHT_RATIO -> (itemWidth * MAX_HEIGHT_RATIO).toInt()
            ratio < MIN_HEIGHT_RATIO -> (itemWidth * MIN_HEIGHT_RATIO).toInt()
            else -> (itemWidth * target.height.toFloat() / target.width.toFloat()).toInt()
        }
        bindView.baseBind.illustImage.layoutParams = params

        val bookmarkedColor = ContextCompat.getColor(mContext, R.color.has_bookmarked)
        val notBookmarkedColor = ContextCompat.getColor(mContext, R.color.not_bookmarked)

        if (target.is_bookmarked) {
            bindView.baseBind.likeButton.imageTintList =
                ColorStateList.valueOf(bookmarkedColor)
        } else {
            bindView.baseBind.likeButton.imageTintList =
                ColorStateList.valueOf(notBookmarkedColor)
        }

        bindView.baseBind.likeButton.setOnClickListener {
            if (target.is_bookmarked) {
                bindView.baseBind.likeButton.imageTintList =
                    ColorStateList.valueOf(notBookmarkedColor)
            } else {
                bindView.baseBind.likeButton.imageTintList =
                    ColorStateList.valueOf(bookmarkedColor)
            }
            if (Shaft.sSettings.isPrivateStar) {
                PixivOperate.postLike(target, Params.TYPE_PRIVATE, showRelated, position + 2)
            } else {
                PixivOperate.postLike(target, Params.TYPE_PUBLIC, showRelated, position + 2)
            }
        }

        bindView.baseBind.likeButton.setOnLongClickListener {
            AppRoute.SbTag(target.id, Params.TYPE_ILLUST, target.tagNames).start(mContext)
            true
        }

        val imgUrl: GlideUrl = if (Shaft.sSettings.isShowLargeThumbnailImage) {
            GlideUtil.getLargeImage(target)
        } else {
            GlideUtil.getMediumImg(target)
        }
        val requestBuilder = Glide.with(mContext)
            .load(imgUrl)
        requestBuilder
            .placeholder(R.color.second_light_bg)
            .transition(DrawableTransitionOptions.withCrossFade())
            .error(getBuilder(target))
            .into(bindView.baseBind.illustImage)

        if (target.page_count == 1) {
            bindView.baseBind.pSize.visibility = View.GONE
        } else {
            bindView.baseBind.pSize.visibility = View.VISIBLE
            bindView.baseBind.pSize.text = String.format(Locale.getDefault(), "%dP", target.page_count)
        }
        bindView.baseBind.pGif.visibility = if (target.isGif) View.VISIBLE else View.GONE

        bindView.itemView.setOnClickListener { v ->
            mOnItemClickListener?.onItemClick(v, position, 0)
        }
        bindView.itemView.setOnLongClickListener { v ->
            if (mOnItemLongClickListener != null) {
                mOnItemLongClickListener.onItemLongClick(v, position, 0)
                return@setOnLongClickListener true
            }
            false
        }

        bindView.baseBind.pRelated.visibility = if (target.isRelated) View.VISIBLE else View.GONE
        bindView.baseBind.createdByAi.visibility = if (target.isCreatedByAI) View.VISIBLE else View.GONE
    }

    fun getBuilder(target: IllustsBean): RequestBuilder<Drawable> {
        val imgUrl: GlideUrl = if (Shaft.sSettings.isShowLargeThumbnailImage) {
            GlideUtil.getLargeImage(target)
        } else {
            GlideUtil.getMediumImg(target)
        }
        return Glide.with(mContext)
            .load(imgUrl)
            .placeholder(R.color.second_light_bg)
            .transition(DrawableTransitionOptions.withCrossFade())
    }

    override fun getContext(): Context = mContext

    override fun getIllustList(): List<IllustsBean> = allItems

    private fun handleClick() {
        setOnItemClickListener(object : OnItemClickListener {
            override fun onItemClick(v: View, position: Int, viewType: Int) {
                val pageData = PageData(uuid, nextUrl, allItems)
                Container.get().addPageToMap(pageData)
                AppRoute.FullScreen(uuid, position, null).start(mContext)
            }
        })
    }

    private fun handleLongClick() {
        setOnItemLongClickListener { v, position, _ ->
            val illust = allItems[position]
            val popView = View.inflate(mContext, R.layout.pop_window_2, null)

            val mNormalPopup = QMUIPopups.popup(mContext)
                .preferredDirection(QMUIPopup.DIRECTION_BOTTOM)
                .view(popView)
                .dimAmount(0.5f)
                .edgeProtection(QMUIDisplayHelper.dp2px(mContext, 20))
                .offsetX(QMUIDisplayHelper.dp2px(mContext, 20))
                .offsetYIfBottom(QMUIDisplayHelper.dp2px(mContext, 5))
                .shadow(true)
                .arrow(true)
                .bgColor(mContext.resources.getColor(R.color.fragment_center))
                .animStyle(QMUIPopup.ANIM_GROW_FROM_RIGHT)
                .onDismiss { }
                .show(v)

            popView.findViewById<View>(R.id.mute_setting).setOnClickListener {
                val muteDialog = MuteDialog.newInstance(illust)
                muteDialog.show(
                    (mContext as FragmentActivity).supportFragmentManager,
                    "MuteDialog"
                )
                mNormalPopup.dismiss()
            }
            popView.findViewById<View>(R.id.batch_download).setOnClickListener {
                startDownload()
                mNormalPopup.dismiss()
            }
            popView.findViewById<View>(R.id.download_this_one).setOnClickListener {
                IllustDownload.downloadIllustAllPages(illust)
                if (Shaft.sSettings.isAutoPostLikeWhenDownload && !illust.is_bookmarked) {
                    PixivOperate.postLikeDefaultStarType(illust)
                }
                mNormalPopup.dismiss()
            }
            popView.findViewById<View>(R.id.show_comment).setOnClickListener {
                AppRoute.Comments(illust.id, false).start(mContext)
                mNormalPopup.dismiss()
            }
        }
    }

    fun isShowRelated(): Boolean = showRelated

    fun setShowRelated(showRelated: Boolean): IAdapter {
        this.showRelated = showRelated
        return this
    }

    companion object {
        private const val MIN_HEIGHT_RATIO = 0.6f
        private const val MAX_HEIGHT_RATIO = 2.0f
    }
}
