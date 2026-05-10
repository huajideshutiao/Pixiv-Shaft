package ceui.lisa.fragments;

import androidx.viewbinding.ViewBinding;


public abstract class BaseLazyFragment<T extends ViewBinding> extends BaseFragment<T> {

    protected boolean isLoaded;

    public void lazyData() {
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        shouldLoadData();
    }

    @Override
    protected void initData() {
        shouldLoadData();
    }

    @SuppressWarnings("deprecation")
    public void shouldLoadData() {
        if (!isInit) {
            return;
        }

        if (getUserVisibleHint() && isLazy() && !isLoaded) {
            lazyData();
            isLoaded = true;
        }
    }

    public boolean isLazy() {
        return true;
    }
}
