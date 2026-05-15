package ceui.pixiv.ui.user


import android.util.Log

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import ceui.lisa.R
import ceui.lisa.activities.followUser
import ceui.lisa.activities.unfollowUser
import ceui.lisa.databinding.FragmentUserBinding
import ceui.lisa.utils.GlideUrlChild
import ceui.lisa.utils.Params
import ceui.loxia.ObjectType
import ceui.loxia.RefreshHint
import ceui.loxia.RefreshState
import ceui.loxia.pushFragment
import ceui.loxia.requireEntityWrapper
import ceui.pixiv.ui.chats.SeeMoreAction
import ceui.pixiv.ui.chats.SeeMoreType
import ceui.pixiv.ui.common.FitsSystemWindowFragment
import ceui.pixiv.ui.common.ImageUrlViewer
import ceui.pixiv.ui.common.PixivFragment
import ceui.pixiv.ui.common.ViewPagerFragment
import ceui.pixiv.ui.common.setupMaterialHeader
import ceui.pixiv.ui.common.constructVM
import ceui.pixiv.ui.common.viewBinding
import ceui.pixiv.utils.FastBlurTransformation
import ceui.pixiv.utils.ppppx
import ceui.pixiv.utils.setOnClick
import com.blankj.utilcode.util.BarUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions.bitmapTransform
class UserFragment : PixivFragment(R.layout.fragment_user), ViewPagerFragment, SeeMoreAction,
    FitsSystemWindowFragment {

    private val safeArgs by navArgs<UserFragmentArgs>()
    private val binding by viewBinding(FragmentUserBinding::bind)
    private val viewModel by constructVM({ safeArgs.userId }) { userId ->
        Log.d(TAG, "userId-${userId}")
        UserViewModel(userId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top - 10.ppppx
            }
            binding.headerContent.updatePaddingRelative(top = insets.top + BarUtils.getActionBarHeight())
            windowInsets
        }
        viewModel.userLiveData.observe(viewLifecycleOwner) { user ->
            runOnceWithinFragmentLifecycle("visit-user-${safeArgs.userId}") {
                requireEntityWrapper().visitUser(requireContext(), user)
            }
            binding.iconOfficial.isVisible = user.isOfficial()
            binding.iconVolunteer.isVisible = user.isVolunteer()
            val avatarUrl = user.profile_image_urls?.findMaxSizeUrl()
            if (!avatarUrl.isNullOrEmpty()) {
                Glide.with(this).load(GlideUrlChild(avatarUrl))
                    .into(binding.userAvatar)
                binding.userAvatar.setOnClick {
                    ImageUrlViewer.open(
                        requireContext(),
                        avatarUrl,
                        saveName = "avatar_${user.id}_${user.name ?: ""}",
                    )
                }
            } else {
                binding.userAvatar.setOnClickListener(null)
                binding.userAvatar.isClickable = false
            }
            binding.userName.text = user.name ?: ""
            binding.userAccount.text = "@" + (user.account ?: "")
            binding.naviTitle.text = user.name ?: ""
        }
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            binding.iconPrime.isVisible = profile.isPremium()
            binding.followersCount.text = (profile.profile?.total_follow_users ?: 0).toString()
            binding.followingCount.text = (profile.profile?.total_mypixiv_users ?: 0).toString()
            val bannerUrl = profile.profile?.background_image_url
            if (!bannerUrl.isNullOrEmpty()) {
                Glide.with(this).load(GlideUrlChild(bannerUrl))
                    .override(200)
                    .apply(bitmapTransform(FastBlurTransformation(15)))
                    .transition(withCrossFade())
                    .into(binding.pageBackground)
                val uid = profile.user?.id ?: safeArgs.userId
                val uname = profile.user?.name ?: ""
                binding.pageBackground.setOnClick {
                    ImageUrlViewer.open(
                        requireContext(),
                        bannerUrl,
                        saveName = "banner_${uid}_${uname}",
                    )
                }
            } else {
                binding.pageBackground.setOnClickListener(null)
                binding.pageBackground.isClickable = false
            }
        }
        viewModel.blurBackground.observe(viewLifecycleOwner) { blurIllust ->
            if (viewModel.userProfile.value?.profile?.background_image_url.isNullOrEmpty()) {
                val url = blurIllust?.image_urls?.large
                if (url?.isNotEmpty() == true) {
                    Glide.with(this).load(GlideUrlChild(url))
                        .override(200)
                        .apply(bitmapTransform(FastBlurTransformation(15)))
                        .transition(withCrossFade())
                        .into(binding.pageBackground)
                }
            }
        }
        binding.postFollow.setOnClick {
            followUser(it, safeArgs.userId.toInt(), Params.TYPE_PUBLIC)
        }
        binding.removeFollow.setOnClick {
            unfollowUser(it, safeArgs.userId.toInt())
        }
        binding.naviBack.setOnClick {
            findNavController().popBackStack()
        }
        binding.refreshLayout.setupMaterialHeader(this)
        binding.refreshLayout.setOnRefreshListener {
            viewModel.refresh(RefreshHint.PullToRefresh)
        }
        viewModel.refreshState.observe(viewLifecycleOwner) { state ->
            if (state is RefreshState.LOADED || state is RefreshState.ERROR) {
                binding.refreshLayout.finishRefresh()
            } else {
            }
        }
        binding.appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            binding.refreshLayout.isEnabled = verticalOffset == 0
            val totalScrollRange = appBarLayout.totalScrollRange
            if (totalScrollRange == 0) {
                return@addOnOffsetChangedListener
            }

            val percentage = (Math.abs(verticalOffset) / totalScrollRange.toFloat())
            binding.headerContent.alpha = 1F - percentage
            binding.naviTitle.isVisible = (percentage == 1F)
        }
        binding.userViewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return 1
            }

            override fun createFragment(position: Int): Fragment {
                return UserContentFragment()
            }
        }
    }

    override fun seeMore(type: Int) {
        if (type == SeeMoreType.USER_CREATED_ILLUST) {
            pushFragment(
                R.id.navigation_user_created_illust, UserCreatedIllustsFragmentArgs(
                    userId = safeArgs.userId, objectType = ObjectType.ILLUST
                ).toBundle()
            )
        } else if (type == SeeMoreType.USER_CREATED_MANGA) {
            pushFragment(
                R.id.navigation_user_created_illust, UserCreatedIllustsFragmentArgs(
                    userId = safeArgs.userId, objectType = ObjectType.MANGA
                ).toBundle()
            )
        } else if (type == SeeMoreType.USER_BOOKMARKED_ILLUST) {
            pushFragment(
                R.id.navigation_user_bookmarked_illust, UserBookmarkedIllustsFragmentArgs(
                    safeArgs.userId, Params.TYPE_PUBLIC
                ).toBundle()
            )
        } else if (type == SeeMoreType.USER_CREATED_NOVEL) {
            pushFragment(
                R.id.navigation_user_created_novel, UserCreatedNovelFragmentArgs(
                    safeArgs.userId
                ).toBundle()
            )
        }
    }


    companion object {
        private const val TAG = "UserFragment"
    }
}