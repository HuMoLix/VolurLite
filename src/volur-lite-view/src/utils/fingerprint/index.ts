/**
 * Volur 统一设备指纹与风控探针中心 (Fingerprint Core)
 *
 * 聚合 Canvas 2D 离屏微特征、WebGL 显卡硬件特征以及浏览器环境特征。
 * 内存单例缓存，页面仅需在首次启动时执行一次（< 2ms），后续无感秒读。
 */

import { getCanvasFingerprint, murmurhash3 } from './canvas'
import { getWebGLFingerprint, type WebGLFingerprint } from './webgl'
import { detectBrowserEnvironment, type BrowserFamily, type BrowserEnvironmentInfo } from './browser'

export interface DeviceFingerprint {
  canvasHash: string
  browserFamily: BrowserFamily
  renderer: string
  vendor: string
  isHeadless: boolean
  timestamp: number
  deviceHash: string
  deviceToken: string
}

let cachedFingerprint: DeviceFingerprint | null = null

/**
 * 提取并生成当前客户端设备环境指纹 (内存单例缓存)
 */
export function getDeviceFingerprint(): DeviceFingerprint {
  if (cachedFingerprint) {
    return cachedFingerprint
  }

  const canvasHash = getCanvasFingerprint()
  const webgl: WebGLFingerprint = getWebGLFingerprint()
  const env: BrowserEnvironmentInfo = detectBrowserEnvironment()

  const isHeadless = env.isSuspiciousAutomation || webgl.isHeadlessRenderer
  const timestamp = Date.now()

  // 生成紧凑唯一的设备特征散列 (结合浏览器家族、Canvas 与 WebGL 渲染器)
  const rawSignature = `${env.browserFamily}|${canvasHash}|${webgl.renderer}|${webgl.vendor}`
  const hash1 = murmurhash3(rawSignature, 0)
  const hash2 = murmurhash3(rawSignature, 42)
  const deviceHash = `${hash1}${hash2}`

  // 构造标准化的设备凭证 Token (供 HTTP 请求头 X-Device-Fingerprint 透传)
  const payload = {
    dh: deviceHash,
    ch: canvasHash,
    bf: env.browserFamily,
    hl: isHeadless ? 1 : 0,
    ts: timestamp
  }

  let deviceToken = ''
  try {
    deviceToken = btoa(JSON.stringify(payload))
  } catch {
    deviceToken = deviceHash
  }

  cachedFingerprint = {
    canvasHash,
    browserFamily: env.browserFamily,
    renderer: webgl.renderer,
    vendor: webgl.vendor,
    isHeadless,
    timestamp,
    deviceHash,
    deviceToken
  }

  return cachedFingerprint
}

/**
 * 同步快捷获取已生成的设备 Token 请求头字符串
 */
export function getDeviceTokenHeader(): string {
  return getDeviceFingerprint().deviceToken
}

export type { BrowserFamily } from './browser'
