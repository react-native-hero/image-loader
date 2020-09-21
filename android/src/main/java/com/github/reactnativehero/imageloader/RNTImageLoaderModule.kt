package com.github.reactnativehero.imageloader

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.URLUtil
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import java.io.File
import java.nio.ByteBuffer
import java.util.*

class RNTImageLoaderModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {

        @JvmStatic fun init(app: Application) {

            // 和 ios 保持一致，预留初始化接口

        }

        @JvmStatic fun loadImage(context: Context, url: String, onProgress: (Long, Long) -> Unit, onComplete: (Bitmap?) -> Unit) {

            addProgressListener(url, onProgress)

            Glide.with(context).asBitmap().load(url).listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Bitmap>, isFirstResource: Boolean): Boolean {
                    removeProgressListener(url)
                    runMainThread {
                        onComplete(null)
                    }
                    return false
                }
                override fun onResourceReady(resource: Bitmap, model: Any, target: Target<Bitmap>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                    removeProgressListener(url)
                    runMainThread {
                        onComplete(resource)
                    }
                    return false
                }
            }).submit()

        }

        @JvmStatic fun setThumbnailImage(imageView: ImageView, url: String, loading: Int, error: Int, onProgress: (Long, Long) -> Unit, onComplete: (Drawable?) -> Unit) {

            var options = RequestOptions()
            if (loading > 0) {
                options = options.placeholder(loading)
            }
            if (error > 0) {
                options = options.error(error)
            }

            runMainThread {

                addProgressListener(url, onProgress)

                Glide.with(imageView.context).load(url).apply(options).listener(object: RequestListener<Drawable> {

                    override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                        removeProgressListener(url)
                        runMainThread {
                            onComplete(null)
                        }
                        return false
                    }
                    override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        removeProgressListener(url)
                        runMainThread {
                            onComplete(resource)
                        }
                        return false
                    }

                }).into(imageView)
            }

        }

        @JvmStatic fun setHDImage(imageView: ImageView, url: String, loading: Int, error: Int, onProgress: (Long, Long) -> Unit, onComplete: (Drawable?) -> Unit) {

            val fileName = URLUtil.guessFileName(url, null, null).toLowerCase(Locale.ENGLISH)
            var extName = ""
            val index = fileName.lastIndexOf(".")
            if (index > 0) {
                extName = fileName.substring(index + 1)
            }

            var options = RequestOptions()
            if (loading > 0) {
                options = options.placeholder(loading)
            }
            if (error > 0) {
                options = options.error(error)
            }

            runMainThread {

                addProgressListener(url, onProgress)

                if (extName == "gif") {
                    Glide.with(imageView.context).load(url).apply(options).listener(object : RequestListener<Drawable> {

                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            removeProgressListener(url)
                            runMainThread {
                                onComplete(null)
                            }
                            return false
                        }

                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            removeProgressListener(url)
                            runMainThread {
                                onComplete(resource)
                            }
                            return false
                        }

                    }).into(imageView)
                }
                else {
                    if (loading > 0) {
                        imageView.setImageDrawable(options.placeholderDrawable)
                    }
                    Glide.with(imageView.context).downloadOnly().load(url).listener(object : RequestListener<File> {

                        override fun onLoadFailed(e: GlideException?, model: Any, target: Target<File>, isFirstResource: Boolean): Boolean {
                            removeProgressListener(url)
                            runMainThread {
                                if (error > 0) {
                                    imageView.setImageDrawable(options.errorPlaceholder)
                                }
                                onComplete(null)
                            }
                            return false
                        }

                        override fun onResourceReady(resource: File, model: Any, target: Target<File>, dataSource: DataSource, isFirstResource: Boolean): Boolean {

                            removeProgressListener(url)

                            var absolutePath = resource.absolutePath
                            if (absolutePath.startsWith("/")) {
                                absolutePath = absolutePath.substring(1)
                            }

                            runMainThread {
                                imageView.setImageURI(Uri.parse("file:///$absolutePath"))
                                onComplete(imageView.drawable)
                            }

                            return false
                        }

                    }).submit()
                }
            }

        }

        @JvmStatic fun getImageCachePath(context: Context, url: String, callback: (String) -> Unit) {

            Thread(Runnable {
                try {
                    val options = RequestOptions().onlyRetrieveFromCache(true)
                    val file = Glide.with(context).asFile().apply(options).load(url).submit().get()
                    runMainThread {
                        callback(file.absolutePath)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runMainThread {
                        callback("")
                    }
                }
            }).start()

        }

        @JvmStatic fun getImageBuffer(drawable: Drawable): ByteBuffer? {
            return if (drawable is GifDrawable) {
                drawable.buffer
            } else null
        }

        private fun addProgressListener(url: String, onProgress: (Long, Long) -> Unit) {

            val listener = object : ProgressListener {
                override fun onProgress(loaded: Long, total: Long) {
                    runMainThread {
                        onProgress(loaded, total)
                    }
                    if (loaded > 0 && loaded >= total) {
                        ProgressInterceptor.removeListener(url)
                    }
                }
            }

            ProgressInterceptor.addListener(url, listener)

        }

        private fun removeProgressListener(url: String) {

            ProgressInterceptor.removeListener(url)

        }

        private fun runMainThread(callback: () -> Unit) {

            if (Looper.myLooper() == Looper.getMainLooper()) {
                callback()
                return
            }

            val mainThread = Handler(Looper.getMainLooper())
            mainThread.post { callback() }

        }

    }

    override fun getName(): String {
        return "RNTImageLoader"
    }

}
