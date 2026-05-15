package ceui.pixiv.ui.common

import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import ceui.lisa.R
import ceui.lisa.activities.Shaft
import ceui.lisa.annotations.ItemHolder
import ceui.lisa.databinding.CellNovelCardBinding
import ceui.lisa.utils.GlideUrlChild
import ceui.loxia.DateParse
import ceui.loxia.Novel
import ceui.loxia.ObjectPool
import ceui.loxia.ProgressIndicator
import ceui.loxia.findActionReceiverOrNull
import ceui.pixiv.ui.novel.NovelSeriesActionReceiver
import ceui.pixiv.ui.user.UserActionReceiver
import ceui.pixiv.utils.ppppx
import ceui.pixiv.utils.setOnClick
import com.bumptech.glide.Glide

class NovelCardHolder(val novel: Novel, val showExtraMargin: Boolean = false) : ListItemHolder() {
    var isMultiSelectMode: Boolean = false
    var isSelected: Boolean = false

    init {
        ObjectPool.update(novel)
        novel.user?.let {
            ObjectPool.update(it)
        }
    }

    override fun getItemId(): Long {
        return novel.id
    }

    override fun areContentsTheSame(other: ListItemHolder): Boolean {
        if (other !is NovelCardHolder) return false
        return novel.id == other.novel.id
                && isMultiSelectMode == other.isMultiSelectMode
                && isSelected == other.isSelected
    }
}

@ItemHolder(NovelCardHolder::class)
class NovelCardViewHolder(bd: CellNovelCardBinding) :
    ListItemViewHolder<CellNovelCardBinding, NovelCardHolder>(bd) {

    override fun onBindViewHolder(holder: NovelCardHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        if (holder.showExtraMargin) {
            binding.novelRoot.updatePadding(12.ppppx, 0, 12.ppppx, 0)
        }

        binding.selectIndicator.isVisible = holder.isMultiSelectMode
        if (holder.isMultiSelectMode) {
            if (holder.isSelected) {
                binding.selectIndicator.setImageResource(R.drawable.ic_check_circle_black_24dp)
                binding.selectIndicator.clearColorFilter()
                binding.selectIndicator.imageAlpha = 255
            } else {
                binding.selectIndicator.setImageResource(R.drawable.ic_checkbox_off)
                binding.selectIndicator.setColorFilter(0xFFFFFFFF.toInt())
                binding.selectIndicator.imageAlpha = 200
            }
        }

        binding.userLayout.setOnClick { sender ->
            if (holder.isMultiSelectMode) {
                sender.findActionReceiverOrNull<NovelMultiSelectReceiver>()
                    ?.onToggleNovelSelection(holder.novel.id)
                return@setOnClick
            }
            holder.novel.user?.id?.let {
                sender.findActionReceiverOrNull<UserActionReceiver>()?.onClickUser(it)
            }
        }
        binding.seriesName.setOnClick { sender ->
            if (holder.isMultiSelectMode) {
                sender.findActionReceiverOrNull<NovelMultiSelectReceiver>()
                    ?.onToggleNovelSelection(holder.novel.id)
                return@setOnClick
            }
            holder.novel.series?.let { series ->
                sender.findActionReceiverOrNull<NovelSeriesActionReceiver>()
                    ?.onClickNovelSeries(sender, series)
            }
        }
        binding.root.setOnClick {
            if (holder.isMultiSelectMode) {
                it.findActionReceiverOrNull<NovelMultiSelectReceiver>()
                    ?.onToggleNovelSelection(holder.novel.id)
                return@setOnClick
            }
            it.findActionReceiverOrNull<NovelActionReceiver>()?.onClickNovel(holder.novel.id)
        }
        binding.bookmark.setOnClick {
            if (holder.isMultiSelectMode) {
                it.findActionReceiverOrNull<NovelMultiSelectReceiver>()
                    ?.onToggleNovelSelection(holder.novel.id)
                return@setOnClick
            }
            it.findActionReceiverOrNull<NovelActionReceiver>()
                ?.onClickBookmarkNovel(it, holder.novel.id)
        }
        binding.novelTag.setOnTouchListener { _, _ ->
            holder.isMultiSelectMode
        }
        binding.publishTime.text = context.getString(
            R.string.published_on,
            DateParse.getTimeAgo(context, holder.novel.create_date)
        )
        binding.textCount.text =
            context.getString(R.string.how_many_words, holder.novel.text_length.toString())
        binding.title.text = holder.novel.title ?: ""
        binding.seriesName.text = holder.novel.series?.title ?: ""
        val user = holder.novel.user
        val avatarUrl = user?.profile_image_urls?.findMaxSizeUrl()
        if (!avatarUrl.isNullOrEmpty()) {
            Glide.with(binding.root.context)
                .load(GlideUrlChild(avatarUrl))
                .into(binding.userIcon)
        }
        binding.userName.text = user?.name ?: ""
    }
}

interface NovelActionReceiver {
    fun onClickNovel(novelId: Long)
    fun onClickBookmarkNovel(sender: ProgressIndicator, novelId: Long)
    fun visitNovelById(novelId: Long)
}

interface NovelMultiSelectReceiver {
    fun isNovelMultiSelectMode(): Boolean
    fun isNovelSelected(novelId: Long): Boolean
    fun onToggleNovelSelection(novelId: Long)
}
