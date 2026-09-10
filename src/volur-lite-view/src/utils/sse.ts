import { getAccessToken } from './auth'
import { safeJsonParse } from './request'

export type SseEventHandler<T = any> = (data: T) => void

export class SseClient {
  private eventSource: EventSource | null = null
  private url: string
  private listeners: Map<string, Set<SseEventHandler>> = new Map()

  constructor(url: string) {
    this.url = url
  }

  public connect(): void {
    if (this.eventSource) {
      this.close()
    }

    const token = getAccessToken()
    const separator = this.url.includes('?') ? '&' : '?'
    const fullUrl = token ? `${this.url}${separator}token=${encodeURIComponent(token)}` : this.url

    this.eventSource = new EventSource(fullUrl)

    this.eventSource.onopen = () => {
      // connected
    }

    this.eventSource.onerror = (err) => {
      console.error('[SSE] Connection error:', err)
    }

    this.listeners.forEach((handlers, eventName) => {
      this.eventSource?.addEventListener(eventName, (e: MessageEvent) => {
        try {
          const parsed = safeJsonParse(e.data)
          handlers.forEach((h) => h(parsed))
        } catch {
          handlers.forEach((h) => h(e.data))
        }
      })
    })
  }

  public on<T = any>(event: string, handler: SseEventHandler<T>): void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set())
    }
    this.listeners.get(event)!.add(handler)

    if (this.eventSource) {
      this.eventSource.addEventListener(event, (e: MessageEvent) => {
        try {
          const parsed = safeJsonParse(e.data)
          handler(parsed)
        } catch {
          handler(e.data)
        }
      })
    }
  }

  public off(event: string, handler: SseEventHandler): void {
    if (this.listeners.has(event)) {
      this.listeners.get(event)!.delete(handler)
    }
  }

  public close(): void {
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }
  }
}
