package ceui.lisa.fragments

import ceui.lisa.R
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import ceui.lisa.adapters.BaseAdapter
import ceui.lisa.adapters.NovelMarkersAdapter
import ceui.lisa.core.BaseRepo
import ceui.lisa.databinding.FragmentBaseListBinding
import ceui.lisa.model.ListNovelMarkers
import ceui.lisa.models.MarkedNovelItem
import ceui.lisa.repo.NovelMarkersRepo

class FragmentNovelMarkers: NetListFragment<FragmentBaseListBinding, ListNovelMarkers, MarkedNovelItem>() {
    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        attachToParent: Boolean
    ): FragmentBaseListBinding {
        return FragmentBaseListBinding.inflate(inflater, container, attachToParent)
    }

    override fun adapter(): BaseAdapter<*, out ViewBinding> {
        return NovelMarkersAdapter(allItems, mContext)
    }

    override fun repository(): BaseRepo {
        return NovelMarkersRepo()
    }

    override fun getToolbarTitle(): String {
        return getString(R.string.core_string_novel_marker)
    }
}