package ceui.lisa.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.RecyclerView
import ceui.lisa.R
import ceui.lisa.activities.ImageDetailActivity
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

            val intent = Intent(mContext, ImageDetailActivity::class.java).apply {
                putExtra("illust", allIllust)
                putExtra("dataType", "二级详情")
                putExtra("index", position)
            }

            if (mContext is Activity) {
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    mContext as Activity,
                    holder.itemView.findViewById(R.id.illust_image),
                    "image_$position"
                )
                mContext?.startActivity(intent, options.toBundle())
            } else {
                mContext?.startActivity(intent)
            }
        }
    }
}
