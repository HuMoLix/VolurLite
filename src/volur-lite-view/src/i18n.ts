import { createI18n } from 'vue-i18n'

function loadLocaleMessages() {
  const locales = import.meta.glob('./locales/**/*.json', { eager: true })
  const messages: Record<string, any> = {}

  for (const path in locales) {
    const matched = path.match(/\/locales\/(.+)\.json$/i)
    if (matched && matched[1]) {
      const parts = matched[1].split('/')
      const content = (locales[path] as any).default
      const locale = parts[0]

      if (!messages[locale]) {
        messages[locale] = {}
      }

      if (parts.length === 1) {
        messages[locale] = { ...messages[locale], ...content }
      } else {
        let current = messages[locale]
        for (let i = 1; i < parts.length - 1; i++) {
          const dir = parts[i]
          if (!current[dir] || typeof current[dir] !== 'object') {
            current[dir] = {}
          }
          current = current[dir]
        }
        const name = parts[parts.length - 1]
        current[name] = content
      }
    }
  }
  return messages
}

const messages = loadLocaleMessages()

const savedLocale = localStorage.getItem('locale')
const defaultLocale = savedLocale === 'zh' ? 'zh_CN' : (savedLocale === 'en' ? 'en_US' : (savedLocale || 'zh_CN'))

const i18n = createI18n({
  legacy: false,
  locale: defaultLocale,
  fallbackLocale: 'zh_CN',
  messages
})

export default i18n
