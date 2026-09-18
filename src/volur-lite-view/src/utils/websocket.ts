/**
 * 高性能通用 WebSocket 基础客户端
 * 特性：
 * - 完备的连接状态机管理 (DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, CLOSING)
 * - 智能指数退避重连机制 (Exponential Backoff + Random Jitter)
 * - 双向心跳探活与静默断流检测 (Ping/Pong Watchdog)
 * - 离线发信队列与重连自动回放 (Offline Message Queue)
 * - 强类型事件总线 (Typed EventEmitter)
 * - 浏览器生命周期自动垃圾回收 (beforeunload hook)
 */

export type WebSocketStatus = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'RECONNECTING' | 'CLOSING'

export interface WebSocketClientOptions {
  /** WebSocket 服务端完整地址或动态获取函数 */
  url: string | (() => string | Promise<string>)
  /** 是否在初始化后立即连接，默认 false */
  autoConnect?: boolean
  /** 心跳探测间隔时间 (毫秒)，默认 25000ms */
  pingInterval?: number
  /** 等待心跳响应超时时间 (毫秒)，默认 10000ms */
  pongTimeout?: number
  /** 是否开启断线自动重连，默认 true */
  autoReconnect?: boolean
  /** 最大重连尝试次数，默认 Infinity */
  maxReconnectAttempts?: number
  /** 初始重连延迟 (毫秒)，默认 1000ms */
  baseReconnectDelay?: number
  /** 最大重连延迟 (毫秒)，默认 30000ms */
  maxReconnectDelay?: number
  /** 随机抖动因子 (毫秒)，默认 1000ms */
  jitterRange?: number
  /** 离线消息缓冲队列上限，默认 100 */
  maxOfflineQueueSize?: number
  /** 自定义 WebSocket 子协议 */
  protocols?: string | string[]
}

export type WebSocketEventMap = {
  open: [event: Event]
  message: [data: string, event: MessageEvent]
  close: [event: CloseEvent]
  error: [event: Event]
  statusChange: [status: WebSocketStatus, prevStatus: WebSocketStatus]
  reconnect: [attempt: number, delayMs: number]
}

type EventCallback<T extends keyof WebSocketEventMap> = (...args: WebSocketEventMap[T]) => void

export class WebSocketClient {
  protected ws: WebSocket | null = null
  protected status: WebSocketStatus = 'DISCONNECTED'
  protected options: Required<WebSocketClientOptions>

  // 心跳相关定时器与状态
  private pingTimer: ReturnType<typeof setTimeout> | null = null
  private pongTimeoutTimer: ReturnType<typeof setTimeout> | null = null

  // 重连相关状态
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private reconnectAttempts = 0
  private manualClosed = false

  // 离线发信缓冲队列
  private offlineQueue: string[] = []

  // 事件监听器集合
  private listeners: { [K in keyof WebSocketEventMap]?: Set<EventCallback<K>> } = {}

  // 浏览器卸载事件解绑句柄
  private beforeUnloadHandler: (() => void) | null = null

  constructor(options: WebSocketClientOptions) {
    this.options = {
      url: options.url,
      autoConnect: options.autoConnect ?? false,
      pingInterval: options.pingInterval ?? 25000,
      pongTimeout: options.pongTimeout ?? 10000,
      autoReconnect: options.autoReconnect ?? true,
      maxReconnectAttempts: options.maxReconnectAttempts ?? Infinity,
      baseReconnectDelay: options.baseReconnectDelay ?? 1000,
      maxReconnectDelay: options.maxReconnectDelay ?? 30000,
      jitterRange: options.jitterRange ?? 1000,
      maxOfflineQueueSize: options.maxOfflineQueueSize ?? 100,
      protocols: options.protocols ?? []
    }

    if (typeof window !== 'undefined') {
      this.beforeUnloadHandler = () => this.destroy()
      window.addEventListener('beforeunload', this.beforeUnloadHandler)
    }

    if (this.options.autoConnect) {
      this.connect()
    }
  }

  /**
   * 获取当前连接状态
   */
  public getStatus(): WebSocketStatus {
    return this.status
  }

  /**
   * 是否处于已就绪连通状态
   */
  public isConnected(): boolean {
    return this.status === 'CONNECTED' && this.ws?.readyState === WebSocket.OPEN
  }

  /**
   * 建立 WebSocket 连接
   */
  public async connect(): Promise<void> {
    if (this.status === 'CONNECTED' || this.status === 'CONNECTING') {
      return
    }

    this.manualClosed = false
    this.setStatus('CONNECTING')

    try {
      const targetUrl = typeof this.options.url === 'function'
        ? await this.options.url()
        : this.options.url

      if (!targetUrl) {
        throw new Error('[WebSocketClient] 未能解析到有效的目标 WebSocket 地址')
      }

      this.cleanupSocket()
      this.ws = new WebSocket(targetUrl, this.options.protocols)

      this.ws.onopen = (event: Event) => this.handleOpen(event)
      this.ws.onmessage = (event: MessageEvent) => this.handleMessage(event)
      this.ws.onerror = (event: Event) => this.handleError(event)
      this.ws.onclose = (event: CloseEvent) => this.handleClose(event)

    } catch (err) {
      this.handleError(err as Event)
      this.triggerReconnect()
    }
  }

  /**
   * 发送原始字符串消息
   * 若当前处于断线状态，自动放入离线消息队列
   */
  public send(data: string): boolean {
    if (this.isConnected() && this.ws) {
      try {
        this.ws.send(data)
        return true
      } catch (err) {
        console.warn('[WebSocketClient] 消息实时发送异常，压入离线重发队列', err)
      }
    }

    if (this.offlineQueue.length >= this.options.maxOfflineQueueSize) {
      this.offlineQueue.shift() // 超出缓冲区淘汰最老的消息
    }
    this.offlineQueue.push(data)
    return false
  }

  /**
   * 主动安全断开连接
   */
  public disconnect(code: number = 1000, reason: string = 'Normal Closure'): void {
    this.manualClosed = true
    this.stopHeartbeat()
    this.clearReconnectTimer()

    if (this.ws) {
      this.setStatus('CLOSING')
      try {
        this.ws.close(code, reason)
      } catch {
        // 忽略静默关闭异常
      }
    }
    this.setStatus('DISCONNECTED')
  }

  /**
   * 彻底销毁客户端并释放全局监听
   */
  public destroy(): void {
    this.disconnect(1000, 'Client Destroyed')
    this.cleanupSocket()
    this.offlineQueue = []
    this.listeners = {}

    if (typeof window !== 'undefined' && this.beforeUnloadHandler) {
      window.removeEventListener('beforeunload', this.beforeUnloadHandler)
      this.beforeUnloadHandler = null
    }
  }

  // ==========================================
  // 事件系统 (Typed Event Emitter)
  // ==========================================

  public on<K extends keyof WebSocketEventMap>(event: K, handler: EventCallback<K>): () => void {
    if (!this.listeners[event]) {
      this.listeners[event] = new Set() as any
    }
    this.listeners[event]?.add(handler as any)

    return () => this.off(event, handler)
  }

  public off<K extends keyof WebSocketEventMap>(event: K, handler: EventCallback<K>): void {
    this.listeners[event]?.delete(handler as any)
  }

  public once<K extends keyof WebSocketEventMap>(event: K, handler: EventCallback<K>): void {
    const onceWrapper: EventCallback<K> = (...args: WebSocketEventMap[K]) => {
      this.off(event, onceWrapper)
      handler(...args)
    }
    this.on(event, onceWrapper)
  }

  protected emit<K extends keyof WebSocketEventMap>(event: K, ...args: WebSocketEventMap[K]): void {
    const handlers = this.listeners[event]
    if (handlers && handlers.size > 0) {
      handlers.forEach((fn) => {
        try {
          (fn as any)(...args)
        } catch (e) {
          console.error(`[WebSocketClient] 事件处理异常 [${event}]:`, e)
        }
      })
    }
  }

  // ==========================================
  // 内部连接与生命周期处理
  // ==========================================

  private setStatus(newStatus: WebSocketStatus): void {
    if (this.status !== newStatus) {
      const prev = this.status
      this.status = newStatus
      this.emit('statusChange', newStatus, prev)
    }
  }

  private handleOpen(event: Event): void {
    this.setStatus('CONNECTED')
    this.reconnectAttempts = 0
    this.startHeartbeat()
    this.flushOfflineQueue()
    this.emit('open', event)
  }

  private handleMessage(event: MessageEvent): void {
    // 收到任意数据帧均证明通道存活，重置心跳探活判定
    this.resetPongWatchdog()

    const rawData = String(event.data || '')
    if (this.isPongMessage(rawData)) {
      return
    }

    this.emit('message', rawData, event)
  }

  private handleError(event: Event): void {
    this.emit('error', event)
  }

  private handleClose(event: CloseEvent): void {
    this.stopHeartbeat()
    this.cleanupSocket()

    if (this.manualClosed) {
      this.setStatus('DISCONNECTED')
    } else {
      this.setStatus('DISCONNECTED')
      this.triggerReconnect()
    }

    this.emit('close', event)
  }

  /**
   * 触发指数退避带抖动重连
   */
  private triggerReconnect(): void {
    if (this.manualClosed || !this.options.autoReconnect) {
      return
    }

    if (this.reconnectAttempts >= this.options.maxReconnectAttempts) {
      console.warn(`[WebSocketClient] 已达到最大重连次数 (${this.options.maxReconnectAttempts})，终止自动重连`)
      return
    }

    this.clearReconnectTimer()
    this.setStatus('RECONNECTING')

    this.reconnectAttempts++
    // 指数递增计算: min(maxDelay, baseDelay * 1.5^n) + 随机抖动
    const factor = Math.pow(1.5, Math.min(this.reconnectAttempts - 1, 10))
    const rawDelay = this.options.baseReconnectDelay * factor
    const cappedDelay = Math.min(rawDelay, this.options.maxReconnectDelay)
    const jitter = Math.random() * this.options.jitterRange
    const delay = Math.round(cappedDelay + jitter)

    this.emit('reconnect', this.reconnectAttempts, delay)

    this.reconnectTimer = setTimeout(() => {
      this.connect()
    }, delay)
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  // ==========================================
  // 心跳机制与静默断网探测
  // ==========================================

  private startHeartbeat(): void {
    this.stopHeartbeat()
    this.pingTimer = setInterval(() => {
      if (this.isConnected()) {
        this.sendPing()
        // 发送 Ping 后等待对应 Pong 响应，若超时则主动判定连接已死
        this.pongTimeoutTimer = setTimeout(() => {
          console.warn('[WebSocketClient] 心跳超时未应答，判定死连接，强制触发重连')
          this.ws?.close(4000, 'Heartbeat Timeout')
        }, this.options.pongTimeout)
      }
    }, this.options.pingInterval)
  }

  private stopHeartbeat(): void {
    if (this.pingTimer) {
      clearInterval(this.pingTimer)
      this.pingTimer = null
    }
    if (this.pongTimeoutTimer) {
      clearTimeout(this.pongTimeoutTimer)
      this.pongTimeoutTimer = null
    }
  }

  private resetPongWatchdog(): void {
    if (this.pongTimeoutTimer) {
      clearTimeout(this.pongTimeoutTimer)
      this.pongTimeoutTimer = null
    }
  }

  /**
   * 发送心跳 Ping，子类可覆盖自定义格式
   */
  protected sendPing(): void {
    this.send(JSON.stringify({ type: 'PING' }))
  }

  /**
   * 判定收到的是否为心跳 Pong，子类可覆盖自定义判定
   */
  protected isPongMessage(raw: string): boolean {
    try {
      const data = JSON.parse(raw)
      return data && data.type === 'PONG'
    } catch {
      return false
    }
  }

  /**
   * 刷新并重发断线期间缓存的离线消息
   */
  private flushOfflineQueue(): void {
    if (this.offlineQueue.length === 0 || !this.isConnected()) {
      return
    }

    const queue = [...this.offlineQueue]
    this.offlineQueue = []
    for (const msg of queue) {
      this.send(msg)
    }
  }

  private cleanupSocket(): void {
    if (this.ws) {
      this.ws.onopen = null
      this.ws.onmessage = null
      this.ws.onerror = null
      this.ws.onclose = null
      this.ws = null
    }
  }
}
