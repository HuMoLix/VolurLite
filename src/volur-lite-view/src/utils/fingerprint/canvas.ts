/**
 * Canvas 2D 离屏微观渲染指纹生成器
 *
 * 核心原理：
 * 利用不同操作系统（macOS / Windows / Linux / iOS / Android）、
 * 浏览器渲染引擎（WebKit / Blink / Gecko）与图形底层（Core Text / DirectWrite / FreeType）
 * 在次像素抗锯齿、阴影模糊计算及复杂贝塞尔曲线混合渲染上的微观差异，
 * 生成设备与浏览器环境强相关的不可变散列。
 *
 * 跨浏览器兼容铁律：
 * 1. Safari (macOS / iOS)：完美兼容 WebKit 渲染管线与 Display P3/sRGB 色彩空间；
 * 2. Firefox (Gecko)：兼容字体抗锯齿微调；
 * 3. Chrome / Edge (Blink)：兼容 Skia 几何光栅化；
 * 4. 自动化脚本（Python / curl / Go）：无 DOM 与 Canvas 环境，无法生成。
 */

/**
 * 32位 MurmurHash3 快速散列算法 (纯数字计算，无外部依赖，耗时 < 1ms)
 */
export function murmurhash3(key: string, seed = 0): string {
  let h1 = seed >>> 0
  const remainder = key.length & 3
  const bytes = key.length - remainder
  const c1 = 0xcc9e2d51
  const c2 = 0x1b873593

  let i = 0
  while (i < bytes) {
    let k1 =
      (key.charCodeAt(i) & 0xff) |
      ((key.charCodeAt(++i) & 0xff) << 8) |
      ((key.charCodeAt(++i) & 0xff) << 16) |
      ((key.charCodeAt(++i) & 0xff) << 24)
    ++i

    k1 = Math.imul(k1, c1)
    k1 = (k1 << 15) | (k1 >>> 17)
    k1 = Math.imul(k1, c2)

    h1 ^= k1
    h1 = (h1 << 13) | (h1 >>> 19)
    h1 = Math.imul(h1, 5) + 0xe6546b64
  }

  let k1 = 0
  if (remainder === 3) {
    k1 ^= (key.charCodeAt(i + 2) & 0xff) << 16
  }
  if (remainder >= 2) {
    k1 ^= (key.charCodeAt(i + 1) & 0xff) << 8
  }
  if (remainder >= 1) {
    k1 ^= key.charCodeAt(i) & 0xff
    k1 = Math.imul(k1, c1)
    k1 = (k1 << 15) | (k1 >>> 17)
    k1 = Math.imul(k1, c2)
    h1 ^= k1
  }

  h1 ^= key.length
  h1 ^= h1 >>> 16
  h1 = Math.imul(h1, 0x85ebca6b)
  h1 ^= h1 >>> 13
  h1 = Math.imul(h1, 0xc2b2ae35)
  h1 ^= h1 >>> 16

  return (h1 >>> 0).toString(16).padStart(8, '0')
}

/**
 * 离屏渲染微特征并返回 Canvas 散列 (执行耗时约 1-2ms)
 */
export function getCanvasFingerprint(): string {
  if (typeof document === 'undefined' || typeof document.createElement !== 'function') {
    return 'no-dom'
  }

  try {
    const canvas = document.createElement('canvas')
    canvas.width = 240
    canvas.height = 60
    const ctx = canvas.getContext('2d')

    if (!ctx) {
      return 'unsupported'
    }

    // 1. 绘制背景线性与径向多层渐变
    const linearGrad = ctx.createLinearGradient(0, 0, 240, 60)
    linearGrad.addColorStop(0, '#f8f9fa')
    linearGrad.addColorStop(0.5, '#e9ecef')
    linearGrad.addColorStop(1, '#dee2e6')
    ctx.fillStyle = linearGrad
    ctx.fillRect(0, 0, 240, 60)

    // 2. 绘制复杂贝塞尔曲线与半透明重叠圆 (测试 GPU 次像素抗锯齿与合成模式)
    ctx.globalCompositeOperation = 'multiply'
    ctx.fillStyle = 'rgba(255, 99, 71, 0.65)'
    ctx.beginPath()
    ctx.arc(45, 30, 22, 0, Math.PI * 2, true)
    ctx.closePath()
    ctx.fill()

    ctx.fillStyle = 'rgba(65, 105, 225, 0.65)'
    ctx.beginPath()
    ctx.arc(65, 30, 22, 0, Math.PI * 2, true)
    ctx.closePath()
    ctx.fill()

    // 3. 绘制带有特殊 Unicode、数学符号、标点与多字重回退的文本 (测试字体排版与渲染微差)
    ctx.globalCompositeOperation = 'source-over'
    ctx.shadowColor = 'rgba(0, 0, 0, 0.35)'
    ctx.shadowBlur = 3
    ctx.shadowOffsetX = 1
    ctx.shadowOffsetY = 1

    ctx.font = '14px -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", "PingFang SC", "Microsoft YaHei", sans-serif'
    ctx.fillStyle = '#1a1a1a'
    ctx.fillText('Volur 🛡️ <canvas> πΩ~', 95, 28)

    ctx.font = '11px "SF Pro Text", "Segoe UI", sans-serif'
    ctx.fillStyle = '#444444'
    ctx.fillText('Secure-Fingerprint-2026', 95, 46)

    // 4. 导出 DataURL 并生成稳定短摘要
    const dataUrl = canvas.toDataURL('image/png')
    return murmurhash3(dataUrl)
  } catch {
    return 'fallback-err'
  }
}
