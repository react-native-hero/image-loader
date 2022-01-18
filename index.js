
import { NativeModules } from 'react-native'

const { RNTImageLoader } = NativeModules

/**
 * 把 base64 图片保存到本地
 */
 export function saveBase64Image(options) {
  return RNTImageLoader.saveBase64Image(options)
}
