package ceui.lisa.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import androidx.recyclerview.widget.LinearLayoutManager
import ceui.lisa.R
import ceui.lisa.adapters.BaseAdapter
import ceui.lisa.adapters.MangaSeriesAdapter
import ceui.lisa.core.BaseRepo
import ceui.lisa.databinding.FragmentBaseListBinding
import ceui.lisa.model.ListMangaSeries
import ceui.lisa.models.MangaSeriesItem
import ceui.lisa.repo.MangaSeriesRepo
import ceui.lisa.utils.DensityUtil
import ceui.lisa.utils.Params
import ceui.lisa.view.LinearItemDecorationNoLRTB
import ceui.pixiv.route.AppRoute

class FragmentMangaSeries :
    NetListFragment<FragmentBaseListBinding, ListMangaSeries, MangaSeriesItem>() {

    private var userID: Int = 0

    override fun initBundle(bundle: Bundle) {
        userID = bundle.getInt(Params.USER_ID)
    }

    companion object {
        @JvmStatic
        fun newInstance(userID: Int): FragmentMangaSeries {
            return FragmentMangaSeries().apply {
                arguments = Bundle().apply {
                    putInt(Params.USER_ID, userID)
                }
            }
        }
    }

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        attachToParent: Boolean
    ): FragmentBaseListBinding {
        return FragmentBaseListBinding.inflate(inflater, container, attachToParent)
    }

    override fun adapter(): BaseAdapter<*, out ViewBinding> {
        return MangaSeriesAdapter(
            allItems,
            mContext
        ).setOnItemClickListener { _, position, _ ->
            AppRoute.MangaSeriesDetail(allItems[position].id).start(requireContext())
        }
    }

    override fun repository(): BaseRepo {
        return MangaSeriesRepo(userID)
    }

    override fun getToolbarTitle(): String {
        return getString(R.string.string_230)
    }

    override fun initRecyclerView() {
        mRecyclerView.layoutManager = LinearLayoutManager(mContext)
        mRecyclerView.addItemDecoration(LinearItemDecorationNoLRTB(DensityUtil.dp2px(1.0f)))
    }
}
