package ceui.lisa.core;

public abstract class TryCatchObserver<T> {

    public abstract void next(T t);

    public abstract void error(Throwable e);

    public abstract void complete();

    public void must() {
    }
}