import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getAccessToken, getUserInfo, isSelfMemberIdentity } from '@/utils/auth'
import { ChatWebSocketClient } from '@/utils/chatWebSocket'
import type { WebSocketStatus } from '@/utils/websocket'
import type {
  ChatMessageVO,
  ChatChannelType,
  ChatMessageType,
  WebSocketReadyData,
  RefereeTeamSummaryVO,
  ChatUnreadCountVO
} from '@/types/competition/chat'
import {
  getChatHistoryApi,
  getRefereeTeamsApi,
  sendChatMessageApi,
  getChatUnreadCountApi,
  markChatReadApi
} from '@/api/competition/chat'
import { useCompetitionStore } from '@/store/modules/competition'
import { useUserStore } from '@/store/modules/user'

export interface TeammateItem {
  userId: number | string
  username: string
  role: string
  avatarUrl: string | null
}

export const useCompetitionChatStore = defineStore('competitionChat', () => {
  const competitionStore = useCompetitionStore()
  const userStore = useUserStore()

  const currentCompetitionId = ref<number | string | null>(null)
  const client = ref<ChatWebSocketClient | null>(null)
  const status = ref<WebSocketStatus>('DISCONNECTED')
  const readyData = ref<WebSocketReadyData | null>(null)

  /**
   * 当前沟通目标:
   * - 'TEAM': 全队公屏
   * - 'REFEREE': 裁判组
   * - 'USER_{userId}': 私信指定队员
   */
  const currentTarget = ref<string>('TEAM')

  // 消息分片存储
  const teamMessages = ref<ChatMessageVO[]>([])
  // 私信存储: key 为与当前登录用户对话的对方 userId
  const privateMessages = ref<Record<string, ChatMessageVO[]>>({})
  // 裁判工单存储: key 为 teamId
  const refereeMessages = ref<Record<string, ChatMessageVO[]>>({})
  const refereeTeams = ref<RefereeTeamSummaryVO[]>([])
  const selectedRefereeTeamId = ref<number | string | null>(null)

  // 记录已懒加载历史消息的目标集合，避免重复拉取
  const loadedHistoryTargets = ref<Set<string>>(new Set())

  // 未读统计
  const unreadTeamCount = ref<number>(0)
  const unreadPrivateCount = ref<number>(0)
  const unreadRefereeCount = ref<number>(0)
  const unreadPrivateDetails = ref<Record<string, number>>({})

  const isDrawerOpen = ref<boolean>(false)
  const loadingHistory = ref<boolean>(false)

  // 计算属性
  const isReferee = computed(() => !!readyData.value?.isReferee)

  const currentUserId = computed(() => {
    return readyData.value?.userId || userStore.user?.id || (userStore.user as any)?.userId || getUserInfo()?.id || (getUserInfo() as any)?.userId || null
  })

  // 同队其他队员列表（直接由 competitionStore.myTeam.members 响应式派生，严格使用多源凭据排除选手自身）
  const teammateList = computed<TeammateItem[]>(() => {
    const members = competitionStore.myTeam?.members || []
    return members
      .filter((m) => !isSelfMemberIdentity(m, userStore.user, readyData.value))
      .map((m) => ({
        userId: m.userId,
        username: m.username || m.realName || `用户${m.userId}`,
        role: m.role || 'MEMBER',
        avatarUrl: m.avatarUrl || null
      }))
  })

  // 当前激活目标所对应的消息列表
  const currentMessages = computed<ChatMessageVO[]>(() => {
    if (currentTarget.value === 'TEAM') {
      return teamMessages.value
    }
    if (currentTarget.value.startsWith('USER_')) {
      const targetUserId = currentTarget.value.replace('USER_', '')
      return privateMessages.value[targetUserId] || []
    }
    if (currentTarget.value === 'REFEREE') {
      const targetTeamId = isReferee.value
        ? selectedRefereeTeamId.value
        : (readyData.value?.teamId || competitionStore.myTeam?.teamId || 'my_team')

      if (!targetTeamId) return []
      return refereeMessages.value[String(targetTeamId)] || []
    }
    return []
  })

  const totalUnreadCount = computed(
    () => unreadTeamCount.value + unreadPrivateCount.value + unreadRefereeCount.value
  )

  /**
   * 应用未读统计数据
   */
  function applyUnreadCounts(data: ChatUnreadCountVO) {
    unreadTeamCount.value = Number(data.teamUnreadCount || 0)
    unreadRefereeCount.value = Number(data.refereeUnreadCount || 0)
    unreadPrivateCount.value = Number(data.privateTotalUnreadCount || 0)
    unreadPrivateDetails.value = data.privateDetails || {}
  }

  /**
   * 用户刷新页面时轻量拉取各频道未读数（无需拉取聊天历史与联系人）
   */
  async function fetchUnreadCount(competitionId?: number | string) {
    const compId = competitionId || currentCompetitionId.value
    if (!compId) return
    try {
      const res = await getChatUnreadCountApi(compId)
      if (res.data) {
        applyUnreadCounts(res.data)
      }
    } catch (err) {
      console.warn('[CompetitionChat] 获取未读消息统计失败:', err)
    }
  }

  /**
   * 初始化并连接竞赛专属聊天 WebSocket（仅保持静默监听与未读感知）
   */
  async function connectChat(competitionId: number | string) {
    if (currentCompetitionId.value === competitionId && client.value?.isConnected()) {
      return
    }

    disconnectChat()

    currentCompetitionId.value = competitionId
    status.value = 'CONNECTING'

    const wsClient = new ChatWebSocketClient({
      competitionId,
      getToken: () => getAccessToken(),
      autoConnect: true
    })

    wsClient.onChat('statusChange', (newStatus) => {
      status.value = newStatus
    })

    wsClient.onChat('connectedReady', (data) => {
      readyData.value = data
      if (data?.unread) {
        applyUnreadCounts(data.unread)
      }
    })

    wsClient.onChat('chatMessage', (message) => {
      handleIncomingMessage(message)
    })

    wsClient.onChat('chatError', (err) => {
      console.warn('[CompetitionChat] 收到服务端错误帧:', err)
    })

    client.value = wsClient

    // 刷新页面或初次进入时，异步获取未读数（轻量统计）
    fetchUnreadCount(competitionId)
  }

  /**
   * 处理实时推达的消息
   */
  function handleIncomingMessage(message: ChatMessageVO) {
    const myId = String(currentUserId.value)

    if (message.channelType === 'TEAM') {
      // 队伍公屏
      if (!teamMessages.value.some((m) => String(m.id) === String(message.id))) {
        teamMessages.value.push(message)
      }
      if (isDrawerOpen.value && currentTarget.value === 'TEAM') {
        markRead('TEAM')
      } else if (String(message.senderId) !== myId) {
        unreadTeamCount.value++
      }
    } else if (message.channelType === 'PRIVATE') {
      // 私信
      const senderIdStr = String(message.senderId)
      const receiverIdStr = String(message.receiverId)
      const otherUserId = senderIdStr === myId ? receiverIdStr : senderIdStr
      const userKey = `USER_${otherUserId}`

      if (!privateMessages.value[otherUserId]) {
        privateMessages.value[otherUserId] = []
      }
      if (!privateMessages.value[otherUserId].some((m) => String(m.id) === String(message.id))) {
        privateMessages.value[otherUserId].push(message)
      }

      const isCurrentViewing = isDrawerOpen.value && currentTarget.value === userKey

      if (isCurrentViewing) {
        markRead(userKey)
      } else if (senderIdStr !== myId) {
        unreadPrivateCount.value++
        unreadPrivateDetails.value[userKey] = (unreadPrivateDetails.value[userKey] || 0) + 1
      }
    } else if (message.channelType === 'REFEREE') {
      // 裁判申诉工单
      const teamKey = String(message.teamId)
      if (!refereeMessages.value[teamKey]) {
        refereeMessages.value[teamKey] = []
      }
      if (!refereeMessages.value[teamKey].some((m) => String(m.id) === String(message.id))) {
        refereeMessages.value[teamKey].push(message)
      }

      if (isReferee.value) {
        const teamItem = refereeTeams.value.find((t) => String(t.teamId) === teamKey)
        if (teamItem) {
          teamItem.lastContent = message.content
          teamItem.lastSenderName = message.senderName
          teamItem.lastSenderRole = message.senderRole
          teamItem.lastCreatedAt = message.createdAt
        }
      }

      const isCurrentViewing =
        isDrawerOpen.value &&
        currentTarget.value === 'REFEREE' &&
        (!isReferee.value || String(selectedRefereeTeamId.value) === teamKey)

      if (isCurrentViewing) {
        markRead('REFEREE')
      } else if (String(message.senderId) !== myId) {
        unreadRefereeCount.value++
      }
    }
  }

  /**
   * 按需懒加载历史聊天消息
   */
  async function fetchHistoryIfNeeded(target: string) {
    if (loadedHistoryTargets.value.has(target)) {
      return
    }
    loadedHistoryTargets.value.add(target)
    await fetchHistory(target)
  }

  /**
   * 分页拉取历史聊天消息
   */
  async function fetchHistory(target: string) {
    if (!currentCompetitionId.value) return

    loadingHistory.value = true
    try {
      let channelType: ChatChannelType = 'TEAM'
      let targetUserId: number | string | undefined
      let teamId: number | string | undefined

      if (target === 'TEAM') {
        channelType = 'TEAM'
      } else if (target.startsWith('USER_')) {
        channelType = 'PRIVATE'
        targetUserId = target.replace('USER_', '')
      } else if (target === 'REFEREE') {
        channelType = 'REFEREE'
        if (isReferee.value) {
          teamId = selectedRefereeTeamId.value || undefined
        }
      }

      const res = await getChatHistoryApi(currentCompetitionId.value, {
        channelType,
        targetUserId,
        teamId,
        size: 50
      })

      if (res.data?.list) {
        const ascending = [...res.data.list].reverse()
        if (channelType === 'TEAM') {
          teamMessages.value = ascending
        } else if (channelType === 'PRIVATE' && targetUserId) {
          privateMessages.value[String(targetUserId)] = ascending
        } else if (channelType === 'REFEREE') {
          const key = String(teamId || readyData.value?.teamId || competitionStore.myTeam?.teamId || 'my_team')
          refereeMessages.value[key] = ascending
        }
      }
    } catch (err) {
      console.error('[CompetitionChat] 拉取历史记录失败:', err)
    } finally {
      loadingHistory.value = false
    }
  }

  /**
   * 标记当前目标已读并同步至后端与 Redis
   */
  function markRead(target: string) {
    let lastMsgId: string | number | undefined

    if (target === 'TEAM') {
      unreadTeamCount.value = 0
      const lastMsg = teamMessages.value[teamMessages.value.length - 1]
      lastMsgId = lastMsg ? lastMsg.id : undefined
    } else if (target.startsWith('USER_')) {
      const otherUserId = target.replace('USER_', '')
      const unreadOfThis = unreadPrivateDetails.value[target] || 0
      unreadPrivateCount.value = Math.max(0, unreadPrivateCount.value - unreadOfThis)
      unreadPrivateDetails.value[target] = 0
      const lastMsg = privateMessages.value[otherUserId]?.[privateMessages.value[otherUserId].length - 1]
      lastMsgId = lastMsg ? lastMsg.id : undefined
    } else if (target === 'REFEREE') {
      unreadRefereeCount.value = 0
      const key = String(selectedRefereeTeamId.value || readyData.value?.teamId || competitionStore.myTeam?.teamId || 'my_team')
      const lastMsg = refereeMessages.value[key]?.[refereeMessages.value[key].length - 1]
      lastMsgId = lastMsg ? lastMsg.id : undefined
    }

    // 优先通过 WebSocket 发送已读同步帧
    if (client.value?.isConnected()) {
      client.value.sendReadAck(target, lastMsgId)
    } else if (currentCompetitionId.value) {
      // 离线时 HTTP 兜底
      markChatReadApi(currentCompetitionId.value, { target, lastMessageId: lastMsgId }).catch(() => {})
    }
  }

  /**
   * 裁判端获取所有工单队伍概览
   */
  async function fetchRefereeTeams() {
    if (!currentCompetitionId.value) return
    try {
      const res = await getRefereeTeamsApi(currentCompetitionId.value)
      if (res.data) {
        refereeTeams.value = res.data
        if (!selectedRefereeTeamId.value && res.data.length > 0) {
          selectRefereeTeam(res.data[0].teamId)
        }
      }
    } catch (err) {
      console.error('[CompetitionChat] 拉取裁判队伍列表失败:', err)
    }
  }

  /**
   * 发送聊天、私信或工单消息（优先 WebSocket，断连时优雅回退 HTTP 降级）
   */
  async function sendMessage(
    content: string,
    msgType: ChatMessageType = 'TEXT',
    mentionUserIds?: (string | number)[]
  ) {
    if (!currentCompetitionId.value || !content.trim()) return

    const trimmed = content.trim()
    let channelType: ChatChannelType = 'TEAM'
    let receiverId: number | string | undefined
    let targetTeamId: number | string | undefined

    if (currentTarget.value === 'TEAM') {
      channelType = 'TEAM'
    } else if (currentTarget.value.startsWith('USER_')) {
      channelType = 'PRIVATE'
      receiverId = currentTarget.value.replace('USER_', '')
      const targetMember = teammateList.value.find((m) => String(m.userId) === String(receiverId))
      if (isSelfMemberIdentity({ userId: receiverId, username: targetMember?.username }, userStore.user, readyData.value)) {
        return
      }
    } else if (currentTarget.value === 'REFEREE') {
      channelType = 'REFEREE'
      targetTeamId = isReferee.value
        ? selectedRefereeTeamId.value || undefined
        : readyData.value?.teamId || competitionStore.myTeam?.teamId || undefined
    }

    const dto = {
      channelType,
      teamId: targetTeamId,
      receiverId,
      mentionUserIds,
      content: trimmed,
      msgType
    }

    if (client.value?.isConnected()) {
      client.value.sendChatMessage(dto)
    } else {
      const res = await sendChatMessageApi(currentCompetitionId.value, dto)
      if (res.data) {
        handleIncomingMessage(res.data)
      }
    }
  }

  /**
   * 切换沟通目标 (TEAM / REFEREE / USER_xxx)，按需懒加载并同步已读
   */
  function switchTarget(target: string) {
    if (target.startsWith('USER_')) {
      const targetUserId = target.replace('USER_', '')
      const targetMember = teammateList.value.find((m) => String(m.userId) === targetUserId)
      if (isSelfMemberIdentity({ userId: targetUserId, username: targetMember?.username }, userStore.user, readyData.value)) {
        return
      }
    }
    currentTarget.value = target
    // 只有在打开抽屉状态下才懒加载历史与标记已读
    if (isDrawerOpen.value) {
      fetchHistoryIfNeeded(target)
      markRead(target)
    }
  }

  /**
   * 裁判选中指定队伍工单
   */
  function selectRefereeTeam(teamId: number | string) {
    selectedRefereeTeamId.value = teamId
    unreadRefereeCount.value = 0
    if (isDrawerOpen.value) {
      fetchHistory('REFEREE')
      markRead('REFEREE')
    }
  }

  /**
   * 展开/关闭抽屉
   */
  function toggleDrawer(open?: boolean) {
    const nextState = open !== undefined ? open : !isDrawerOpen.value
    isDrawerOpen.value = nextState
    if (nextState) {
      // 展开抽屉时，按需懒加载当前目标的历史并标记已读
      fetchHistoryIfNeeded(currentTarget.value)
      markRead(currentTarget.value)
    }
  }

  /**
   * 彻底注销并断开聊天客户端
   */
  function disconnectChat() {
    if (client.value) {
      client.value.destroy()
      client.value = null
    }
    status.value = 'DISCONNECTED'
    readyData.value = null
    teamMessages.value = []
    privateMessages.value = {}
    refereeMessages.value = {}
    refereeTeams.value = []
    selectedRefereeTeamId.value = null
    unreadTeamCount.value = 0
    unreadPrivateCount.value = 0
    unreadRefereeCount.value = 0
    unreadPrivateDetails.value = {}
    loadedHistoryTargets.value.clear()
    currentCompetitionId.value = null
    currentTarget.value = 'TEAM'
  }

  return {
    currentCompetitionId,
    client,
    status,
    readyData,
    currentTarget,
    teamMessages,
    privateMessages,
    refereeMessages,
    refereeTeams,
    selectedRefereeTeamId,
    unreadTeamCount,
    unreadPrivateCount,
    unreadRefereeCount,
    unreadPrivateDetails,
    isDrawerOpen,
    loadingHistory,
    isReferee,
    currentUserId,
    teammateList,
    currentMessages,
    totalUnreadCount,
    connectChat,
    disconnectChat,
    fetchUnreadCount,
    sendMessage,
    switchTarget,
    selectRefereeTeam,
    fetchHistory,
    fetchRefereeTeams,
    toggleDrawer,
    markRead
  }
})
