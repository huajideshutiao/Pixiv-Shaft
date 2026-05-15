package ceui.lisa.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import android.text.TextUtils
import android.view.View
import androidx.lifecycle.ViewModelProvider
import ceui.lisa.R
import ceui.lisa.databinding.FragmentUserRightBinding
import ceui.lisa.databinding.TagItemBinding
import ceui.lisa.utils.Params
import ceui.lisa.viewmodel.UserViewModel
import ceui.pixiv.route.AppRoute
import ceui.pixiv.utils.populate
import com.scwang.smart.refresh.layout.SmartRefreshLayout

class FragmentUserRight : SwipeFragment<FragmentUserRightBinding>() {

    private lateinit var mUserViewModel: UserViewModel

    override fun initLayout() {
        mLayoutID = R.layout.fragment_user_right
    }

    override fun initModel() {
        mUserViewModel = ViewModelProvider(mActivity).get(UserViewModel::class.java)
    }

    override fun getSmartRefreshLayout(): SmartRefreshLayout {
        return baseBind.refreshLayout
    }

    override fun initData() {
        val data = mUserViewModel.user.value ?: return
        val profile = data.profile!!
        val user = data.user!!
        val content: MutableList<String> = ArrayList()
        if (profile.total_illusts > 0) {
            content.add(getString(R.string.string_246) + ": " + profile.total_illusts)
        }
        if (profile.total_manga > 0) {
            content.add(getString(R.string.string_233) + ": " + profile.total_manga)
        }
        if (profile.total_illust_series > 0) {
            content.add(getString(R.string.string_230) + ": " + profile.total_illust_series)
        }
        if (profile.total_novels > 0) {
            content.add(getString(R.string.string_237) + ": " + profile.total_novels)
        }
        if (profile.total_novel_series > 0) {
            content.add(getString(R.string.string_257) + ": " + profile.total_novel_series)
        }
        if (profile.total_illust_bookmarks_public > 0) {
            content.add(getString(R.string.string_164) + ":" + profile.total_illust_bookmarks_public)
        }
        content.add(getString(R.string.string_192))
        content.add(getString(R.string.string_436))
        baseBind.tagLayout.populate(content, R.layout.tag_item) { view, s ->
            val binding: TagItemBinding = TagItemBinding.bind(view)
            binding.tagName.text = s
            binding.root.setOnClickListener {
                val position = content.indexOf(s)
                when {
                    content[position].contains(getString(R.string.string_246)) -> {
                        AppRoute.UserIllust(user.id).start(requireContext())
                    }

                    content[position].contains(getString(R.string.string_233)) -> {
                        AppRoute.UserManga(user.id).start(requireContext())
                    }

                    content[position].contains(getString(R.string.string_230)) -> {
                        AppRoute.MangaSeries(user.id).start(requireContext())
                    }

                    content[position].contains(getString(R.string.string_237)) -> {
                        AppRoute.UserNovel(user.id).start(requireContext())
                    }

                    content[position].contains(getString(R.string.string_257)) -> {
                        AppRoute.NovelSeriesWorks.start(requireContext())
                    }

                    content[position].contains(getString(R.string.string_164)) -> {
                        AppRoute.LikeIllust(user.id).start(requireContext())
                    }

                    content[position].contains(getString(R.string.string_192)) -> {
                        AppRoute.LikeNovel(user.id).start(requireContext())
                    }

                    content[position].contains(getString(R.string.string_436)) -> {
                        AppRoute.RelatedUser(user.id).start(requireContext())
                    }
                }
            }
        }
        if (!TextUtils.isEmpty(user.comment)) {
            baseBind.comment.visibility = View.VISIBLE
            baseBind.comment.text = user.comment
        } else {
            baseBind.comment.visibility = View.GONE
        }

        baseBind.showDetail.setOnClickListener {
            AppRoute.UserInfo.start(requireContext())
        }
        if (!TextUtils.isEmpty(profile.webpage)) {
            baseBind.realHome.text = profile.webpage
        } else {
            baseBind.realHome.text = "https://www.pixiv.net/users/%d".format(user.id)
        }
        if (!TextUtils.isEmpty(profile.twitter_url)) {
            baseBind.realTwitter.text = profile.twitter_url
        } else {
            baseBind.realTwitter.text = getString(R.string.no_info)
        }
        if (!TextUtils.isEmpty(profile.region)) {
            baseBind.realAddress.text = profile.region
        } else {
            baseBind.realAddress.text = getString(R.string.no_info)
        }
        if (!TextUtils.isEmpty(profile.comment)) {
            baseBind.realJob.text = profile.comment
        } else {
            baseBind.realJob.text = getString(R.string.no_info)
        }
    }

    override fun enableLoadMore(): Boolean {
        return false
    }

    override fun enableRefresh(): Boolean {
        return false
    }

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        attachToParent: Boolean
    ): FragmentUserRightBinding {
        return FragmentUserRightBinding.inflate(inflater, container, false)
    }
}
