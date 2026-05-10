package ceui.lisa.fragments

import ceui.lisa.adapters.BaseAdapter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import ceui.lisa.adapters.WatchlistMangaAdapter
import ceui.lisa.core.BaseRepo
import ceui.lisa.databinding.FragmentBaseListBinding
import ceui.lisa.model.ListWatchlistManga
import ceui.lisa.models.WatchlistMangaItem
import ceui.lisa.repo.WatchlistMangaRepo

class FragmentWatchlistManga:
    NetListFragment<FragmentBaseListBinding, ListWatchlistManga, WatchlistMangaItem>() {
    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        attachToParent: Boolean
    ): FragmentBaseListBinding {
        return FragmentBaseListBinding.inflate(inflater, container, attachToParent)
    }

    override fun adapter(): BaseAdapter<*, out ViewBinding> {
        return WatchlistMangaAdapter(allItems, mContext)
    }

    override fun repository(): BaseRepo {
        return WatchlistMangaRepo()
    }

    override fun showToolbar(): Boolean {
        return false
    }
}
