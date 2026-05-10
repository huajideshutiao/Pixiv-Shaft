package ceui.lisa.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

import ceui.lisa.databinding.RecyRecmdHeaderBinding;
import ceui.lisa.models.NovelBean;

public class NAdapterWithHeadView extends NAdapter {

    private NovelHeader novelHeader = null;

    public NAdapterWithHeadView(List<NovelBean> targetList, Context context) {
        super(targetList, context);
    }

    @Override
    public int headerSize() {
        return 1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ViewHolder getHeader(ViewGroup parent) {
        novelHeader = new NovelHeader(
            RecyRecmdHeaderBinding.inflate(
                        LayoutInflater.from(mContext),
                        null,
                        false
                )
        );
        novelHeader.initView(mContext);
        return novelHeader;
    }

    public void setHeadData(List<NovelBean> novelBeans) {
        if (novelHeader != null) {
            novelHeader.show(mContext, novelBeans);
        }
    }
}
