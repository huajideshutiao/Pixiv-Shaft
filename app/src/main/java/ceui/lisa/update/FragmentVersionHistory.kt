package ceui.lisa.update

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import ceui.lisa.R
import ceui.lisa.databinding.FragmentVersionHistoryBinding
import ceui.lisa.fragments.SwipeFragment
import com.scwang.smart.refresh.layout.SmartRefreshLayout

class FragmentVersionHistory : SwipeFragment<FragmentVersionHistoryBinding>() {

    override fun initLayout() {
        mLayoutID = R.layout.fragment_version_history
    }

    override fun getSmartRefreshLayout(): SmartRefreshLayout {
        return baseBind.refreshLayout
    }

    override fun initData() {
        baseBind.toolbar.setNavigationOnClickListener { mActivity.finish() }
        baseBind.recyclerView.layoutManager = LinearLayoutManager(mContext)

        loadReleases()
    }

    private fun loadReleases() {
        baseBind.loadingView.visibility = View.VISIBLE
        baseBind.errorText.visibility = View.GONE
        baseBind.recyclerView.visibility = View.GONE

        AppUpdateChecker.fetchAllReleases { releases, error ->
            baseBind.loadingView.visibility = View.GONE
            if (error != null || releases == null || releases.isEmpty()) {
                baseBind.errorText.setText(R.string.version_history_empty)
                baseBind.errorText.visibility = View.VISIBLE
            } else {
                baseBind.recyclerView.visibility = View.VISIBLE
                baseBind.recyclerView.adapter = ReleaseHistoryAdapter(releases, mContext)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        attachToParent: Boolean
    ): FragmentVersionHistoryBinding {
        return FragmentVersionHistoryBinding.inflate(inflater, container, false)
    }
}