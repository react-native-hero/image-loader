package com.github.reactnativehero.imageloader

interface ProgressListener {
    fun onProgress(loaded: Long, total: Long)
}
