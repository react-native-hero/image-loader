package com.github.reactnativehero.imageloader

import okhttp3.Interceptor
import okhttp3.Response
import java.util.*

class ProgressInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val url = request.url().toString()
        val body = response.body()
        return response.newBuilder().body(ProgressResponseBody(url, body!!)).build()
    }

    companion object {
        val LISTENER_MAP: MutableMap<String, ProgressListener> = HashMap()
        fun addListener(url: String, listener: ProgressListener) {
            LISTENER_MAP[url] = listener
        }
        fun removeListener(url: String) {
            LISTENER_MAP.remove(url)
        }
    }
}
