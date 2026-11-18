import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import { publicResourceMappingPlugin } from './plugins/publicResourceMapping'

// 本地联调可用环境变量覆盖（默认行为不变）：
//   VITE_DEV_PROXY_TARGET    API/WS 代理目标后端，默认 http://localhost:8080
//   VITE_DEV_HMR_CLIENT_PORT HMR 客户端端口，默认 443（走反代 HTTPS 的场景）
const proxyTarget = process.env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080'
const hmrClientPort = Number(process.env.VITE_DEV_HMR_CLIENT_PORT || 443)

export default defineConfig({
  plugins: [
    vue({
      template: {
        transformAssetUrls: {
          includeAbsolute: false
        }
      }
    }),
    publicResourceMappingPlugin()
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3001,
    host: '127.0.0.1',
    allowedHosts: [
      'test-platform.volurctf.com'
    ],
    hmr: {
      clientPort: hmrClientPort
    },
    open: false,
    cors: true,
    proxy: {
      '/api': {
        target: proxyTarget,
        changeOrigin: true,
        xfwd: true
      },
      '/ws': {
        target: proxyTarget,
        ws: true,
        changeOrigin: true
      }
    }
  },
  build: {
    target: 'esnext',
    modulePreload: {
      polyfill: false
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern-compiler',
        additionalData: `@use "@/styles/variables.scss" as *;\n@use "@/styles/mixins.scss" as *;\n`
      }
    }
  },
  esbuild: {
    target: 'esnext'
  }
})
