package ceui.lisa.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.scwang.smart.refresh.header.FalsifyFooter;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshHeader;

import ceui.pixiv.ui.common.RefreshHelper;

public abstract class SwipeFragment<T extends ViewBinding>
        extends BaseLazyFragment<T> implements Swipe {

    @Override
    public RefreshHeader getHeader() {
        return null;
    }

    @Override
    public FalsifyFooter getFooter() {
        return new FalsifyFooter(mContext);
    }

    @Override
    public void init() {
        SmartRefreshLayout layout = getSmartRefreshLayout();
        if (layout != null) {
            layout.setEnableRefresh(true);
            layout.setEnableLoadMore(true);
            RefreshHelper.setupMaterialHeader(layout, this);
            layout.setRefreshFooter(getFooter());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }

    public boolean enableRefresh() {
        return true;
    }

    public boolean enableLoadMore() {
        return true;
    }
}
