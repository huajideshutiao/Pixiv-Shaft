package ceui.lisa.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import ceui.lisa.R
import ceui.lisa.activities.Shaft
import ceui.lisa.models.IllustsBean
import ceui.lisa.transformer.LargeBitmapScaleTransformer
import ceui.lisa.utils.Common
import ceui.lisa.utils.GlideUtil
import ceui.pixiv.route.AppRoute
import ceui.pixiv.ui.task.TaskPool
import ceui.pixiv.utils.ImageCacheChain
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions.withCrossFade
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

/**
 * 作品详情页竖向多P列表
 */
class IllustDetailAdapter : AbstractIllustAdapter<RecyclerView.ViewHolder> {

    private var mFragment: Fragment? = null

    constructor(list: IllustsBean, context: Context, isForceOriginal: Boolean) {
        mContext = context
        allIllust = list
        this.isForceOriginal = isForceOriginal
        imageSize = (context.resources.displayMetrics.widthPixels -
            2 * context.resources.getDimensionPixelSize(R.dimen.twelve_dp))
    }

    constructor(list: IllustsBean, context: Context) : this(list, context, false)

    constructor(fragment: Fragment, list: IllustsBean?) : this(fragment, list, false)

    constructor(fragment: Fragment, list: IllustsBean?, isForceOriginal: Boolean) {
        mFragment = fragment
        val context = fragment.requireContext()
        mContext = context
        allIllust = list
        this.isForceOriginal = isForceOriginal
        imageSize = (context.resources.displayMetrics.widthPixels -
            2 * context.resources.getDimensionPixelSize(R.dimen.twelve_dp))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return TagHolder(
            LayoutInflater.from(mContext).inflate(
                R.layout.recy_illust_grid, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val currentOne = holder as TagHolder
        val illust = allIllust ?: return
        val context = mContext ?: return
        currentOne.illust.transitionName = "image_$position"

        currentOne.illust.setOnClickListener {
            val activity = mFragment?.activity ?: return@setOnClickListener
            AppRoute.ImageDetail(allIllust, position).start(activity, currentOne.illust, "image_$position")
        }

        currentOne.illust.setOnLongClickListener {
            val isLoadOriginalImage = Shaft.sSettings.isShowOriginalPreviewImage || isForceOriginal
            val imageUrlStr = if (isLoadOriginalImage) {
                GlideUtil.getOriginalImage(illust, position).toStringUrl()
            } else {
                GlideUtil.getLargeImage(illust, position).toStringUrl()
            }
            val cachedFile = TaskPool.peekCachedFile(imageUrlStr)
            if (cachedFile != null && cachedFile.exists()) {
                Common.shareImageFile(context, cachedFile, "${illust.id}_p$position.jpg")
            } else {
                Common.showToast(R.string.msg_load_fail)
            }
            true
        }

        Common.showLog("IllustDetailAdapter onBindViewHolder 000")

        val cached = ImageCacheChain.peek(context, illust, position)
        if (cached is Bitmap) {
            val params = currentOne.illust.layoutParams
            params.width = imageSize
            params.height = imageSize * cached.height / cached.width
            currentOne.illust.layoutParams = params
            currentOne.illust.setImageBitmap(cached)
        }

        val isLoadOriginalImage = Shaft.sSettings.isShowOriginalPreviewImage || isForceOriginal
        val imageUrl = if (isLoadOriginalImage) {
            GlideUtil.getOriginalImage(illust, position)
        } else {
            GlideUtil.getLargeImage(illust, position)
        }
        val requestManager: RequestManager =
            if (mFragment != null) Glide.with(mFragment!!) else Glide.with(context)
        if (position == 0) {
            val params = currentOne.illust.layoutParams
            params.height = imageSize * illust.height / illust.width
            params.width = imageSize
            currentOne.illust.layoutParams = params
            requestManager
                .asBitmap()
                .load(imageUrl)
                .override(imageSize, params.height)
                .transform(LargeBitmapScaleTransformer())
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        currentOne.illust.setImageBitmap(resource)
                        if (isLoadOriginalImage) {
                            Shaft.getDefaultPrefs()
                                .edit { putBoolean(imageUrl.toStringUrl(), true) }
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        } else {
            requestManager
                .asBitmap()
                .load(imageUrl)
                .override(imageSize, imageSize)
                .transform(LargeBitmapScaleTransformer())
                .transition(withCrossFade())
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        val params = currentOne.illust.layoutParams
                        params.width = imageSize
                        params.height = imageSize * resource.height / resource.width
                        currentOne.illust.layoutParams = params
                        currentOne.illust.setImageBitmap(resource)
                        if (isLoadOriginalImage) {
                            Shaft.getDefaultPrefs()
                                .edit { putBoolean(imageUrl.toStringUrl(), true) }
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }
    }

    class TagHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var illust: ImageView = itemView.findViewById(R.id.illust_image)
    }
}
