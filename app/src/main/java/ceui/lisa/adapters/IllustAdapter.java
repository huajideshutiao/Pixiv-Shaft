package ceui.lisa.adapters;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import ceui.lisa.R;
import ceui.lisa.activities.BaseActivity;
import ceui.lisa.activities.Shaft;
import ceui.lisa.databinding.RecyIllustDetailBinding;
import ceui.lisa.download.IllustDownload;
import ceui.lisa.models.IllustsBean;
import ceui.lisa.transformer.LargeBitmapScaleTransformer;
import ceui.lisa.transformer.UniformScaleTransformation;
import ceui.lisa.utils.Common;
import ceui.lisa.utils.Params;
import ceui.lisa.utils.PixivOperate;
import ceui.pixiv.ui.task.LoadTask;
import ceui.pixiv.ui.task.NamedUrl;
import ceui.pixiv.ui.task.TaskPool;
import ceui.pixiv.ui.task.TaskStatus;

public class IllustAdapter extends AbstractIllustAdapter<ViewHolder<RecyIllustDetailBinding>> {

    private static final String TAG = "IllustAdapter";

    private final int maxHeight;
    private final FragmentActivity mActivity;
    private final Fragment mFragment;
    private static final boolean longPressDownload = Shaft.sSettings.isIllustLongPressDownload();

    public IllustAdapter(FragmentActivity activity, Fragment fragment, IllustsBean illustsBean, int maxHeight, boolean isForceOriginal) {
        Common.showLog("IllustAdapter maxHeight " + maxHeight);
        mActivity = activity;
        mContext = fragment.requireContext();
        allIllust = illustsBean;
        this.maxHeight = maxHeight;
        imageSize = mContext.getResources().getDisplayMetrics().widthPixels;
        this.isForceOriginal = isForceOriginal;
        this.mFragment = fragment;
    }

    @NonNull
    @Override
    public ViewHolder<RecyIllustDetailBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder<>(RecyIllustDetailBinding.inflate(
            LayoutInflater.from(mContext), parent, false
        ));
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder<RecyIllustDetailBinding> holder) {
        super.onViewRecycled(holder);
        // Cancel any in-flight Glide load targeting this ImageView so a late-arriving
        // bitmap from the previous bind can't leak into the recycled holder.
        Glide.with(mFragment).clear(holder.baseBind.illust);
        holder.baseBind.illust.setTag(R.id.tag_image_url, null);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder<RecyIllustDetailBinding> holder, int position) {
        super.onBindViewHolder(holder, position);
        if(longPressDownload && mActivity instanceof BaseActivity<?>){
            holder.itemView.setOnLongClickListener(v -> {
                IllustDownload.downloadIllustCertainPage(allIllust, position, (BaseActivity<?>) mActivity);
                if(Shaft.sSettings.isAutoPostLikeWhenDownload() && !allIllust.isIs_bookmarked()){
                    PixivOperate.postLikeDefaultStarType(allIllust);
                }
                return true;
            });
        }

        if (position == 0) {
            // 第一张图：宽 = 屏宽，FIT_CENTER 不裁切。
            // 单 P：高 = max(自然高, maxHeight)，扁图保留 maxHeight 占位。
            // 多 P（≥2P）：高 = 自然高，不施加 maxHeight 约束，消除上下黑边。
            int iw = allIllust.getWidth();
            int ih = allIllust.getHeight();
            boolean hasValidDims = iw > 0 && ih > 0;

            ImageView.ScaleType scaleType;
            int targetHeight;
            boolean changeSize;
            String branchTag;
            if (!hasValidDims) {
                scaleType = ImageView.ScaleType.FIT_CENTER;
                targetHeight = maxHeight > 0 ? maxHeight : holder.baseBind.illust.getLayoutParams().height;
                changeSize = true;
                branchTag = "fallback(noValidDims)";
            } else {
                int naturalHeight = Math.round((float) imageSize * ih / iw);
                scaleType = ImageView.ScaleType.FIT_CENTER;
                if (allIllust.getPage_count() >= 2) {
                    // 多P：第一P 直接用自然高度，不施加 maxHeight 约束，消除上下黑边
                    targetHeight = naturalHeight;
                } else {
                    targetHeight = maxHeight > 0 ? Math.max(naturalHeight, maxHeight) : naturalHeight;
                }
                changeSize = false;
                boolean tall = maxHeight <= 0 || naturalHeight >= maxHeight;
                branchTag = (allIllust.getPage_count() == 1 ? "single_" : "multiP_")
                        + (tall ? "tall_natural" : "flat_padToMax");
            }

            int pageCount = allIllust.getPage_count();
            Log.d(
                "V3MultiP",
                "[IllustAdapter.bind pos=0] illustId=" + allIllust.getId() + ", page_count=" + pageCount + ", iw=" + iw + ", ih=" + ih + ", " +
                    "imageSize(=screenW)=" + imageSize + ", maxHeight=" + maxHeight + ", branch=" + branchTag + ", targetHeight=" + targetHeight + ", " +
                    "scaleType=" + scaleType + ", changeSize=" + changeSize + ", adapterClass=" + this.getClass()
                    .getSimpleName() + ", getItemCount=" + getItemCount()
            );

            holder.baseBind.illust.setScaleType(scaleType);
            ViewGroup.LayoutParams params = holder.baseBind.illust.getLayoutParams();
            params.width = imageSize;
            params.height = targetHeight;
            holder.baseBind.illust.setLayoutParams(params);
            loadIllust(holder, position, changeSize);
        } else {
            Log.d(
                "V3MultiP",
                "[IllustAdapter.bind pos=" + position + "] non-first page, illustId=" + allIllust.getId()
            );
            holder.baseBind.illust.setScaleType(ImageView.ScaleType.CENTER_CROP);
            loadIllust(holder, position, true);
        }
    }

    /**
     * @param holder
     * @param position
     * @param changeSize 是否自动计算宽高
     */
    private void loadIllust(ViewHolder<RecyIllustDetailBinding> holder, int position, boolean changeSize) {
        final String imageUrl;
        boolean isLoadOriginalImage = Shaft.sSettings.isShowOriginalPreviewImage() || isForceOriginal;
        if (isLoadOriginalImage) {
            imageUrl = IllustDownload.getUrl(allIllust, position, Params.IMAGE_RESOLUTION_ORIGINAL);
        } else {
            imageUrl = IllustDownload.getUrl(allIllust, position, Params.IMAGE_RESOLUTION_LARGE);
        }

        // Stamp the ImageView with the URL this bind intends to display. Task LiveData
        // observers are attached to the fragment's viewLifecycleOwner and persist across
        // rebinds; a late-arriving callback from a previous position would otherwise
        // push the wrong bitmap into a recycled holder. Every callback below checks the
        // tag before mutating UI so stale callbacks become no-ops.
        holder.baseBind.illust.setTag(R.id.tag_image_url, imageUrl);

        holder.baseBind.reload.setOnClickListener(v -> {
            holder.baseBind.reload.setVisibility(View.GONE);
            holder.baseBind.progressLayout.donutProgress.setVisibility(View.VISIBLE);
            holder.baseBind.progressLayout.donutProgress.setProgress(0);
            TaskPool.INSTANCE.removeTask(imageUrl);
            loadIllust(holder, position, changeSize);
        });

        LifecycleOwner lifecycleOwner = mFragment.getViewLifecycleOwner();
        String shortUrl = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        LoadTask task = TaskPool.INSTANCE.getLoadTask(new NamedUrl("", imageUrl), true);
        Log.d(
            TAG,
            "[IllustAdapter] loadIllust pos=" + position + ", isOriginal=" + isLoadOriginalImage + ", taskId=" + task.getTaskId() + ", taskStatus=" + task.getStatus()
                .getValue() + ", url=" + shortUrl
        );

        task.getStatus().observe(lifecycleOwner, status -> {
            boolean tagMatch = imageUrl.equals(holder.baseBind.illust.getTag(R.id.tag_image_url));
            if (!tagMatch) {
                Log.d(
                    TAG,
                    "[IllustAdapter] status STALE callback ignored. pos=" + position + ", status=" + status + ", url=" + shortUrl
                );
                return;
            }
            Log.d(
                TAG,
                "[IllustAdapter] status -> " + status + ", pos=" + position + ", url=" + shortUrl
            );
            if (status instanceof TaskStatus.Executing) {
                holder.baseBind.progressLayout.donutProgress.setVisibility(View.VISIBLE);
                holder.baseBind.progressLayout.donutProgress.setProgress(
                        ((TaskStatus.Executing) status).getPercentage()
                );
            } else if (status instanceof TaskStatus.Finished) {
                holder.baseBind.progressLayout.donutProgress.setVisibility(View.GONE);
            } else if (status instanceof TaskStatus.Error) {
                holder.baseBind.progressLayout.donutProgress.setVisibility(View.GONE);
                holder.baseBind.reload.setVisibility(View.VISIBLE);
                Log.w(
                    TAG,
                    "[IllustAdapter] showing reload button. pos=" + position + ", url=" + shortUrl
                );
            }
        });

        task.getResult().observe(lifecycleOwner, file -> {
            if (file == null) {
                Log.d(
                    TAG,
                    "[IllustAdapter] result NULL callback. pos=" + position + ", url=" + shortUrl
                );
                return;
            }
            boolean tagMatch = imageUrl.equals(holder.baseBind.illust.getTag(R.id.tag_image_url));
            if (!tagMatch) {
                Log.d(
                    TAG,
                    "[IllustAdapter] result STALE callback ignored. pos=" + position + ", url=" + shortUrl
                );
                return;
            }
            Log.d(
                TAG,
                "[IllustAdapter] result -> file=" + file.getAbsolutePath() + ", exists=" + file.exists() + ", size=" + file.length() + ", pos=" + position + ", url=" + shortUrl
            );
            holder.baseBind.reload.setVisibility(View.GONE);
            holder.baseBind.progressLayout.donutProgress.setVisibility(View.GONE);

            RequestManager requestManager = mFragment != null ? Glide.with(mFragment) : Glide.with(mContext);
            requestManager
                    .asBitmap()
                    .load(file)
                    .transform(new LargeBitmapScaleTransformer())
                    .transition(BitmapTransitionOptions.withCrossFade())
                    .listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            if (!imageUrl.equals(holder.baseBind.illust.getTag(R.id.tag_image_url))) return false;
                            Log.w(
                                TAG,
                                "[IllustAdapter] Glide bitmap FAIL. pos=" + position + ", model=" + model + ", url=" + shortUrl,
                                e
                            );
                            holder.baseBind.reload.setVisibility(View.VISIBLE);
                            holder.baseBind.progressLayout.donutProgress.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            if (!imageUrl.equals(holder.baseBind.illust.getTag(R.id.tag_image_url))) return false;
                            Log.d(
                                TAG,
                                "[IllustAdapter] Glide bitmap OK. pos=" + position + ", " + resource.getWidth() + "x" + resource.getHeight() + ", dataSource=" + dataSource.name() + ", url=" + shortUrl
                            );
                            holder.baseBind.reload.setVisibility(View.GONE);
                            holder.baseBind.progressLayout.donutProgress.setVisibility(View.GONE);
                            if (isLoadOriginalImage) {
                                Shaft.getDefaultPrefs().edit().putBoolean(imageUrl.toString(), true).apply();
                            }
                            return false;
                        }
                    })
                    .into(new UniformScaleTransformation(holder.baseBind.illust, changeSize));
        });
    }
}
