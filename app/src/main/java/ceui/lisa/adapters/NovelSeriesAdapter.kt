package ceui.lisa.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.content.Intent
import ceui.lisa.R
import ceui.lisa.activities.ContainerActivity
import ceui.lisa.databinding.RecyNovelSeriesOfUserBinding
import ceui.lisa.models.NovelSeriesItem
import ceui.lisa.utils.Params
import kotlin.math.floor

class NovelSeriesAdapter(
    list: MutableList<NovelSeriesItem>,
    context: Context
) : BaseAdapter<NovelSeriesItem, RecyNovelSeriesOfUserBinding>(list, context) {

    override fun initLayout() {
        mLayoutID = R.layout.recy_novel_series_of_user
    }

    override fun createViewBinding(inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean): RecyNovelSeriesOfUserBinding {
        return RecyNovelSeriesOfUserBinding.inflate(inflater, parent, attachToParent)
    }

    override fun bindData(
        target: NovelSeriesItem,
        bindView: ViewHolder<RecyNovelSeriesOfUserBinding>,
        position: Int
    ) {
        bindView.baseBind.title.text = target.title
        bindView.baseBind.description.text = target.display_text

        val minute: Float = target.total_character_count / 500.0f
        bindView.baseBind.extraDescription.text = mContext.getString(
            R.string.how_many_novels,
            target.content_count,
            target.total_character_count,
            floor(minute / 60).toInt(),
            (minute % 60).toInt()
        )

        bindView.itemView.setOnClickListener {
            val intent = Intent(mContext, ContainerActivity::class.java)
            intent.putExtra(Params.ID, allItems[position].id)
            intent.putExtra(ContainerActivity.EXTRA_FRAGMENT, "小说系列详情")
            mContext.startActivity(intent)
        }
    }
}