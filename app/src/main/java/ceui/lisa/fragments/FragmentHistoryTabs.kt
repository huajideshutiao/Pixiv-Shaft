package ceui.lisa.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.lifecycleScope
import ceui.lisa.R
import ceui.lisa.database.AppDatabase
import ceui.lisa.databinding.ViewpagerWithTablayoutBinding
import ceui.pixiv.db.RecordType
import ceui.pixiv.ui.common.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Tab wrapper for the browsing-history page.
 * Three tabs: 插画/漫画 (type=0) | 小说 (type=1) | 用户 (GeneralEntity)
 */
class FragmentHistoryTabs : Fragment(R.layout.viewpager_with_tablayout) {

    private val binding by viewBinding(ViewpagerWithTablayoutBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.placeHolder.visibility = View.VISIBLE
        binding.placeHolder.layoutParams.height = ceui.lisa.activities.Shaft.statusHeight
        binding.placeHolder.requestLayout()
        binding.toolbar.title = " "
        binding.toolbarTitle.text = getString(R.string.view_history)
        binding.toolbar.setNavigationOnClickListener { activity?.finish() }
        binding.toolbar.inflateMenu(R.menu.history_menu)
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_delete) {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.string_143)
                    .setMessage(R.string.string_253)
                    .setPositiveButton(R.string.string_141) { _, _ ->
                        val db = AppDatabase.getAppDatabase(requireContext())
                        lifecycleScope.launch(Dispatchers.IO) {
                            db.downloadDao().deleteAllHistory()
                            db.generalDao().deleteByRecordType(RecordType.VIEW_USER_HISTORY)
                            db.generalDao().deleteByRecordType(RecordType.VIEW_ILLUST_HISTORY)
                            db.generalDao().deleteByRecordType(RecordType.VIEW_NOVEL_HISTORY)
                            withContext(Dispatchers.Main) {
                                activity?.finish()
                            }
                        }
                    }
                    .setNegativeButton(R.string.string_142, null)
                    .show()
                true
            } else {
                false
            }
        }

        val tabs = listOf(
            getString(R.string.string_246) to 0,    // 插画/漫画
            getString(R.string.string_237) to 1,    // 小说
            getString(R.string.tab_user) to -1,     // 用户
        )

        val fragments = tabs.map { (_, type) ->
            if (type >= 0) {
                FragmentHistoryList.newInstance(type)
            } else {
                FragmentHistoryUserList()
            }
        }

        binding.viewPager.adapter = object : FragmentPagerAdapter(
            childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        ) {
            override fun getItem(position: Int): Fragment = fragments[position]
            override fun getCount(): Int = tabs.size
            override fun getPageTitle(position: Int): CharSequence = tabs[position].first
        }
        binding.tabLayout.setupWithViewPager(binding.viewPager)
    }
}
