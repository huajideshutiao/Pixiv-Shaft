package ceui.lisa.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import ceui.lisa.databinding.RecyRecmdHeaderBinding
import ceui.lisa.models.IllustsBean

class IAdapterWithHeadView(
    targetList: List<IllustsBean>,
    context: Context,
    private val type: String
) : IAdapter(targetList, context) {

    private var mIllustHeader: IllustHeader? = null

    override fun headerSize(): Int = 1

    override fun getHeader(parent: ViewGroup): ViewHolder<RecyRecmdHeaderBinding> {
        val header = IllustHeader(
            RecyRecmdHeaderBinding.inflate(
                LayoutInflater.from(mContext),
                null,
                false
            ),
            type
        )
        header.initView(mContext)
        mIllustHeader = header
        return header
    }

    fun setHeadData(illustsBeans: List<IllustsBean>) {
        mIllustHeader?.show(mContext, illustsBeans)
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        val lp = holder.itemView.layoutParams
        if (lp is StaggeredGridLayoutManager.LayoutParams && holder.layoutPosition == 0) {
            lp.isFullSpan = true
        }
    }
}
