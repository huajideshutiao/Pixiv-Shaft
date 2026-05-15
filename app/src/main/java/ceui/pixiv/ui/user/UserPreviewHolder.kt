package ceui.pixiv.ui.user

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import ceui.lisa.activities.followUser
import ceui.lisa.activities.unfollowUser
import ceui.lisa.annotations.ItemHolder
import ceui.lisa.databinding.CellUserPreviewBinding
import ceui.lisa.utils.GlideUrlChild
import ceui.lisa.utils.Params
import ceui.loxia.Illust
import ceui.loxia.ObjectPool
import ceui.loxia.User
import ceui.loxia.UserPreview
import ceui.loxia.findActionReceiverOrNull
import ceui.loxia.findFragmentOrNull
import ceui.pixiv.ui.common.IllustCardActionReceiver
import ceui.pixiv.ui.common.ListItemHolder
import ceui.pixiv.ui.common.ListItemViewHolder
import ceui.pixiv.utils.setOnClick
import com.bumptech.glide.Glide

class UserPreviewHolder(val userPreview: UserPreview) : ListItemHolder() {
    init {
        userPreview.user?.let {
            ObjectPool.update(it)
        }
        userPreview.illusts.forEach {
            ObjectPool.update(it)
        }
    }

    override fun areItemsTheSame(other: ListItemHolder): Boolean {
        return userPreview.user?.id == (other as? UserPreviewHolder)?.userPreview?.user?.id
    }

    override fun areContentsTheSame(other: ListItemHolder): Boolean {
        return userPreview == (other as? UserPreviewHolder)?.userPreview
    }

    val illust0: Illust? get() {
        return userPreview.illusts.getOrNull(0)
    }
    val illust1: Illust? get() {
        return userPreview.illusts.getOrNull(1)
    }
    val illust2: Illust? get() {
        return userPreview.illusts.getOrNull(2)
    }
}

@ItemHolder(UserPreviewHolder::class)
class UserPreviewViewHolder(bd: CellUserPreviewBinding) :
    ListItemViewHolder<CellUserPreviewBinding, UserPreviewHolder>(bd) {
    override fun onBindViewHolder(holder: UserPreviewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        binding.root.setOnClickListener { sender ->
            holder.userPreview.user?.id?.let {
                sender.findActionReceiverOrNull<UserActionReceiver>()?.onClickUser(it)
            }
        }
        binding.follow.setOnClick { sender ->
            holder.userPreview.user?.id?.let {
                sender.findFragmentOrNull<Fragment>()?.followUser(sender, it.toInt(), Params.TYPE_PUBLIC)
            }
        }
        binding.unfollow.setOnClick { sender ->
            holder.userPreview.user?.id?.let {
                sender.findFragmentOrNull<Fragment>()?.unfollowUser(sender, it.toInt())
            }
        }
        binding.illust1.setOnClick { sender ->
            holder.illust0?.let {
                sender.findActionReceiverOrNull<IllustCardActionReceiver>()?.onClickIllustCard(it)
            }
        }
        binding.illust2.setOnClick { sender ->
            holder.illust1?.let {
                sender.findActionReceiverOrNull<IllustCardActionReceiver>()?.onClickIllustCard(it)
            }
        }
        binding.illust3.setOnClick { sender ->
            holder.illust2?.let {
                sender.findActionReceiverOrNull<IllustCardActionReceiver>()?.onClickIllustCard(it)
            }
        }

        val user = holder.userPreview.user
        val avatarUrl = user?.profile_image_urls?.findMaxSizeUrl()
        if (!avatarUrl.isNullOrEmpty()) {
            Glide.with(binding.root.context)
                .load(GlideUrlChild(avatarUrl))
                .into(binding.userIcon)
        }
        binding.userName.text = user?.name ?: ""
        binding.userInfo.text = "@" + (user?.account ?: "")

        val i0 = holder.illust0
        val i1 = holder.illust1
        val i2 = holder.illust2
        binding.illustsPreview.isVisible = i0 != null || i1 != null || i2 != null
        binding.illust1.isVisible = i0 != null
        bindIllustImage(binding.illust1, i0)
        binding.illust2.isVisible = i1 != null
        bindIllustImage(binding.illust2, i1)
        binding.illust3.isVisible = i2 != null
        bindIllustImage(binding.illust3, i2)
    }

    private fun bindIllustImage(view: android.widget.ImageView, illust: Illust?) {
        val url = illust?.image_urls?.medium
        if (!url.isNullOrEmpty()) {
            Glide.with(view.context).load(GlideUrlChild(url)).into(view)
        }
    }
}