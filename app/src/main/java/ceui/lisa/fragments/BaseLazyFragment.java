package ceui.lisa.fragments;

import androidx.viewbinding.ViewBinding;


public abstract class BaseLazyFragment<T extends ViewBinding> extends BaseFragment<T> {

    protected boolean isLoaded;

    public void lazyData() {
    }

    @Override
    public void onResume() {
        super.onResume();
        shouldLoadData();
    }

    @Override
    protected void initData() {
        // 移除 initData 中的自动触发，改由 onResume 触发
    }

    public void shouldLoadData() {
        if (!isInit) {
            return;
        }

        if (isResumed() && !isLoaded) {
            lazyData();
            isLoaded = true;
        }
    }

    public boolean isLazy() {
        return true;
    }
}
