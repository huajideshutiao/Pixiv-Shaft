package ceui.lisa.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.SystemClock
import androidx.recyclerview.widget.RecyclerView
import ceui.pixiv.route.AppRoute
import ceui.lisa.models.IllustsBean

abstract class AbstractIllustAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {

    @JvmField
    protected var allIllust: IllustsBean? = null

    @JvmField
    protected var mContext: Context? = null

    @JvmField
    protected var imageSize: Int = 0

    @JvmField
    protected var isForceOriginal: Boolean = false

    private var lastClickTime: Long = 0

    override fun getItemCount(): Int {
        return allIllust?.page_count ?: 0
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.itemView.setOnClickListener {
            if (SystemClock.elapsedRealtime() - lastClickTime < 1000) {
                return@setOnClickListener
            }
            lastClickTime = SystemClock.elapsedRealtime()
            AppRoute.ImageDetail(allIllust, position).start(mContext!!)
        }
    }
}