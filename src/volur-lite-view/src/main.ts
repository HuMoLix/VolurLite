import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './store'
import i18n, { loadLocaleMessagesAsync, defaultLocale } from './i18n'

// Import vfonts
import 'vfonts/Lato.css'
import 'vfonts/FiraCode.css'

// Import global SCSS styles
import './styles/index.scss'

async function bootstrap() {
  try {
    // 首屏仅异步预加载当前默认语言包（如 zh_CN），阻断未使用语言全量拉取
    await loadLocaleMessagesAsync(defaultLocale)
  } catch (err) {
    console.error('[i18n] Failed to preload default locale messages', err)
  }

  const app = createApp(App)
  app.use(pinia)
  app.use(router)
  app.use(i18n)
  app.mount('#app')
}

bootstrap()
