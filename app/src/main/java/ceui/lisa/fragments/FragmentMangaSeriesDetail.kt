package ceui.lisa.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import ceui.lisa.R
import ceui.lisa.adapters.BaseAdapter
import ceui.lisa.adapters.IAdapter
import ceui.lisa.core.BaseRepo
import ceui.lisa.databinding.FragmentBaseListBinding
import ceui.lisa.model.ListMangaOfSeries
import ceui.lisa.models.IllustsBean
import ceui.lisa.repo.MangaSeriesDetailRepo
import ceui.lisa.utils.Params

class FragmentMangaSeriesDetail :
    NetListFragment<FragmentBaseListBinding, ListMangaOfSeries, IllustsBean>() {

    private var seriesId: Int = 0

    override fun initBundle(bundle: Bundle) {
        seriesId = bundle.getInt(Params.ID)
    }

    companion object {
        @JvmStatic
        fun newInstance(seriesId: Int): FragmentMangaSeriesDetail {
            return FragmentMangaSeriesDetail().apply {
                arguments = Bundle().apply {
                    putInt(Params.ID, seriesId)
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
        return IAdapter(allItems, mContext)
    }

    override fun repository(): BaseRepo {
        return MangaSeriesDetailRepo(seriesId)
    }

    override fun getToolbarTitle(): String {
        return getString(R.string.string_230)
    }

    override fun initRecyclerView() {
        staggerRecyclerView()
    }
}
