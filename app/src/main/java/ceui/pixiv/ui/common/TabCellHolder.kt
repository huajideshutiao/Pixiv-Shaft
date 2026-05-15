package ceui.pixiv.ui.common

import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import ceui.lisa.annotations.ItemHolder
import ceui.lisa.databinding.CellTabBinding

class TabCellHolder(
    val title: String,
    val secondaryTitle: String? = null,
    val extraInfo: String? = null,
    val showGreenDone: Boolean = false,
    val selected: LiveData<Boolean>? = null
) : ListItemHolder() {

}

@ItemHolder(TabCellHolder::class)
class TabCellViewHolder(bd: CellTabBinding) : ListItemViewHolder<CellTabBinding, TabCellHolder>(bd) {

    override fun onBindViewHolder(holder: TabCellHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        binding.firstTitle.text = holder.title
        binding.secondaryTitle.text = holder.secondaryTitle ?: ""
        binding.secondaryTitle.isVisible = !holder.secondaryTitle.isNullOrEmpty()
        binding.extraInfo.text = holder.extraInfo ?: ""
        binding.extraInfo.isVisible = !holder.extraInfo.isNullOrEmpty()
    }
}