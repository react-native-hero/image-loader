
import { NativeModules } from 'react-native'

const { RNTImageLoader } = NativeModules

/**
 * 把 base64 图片保存到本地
 */
export function saveBase64Image(options) {
  options.base64 = options.base64.replace(/^data:image\/[^;]+;base64,/, '')
  return RNTImageLoader.saveBase64Image(options)
}

/**
 * 识别本地图片二维码
 */
export function decodeImageQRCode(options) {
  return RNTImageLoader.decodeImageQRCode(options)
}

/**
 * 压缩本地图片
 */
export function compressImage(options) {
  return RNTImageLoader.compressImage(options)
}