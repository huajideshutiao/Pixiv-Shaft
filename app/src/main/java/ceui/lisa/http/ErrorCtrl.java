package ceui.lisa.http;

import ceui.lisa.core.NetCallback;

public abstract class ErrorCtrl<T> extends NetCallback<T> {

    @Override
    public void onSuccess(T t) {
        next(t);
    }

    @Override
    public void onError(Throwable e) {
        super.onError(e);
    }
}