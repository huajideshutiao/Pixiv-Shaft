package ceui.lisa.core

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object ThreadUtil {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())

    fun runOnIo(runnable: Runnable) {
        scope.launch {
            runnable.run()
        }
    }

    fun runOnMain(runnable: Runnable) {
        handler.post(runnable)
    }

    fun isMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }

    class DownloadHandle {
        private val cancelled = AtomicBoolean(false)
        private val job = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch { }

        fun dispose() {
            cancelled.set(true)
            job.cancel()
        }

        fun isDisposed(): Boolean = cancelled.get()
    }
}