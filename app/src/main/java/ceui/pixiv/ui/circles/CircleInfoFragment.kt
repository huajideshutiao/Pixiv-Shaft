package ceui.pixiv.ui.circles

import android.os.Bundle
import android.view.View
import ceui.lisa.R
import ceui.lisa.databinding.FragmentCircleInfoBinding
import ceui.loxia.Client
import ceui.pixiv.ui.common.PixivFragment
import ceui.pixiv.ui.common.pixivValueViewModel
import ceui.pixiv.ui.common.viewBinding

class CircleInfoFragment : PixivFragment(R.layout.fragment_circle_info) {

    private val binding by viewBinding(FragmentCircleInfoBinding::bind)
    private val viewModel by pixivValueViewModel(ownerProducer = { requireParentFragment() }) {
        Client.webApi.getCircleDetail("aa")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.result.observe(viewLifecycleOwner) { circle ->
            binding.description.text = circle.body?.meta?.pixpedia?.abstract ?: ""
        }
    }
}