package ceui.lisa.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.HttpException
import java.util.function.Function

fun <T> executeCall(
    call: Call<T>,
    mapper: Function<T, T>,
    callback: NetCallback<T>
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            callback.onStart()
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val mapped = mapper.apply(body)
                    withContext(Dispatchers.Main) {
                        callback.onSuccess(mapped)
                        callback.must(true)
                        callback.onFinish()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback.onError(HttpException(response))
                        callback.must(true)
                        callback.onFinish()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    callback.onError(HttpException(response))
                    callback.must(false)
                    callback.onFinish()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback.onError(e)
                callback.must(false)
                callback.onFinish()
            }
        }
    }
}