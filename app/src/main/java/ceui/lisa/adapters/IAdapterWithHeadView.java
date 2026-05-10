package ceui.lisa.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.List;

import ceui.lisa.databinding.RecyRecmdHeaderBinding;
import ceui.lisa.models.IllustsBean;

public class IAdapterWithHeadView extends IAdapter {

    private IllustHeader mIllustHeader = null;
    private String type = "";

    public IAdapterWithHeadView(List<IllustsBean> targetList, Context context, String type) {
        super(targetList, context);
        this.type = type;
    }

    @Override
    public int headerSize() {
        return 1;
    }

    @Override
    public ViewHolder<RecyRecmdHeaderBinding> getHeader(ViewGroup parent) {
        mIllustHeader = new IllustHeader(
            RecyRecmdHeaderBinding.inflate(
                        LayoutInflater.from(mContext),
                        null,
                        false
                ), type
        );
        mIllustHeader.initView(mContext);
        return mIllustHeader;
    }

    public void setHeadData(List<IllustsBean> illustsBeans) {
        if (mIllustHeader != null) {
            mIllustHeader.show(mContext, illustsBeans);
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
        if (lp instanceof StaggeredGridLayoutManager.LayoutParams
                && holder.getLayoutPosition() == 0) {
            StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams) lp;
            p.setFullSpan(true);
        }
    }
}
