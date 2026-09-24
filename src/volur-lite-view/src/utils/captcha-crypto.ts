import { getCaptchaPublicKeyApi } from '@/api/verification'

const DEFAULT_RSA_PUBLIC_KEY_PEM =
  'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu+AXPmYoGzaA+F7Qbv2x' +
  'XyE79kBYhOkMTzW5NJlOPwsCQpB7q7O868GsZTRH3PHglYOnbbSpvJi3rstEHgKQ' +
  'd9p9G53c4xcZk3EDWCCtTt9kU7cRiwYjSouxEMxUoYsgVEKwDw4N5mHFrUGMcU4L' +
  'RPnL/lsJJQ91uBDUDb3idwTRl5a4mv/EMdB+pKy0CwL2TNF8+31CAAhbxMFWvINQ' +
  'emwT5TxoTXFVDViODXm6F5s4ALQjKbY2RY3gflG9RIEsjz5wPu2rqU0AHAOVLdBQ' +
  'x71sYNlxXm70PADaNbDNToOSNAUGvQrzGD1tLcqacVHbDKy6Dkdbd0+FRiPfAjk0' +
  'zQIDAQAB'

let activePublicKeyPem: string = DEFAULT_RSA_PUBLIC_KEY_PEM
let inFlightPublicKeyPromise: Promise<string> | null = null

interface RsaPublicKey {
  n: bigint
  e: bigint
  keyBytes: number
}

let cachedPublicKey: RsaPublicKey | null = null

/**
 * 手动设置/更新当前生效的 RSA 公钥
 */
export function setCaptchaPublicKey(pem: string): void {
  if (pem && pem.trim()) {
    const clean = pem.replace(/-----BEGIN PUBLIC KEY-----|-----END PUBLIC KEY-----|[\r\n\s]/g, '')
    if (clean !== activePublicKeyPem) {
      activePublicKeyPem = clean
      cachedPublicKey = null
    }
  }
}

let isPublicKeyFetched = false

/**
 * 异步获取并缓存服务端最新下发的 RSA 公钥
 * - 会话生命周期内只请求一次（单例内存缓存）；
 * - 支持并发在途 Promise 合并去重；
 * - 支持通过 forceRefresh=true 强制重新拉取。
 */
export async function fetchAndCacheCaptchaPublicKey(forceRefresh = false): Promise<string> {
  // 1. 若已经成功获取且未强制刷新，直接返回内存中已生效的公钥，零网络开销
  if (isPublicKeyFetched && !forceRefresh) {
    return activePublicKeyPem
  }

  // 2. 若当前有正在发起的请求，复用在途 Promise 避免并发重复请求
  if (inFlightPublicKeyPromise) {
    return inFlightPublicKeyPromise
  }

  inFlightPublicKeyPromise = (async () => {
    try {
      const res = await getCaptchaPublicKeyApi()
      if (res.data?.publicKey) {
        setCaptchaPublicKey(res.data.publicKey)
        isPublicKeyFetched = true
      }
      return activePublicKeyPem
    } catch (err) {
      console.warn('[VolurCaptcha] 动态获取服务端最新公钥失败，保持使用现有公钥', err)
      return activePublicKeyPem
    } finally {
      inFlightPublicKeyPromise = null
    }
  })()

  return inFlightPublicKeyPromise
}

/**
 * 解析 SPKI DER 格式的 RSA 公钥
 */
function parseSpkiPublicKey(base64: string): RsaPublicKey {
  if (cachedPublicKey) {
    return cachedPublicKey
  }

  const binary = atob(base64.replace(/[\r\n\s]/g, ''))
  const der = new Uint8Array(binary.length)
  for (let idx = 0; idx < binary.length; idx++) {
    der[idx] = binary.charCodeAt(idx)
  }

  let p = 0
  function readLength(): number {
    let len = der[p++]
    if (len & 0x80) {
      const count = len & 0x7f
      len = 0
      for (let idx = 0; idx < count; idx++) {
        len = (len << 8) | der[p++]
      }
    }
    return len
  }

  // 1. Skip outer SEQUENCE
  if (der[p++] !== 0x30) throw new Error('Invalid SPKI key: expected 0x30')
  readLength()

  // 2. Skip AlgorithmIdentifier SEQUENCE
  if (der[p++] !== 0x30) throw new Error('Invalid SPKI key: expected alg 0x30')
  const algLen = readLength()
  p += algLen

  // 3. Read BIT STRING
  if (der[p++] !== 0x03) throw new Error('Invalid SPKI key: expected bit string 0x03')
  readLength()
  p++ // skip unused bits count byte

  // 4. Inner RSAPublicKey SEQUENCE
  if (der[p++] !== 0x30) throw new Error('Invalid SPKI key: expected inner 0x30')
  readLength()

  // 5. Modulus INTEGER
  if (der[p++] !== 0x02) throw new Error('Invalid SPKI key: expected modulus INTEGER')
  const modLen = readLength()
  const modBytes = der.slice(p, p + modLen)
  p += modLen

  // 6. Exponent INTEGER
  if (der[p++] !== 0x02) throw new Error('Invalid SPKI key: expected exponent INTEGER')
  const expLen = readLength()
  const expBytes = der.slice(p, p + expLen)

  let n = 0n
  for (let idx = 0; idx < modBytes.length; idx++) {
    n = (n << 8n) | BigInt(modBytes[idx])
  }

  let e = 0n
  for (let idx = 0; idx < expBytes.length; idx++) {
    e = (e << 8n) | BigInt(expBytes[idx])
  }

  const keyBytes = modBytes[0] === 0 ? modLen - 1 : modLen
  cachedPublicKey = { n, e, keyBytes }
  return cachedPublicKey
}

/**
 * 快速模幂运算: (base ^ exp) % mod
 */
function modPow(base: bigint, exp: bigint, mod: bigint): bigint {
  let res = 1n
  base = base % mod
  while (exp > 0n) {
    if (exp % 2n === 1n) {
      res = (res * base) % mod
    }
    base = (base * base) % mod
    exp /= 2n
  }
  return res
}

/**
 * 原生 BigInt 实现 PKCS#1 v1.5 RSA 加密
 */
function rsaEncryptPkcs1(msgBytes: Uint8Array, pub: RsaPublicKey): Uint8Array {
  const k = pub.keyBytes
  if (msgBytes.length > k - 11) {
    throw new Error('Message too long for RSA PKCS#1 v1.5')
  }

  const psLen = k - msgBytes.length - 3
  const em = new Uint8Array(k)
  em[0] = 0x00
  em[1] = 0x02

  // PS 填充：非零随机字节
  for (let idx = 0; idx < psLen; idx++) {
    let r = 0
    while (r === 0) {
      r = Math.floor(Math.random() * 255) + 1
    }
    em[2 + idx] = r
  }
  em[2 + psLen] = 0x00
  em.set(msgBytes, 3 + psLen)

  let m = 0n
  for (let idx = 0; idx < em.length; idx++) {
    m = (m << 8n) | BigInt(em[idx])
  }

  const c = modPow(m, pub.e, pub.n)
  let hex = c.toString(16)
  if (hex.length % 2 !== 0) {
    hex = '0' + hex
  }

  const cBytes = new Uint8Array(hex.length / 2)
  for (let idx = 0; idx < hex.length; idx += 2) {
    cBytes[idx / 2] = parseInt(hex.substring(idx, idx + 2), 16)
  }

  if (cBytes.length < k) {
    const padded = new Uint8Array(k)
    padded.set(cBytes, k - cBytes.length)
    return padded
  }
  return cBytes
}

/**
 * Volur 平台验证码专用魔改 RC4 编码器 (Rc4CaptchaEncoder)
 * 与后端 com.humolix.volur.verification.crypto.Rc4CaptchaEncoder 100% 对齐
 */
export class Rc4CaptchaEncoder {
  /**
   * 编码（加密）数据字节数组（对称加密）
   */
  public encode(data: Uint8Array, key: Uint8Array): Uint8Array {
    return this.process(data, key)
  }

  /**
   * 解码（解密）数据字节数组（对称解密）
   */
  public decode(data: Uint8Array, key: Uint8Array): Uint8Array {
    return this.process(data, key)
  }

  /**
   * 对称流数据处理
   */
  public process(data: Uint8Array, key: Uint8Array): Uint8Array {
    return Rc4CaptchaEncoder.process(data, key)
  }

  /**
   * 静态编码（加密）方法
   */
  public static encode(data: Uint8Array, key: Uint8Array): Uint8Array {
    return this.process(data, key)
  }

  /**
   * 静态解码（解密）方法
   */
  public static decode(data: Uint8Array, key: Uint8Array): Uint8Array {
    return this.process(data, key)
  }

  /**
   * 静态对称流处理方法
   */
  public static process(data: Uint8Array, key: Uint8Array): Uint8Array {
    if (!key || key.length === 0) {
      throw new Error('RC4 key cannot be empty')
    }
    const cipher = new Rc4CipherState(key)
    return cipher.process(data)
  }

  /**
   * 创建底层流状态机实例
   */
  public static createCipher(key: Uint8Array): Rc4CipherState {
    return new Rc4CipherState(key)
  }
}

/**
 * 内部魔改 RC4 状态机实现
 */
export class Rc4CipherState {
  private s: number[] = new Array(256)
  private i = 0
  private j = 0

  constructor(key: Uint8Array) {
    if (!key || key.length === 0) {
      throw new Error('RC4 key cannot be empty')
    }
    // 1. S-Box 掩码混淆初始化
    for (let k = 0; k < 256; k++) {
      this.s[k] = (k ^ 0x5a) & 0xff
    }

    // 2. KSA 双轮置换引入 0x3c
    let curJ = 0
    for (let round = 0; round < 2; round++) {
      for (let k = 0; k < 256; k++) {
        const keyByte = key[k % key.length]
        curJ = (curJ + this.s[k] + keyByte + 0x3c) & 0xff
        const tmp = this.s[k]
        this.s[k] = this.s[curJ]
        this.s[curJ] = tmp
      }
    }

    // 3. 丢弃前 256 字节弱流
    for (let k = 0; k < 256; k++) {
      this.nextByte()
    }
  }

  private nextByte(): number {
    this.i = (this.i + 1) & 0xff
    this.j = (this.j + this.s[this.i] + 0x07) & 0xff
    const tmp = this.s[this.i]
    this.s[this.i] = this.s[this.j]
    this.s[this.j] = tmp
    return this.s[(this.s[this.i] + this.s[this.j] + 0xa5) & 0xff]
  }

  public process(data: Uint8Array): Uint8Array {
    const out = new Uint8Array(data.length)
    for (let k = 0; k < data.length; k++) {
      out[k] = data[k] ^ this.nextByte()
    }
    return out
  }
}

// 统一导出默认实例与兼容别名
export const rc4CaptchaEncoder = new Rc4CaptchaEncoder()
export const Rc4Util = Rc4CaptchaEncoder
export const VolurModifiedRc4 = Rc4CipherState

function uint8ArrayToBase64(bytes: Uint8Array): string {
  let binary = ''
  const len = bytes.byteLength
  for (let idx = 0; idx < len; idx++) {
    binary += String.fromCharCode(bytes[idx])
  }
  return btoa(binary)
}

/**
 * 加密人机验证场景标识为 captchaSceneId
 *
 * @param routePath 当前触发组件所在路由路径 (如 "/auth/login")
 * @param customPublicKey 可选自定义服务端公钥
 * @returns 格式为 ${rsaKeyBase64}.${rc4CipherBase64} 的安全密文
 */
export function encryptCaptchaScene(routePath: string, customPublicKey?: string): string {
  const normalizedPath = routePath && routePath.trim() ? routePath.trim() : '/auth/login'
  const pub = parseSpkiPublicKey(customPublicKey || activePublicKeyPem || DEFAULT_RSA_PUBLIC_KEY_PEM)

  // 1. 生成 16 字节随机会话密钥
  const sessionKey = new Uint8Array(16)
  if (typeof window !== 'undefined' && window.crypto && window.crypto.getRandomValues) {
    window.crypto.getRandomValues(sessionKey)
  } else {
    for (let idx = 0; idx < 16; idx++) {
      sessionKey[idx] = Math.floor(Math.random() * 256)
    }
  }

  // 2. RSA 加密 sessionKey
  const encSessionKeyBytes = rsaEncryptPkcs1(sessionKey, pub)
  const encKeyBase64 = uint8ArrayToBase64(encSessionKeyBytes)

  // 3. 构建带防重放时间戳的 JSON 载荷
  const payload = JSON.stringify({
    path: normalizedPath,
    ts: Date.now()
  })
  const textEncoder = new TextEncoder()
  const plainBytes = textEncoder.encode(payload)

  // 4. 魔改 RC4 流加密 (通过 Rc4CaptchaEncoder 编码器)
  const cipherBytes = rc4CaptchaEncoder.encode(plainBytes, sessionKey)
  const cipherBase64 = uint8ArrayToBase64(cipherBytes)

  return `${encKeyBase64}.${cipherBase64}`
}
