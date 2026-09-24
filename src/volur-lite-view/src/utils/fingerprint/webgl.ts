/**
 * WebGL 硬件与显卡渲染器特征探针
 *
 * 跨浏览器合法特征白名单与容错：
 * - Safari (macOS / iOS)：
 *   在 Safari 17+ / macOS Sonoma 的“高级防跟踪与指纹保护”下，WEBGL_debug_renderer_info 会被规范化为
 *   vendor="Apple", renderer="Apple GPU"（合法正常用户）；早期版本返回如 "Apple M1", "Apple M2 Pro" 等。
 *   必须视作 100% 合法真机！
 * - Edge (Windows / macOS)：
 *   Windows 上通常返回 "Google Inc. (NVIDIA ...)" 或 "ANGLE (Microsoft ... Direct3D11 ...)"。
 *   必须视作 100% 合法真机！
 * - Firefox (Gecko)：
 *   通常返回 "Mozilla" 或真实 GPU 标识。在隐私保护开启时返回泛化描述。
 *   必须视作 100% 合法真机！
 * - Chrome (Blink)：
 *   返回系统对应真实显卡型号（NVIDIA, Intel, AMD, Apple）。
 *
 * 恶意脚本 / 云端无头自动化特征识别：
 * - SwiftShader (Chromium CPU 软解，无头爬虫 / Docker 容器中最常见)
 * - llvmpipe (Linux Mesa CPU 软解，云服务器自动化脚本)
 * - softpipe / VirtualBox / VMware / Microsoft Basic Render Driver
 */

export interface WebGLFingerprint {
  vendor: string
  renderer: string
  isHeadlessRenderer: boolean
}

const SUSPICIOUS_RENDERER_PATTERNS = [
  /swiftshader/i,
  /llvmpipe/i,
  /softpipe/i,
  /virtualbox/i,
  /vmware/i,
  /mesa offscreen/i,
  /microsoft basic render driver/i
]

export function getWebGLFingerprint(): WebGLFingerprint {
  if (typeof document === 'undefined' || typeof document.createElement !== 'function') {
    return {
      vendor: 'no-dom',
      renderer: 'no-dom',
      isHeadlessRenderer: true
    }
  }

  try {
    const canvas = document.createElement('canvas')
    const gl = (canvas.getContext('webgl2') ||
      canvas.getContext('webgl') ||
      canvas.getContext('experimental-webgl')) as WebGLRenderingContext | null

    if (!gl) {
      return {
        vendor: 'unsupported',
        renderer: 'unsupported',
        isHeadlessRenderer: false
      }
    }

    const debugInfo = gl.getExtension('WEBGL_debug_renderer_info')
    let vendor = 'generic'
    let renderer = 'generic'

    if (debugInfo) {
      vendor = gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL) || 'generic'
      renderer = gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL) || 'generic'
    } else {
      vendor = gl.getParameter(gl.VENDOR) || 'generic'
      renderer = gl.getParameter(gl.RENDERER) || 'generic'
    }

    const isHeadlessRenderer = SUSPICIOUS_RENDERER_PATTERNS.some((pattern) =>
      pattern.test(renderer)
    )

    return {
      vendor,
      renderer,
      isHeadlessRenderer
    }
  } catch {
    return {
      vendor: 'error',
      renderer: 'error',
      isHeadlessRenderer: false
    }
  }
}
