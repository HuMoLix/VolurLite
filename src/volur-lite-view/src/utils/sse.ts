import { getAccessToken } from './auth'
import { safeJsonParse } from './request'

export type SseEventHandler<T = any> = (data: T) => void

/**
 * 全局通用 Server-Sent Events (SSE) 长连接客户端
 * 支持全局单例复用、防重复连接、多组件事件监听共享与窗口卸载安全清理
 */
export class SseClient {
  private static instances: Map<string, SseClient> = new Map()

  private eventSource: EventSource | null = null
  private url: string
  private listeners: Map<string, Set<SseEventHandler>> = new Map()
  private isConnecting: boolean = false
  private beforeUnloadHandler: (() => void) | null = null

  constructor(url: string) {
    this.url = url
    this.setupBeforeUnload()
  }

  /**
   * 获取或创建指定 URL 的全局共享单例客户端
   * 全平台默认建议复用同一单例，彻底杜绝多组件并发建立多条冗余长连接
   */
  public static getInstance(url: string = '/api/v1/realtime/stream'): SseClient {
    if (!SseClient.instances.has(url)) {
      SseClient.instances.set(url, new SseClient(url))
    }
    return SseClient.instances.get(url)!
  }

  /**
   * 建立长连接（具有防重入与状态感知机制）
   */
  public connect(): void {
    // 若已处于已连接或正在连接状态，直接复用当前连接，严禁重复建连
    if (this.eventSource && (this.eventSource.readyState === EventSource.OPEN || this.eventSource.readyState === EventSource.CONNECTING)) {
      return
    }

    if (this.isConnecting) {
      return
    }

    this.isConnecting = true

    // 若存在已关闭的旧连接对象，先彻底清理
    if (this.eventSource) {
      this.close()
    }

    const token = getAccessToken()
    const separator = this.url.includes('?') ? '&' : '?'
    const fullUrl = token ? `${this.url}${separator}token=${encodeURIComponent(token)}` : this.url

    try {
      this.eventSource = new EventSource(fullUrl)

      this.eventSource.onopen = () => {
        this.isConnecting = false
      }

      this.eventSource.onerror = (err) => {
        this.isConnecting = false
        console.warn('[SSE] Connection error/disconnected:', err)
      }

      // 将现有的监听器批量挂载至新连接
      this.listeners.forEach((handlers, eventName) => {
        this.eventSource?.addEventListener(eventName, (e: MessageEvent) => {
          this.dispatchToHandlers(handlers, e.data)
        })
      })
    } catch (e) {
      this.isConnecting = false
      console.error('[SSE] Failed to construct EventSource:', e)
    }
  }

  /**
   * 注册指定事件类型的监听器
   * 返回一个注销函数，便于组件在 onUnmounted 中一行代码解绑
   */
  public on<T = any>(event: string, handler: SseEventHandler<T>): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set())

      // 若当前连接已建立，即时注册到底层 EventSource
      if (this.eventSource) {
        this.eventSource.addEventListener(event, (e: MessageEvent) => {
          const handlers = this.listeners.get(event)
          if (handlers) {
            this.dispatchToHandlers(handlers, e.data)
          }
        })
      }
    }

    const set = this.listeners.get(event)!
    set.add(handler)

    return () => this.off(event, handler)
  }

  /**
   * 注销指定事件的某个监听器
   */
  public off(event: string, handler: SseEventHandler): void {
    const handlers = this.listeners.get(event)
    if (handlers) {
      handlers.delete(handler)
      if (handlers.size === 0) {
        this.listeners.delete(event)
      }
    }
  }

  /**
   * 安全关闭长连接并释放底层网络资源
   */
  public close(): void {
    this.isConnecting = false
    if (this.eventSource) {
      try {
        this.eventSource.close()
      } catch (e) {
        console.warn('[SSE] Error closing EventSource:', e)
      }
      this.eventSource = null
    }
  }

  /**
   * 重新连接（如 Token 更新或网络恢复时调用）
   */
  public reconnect(): void {
    this.close()
    this.connect()
  }

  /**
   * 获取当前连接状态
   */
  public get readyState(): number {
    return this.eventSource ? this.eventSource.readyState : EventSource.CLOSED
  }

  /**
   * 派发数据至已注册的监听器回调
   */
  private dispatchToHandlers(handlers: Set<SseEventHandler>, rawData: string) {
    let parsed = rawData
    try {
      parsed = safeJsonParse(rawData)
    } catch {
      // 保持原始数据
    }
    handlers.forEach((h) => {
      try {
        h(parsed)
      } catch (err) {
        console.error('[SSE] Error in event listener callback:', err)
      }
    })
  }

  /**
   * 绑定页面卸载钩子，确保浏览器刷新/关闭时强制通知释放套接字
   */
  private setupBeforeUnload(): void {
    if (typeof window !== 'undefined' && !this.beforeUnloadHandler) {
      this.beforeUnloadHandler = () => {
        this.close()
      }
      window.addEventListener('beforeunload', this.beforeUnloadHandler)
    }
  }
}

/**
 * 获取全站全局默认 SSE 长连接单例
 */
export const getGlobalSseClient = (url: string = '/api/v1/realtime/stream'): SseClient => {
  return SseClient.getInstance(url)
}

