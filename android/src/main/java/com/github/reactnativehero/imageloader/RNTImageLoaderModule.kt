package com.github.reactnativehero.imageloader

import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.facebook.react.bridge.*
import java.util.*

class RNTImageLoaderModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        private const val PURPOSE_NONE = 1
        private const val PURPOSE_SIGN_IN = 2
        private const val PURPOSE_TRANSPORT = 3
        private const val PURPOSE_SPORT = 4

        private const val MODE_HIGH_ACCURACY = 1
        private const val MODE_BATTERY_SAVING = 2
        private const val MODE_DEVICE_SENSORS = 3
    }

    private var client = AMapLocationClient(reactContext)
    private var clientOptions = AMapLocationClientOption()

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        // 销毁定位客户端，同时销毁本地定位服务
        client.onDestroy()
    }

    override fun getName(): String {
        return "RNTImageLoader"
    }

    override fun getConstants(): Map<String, Any>? {

        val constants: MutableMap<String, Any> = HashMap()

        constants["PURPOSE_NONE"] = PURPOSE_NONE
        constants["PURPOSE_SIGN_IN"] = PURPOSE_SIGN_IN
        constants["PURPOSE_TRANSPORT"] = PURPOSE_TRANSPORT
        constants["PURPOSE_SPORT"] = PURPOSE_SPORT

        constants["MODE_HIGH_ACCURACY"] = MODE_HIGH_ACCURACY
        constants["MODE_BATTERY_SAVING"] = MODE_BATTERY_SAVING
        constants["MODE_DEVICE_SENSORS"] = MODE_DEVICE_SENSORS

        return constants

    }

    @ReactMethod
    fun setOptions(options: ReadableMap) {

        if (options.hasKey("purpose")) {
            // 设置定位场景，目前支持三种场景（签到、出行、运动，默认无场景）
            clientOptions.locationPurpose = when (options.getInt("purpose")) {
                PURPOSE_SIGN_IN -> {
                    AMapLocationClientOption.AMapLocationPurpose.SignIn
                }
                PURPOSE_TRANSPORT -> {
                    AMapLocationClientOption.AMapLocationPurpose.Transport
                }
                PURPOSE_SPORT -> {
                    AMapLocationClientOption.AMapLocationPurpose.Sport
                }
                else -> {
                    null
                }
            }
        }

        if (options.hasKey("mode")) {
            clientOptions.locationMode = when (options.getInt("mode")) {
                // 高精度定位模式：会同时使用网络定位和GPS定位，优先返回最高精度的定位结果，以及对应的地址描述信息
                MODE_HIGH_ACCURACY -> {
                    AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                }
                // 低功耗定位模式：不会使用GPS和其他传感器，只会使用网络定位（Wi-Fi和基站定位）
                MODE_BATTERY_SAVING -> {
                    AMapLocationClientOption.AMapLocationMode.Battery_Saving
                }
                // 仅用设备定位模式：不需要连接网络，只使用GPS进行定位，这种模式下不支持室内环境的定位，需要在室外环境下才可以成功定位
                else -> {
                    AMapLocationClientOption.AMapLocationMode.Device_Sensors
                }
            }
        }

        if (options.hasKey("timeout")) {
            // 单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒
            clientOptions.httpTimeOut = options.getInt("timeout") * 1000L
        }

        if (options.hasKey("enableWifiScan")) {
            // 是否主动刷新设备wifi模块，获取到最新鲜的wifi列表，默认 false
            clientOptions.isWifiScan = options.getBoolean("enableWifiScan")
        }

        if (options.hasKey("enableMock")) {
            // 设置是否允许模拟位置，默认为 true，允许模拟位置
            clientOptions.isMockEnable = options.getBoolean("enableMock")
        }

        if (options.hasKey("enableCache")) {
            // 是否开启定位缓存机制，默认开启
            clientOptions.isLocationCacheEnable = options.getBoolean("enableCache")
        }

        // 暂不开放连续定位
//        if (options.hasKey("interval")) {
//            // 设置定位间隔，单位毫秒，默认为2000ms，最低1000ms
//            clientOptions.interval = options.getInt("interval").toLong()
//        }

    }

    @ReactMethod
    fun location(promise: Promise) {

        stopLocationIfNeeded()

        // 获取最近3s内精度最高的一次定位结果
        clientOptions.isOnceLocationLatest = true

        startLocation {
            if (it.errorCode == AMapLocation.LOCATION_SUCCESS) {
                val coords = Arguments.createMap()
                // 纬度
                coords.putDouble("latitude", it.latitude)
                // 纬度
                coords.putDouble("longitude", it.longitude)
                // 位置精度
                coords.putDouble("accuracy", it.accuracy.toDouble())
                // 海拔
                coords.putDouble("altitude", it.altitude)

                val map = Arguments.createMap()
                map.putMap("coords", coords)
                map.putString("timestamp", it.time.toString())

                promise.resolve(map)
            }
            else {
                promise.reject(it.errorCode.toString(), it.errorInfo)
            }
        }

    }

    @ReactMethod
    fun reGeocode(promise: Promise) {

        stopLocationIfNeeded()

        // 获取最近3s内精度最高的一次定位结果
        clientOptions.isOnceLocationLatest = true

        startLocation {
            if (it.errorCode == AMapLocation.LOCATION_SUCCESS) {
                val map = Arguments.createMap()
                map.putString("address", it.address)
                map.putString("country", it.country)
                map.putString("province", it.province)
                map.putString("city", it.city)
                map.putString("district", it.district)
                map.putString("street", it.street)
                map.putString("number", it.streetNum)
                promise.resolve(map)
            }
            else {
                promise.reject(it.errorCode.toString(), it.errorInfo)
            }
        }

    }

    private fun startLocation(listener: (AMapLocation) -> Unit) {
        client.setLocationOption(clientOptions)
        client.setLocationListener(listener)
        client.startLocation()
    }

    private fun stopLocationIfNeeded() {
        if (client.isStarted) {
            client.stopLocation()
            client.setLocationListener {  }
        }
    }

}
