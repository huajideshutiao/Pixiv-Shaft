package ceui.lisa.core;

public class RxRun {

    public static <T> void runOn(RxRunnable<T> runnable, NetCallback<T> observer) {
        if (runnable == null) {
            return;
        }
        runnable.beforeExecute();
        ThreadUtil.INSTANCE.runOnIo(() -> {
            try {
                T result = runnable.execute();
                ThreadUtil.INSTANCE.runOnMain(() -> {
                    observer.onSuccess(result);
                    observer.onFinish();
                });
            } catch (Exception e) {
                ThreadUtil.INSTANCE.runOnMain(() -> {
                    observer.onError(e);
                    observer.onFinish();
                });
            }
        });
    }
}