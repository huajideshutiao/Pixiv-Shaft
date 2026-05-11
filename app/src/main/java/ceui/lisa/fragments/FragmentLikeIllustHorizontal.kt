package ceui.lisa.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import ceui.lisa.R
import ceui.lisa.activities.ContainerActivity
import ceui.lisa.adapters.LAdapter
import ceui.lisa.core.Container
import ceui.lisa.core.PageData
import ceui.lisa.core.executeCall
import ceui.lisa.databinding.FragmentLikeIllustHorizontalBinding
import ceui.lisa.helper.UserIllustJumpHelper
import ceui.lisa.http.NullCtrl
import ceui.lisa.http.Retro
import ceui.lisa.model.ListIllust
import ceui.lisa.models.IllustsBean
import ceui.lisa.models.UserDetailResponse
import ceui.lisa.utils.DensityUtil
import ceui.lisa.utils.Params
import ceui.lisa.view.LinearItemHorizontalDecoration
import com.github.ybq.android.spinkit.style.Wave
import jp.wasabeef.recyclerview.animators.FadeInLeftAnimator
import retrofit2.Call
import java.util.function.Function

class FragmentLikeIllustHorizontal : BaseFragment<FragmentLikeIllustHorizontalBinding>() {

    private val allItems = mutableListOf<IllustsBean>()
    private var mUserDetailResponse: UserDetailResponse? = null
    private var mAdapter: LAdapter? = null
    private var type: Int = 0

    override fun initBundle(bundle: Bundle) {
        mUserDetailResponse = bundle.getSerializable(Params.CONTENT) as? UserDetailResponse
        type = bundle.getInt(Params.DATA_TYPE)
    }

    override fun initLayout() {
        mLayoutID = R.layout.fragment_like_illust_horizontal
    }

    override fun initView() {
        val wave = Wave()
        wave.setColor(androidx.appcompat.R.attr.colorPrimary)
        baseBind.progress.setIndeterminateDrawable(wave)
        baseBind.recyclerView.addItemDecoration(
            LinearItemHorizontalDecoration(DensityUtil.dp2px(8.0f))
        )
        val landingAnimator = FadeInLeftAnimator().apply {
            addDuration = ListFragment.animateDuration.toLong()
            removeDuration = ListFragment.animateDuration.toLong()
            moveDuration = ListFragment.animateDuration.toLong()
            changeDuration = ListFragment.animateDuration.toLong()
        }
        baseBind.recyclerView.itemAnimator = landingAnimator
        val manager = LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false)
        baseBind.recyclerView.layoutManager = manager
        baseBind.recyclerView.setHasFixedSize(true)
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(baseBind.recyclerView)

        mAdapter = LAdapter(allItems, mContext)
        mAdapter?.setOnItemClickListener { _, position, _ ->
            val pageData = PageData(allItems)
            Container.get().addPageToMap(pageData)

            val intent = Intent(mContext, ContainerActivity::class.java).apply {
                putExtra(ContainerActivity.EXTRA_FRAGMENT, "全屏查看")
                putExtra(Params.POSITION, position)
                putExtra(Params.PAGE_UUID, pageData.uuid)
            }
            startActivity(intent)
        }
        baseBind.recyclerView.adapter = mAdapter

        val layoutParams = baseBind.recyclerView.layoutParams
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams.height =
            (mAdapter?.imageSize ?: 0) + resources.getDimensionPixelSize(R.dimen.sixteen_dp)
        baseBind.recyclerView.layoutParams = layoutParams

        when (type) {
            1 -> {
                baseBind.title.text = "插画/漫画收藏"
                baseBind.howMany.text = String.format(
                    getString(R.string.how_many_illust_works),
                    mUserDetailResponse?.profile?.total_illust_bookmarks_public ?: 0
                )
            }

            2 -> {
                baseBind.title.text = "插画作品"
                baseBind.howMany.text = String.format(
                    getString(R.string.how_many_illust_works),
                    mUserDetailResponse?.profile?.total_illusts ?: 0
                )
            }

            3 -> {
                baseBind.title.text = "漫画作品"
                baseBind.howMany.text = String.format(
                    getString(R.string.how_many_illust_works),
                    mUserDetailResponse?.profile?.total_manga ?: 0
                )
            }
        }

        baseBind.howMany.setOnClickListener {
            val intent = Intent(mContext, ContainerActivity::class.java).apply {
                putExtra(ContainerActivity.EXTRA_FRAGMENT, baseBind.title.text.toString())
                putExtra(Params.USER_ID, mUserDetailResponse?.user?.id ?: 0)
            }
            startActivity(intent)
        }

        if (type == 2 || type == 3) {
            baseBind.jumpAction.visibility = View.VISIBLE
            baseBind.jumpAction.setOnClickListener {
                val userID = mUserDetailResponse?.user?.id ?: 0
                val kind =
                    if (type == 3) UserIllustJumpHelper.Kind.MANGA else UserIllustJumpHelper.Kind.ILLUST
                val fragmentTag = if (type == 3) "漫画作品" else "插画作品"
                UserIllustJumpHelper.showJumpDialog(mActivity, userID, kind) { offset, pickedDate ->
                    if (!isAdded) return@showJumpDialog
                    val intent = Intent(mContext, ContainerActivity::class.java).apply {
                        putExtra(ContainerActivity.EXTRA_FRAGMENT, fragmentTag)
                        putExtra(Params.USER_ID, userID)
                        putExtra(Params.INITIAL_OFFSET, offset)
                        pickedDate?.let { putExtra(Params.TARGET_DATE, it) }
                    }
                    startActivity(intent)
                }
            }
        }
    }

    override fun initData() {
        val userID = mUserDetailResponse?.user?.id ?: return
        val api: Call<ListIllust>? = when (type) {
            1 -> Retro.getAppApi().getUserLikeIllust(userID, Params.TYPE_PUBLIC)
            2 -> Retro.getAppApi().getUserSubmitIllust(userID, Params.TYPE_ILLUST)
            3 -> Retro.getAppApi().getUserSubmitIllust(userID, Params.TYPE_MANGA)
            else -> null
        }

        api?.let {
            executeCall(it, Function.identity(), object : NullCtrl<ListIllust>() {
                override fun success(listIllust: ListIllust) {
                    allItems.clear()
                    if (listIllust.list.size > 10) {
                        allItems.addAll(listIllust.list.subList(0, 10))
                    } else {
                        allItems.addAll(listIllust.list)
                    }
                    mAdapter?.notifyItemRangeInserted(0, allItems.size)
                }

                override fun must(isSuccess: Boolean) {
                    baseBind.progress.visibility = View.INVISIBLE
                }
            })
        }
    }

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        attachToParent: Boolean
    ): FragmentLikeIllustHorizontalBinding {
        return FragmentLikeIllustHorizontalBinding.inflate(inflater, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance(
            userDetailResponse: UserDetailResponse,
            pType: Int
        ): FragmentLikeIllustHorizontal {
            val args = Bundle().apply {
                putSerializable(Params.CONTENT, userDetailResponse)
                putInt(Params.DATA_TYPE, pType)
            }
            return FragmentLikeIllustHorizontal().apply {
                arguments = args
            }
        }
    }
}