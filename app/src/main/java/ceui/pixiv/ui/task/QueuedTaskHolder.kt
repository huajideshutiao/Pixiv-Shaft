package ceui.pixiv.ui.task

import android.graphics.Color
import android.os.Build
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.LiveData
import ceui.lisa.R
import ceui.lisa.annotations.ItemHolder
import ceui.lisa.databinding.CellQueuedTaskBinding
import ceui.lisa.databinding.CellUsersYoruItemBinding
import ceui.loxia.Illust
import ceui.loxia.findActionReceiverOrNull
import ceui.pixiv.ui.bottom.UsersYoriActionReceiver
import ceui.pixiv.ui.common.ListItemHolder
import ceui.pixiv.ui.common.ListItemViewHolder

class QueuedTaskHolder(val downloadTask: DownloadTask, val illust: Illust) : ListItemHolder() {
    override fun getItemId(): Long {
        return downloadTask.taskId
    }
}

@ItemHolder(QueuedTaskHolder::class)
class QueuedTaskViewHolder(bd: CellQueuedTaskBinding) :
    ListItemViewHolder<CellQueuedTaskBinding, QueuedTaskHolder>(bd) {

    override fun onBindViewHolder(holder: QueuedTaskHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        binding.taskName.text = holder.downloadTask.content.name
    }
}