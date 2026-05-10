package ceui.lisa.fragments

import ceui.lisa.adapters.BaseAdapter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import ceui.lisa.adapters.WatchlistNovelAdapter
import ceui.lisa.core.BaseRepo
import ceui.lisa.databinding.FragmentBaseListBinding
import ceui.lisa.model.ListWatchlistNovel
import ceui.lisa.models.WatchlistNovelItem
import ceui.lisa.repo.WatchlistNovelRepo

class FragmentWatchlistNovel:
    NetListFragment<FragmentBaseListBinding, ListWatchlistNovel, WatchlistNovelItem>() {
    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        attachToParent: Boolean
    ): FragmentBaseListBinding {
        return FragmentBaseListBinding.inflate(inflater, container, attachToParent)
    }

    override fun adapter(): BaseAdapter<*, out ViewBinding> {
        return WatchlistNovelAdapter(allItems, mContext)
    }

    override fun repository(): BaseRepo {
        return WatchlistNovelRepo()
    }

    override fun showToolbar(): Boolean {
        return false
    }
}