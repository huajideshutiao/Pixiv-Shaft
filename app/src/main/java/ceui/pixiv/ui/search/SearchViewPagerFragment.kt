package ceui.pixiv.ui.search

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import ceui.lisa.R
import ceui.lisa.databinding.FragmentSearchViewpagerBinding
import ceui.lisa.utils.SearchTypeUtil
import ceui.loxia.Tag
import ceui.loxia.combineLatest
import ceui.loxia.hideKeyboard
import ceui.pixiv.ui.circles.PagedFragmentItem
import ceui.pixiv.ui.circles.SmartFragmentPagerAdapter
import ceui.pixiv.ui.common.TitledViewPagerFragment
import ceui.pixiv.ui.common.constructVM
import ceui.pixiv.ui.common.viewBinding
import ceui.pixiv.utils.setOnClick
import ceui.pixiv.widgets.DialogViewModel
import ceui.pixiv.widgets.setUpWith
import com.qmuiteam.qmui.widget.dialog.QMUIDialog

class SearchViewPagerFragment : TitledViewPagerFragment(R.layout.fragment_search_viewpager) {

    private val binding by viewBinding(FragmentSearchViewpagerBinding::bind)
    private val args by navArgs<SearchViewPagerFragmentArgs>()
    private val dialogViewModel by activityViewModels<DialogViewModel>()
    private val searchViewModel by constructVM({ args.keyword }) { word ->
        SearchViewModel(word)
    }

    override fun onViewFirstCreated(view: View) {
        super.onViewFirstCreated(view)
        dialogViewModel.chosenUsersYoriCount.value = 0
        dialogViewModel.choosenOffsetPage.value = 0
        searchViewModel.illustSelectedRadioTabIndex.value = 0
        searchViewModel.novelSelectedRadioTabIndex.value = 0
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val headParams = binding.head.layoutParams
        headParams.height = ceui.lisa.activities.Shaft.statusHeight
        binding.head.layoutParams = headParams

        binding.tagEditer.doAfterTextChanged { text ->
            searchViewModel.inputDraft.value = text?.toString() ?: ""
        }

        combineLatest(searchViewModel.tagList, searchViewModel.inputDraft).observe(viewLifecycleOwner) {
            val tags = it?.first ?: listOf()
            val inputing = it?.second ?: ""
            val hasContent = tags.isNotEmpty() || inputing.isNotEmpty()
            binding.search.isEnabled = true
            binding.search.text =
                if (hasContent) getString(R.string.search) else getString(R.string.string_86)
        }

        searchViewModel.searchType.observe(viewLifecycleOwner) { index ->
            binding.tagEditer.hint = SearchTypeUtil.SEARCH_TYPE_NAME[index]
        }

        binding.search.setOnClick {
            if (binding.search.text == getString(R.string.string_86)) {
                val searchTypes = SearchTypeUtil.SEARCH_TYPE_NAME
                QMUIDialog.CheckableDialogBuilder(requireContext())
                    .setTitle(R.string.string_424)
                    .setCheckedIndex(searchViewModel.searchType.value ?: 5)
                    .addItems(searchTypes) { dialog: DialogInterface, which: Int ->
                        searchViewModel.searchType.value = which
                        if (which == 1) {
                            binding.searchViewPager.currentItem = 0
                        } else if (which == 2) {
                            binding.searchViewPager.currentItem = 2
                        } else if (which == 3) {
                            binding.searchViewPager.currentItem = 1
                        }
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            } else {
                commitEditingTag()
            }
        }
        binding.tagEditer.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                commitEditingTag()
            }
            true
        }
        binding.tagsFlowView.setOnCellClickListener { cell, index ->

        }
        val adapter = SmartFragmentPagerAdapter(
            listOf(
                PagedFragmentItem(
                    builder = {
                        SearchIlllustMangaFragment()
                    },
                    initialTitle = getString(R.string.string_136)
                ),
                PagedFragmentItem(
                    builder = {
                        SearchNovelFragment()
                    },
                    initialTitle = getString(R.string.type_novel)
                ),
                PagedFragmentItem(
                    builder = {
                        SearchUserFragment()
                    },
                    initialTitle = getString(R.string.type_user)
                )
            ),
            this
        )
        binding.searchViewPager.adapter = adapter
        binding.tabLayoutList.setUpWith(binding.searchViewPager, binding.slidingCursor, viewLifecycleOwner, {})

        if (args.landingIndex > 0) {
            binding.searchViewPager.setCurrentItem(args.landingIndex, false)
        }
    }

    private fun commitEditingTag() {
        val draft = searchViewModel.inputDraft.value ?: ""
        if (draft.isNotEmpty()) {
            (searchViewModel.tagList.value ?: listOf()).toMutableList().also {
                it.add(Tag(draft))
                searchViewModel.tagList.value = it
                searchViewModel.inputDraft.value = ""
                binding.tagEditer.clearFocus()
                binding.root.requestFocus()
                hideKeyboard()
                searchViewModel.triggerAllRefreshEvent()
            }
        }
    }
}