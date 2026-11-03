import type { Plugin } from 'vite'
import { fileURLToPath } from 'url'
import path from 'path'
import fs from 'fs'

export interface PublicResourceMappingOptions {
  rootDir?: string
}

export function publicResourceMappingPlugin(options?: PublicResourceMappingOptions): Plugin {
  const rootDir = options?.rootDir || fileURLToPath(new URL('..', import.meta.url))
  const staticDir = path.resolve(rootDir, 'public/static')
  const distDir = path.resolve(rootDir, 'dist')

  function isSafeSubPath(baseDir: string, relativePath: string): { safe: boolean; resolvedPath: string } {
    let decoded = relativePath
    try {
      let prev = ''
      while (decoded !== prev) {
        prev = decoded
        decoded = decodeURIComponent(decoded)
      }
    } catch {
      return { safe: false, resolvedPath: '' }
    }

    if (/[\0\r\n]/.test(decoded)) {
      return { safe: false, resolvedPath: '' }
    }

    const cleanPath = decoded.replace(/\\/g, '/')
    const segments = cleanPath.split('/')
    for (const seg of segments) {
      if (seg === '..' || seg === '.') {
        return { safe: false, resolvedPath: '' }
      }
    }

    const resolved = path.resolve(baseDir, cleanPath.replace(/^\/+/, ''))
    const baseNormalized = path.normalize(baseDir)
    const targetNormalized = path.normalize(resolved)
    const baseWithSep = baseNormalized.endsWith(path.sep) ? baseNormalized : baseNormalized + path.sep

    if (!targetNormalized.startsWith(baseWithSep) && targetNormalized !== baseNormalized) {
      return { safe: false, resolvedPath: '' }
    }

    return { safe: true, resolvedPath: targetNormalized }
  }

  function handleResourceRequest(req: any, res: any, next: any) {
    const rawUrl = req.url ? req.url.split('?')[0] : ''

    // 兼容浏览器原生访问根路径 /favicon.ico 与 /favicon.svg
    if (rawUrl === '/favicon.ico' || rawUrl === '/favicon.svg') {
      const favFile = path.resolve(staticDir, 'svg/favicon.svg')
      if (fs.existsSync(favFile)) {
        res.setHeader('Content-Type', 'image/svg+xml')
        fs.createReadStream(favFile).pipe(res)
        return
      }
    }

    if (!rawUrl.startsWith('/r/static/')) {
      return next()
    }

    const isScriptRequest =
      req.headers['sec-fetch-dest'] === 'script' ||
      (req.headers.accept && req.headers.accept.includes('application/javascript')) ||
      (req.url && req.url.includes('?import'))

    if (isScriptRequest) {
      res.setHeader('Content-Type', 'application/javascript; charset=utf-8')
      res.end(`export default ${JSON.stringify(rawUrl)};`)
      return
    }

    const subPath = rawUrl.slice('/r/static/'.length)
    const check = isSafeSubPath(staticDir, subPath)
    if (!check.safe) {
      res.statusCode = 403
      res.setHeader('Content-Type', 'text/plain; charset=utf-8')
      res.end('403 Forbidden: Path traversal attempt blocked')
      return
    }

    const targetFile = check.resolvedPath

    if (fs.existsSync(targetFile) && !fs.statSync(targetFile).isDirectory()) {
      const ext = path.extname(targetFile).toLowerCase()
      const mimeTypes: Record<string, string> = {
        '.svg': 'image/svg+xml',
        '.png': 'image/png',
        '.jpg': 'image/jpeg',
        '.jpeg': 'image/jpeg',
        '.gif': 'image/gif',
        '.ico': 'image/x-icon',
        '.webp': 'image/webp',
        '.json': 'application/json',
        '.css': 'text/css',
        '.js': 'application/javascript'
      }
      res.setHeader('Content-Type', mimeTypes[ext] || 'application/octet-stream')
      res.setHeader('Cache-Control', 'public, max-age=3600')
      fs.createReadStream(targetFile).pipe(res)
      return
    }

    next()
  }

  return {
    name: 'vite-plugin-public-resource-mapping',
    resolveId(id) {
      const cleanId = id.split('?')[0]
      if (cleanId.startsWith('/r/static/')) {
        return cleanId
      }
    },
    load(id) {
      const cleanId = id.split('?')[0]
      if (cleanId.startsWith('/r/static/')) {
        return `export default ${JSON.stringify(cleanId)};`
      }
    },
    configureServer(server) {
      server.middlewares.use(handleResourceRequest)
    },
    configurePreviewServer(server) {
      server.middlewares.use(handleResourceRequest)
    },
    closeBundle() {
      const distRStaticDir = path.resolve(distDir, 'r/static')
      if (fs.existsSync(distDir) && fs.existsSync(staticDir)) {
        fs.cpSync(staticDir, distRStaticDir, { recursive: true, force: true })
      }
    }
  }
}
