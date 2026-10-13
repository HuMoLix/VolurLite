export type SseEventHandler<T = any> = (data: T) => void

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

  public static getInstance(url: string = '/api/v1/realtime/stream'): SseClient {
    if (!SseClient.instances.has(url)) {
      SseClient.instances.set(url, new SseClient(url))
    }
    return SseClient.instances.get(url)!
  }

  public connect(): void {
    if (this.eventSource && (this.eventSource.readyState === EventSource.OPEN || this.eventSource.readyState === EventSource.CONNECTING)) {
      return
    }

    if (this.isConnecting) {
      return
    }

    this.isConnecting = true

    if (this.eventSource) {
      this.close()
    }

    try {
      this.eventSource = new EventSource(this.url, { withCredentials: true })

      this.eventSource.onopen = () => {
        this.isConnecting = false
      }

      this.eventSource.onerror = (err) => {
        this.isConnecting = false
        console.warn('[SSE] Connection error/disconnected:', err)
      }

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

  public on<T = any>(event: string, handler: SseEventHandler<T>): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set())

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

  public off(event: string, handler: SseEventHandler): void {
    const handlers = this.listeners.get(event)
    if (handlers) {
      handlers.delete(handler)
      if (handlers.size === 0) {
        this.listeners.delete(event)
      }
    }
  }

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

  public reconnect(): void {
    this.close()
    this.connect()
  }

  public get readyState(): number {
    return this.eventSource ? this.eventSource.readyState : EventSource.CLOSED
  }

  private dispatchToHandlers(handlers: Set<SseEventHandler>, rawData: string) {
    let parsed: any = rawData
    try {
      parsed = typeof rawData === 'string' ? JSON.parse(rawData) : rawData
    } catch {
      parsed = rawData
    }
    handlers.forEach((h) => {
      try {
        h(parsed)
      } catch (err) {
        console.error('[SSE] Error in event listener callback:', err)
      }
    })
  }

  private setupBeforeUnload(): void {
    if (typeof window !== 'undefined' && !this.beforeUnloadHandler) {
      this.beforeUnloadHandler = () => {
        this.close()
      }
      window.addEventListener('beforeunload', this.beforeUnloadHandler)
    }
  }
}

export const getGlobalSseClient = (url: string = '/api/v1/realtime/stream'): SseClient => {
  return SseClient.getInstance(url)
}
