package ceui.pixiv.ui.task


import android.util.Log

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import ceui.lisa.activities.Shaft
import java.util.UUID

abstract class QueuedRunnable<ResultT> {

    protected val context: Context
        get() {
            return Shaft.getContext()
        }

    private var _onNext: (() -> Unit)? = null

    protected val _status = MutableLiveData<TaskStatus>(TaskStatus.NotStart)
    val status: LiveData<TaskStatus> = _status

    protected val _result = MutableLiveData<ResultT>()
    val result: LiveData<ResultT> get() = _result

    open val taskId = UUID.randomUUID().hashCode().toLong()

    val isDownloading: LiveData<Boolean> = status.map { it is TaskStatus.Executing }

    open fun start(onNext: () -> Unit) {
        this._onNext = onNext
    }

    abstract suspend fun execute()

    fun reset() {
        _status.value = TaskStatus.NotStart
    }

    open fun onIgnore() {
        Log.d(
            TAG,
            "[QueuedRunnable] onIgnore class=${this.javaClass.simpleName}, taskId=$taskId, status=${_status.value}, hasResult=${_result.value != null}"
        )
    }

    open fun onStart() {
        Log.d(
            TAG,
            "[QueuedRunnable] onStart class=${this.javaClass.simpleName}, taskId=$taskId, prevStatus=${_status.value}"
        )
    }

    open fun onEnd(resultT: ResultT) {
        Log.d(
            TAG,
            "[QueuedRunnable] onEnd class=${this.javaClass.simpleName}, taskId=$taskId, result=$resultT"
        )
        this._onNext?.invoke()
    }

    open fun onError(ex: Exception?) {
        Log.w(
            TAG,
            "[QueuedRunnable] onError class=${this.javaClass.simpleName}, taskId=$taskId, prevStatus=${_status.value}",
            ex
        )
        if (ex != null) {
            _status.postValue(TaskStatus.Error(ex))
            this._onNext?.invoke()
        }
    }


    companion object {
        private const val TAG = "QueuedRunnable"
    }
}