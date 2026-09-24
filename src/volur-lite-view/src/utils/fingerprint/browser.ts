/**
 * 客户端浏览器家族与自动化运行环境识别探针
 *
 * 核心目标：
 * 1. 准确识别主流用户浏览器家族（Safari, Edge, Firefox, Chrome），防止将其误判为异常；
 * 2. 嗅探自动化框架（Puppeteer, Selenium WebDriver, Playwright）注入的标志位。
 */

export type BrowserFamily = 'CHROME' | 'EDGE' | 'SAFARI' | 'FIREFOX' | 'OTHER'

export interface BrowserEnvironmentInfo {
  browserFamily: BrowserFamily
  isWebDriver: boolean
  hasHeadlessUA: boolean
  isSuspiciousAutomation: boolean
  userAgent: string
}

export function detectBrowserEnvironment(): BrowserEnvironmentInfo {
  if (typeof navigator === 'undefined') {
    return {
      browserFamily: 'OTHER',
      isWebDriver: false,
      hasHeadlessUA: false,
      isSuspiciousAutomation: true,
      userAgent: 'unknown'
    }
  }

  const ua = navigator.userAgent || ''

  // 1. 浏览器家族精准判别 (注意匹配优先级：Edge > Chrome，Safari 需排除 Chrome)
  let browserFamily: BrowserFamily = 'OTHER'

  if (/edg\//i.test(ua)) {
    browserFamily = 'EDGE'
  } else if (/firefox|fxios/i.test(ua)) {
    browserFamily = 'FIREFOX'
  } else if (/safari/i.test(ua) && !/chrome|crios|android/i.test(ua)) {
    browserFamily = 'SAFARI'
  } else if (/chrome|chromium|crios/i.test(ua)) {
    browserFamily = 'CHROME'
  } else {
    browserFamily = 'OTHER'
  }

  // 2. 自动化框架与无头浏览器嗅探
  const isWebDriver = Boolean(navigator.webdriver)
  const hasHeadlessUA = /headlesschrome/i.test(ua)

  // 深度环境特征探针：检测是否有自动化工具特有变量
  const windowObj = typeof window !== 'undefined' ? (window as unknown as Record<string, unknown>) : {}
  const hasPuppeteerFlags = Boolean(
    windowObj.__puppeteer_evaluation_script__ ||
    windowObj.__webdriver_evaluate ||
    windowObj.__selenium_evaluate ||
    windowObj._phantom ||
    windowObj.callPhantom
  )

  const isSuspiciousAutomation = isWebDriver || hasHeadlessUA || hasPuppeteerFlags

  return {
    browserFamily,
    isWebDriver,
    hasHeadlessUA,
    isSuspiciousAutomation,
    userAgent: ua
  }
}
