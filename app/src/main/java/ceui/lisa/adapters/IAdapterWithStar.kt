package ceui.lisa.adapters

import android.content.Context
import android.view.View
import ceui.lisa.databinding.RecyIllustStaggerBinding
import ceui.lisa.models.IllustsBean

class IAdapterWithStar(
    targetList: List<IllustsBean>,
    context: Context
) : IAdapter(targetList, context) {

    private var hideStarIcon = false

    override fun bindData(
        target: IllustsBean,
        bindView: ViewHolder<RecyIllustStaggerBinding>,
        position: Int
    ) {
        super.bindData(target, bindView, position)
        bindView.baseBind.likeButton.visibility = if (hideStarIcon) View.GONE else View.VISIBLE
    }

    fun setHideStarIcon(hideStarIcon: Boolean): IAdapterWithStar {
        this.hideStarIcon = hideStarIcon
        return this
    }
}
