package com.github.reactnativehero.imageloader

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.*

class ProgressResponseBody(url: String, private val responseBody: ResponseBody) : ResponseBody() {

    private val bufferedSource: BufferedSource by lazy {
        Okio.buffer(ProgressSource(responseBody.source()))
    }
    private var listener = ProgressInterceptor.LISTENER_MAP[url]

    override fun contentType(): MediaType? {
        return responseBody.contentType()
    }

    override fun contentLength(): Long {
        return responseBody.contentLength()
    }

    override fun source(): BufferedSource {
        return bufferedSource
    }

    private inner class ProgressSource internal constructor(source: Source?) : ForwardingSource(source) {

        var loadedBytes: Long = 0

        override fun read(sink: Buffer, byteCount: Long): Long {
            val bytes = super.read(sink, byteCount)
            val totalBytes = responseBody.contentLength()
            if (bytes > 0) {
                loadedBytes += bytes
            }
            if (loadedBytes > 0 && totalBytes > 0) {
                listener?.onProgress(loadedBytes, totalBytes)
            }
            return bytes
        }

    }

}
