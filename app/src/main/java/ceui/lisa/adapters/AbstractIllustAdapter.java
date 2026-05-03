package ceui.lisa.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;

import ceui.lisa.R;
import ceui.lisa.activities.ImageDetailActivity;
import ceui.lisa.models.IllustsBean;

public abstract class AbstractIllustAdapter<VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {

    protected IllustsBean allIllust;
    protected Context mContext;
    protected int imageSize;
    protected boolean isForceOriginal;
    private long lastClickTime = 0;

    @Override
    public int getItemCount() {
        return allIllust.getPage_count();
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.itemView.setOnClickListener(v -> {
            if (android.os.SystemClock.elapsedRealtime() - lastClickTime < 1000) {
                return;
            }
            lastClickTime = android.os.SystemClock.elapsedRealtime();

            Intent intent = new Intent(mContext, ImageDetailActivity.class);
            intent.putExtra("illust", allIllust);
            intent.putExtra("dataType", "二级详情");
            intent.putExtra("index", position);
            if (mContext instanceof Activity) {
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    (Activity) mContext,
                    holder.itemView.findViewById(R.id.illust_image),
                    "image_" + position
                );
                mContext.startActivity(intent, options.toBundle());
            } else {
                mContext.startActivity(intent);
            }
        });
    }
}
