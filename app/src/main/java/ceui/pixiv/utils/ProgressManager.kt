package ceui.pixiv.utils

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ForwardingSink
import okio.ForwardingSource
import okio.buffer
import java.io.IOException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

interface ProgressListener {
    fun onProgress(progressInfo: ProgressInfo)
    fun onError(id: Long, e: Exception) {}
}

data class ProgressInfo(
    val id: Long,
    var currentBytes: Long = 0,
    var contentLength: Long = 0,
    var intervalTime: Long = 0,
    var eachBytes: Long = 0,
    var isFinish: Boolean = false
) {
    val percent: Int
        get() = if (currentBytes <= 0 || contentLength <= 0) 0 else (100 * currentBytes / contentLength).toInt()

    val speed: Long
        get() = if (eachBytes <= 0 || intervalTime <= 0) 0 else eachBytes * 1000 / intervalTime
}

object ProgressManager {
    private val requestListeners = ConcurrentHashMap<String, MutableList<ProgressListener>>()
    private val responseListeners = ConcurrentHashMap<String, MutableList<ProgressListener>>()
    private val handler = Handler(Looper.getMainLooper())
    private var refreshTime = 150L

    val interceptor = Interceptor { chain ->
        val request = chain.request()
        val url = request.url.toString()

        val wrappedRequest = wrapRequestBody(request)
        val response = chain.proceed(wrappedRequest)
        
        wrapResponseBody(response)
    }

    private fun wrapRequestBody(request: Request): Request {
        val url = request.url.toString()
        val body = request.body
        if (body == null || !requestListeners.containsKey(url)) {
            return request
        }

        return request.newBuilder()
            .method(request.method, ProgressRequestBody(body, requestListeners[url]!!))
            .build()
    }

    private fun wrapResponseBody(response: Response): Response {
        val url = response.request.url.toString()
        val body = response.body
        
        // 核心同步点：处理重定向情况，确保监听器能迁移到新 URL
        if (response.isRedirect) {
            val location = response.header("Location")
            if (!location.isNullOrEmpty()) {
                responseListeners[url]?.let { listeners ->
                    responseListeners.getOrPut(location) { Collections.synchronizedList(mutableListOf()) }
                        .addAll(listeners)
                }
            }
            return response
        }

        if (body == null || !responseListeners.containsKey(url)) {
            return response
        }

        return response.newBuilder()
            .body(ProgressResponseBody(body, responseListeners[url]!!))
            .build()
    }

    fun addResponseListener(url: String, listener: ProgressListener) {
        responseListeners.getOrPut(url) { Collections.synchronizedList(mutableListOf()) }
            .add(listener)
    }

    fun removeResponseListener(url: String, listener: ProgressListener) {
        responseListeners[url]?.remove(listener)
    }

    @JvmStatic
    fun with(builder: okhttp3.OkHttpClient.Builder): okhttp3.OkHttpClient.Builder {
        return builder.addNetworkInterceptor(interceptor)
    }

    private class ProgressRequestBody(
        private val delegate: RequestBody,
        private val listeners: List<ProgressListener>
    ) : RequestBody() {
        private val progressInfo =
            ProgressInfo(System.currentTimeMillis(), contentLength = delegate.contentLength())

        override fun contentType() = delegate.contentType()
        override fun contentLength() = delegate.contentLength()

        override fun writeTo(sink: BufferedSink) {
            val countingSink = object : ForwardingSink(sink) {
                private var totalBytesRead = 0L
                private var lastRefreshTime = 0L
                private var tempSize = 0L

                override fun write(source: Buffer, byteCount: Long) {
                    try {
                        super.write(source, byteCount)
                    } catch (e: IOException) {
                        listeners.forEach { it.onError(progressInfo.id, e) }
                        throw e
                    }

                    totalBytesRead += byteCount
                    tempSize += byteCount
                    val curTime = SystemClock.elapsedRealtime()
                    if (curTime - lastRefreshTime >= refreshTime || totalBytesRead == progressInfo.contentLength) {
                        val interval = curTime - lastRefreshTime
                        val currentTotal = totalBytesRead
                        val currentTemp = tempSize
                        handler.post {
                            progressInfo.eachBytes = currentTemp
                            progressInfo.currentBytes = currentTotal
                            progressInfo.intervalTime = interval
                            progressInfo.isFinish = currentTotal == progressInfo.contentLength
                            listeners.forEach { it.onProgress(progressInfo) }
                        }
                        lastRefreshTime = curTime
                        tempSize = 0
                    }
                }
            }
            val bufferedSink = countingSink.buffer()
            delegate.writeTo(bufferedSink)
            bufferedSink.flush()
        }
    }

    private class ProgressResponseBody(
        private val delegate: ResponseBody,
        private val listeners: List<ProgressListener>
    ) : ResponseBody() {
        private val progressInfo =
            ProgressInfo(System.currentTimeMillis(), contentLength = delegate.contentLength())
        private var bufferedSource: BufferedSource? = null

        override fun contentType() = delegate.contentType()
        override fun contentLength() = delegate.contentLength()

        override fun source(): BufferedSource {
            return bufferedSource ?: delegate.source().run {
                val forwardingSource = object : ForwardingSource(this) {
                    private var totalBytesRead = 0L
                    private var lastRefreshTime = 0L
                    private var tempSize = 0L

                    override fun read(sink: Buffer, byteCount: Long): Long {
                        val bytesRead = try {
                            super.read(sink, byteCount)
                        } catch (e: IOException) {
                            listeners.forEach { it.onError(progressInfo.id, e) }
                            throw e
                        }

                        if (progressInfo.contentLength <= 0) {
                            progressInfo.contentLength = contentLength()
                        }

                        if (bytesRead != -1L) {
                            totalBytesRead += bytesRead
                            tempSize += bytesRead
                        }

                        val curTime = SystemClock.elapsedRealtime()
                        if (curTime - lastRefreshTime >= refreshTime || bytesRead == -1L || (progressInfo.contentLength > 0 && totalBytesRead == progressInfo.contentLength)) {
                            val interval = curTime - lastRefreshTime
                            val currentTotal = totalBytesRead
                            val currentTemp = tempSize
                            handler.post {
                                progressInfo.eachBytes = if (bytesRead != -1L) currentTemp else -1L
                                progressInfo.currentBytes = currentTotal
                                progressInfo.intervalTime = interval
                                progressInfo.isFinish =
                                    bytesRead == -1L || (progressInfo.contentLength > 0 && currentTotal == progressInfo.contentLength)
                                listeners.forEach { it.onProgress(progressInfo) }
                            }
                            lastRefreshTime = curTime
                            tempSize = 0
                        }
                        return bytesRead
                    }
                }
                forwardingSource.buffer().also { bufferedSource = it }
            }
        }
    }
}
