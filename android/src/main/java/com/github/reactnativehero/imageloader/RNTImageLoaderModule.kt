package com.github.reactnativehero.imageloader

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.URLUtil
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.facebook.react.bridge.*
import com.google.zxing.BinaryBitmap
import com.google.zxing.ChecksumException
import com.google.zxing.FormatException
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
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

            Thread {
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
            }.start()

        }

        @JvmStatic fun getImageBuffer(drawable: Drawable): ByteBuffer? {
            return if (drawable is GifDrawable) {
                drawable.buffer
            } else null
        }

        @JvmStatic fun getBase64Image(base64: String): Bitmap {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        @JvmStatic fun saveImage(image: Bitmap, dirName: String, fileName: String): String {

            val filePath = if (dirName.endsWith(File.separator)) {
                "$dirName${File.separator}$fileName"
            } else {
                "$dirName${File.separator}$fileName"
            }

            val output = FileOutputStream(filePath)
            image.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.close()

            return filePath
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

    @ReactMethod
    fun saveBase64Image(options: ReadableMap, promise: Promise) {

        val base64 = options.getString("base64")!!
        val name = options.getString("name")!!

        val bitmap = getBase64Image(base64)
        val filePath = saveImage(bitmap, reactContext.cacheDir.absolutePath, name)

        val map = Arguments.createMap()
        map.putString("path", filePath)

        promise.resolve(map)

    }

    @ReactMethod
    fun detectImageQRCode(options: ReadableMap, promise: Promise) {

        val path = options.getString("path")!!
        val handler = Handler(Looper.getMainLooper())

        Thread(Runnable {

            val bitmap: Bitmap
            try {
                bitmap = BitmapFactory.decodeFile(path)
            }
            catch (ex: Throwable) {
                ex.printStackTrace()
                handler.post { promise.reject("-1", ex.message) }
                return@Runnable
            }

            val width = bitmap.width
            val height = bitmap.height
            val data = IntArray(width * height)
            bitmap.getPixels(data, 0, width, 0, 0, width, height)

            var text = ""

            val source = RGBLuminanceSource(width, height, data)
            val reader = QRCodeReader()
            try {
                val result = reader.decode(BinaryBitmap(HybridBinarizer(source)))
                text = result.text
            }
            catch (e: NotFoundException) {
                e.printStackTrace()
            }
            catch (e: ChecksumException) {
                e.printStackTrace()
            }
            catch (e: FormatException) {
                e.printStackTrace()
            }

            val map = Arguments.createMap()
            map.putString("text", text)
            handler.post { promise.resolve(map) }

        }).start()

    }

    @ReactMethod
    fun compressImage(options: ReadableMap, promise: Promise) {

        val path = options.getString("path")!!
        val size = options.getInt("size")
        val width = options.getInt("width")
        val height = options.getInt("height")
        val maxSize = options.getInt("maxSize")

        if (size <= maxSize) {
            val map = Arguments.createMap()
            map.putString("path", path)
            map.putInt("size", size)
            map.putInt("width", width)
            map.putInt("height", height)
            promise.resolve(map)
            return
        }

        val ratio = width.toFloat() / height.toFloat()
        val decreaseWidth = width < height

        var outputDir = reactContext.cacheDir.absolutePath
        if (!outputDir.endsWith(File.separator)) {
            outputDir += File.separator
        }
        val uuid = UUID.randomUUID().toString()
        val outputFile = "$outputDir$uuid.jpg"

        val handler = Handler(Looper.getMainLooper())

        Thread(Runnable {

            val bitmap: Bitmap
            try {
                bitmap = BitmapFactory.decodeFile(path)
            }
            catch (ex: Throwable) {
                ex.printStackTrace()
                handler.post { promise.reject("-1", ex.message) }
                return@Runnable
            }

            var outputWidth = width
            var outputHeight = height
            var outputSize = 0

            var success = false
            try {
                while (outputWidth > 0 && outputHeight > 0) {
                    val localBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.RGB_565)
                    val localCanvas = Canvas(localBitmap)
                    val byteArrayOutputStream = ByteArrayOutputStream()

                    localCanvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), Rect(0, 0, outputWidth, outputHeight), null)
                    localBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
                    localBitmap.recycle()

                    outputSize = byteArrayOutputStream.size()
                    if (outputSize <= maxSize) {
                        if (outputSize > 0) {
                            val fileOutputStream = FileOutputStream(outputFile)
                            fileOutputStream.write(byteArrayOutputStream.toByteArray())
                            fileOutputStream.close()
                            success = true
                        }
                        byteArrayOutputStream.close()
                        break
                    }
                    else {
                        byteArrayOutputStream.close()
                        if (decreaseWidth) {
                            outputWidth -= getDecreaseOffset(outputWidth)
                            outputHeight = (outputWidth.toFloat() / ratio).toInt()
                        }
                        else {
                            outputHeight -= getDecreaseOffset(outputHeight)
                            outputWidth = (outputHeight.toFloat() * ratio).toInt()
                        }
                    }
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
                handler.post { promise.reject("-1", ex.message) }
                return@Runnable
            }

            if (success) {
                val map = Arguments.createMap()
                map.putString("path", outputFile)
                map.putInt("size", outputSize)
                map.putInt("width", outputWidth)
                map.putInt("height", outputHeight)

                handler.post { promise.resolve(map) }
            }
            else {
                handler.post { promise.reject("-1", "compress image failed.") }
            }

        }).start()

    }

    private fun getDecreaseOffset(size: Int): Int {
        if (size > 4000) {
            return 2000
        }
        else if (size > 3000) {
            return 1000
        }
        else if (size > 2000) {
            return 500
        }
        else if (size > 1500) {
            return 300
        }
        else if (size > 1000) {
            return 200
        }
        else if (size > 500) {
            return 50
        }
        else if (size > 300) {
            return 30
        }

        return 10
    }
}
