import { WebSocketClient, type WebSocketClientOptions, type WebSocketStatus } from './websocket'
import type {
  ChatMessageVO,
  ChatMessageDTO,
  WebSocketReadyData
} from '@/types/competition/chat'

export interface ChatWebSocketClientOptions extends Partial<Omit<WebSocketClientOptions, 'url'>> {
  competitionId: number | string
  getToken: () => string | null | undefined
}

export type ChatFrameType = 'CONNECTED' | 'MESSAGE' | 'SEND_ACK' | 'ERROR' | 'PONG'

export interface ChatWebSocketEventMap {
  chatMessage: [message: ChatMessageVO]
  connectedReady: [data: WebSocketReadyData]
  sendAck: [clientMsgId: string, messageId: number | string]
  chatError: [errorMessage: string]
  statusChange: [status: WebSocketStatus, prevStatus: WebSocketStatus]
}

type ChatEventCallback<K extends keyof ChatWebSocketEventMap> = (...args: ChatWebSocketEventMap[K]) => void

/**
 * 竞赛聊天专属 WebSocket 客户端
 * 继承自通用 WebSocketClient，微调协议解析与双向沟通逻辑
 */
export class ChatWebSocketClient extends WebSocketClient {
  private competitionId: number | string
  private getToken: () => string | null | undefined
  private readyData: WebSocketReadyData | null = null

  private chatListeners: { [K in keyof ChatWebSocketEventMap]?: Set<ChatEventCallback<K>> } = {}

  constructor(options: ChatWebSocketClientOptions) {
    const { competitionId, getToken, ...restOptions } = options

    // 构建动态 WebSocket URL 解析函数
    const dynamicUrlResolver = () => {
      const token = getToken()
      if (!token) {
        throw new Error('[ChatWebSocketClient] 缺少登录凭证 Token，无法建立聊天通道')
      }

      const protocol = typeof window !== 'undefined' && window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const host = typeof window !== 'undefined' ? window.location.host : 'localhost:3000'
      return `${protocol}//${host}/ws/competitions/${competitionId}/chat?token=${encodeURIComponent(token)}`
    }

    super({
      url: dynamicUrlResolver,
      pingInterval: 20000,
      pongTimeout: 8000,
      autoReconnect: true,
      maxReconnectAttempts: 20,
      baseReconnectDelay: 1000,
      maxReconnectDelay: 15000,
      jitterRange: 800,
      ...restOptions
    })

    this.competitionId = competitionId
    this.getToken = getToken

    // 监听底层状态变化与消息，进行专属协议微调分发
    this.on('statusChange', (status, prevStatus) => {
      if (status !== 'CONNECTED') {
        this.readyData = null
      }
      this.emitChatEvent('statusChange', status, prevStatus)
    })

    this.on('message', (raw) => {
      this.handleChatFrame(raw)
    })
  }

  public getCurrentToken(): string | null | undefined {
    return this.getToken()
  }

  public getCompetitionId(): number | string {
    return this.competitionId
  }

  public getReadyData(): WebSocketReadyData | null {
    return this.readyData
  }

  /**
   * 发送聊天/工单申诉消息
   * @returns 本地分配的 clientMsgId 用于乐观更新与 ACK 匹配
   */
  public sendChatMessage(dto: ChatMessageDTO): string {
    const clientMsgId = `cmsg_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`

    const frame = {
      type: 'SEND',
      clientMsgId,
      payload: {
        channelType: dto.channelType,
        content: dto.content,
        msgType: dto.msgType || 'TEXT',
        teamId: dto.teamId,
        receiverId: dto.receiverId,
        mentionUserIds: dto.mentionUserIds
      }
    }

    this.send(JSON.stringify(frame))
    return clientMsgId
  }

  /**
   * 发送已读状态同步 ACK
   */
  public sendReadAck(target: string, lastMessageId?: number | string): void {
    const frame = {
      type: 'READ_ACK',
      payload: {
        target,
        lastMessageId
      }
    }
    this.send(JSON.stringify(frame))
  }

  // ==========================================
  // 专属聊天事件总线
  // ==========================================

  public onChat<K extends keyof ChatWebSocketEventMap>(event: K, handler: ChatEventCallback<K>): () => void {
    if (!this.chatListeners[event]) {
      this.chatListeners[event] = new Set() as any
    }
    this.chatListeners[event]?.add(handler as any)

    return () => this.offChat(event, handler)
  }

  public offChat<K extends keyof ChatWebSocketEventMap>(event: K, handler: ChatEventCallback<K>): void {
    this.chatListeners[event]?.delete(handler as any)
  }

  private emitChatEvent<K extends keyof ChatWebSocketEventMap>(event: K, ...args: ChatWebSocketEventMap[K]): void {
    const handlers = this.chatListeners[event]
    if (handlers && handlers.size > 0) {
      handlers.forEach((fn) => {
        try {
          (fn as any)(...args)
        } catch (err) {
          console.error(`[ChatWebSocketClient] 聊天事件回调执行异常 [${event}]:`, err)
        }
      })
    }
  }

  /**
   * 解析微调服务端聊天协议帧
   */
  private handleChatFrame(raw: string): void {
    try {
      const frame = JSON.parse(raw)
      if (!frame || !frame.type) return

      switch (frame.type) {
        case 'CONNECTED':
          if (frame.data) {
            this.readyData = frame.data as WebSocketReadyData
            this.emitChatEvent('connectedReady', this.readyData)
          }
          break

        case 'MESSAGE':
          if (frame.data) {
            this.emitChatEvent('chatMessage', frame.data as ChatMessageVO)
          }
          break

        case 'SEND_ACK':
          this.emitChatEvent('sendAck', frame.clientMsgId || '', frame.messageId)
          break

        case 'ERROR':
          this.emitChatEvent('chatError', frame.message || '未知异常')
          break

        default:
          break
      }
    } catch (e) {
      console.warn('[ChatWebSocketClient] 解析服务端聊天数据帧异常:', e, raw)
    }
  }
}
