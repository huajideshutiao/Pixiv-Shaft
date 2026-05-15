package ceui.loxia.flag

import android.os.Bundle
import android.view.View
import ceui.lisa.R
import ceui.lisa.databinding.FragmentPixivListBinding
import ceui.loxia.threadSafeArgs
import ceui.pixiv.route.AppRoute
import ceui.pixiv.ui.common.ListMode
import ceui.pixiv.ui.common.PixivFragment
import ceui.pixiv.ui.common.setUpCustomAdapter
import ceui.pixiv.ui.common.viewBinding
import ceui.pixiv.utils.setOnClick

class FlagReasonFragment : PixivFragment(R.layout.fragment_pixiv_list), FlagActionReceiver {

    private val binding by viewBinding(FragmentPixivListBinding::bind)
    private val safeArgs by threadSafeArgs<FlagReasonFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        binding.toolbarLayout.naviBack.setOnClick { activity.finish() }
        binding.toolbarLayout.naviTitle.text = getString(R.string.violated_rule)
        val adapter = setUpCustomAdapter(binding, ListMode.VERTICAL_NO_MARGIN)
        adapter.submitList(
            listOf(
                FlagReasonHolder(
                    FlagReason.ContainsExcessiveSexualId,
                    getString(R.string.contains_excessive_sexual),
                    FlagReason.ContainsExcessiveSexualContent
                ), FlagReasonHolder(
                    FlagReason.ContainsExcessiveGrotesqueId,
                    getString(R.string.contains_excessive_grotesque),
                    FlagReason.ContainsExcessiveGrotesqueContent
                ), FlagReasonHolder(
                    FlagReason.InfringesOnCopyrightsId,
                    getString(R.string.infringes_on_copyrights),
                    FlagReason.InfringesOnCopyrights
                ), FlagReasonHolder(
                    FlagReason.ViolatedOtherRulesId,
                    getString(R.string.violated_other_rules),
                    FlagReason.ViolatedOtherRules
                )
            )
        )
    }

    companion object {
        fun newInstance(flagObjectId: Int, flagObjectType: Int): FlagReasonFragment {
            val fragment = FlagReasonFragment()
            fragment.arguments = Bundle().apply {
                putInt(FlagDescFragment.FlagObjectIdKey, flagObjectId)
                putInt(FlagDescFragment.FlagObjectTypeKey, flagObjectType)
            }
            return fragment
        }

        var shouldAutoFinish = false
    }

    override fun onResume() {
        super.onResume()
        if (shouldAutoFinish) {
            shouldAutoFinish = false
            requireActivity().finish()
        }
    }

    override fun onClickFlagReason(holder: FlagReasonHolder) {
        AppRoute.FlagDesc(holder.id.toInt(), safeArgs.flagObjectId.toInt(), safeArgs.flagObjectType.toInt()).start(requireContext())
    }
}
