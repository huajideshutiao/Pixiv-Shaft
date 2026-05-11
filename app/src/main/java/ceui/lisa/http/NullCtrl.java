package ceui.lisa.http;

import ceui.lisa.core.NetCallback;

public abstract class NullCtrl<T> extends NetCallback<T> {

    public abstract void success(T t);

    public void nullSuccess() {
    }

    @Override
    public void onSuccess(T t) {
        if (t != null) {
            success(t);
        } else {
            nullSuccess();
        }
        must(true);
    }

    @Override
    public void onError(Throwable e) {
        super.onError(e);
        must(false);
    }

    @Override
    public void must(boolean isSuccess) {
        must();
    }
}