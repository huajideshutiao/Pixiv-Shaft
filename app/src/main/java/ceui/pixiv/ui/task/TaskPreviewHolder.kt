package ceui.pixiv.ui.task

import ceui.lisa.R
import ceui.lisa.annotations.ItemHolder
import ceui.lisa.databinding.CellTaskPreviewBinding
import ceui.lisa.utils.GlideUrlChild
import ceui.loxia.Illust
import ceui.loxia.findActionReceiverOrNull
import ceui.pixiv.ui.common.ListItemHolder
import ceui.pixiv.ui.common.ListItemViewHolder
import com.bumptech.glide.Glide


class TaskPreviewHolder(val humanReadableTask: HumanReadableTask, val illusts: List<Illust>) : ListItemHolder() {

    override fun areItemsTheSame(other: ListItemHolder): Boolean {
        return humanReadableTask.taskUUID == (other as? TaskPreviewHolder)?.humanReadableTask?.taskUUID
    }

    override fun areContentsTheSame(other: ListItemHolder): Boolean {
        return humanReadableTask == (other as? TaskPreviewHolder)?.humanReadableTask
    }

    fun getIllustOrNull(index: Int): Illust? {
        return illusts.getOrNull(index)
    }
}

@ItemHolder(TaskPreviewHolder::class)
class TaskPreviewViewHolder(bd: CellTaskPreviewBinding) : ListItemViewHolder<CellTaskPreviewBinding, TaskPreviewHolder>(bd) {

    override fun onBindViewHolder(holder: TaskPreviewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        binding.root.setOnClickListener {
            it.findActionReceiverOrNull<TaskPreviewActionReceiver>()?.onClickTaskPreview(holder.humanReadableTask)
        }
        binding.taskSize.text = context.getString(R.string.all_works_count, holder.illusts.size)
        binding.taskName.text = holder.humanReadableTask.taskFullName
        bindPreviewImage(binding.preview1, holder.getIllustOrNull(0))
        bindPreviewImage(binding.preview2, holder.getIllustOrNull(1))
        bindPreviewImage(binding.preview3, holder.getIllustOrNull(2))
        bindPreviewImage(binding.preview4, holder.getIllustOrNull(3))
        bindPreviewImage(binding.preview5, holder.getIllustOrNull(4))
        bindPreviewImage(binding.preview6, holder.getIllustOrNull(5))
    }

    private fun bindPreviewImage(view: android.widget.ImageView, illust: Illust?) {
        val url = illust?.image_urls?.medium
        if (!url.isNullOrEmpty()) {
            Glide.with(view.context).load(GlideUrlChild(url)).into(view)
        }
    }
}

interface TaskPreviewActionReceiver {
    fun onClickTaskPreview(humanReadableTask: HumanReadableTask)
}
