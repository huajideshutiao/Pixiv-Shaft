package ceui.pixiv.ui.task

import android.net.Uri
import android.util.Log
import ceui.lisa.activities.Shaft
import ceui.lisa.utils.GlideUrlChild
import ceui.pixiv.utils.ProgressInfo
import ceui.pixiv.utils.ProgressListener
import ceui.pixiv.utils.ProgressManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

open class LoadTask(
    val content: NamedUrl,
    coroutineScope: CoroutineScope,
    autoStart: Boolean = true
) : QueuedRunnable<File>() {

    override val taskId: Long
        get() = content.url.hashCode().toLong()

    init {
        if (autoStart) {
            coroutineScope.launch {
                execute()
            }
        }
    }

    override suspend fun execute() {
        val shortUrl = content.url.substringAfterLast('/')
        if (_status.value is TaskStatus.Executing || _status.value is TaskStatus.Finished) {
            if (_result.value != null) {
                val cachedFile = _result.value
                val fileInfo = cachedFile?.let { "path=${it.absolutePath}, exists=${it.exists()}, size=${it.length()}" } ?: "null"
                Log.d(
                    TAG,
                    "[LoadTask] SKIP duplicate taskId=$taskId, status=${_status.value}, file=[$fileInfo], url=$shortUrl"
                )
                onIgnore()
                return
            }
            Log.w(
                TAG,
                "[LoadTask] status=${_status.value} but result is NULL, will re-execute. taskId=$taskId, url=$shortUrl"
            )
        }
        Log.d(
            TAG,
            "[LoadTask] START taskId=$taskId, thread=${Thread.currentThread().name}, url=$shortUrl"
        )

        try {
            onStart()
            _status.value = TaskStatus.Executing(0)
            observeProgress()

            val startMs = System.currentTimeMillis()
            val file = downloadFile()
            val elapsedMs = System.currentTimeMillis() - startMs
            if (file != null) {
                Log.d(
                    TAG,
                    "[LoadTask] SUCCESS taskId=$taskId, elapsed=${elapsedMs}ms, path=${file.absolutePath}, size=${file.length()}, url=$shortUrl"
                )
                _result.value = file
                _status.value = TaskStatus.Finished
                onEnd(file)
            } else {
                throw IllegalStateException("Unexpected null file")
            }
        } catch (ex: Exception) {
            Log.e(
                TAG,
                "[LoadTask] ERROR taskId=$taskId, status=${_status.value}, url=$shortUrl",
                ex
            )
            onError(ex)
        }
    }

    private fun observeProgress() {
        val shortUrl = content.url.substringAfterLast('/')
        ProgressManager.addResponseListener(content.url, object : ProgressListener {
            override fun onProgress(progressInfo: ProgressInfo) {
                val percent = progressInfo.percent
                if (progressInfo.isFinish || percent == 100) {
                    Log.d(TAG, "[LoadTask] PROGRESS 100%% finished. taskId=$taskId, url=$shortUrl")
                    _status.value = TaskStatus.Finished
                } else {
                    if (_status.value != TaskStatus.Finished) {
                        _status.value = TaskStatus.Executing(percent)
                    }
                }
            }

            override fun onError(id: Long, e: Exception) {
                Log.e(TAG, "[LoadTask] PROGRESS error. taskId=$taskId, url=$shortUrl", e)
            }
        })
    }

    private suspend fun downloadFile(): File? {
        val shortUrl = content.url.substringAfterLast('/')
        Log.d(TAG, "[LoadTask] downloadFile enter IO. taskId=$taskId, url=$shortUrl")
        return withContext(Dispatchers.IO) {
            val loadSource = content.url.takeIf { it.startsWith("http") }
                ?.let { GlideUrlChild(it) }
                ?: Uri.parse(content.url)
            Glide.with(Shaft.getContext())
                .asFile()
                .load(loadSource)
                .listener(createGlideListener())
                .submit()
                .get()
        }
    }

    private fun createGlideListener(): RequestListener<File> {
        val shortUrl = content.url.substringAfterLast('/')
        return object : RequestListener<File> {
            override fun onLoadFailed(
                ex: GlideException?,
                model: Any?,
                target: Target<File>,
                isFirstResource: Boolean
            ): Boolean {
                Log.e(
                    TAG,
                    "[LoadTask] Glide onLoadFailed. taskId=$taskId, model=$model, url=$shortUrl",
                    ex
                )
                return false
            }

            override fun onResourceReady(
                resource: File,
                model: Any,
                target: Target<File>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                Log.d(
                    TAG,
                    "[LoadTask] Glide onResourceReady. taskId=$taskId, dataSource=${dataSource.name}, path=${resource.path}, size=${resource.length()}, url=$shortUrl"
                )
                return false
            }
        }
    }

    companion object {
        private const val TAG = "LoadTask"
    }
}
