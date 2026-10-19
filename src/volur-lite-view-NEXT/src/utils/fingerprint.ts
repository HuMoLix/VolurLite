import FingerprintJS from '@fingerprintjs/fingerprintjs'
import { getCookie, setCookie } from './cookie'

export const COOKIE_DEVICE_ID = '_v_did'
export const COOKIE_DEVICE_TOKEN = '_v_dtk'
const STORAGE_DEVICE_ID_KEY = 'volur_device_id'

export interface FingerprintMatrixResult {
  deviceId: string
  deviceToken: string
  visitorId: string
}

let cachedVisitorId = ''
let cachedDeviceToken = ''
let fpPromise: ReturnType<typeof FingerprintJS.load> | null = null

export function getBrowserFamily(): string {
  if (typeof navigator === 'undefined') return 'OTHER'
  const ua = navigator.userAgent.toLowerCase()
  if (ua.includes('edg/')) return 'EDGE'
  if (ua.includes('firefox') || ua.includes('fxios')) return 'FIREFOX'
  if (ua.includes('safari') && !ua.includes('chrome') && !ua.includes('crios') && !ua.includes('android')) {
    return 'SAFARI'
  }
  if (ua.includes('chrome') || ua.includes('crios') || ua.includes('chromium')) {
    return 'CHROME'
  }
  return 'OTHER'
}

function generateUUID(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

export function getDeviceId(): string {
  let cookieDid = getCookie(COOKIE_DEVICE_ID)
  let storageDid: string | null = null

  try {
    if (typeof localStorage !== 'undefined') {
      storageDid = localStorage.getItem(STORAGE_DEVICE_ID_KEY)
    }
  } catch {
  }

  let finalDid = cookieDid || storageDid

  if (!finalDid) {
    finalDid = generateUUID()
  }

  if (cookieDid !== finalDid) {
    setCookie(COOKIE_DEVICE_ID, finalDid, 365)
  }
  if (storageDid !== finalDid) {
    try {
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem(STORAGE_DEVICE_ID_KEY, finalDid)
      }
    } catch {
    }
  }

  return finalDid
}

function getFingerprintAgent(): ReturnType<typeof FingerprintJS.load> {
  if (!fpPromise) {
    fpPromise = FingerprintJS.load()
  }
  return fpPromise
}

export async function initDeviceFingerprint(): Promise<FingerprintMatrixResult> {
  const deviceId = getDeviceId()

  let visitorId = cachedVisitorId
  if (!visitorId) {
    try {
      const fp = await getFingerprintAgent()
      const result = await fp.get()
      visitorId = result.visitorId
      cachedVisitorId = visitorId
    } catch (err) {
      visitorId = deviceId.replace(/-/g, '').substring(0, 32)
      cachedVisitorId = visitorId
    }
  }

  const isHeadless = typeof navigator !== 'undefined' && Boolean(navigator.webdriver)
  const browserFamily = getBrowserFamily()
  const timestamp = Date.now()

  const payload = {
    did: deviceId,
    dh: visitorId,
    ch: visitorId.substring(0, 16),
    bf: browserFamily,
    hl: isHeadless ? 1 : 0,
    ts: timestamp
  }

  let deviceToken = ''
  try {
    deviceToken = btoa(JSON.stringify(payload))
  } catch {
    deviceToken = visitorId
  }

  cachedDeviceToken = deviceToken
  setCookie(COOKIE_DEVICE_TOKEN, deviceToken, 7)

  return {
    deviceId,
    deviceToken,
    visitorId
  }
}

export function getDeviceFingerprint(): string {
  if (cachedVisitorId) {
    return cachedVisitorId
  }
  const did = getDeviceId()
  return did.replace(/-/g, '').substring(0, 32)
}

export function getDeviceToken(): string {
  if (cachedDeviceToken) {
    return cachedDeviceToken
  }
  return getCookie(COOKIE_DEVICE_TOKEN) || getDeviceFingerprint()
}

export function getCanvasHash(): string {
  try {
    const canvas = document.createElement('canvas')
    canvas.width = 200
    canvas.height = 50
    const ctx = canvas.getContext('2d')
    if (!ctx) return ''
    ctx.textBaseline = 'top'
    ctx.font = "14px 'Arial'"
    ctx.textBaseline = 'alphabetic'
    ctx.fillStyle = '#f60'
    ctx.fillRect(125, 1, 62, 20)
    ctx.fillStyle = '#069'
    ctx.fillText('Volur<canvas>2026', 2, 15)
    ctx.fillStyle = 'rgba(102, 204, 0, 0.7)'
    ctx.fillText('Volur<canvas>2026', 4, 17)
    const dataUrl = canvas.toDataURL()
    let hash = 0
    for (let i = 0; i < dataUrl.length; i++) {
      hash = ((hash << 5) - hash) + dataUrl.charCodeAt(i)
      hash |= 0
    }
    return Math.abs(hash).toString(16)
  } catch {
    return ''
  }
}

export function getWebGLInfo(): { renderer: string; vendor: string } {
  try {
    const canvas = document.createElement('canvas')
    const gl = (canvas.getContext('webgl') || canvas.getContext('experimental-webgl')) as WebGLRenderingContext | null
    if (!gl) return { renderer: '', vendor: '' }
    const dbg = gl.getExtension('WEBGL_debug_renderer_info')
    const renderer = dbg ? (gl.getParameter(dbg.UNMASKED_RENDERER_WEBGL) as string) : ''
    const vendor = dbg ? (gl.getParameter(dbg.UNMASKED_VENDOR_WEBGL) as string) : (navigator.vendor || '')
    return { renderer: renderer || '', vendor: vendor || '' }
  } catch {
    return { renderer: '', vendor: '' }
  }
}

export function getOsFamily(): string {
  if (typeof navigator === 'undefined') return 'OTHER'
  const ua = navigator.userAgent.toLowerCase()
  if (ua.includes('win')) return 'WINDOWS'
  if (ua.includes('mac') && !ua.includes('iphone') && !ua.includes('ipad')) return 'MACOS'
  if (ua.includes('android')) return 'ANDROID'
  if (ua.includes('iphone') || ua.includes('ipad') || ua.includes('ipod')) return 'IOS'
  if (ua.includes('linux')) return 'LINUX'
  return 'OTHER'
}

export function collectDeviceParams(): Record<string, any> {
  if (typeof window === 'undefined') return {}
  const webgl = getWebGLInfo()
  const fp = getDeviceFingerprint()
  const did = getDeviceId()
  return {
    deviceId: did,
    fingerprint: fp,
    deviceHash: fp,
    deviceToken: getDeviceToken(),
    canvasHash: getCanvasHash(),
    browserFamily: getBrowserFamily(),
    osFamily: getOsFamily(),
    renderer: webgl.renderer,
    vendor: webgl.vendor,
    isHeadless: Boolean(navigator.webdriver),
    screenResolution: `${window.screen?.width || 0}x${window.screen?.height || 0}`,
    colorDepth: window.screen?.colorDepth || 24,
    timezone: Intl?.DateTimeFormat?.().resolvedOptions?.().timeZone || '',
    language: navigator.language || '',
    platform: navigator.platform || ''
  }
}
