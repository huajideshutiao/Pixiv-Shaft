package ceui.lisa.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import ceui.lisa.R
import ceui.lisa.databinding.RecyArticalHorizonBinding
import ceui.lisa.models.SpotlightArticlesBean
import ceui.lisa.utils.GlideUtil
import com.bumptech.glide.Glide
import android.view.View as View1

class PivisionHAdapter(targetList: MutableList<SpotlightArticlesBean>, context: Context) :
    BaseAdapter<SpotlightArticlesBean, RecyArticalHorizonBinding>(targetList, context) {

    override fun initLayout() {
        mLayoutID = R.layout.recy_artical_horizon
    }

    override fun createViewBinding(inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean): RecyArticalHorizonBinding {
        return RecyArticalHorizonBinding.inflate(inflater, parent, attachToParent)
    }

    override fun bindData(
        target: SpotlightArticlesBean,
        bindView: ViewHolder<RecyArticalHorizonBinding>,
        position: Int
    ) {
        bindView.baseBind.title.text = allItems[position].title
        Glide.with(mContext)
            .load(GlideUtil.getUrl(allItems[position].thumbnail))
            .into(bindView.baseBind.illustImage)
        if (mOnItemClickListener != null) {
            bindView.itemView.setOnClickListener { v: View1? ->
                mOnItemClickListener.onItemClick(v, position, 0)
            }
        }
    }
}
