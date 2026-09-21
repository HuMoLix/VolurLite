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
  const loadingMoreHistory = ref<boolean>(false)

  // 游标流式分页元数据 (按 target 存储下一次向上翻页游标与是否还有更多历史)
  const nextPageTokens = ref<Record<string, string | null>>({})
  const hasMoreHistory = ref<Record<string, boolean>>({})

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
      const targetTeamId = (isReferee.value && selectedRefereeTeamId.value)
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
   * 清除本地指定目标的未读计数
   */
  function clearLocalUnread(target: string) {
    if (target === 'TEAM') {
      unreadTeamCount.value = 0
    } else if (target.startsWith('USER_')) {
      const unreadOfThis = unreadPrivateDetails.value[target] || 0
      unreadPrivateCount.value = Math.max(0, unreadPrivateCount.value - unreadOfThis)
      unreadPrivateDetails.value[target] = 0
    } else if (target === 'REFEREE') {
      unreadRefereeCount.value = 0
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
   * 游标流式拉取最新历史聊天消息（服务端收到 /history 会自动将该频道标记已读）
   */
  async function fetchHistory(target: string) {
    if (!currentCompetitionId.value) return

    loadingHistory.value = true
    // 直线自动已读：先乐观清空本地当前频道的未读计数
    clearLocalUnread(target)

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
        if (isReferee.value && selectedRefereeTeamId.value) {
          teamId = selectedRefereeTeamId.value
        } else {
          teamId = readyData.value?.teamId || competitionStore.myTeam?.teamId || undefined
        }
      }

      const res = await getChatHistoryApi(currentCompetitionId.value, {
        channelType,
        targetUserId,
        teamId,
        size: 50
      })

      if (res.data) {
        nextPageTokens.value[target] = res.data.nextPageToken
        hasMoreHistory.value[target] = res.data.hasMore

        if (res.data.list) {
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
      }
    } catch (err) {
      console.error('[CompetitionChat] 拉取历史记录失败:', err)
    } finally {
      loadingHistory.value = false
    }
  }

  /**
   * 游标向上翻页加载更早的历史记录
   */
  async function loadMoreHistory(target: string) {
    if (!currentCompetitionId.value || loadingMoreHistory.value) return
    const token = nextPageTokens.value[target]
    const hasMore = hasMoreHistory.value[target]
    if (!hasMore || !token) return

    loadingMoreHistory.value = true
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
        if (isReferee.value && selectedRefereeTeamId.value) {
          teamId = selectedRefereeTeamId.value
        } else {
          teamId = readyData.value?.teamId || competitionStore.myTeam?.teamId || undefined
        }
      }

      const res = await getChatHistoryApi(currentCompetitionId.value, {
        channelType,
        targetUserId,
        teamId,
        beforeId: token,
        size: 50
      })

      if (res.data) {
        nextPageTokens.value[target] = res.data.nextPageToken
        hasMoreHistory.value[target] = res.data.hasMore

        if (res.data.list && res.data.list.length > 0) {
          const olderAscending = [...res.data.list].reverse()
          if (channelType === 'TEAM') {
            teamMessages.value = [...olderAscending, ...teamMessages.value]
          } else if (channelType === 'PRIVATE' && targetUserId) {
            const currentList = privateMessages.value[String(targetUserId)] || []
            privateMessages.value[String(targetUserId)] = [...olderAscending, ...currentList]
          } else if (channelType === 'REFEREE') {
            const key = String(teamId || readyData.value?.teamId || competitionStore.myTeam?.teamId || 'my_team')
            const currentList = refereeMessages.value[key] || []
            refereeMessages.value[key] = [...olderAscending, ...currentList]
          }
        }
      }
    } catch (err) {
      console.error('[CompetitionChat] 加载更早历史记录失败:', err)
    } finally {
      loadingMoreHistory.value = false
    }
  }

  /**
   * 实时消息到达时的轻量已读同步确认（优先 WebSocket，HTTP 降级）
   */
  function markRead(target: string) {
    clearLocalUnread(target)
    let lastMsgId: string | number | undefined

    if (target === 'TEAM') {
      const lastMsg = teamMessages.value[teamMessages.value.length - 1]
      lastMsgId = lastMsg ? lastMsg.id : undefined
    } else if (target.startsWith('USER_')) {
      const otherUserId = target.replace('USER_', '')
      const list = privateMessages.value[otherUserId]
      const lastMsg = list ? list[list.length - 1] : undefined
      lastMsgId = lastMsg ? lastMsg.id : undefined
    } else if (target === 'REFEREE') {
      const key = String((isReferee.value && selectedRefereeTeamId.value) || readyData.value?.teamId || competitionStore.myTeam?.teamId || 'my_team')
      const list = refereeMessages.value[key]
      const lastMsg = list ? list[list.length - 1] : undefined
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
      targetTeamId = (isReferee.value && selectedRefereeTeamId.value)
        ? selectedRefereeTeamId.value
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
   * 切换沟通目标 (TEAM / REFEREE / USER_xxx)，拉取历史记录并自动在服务端同步已读
   */
  async function switchTarget(target: string) {
    if (target.startsWith('USER_')) {
      const targetUserId = target.replace('USER_', '')
      const targetMember = teammateList.value.find((m) => String(m.userId) === targetUserId)
      if (isSelfMemberIdentity({ userId: targetUserId, username: targetMember?.username }, userStore.user, readyData.value)) {
        return
      }
    }
    currentTarget.value = target
    // 只有在打开抽屉状态下才拉取历史（自动置已读）
    if (isDrawerOpen.value) {
      clearLocalUnread(target)
      await fetchHistory(target)
    }
  }

  /**
   * 裁判选中指定队伍工单
   */
  async function selectRefereeTeam(teamId: number | string) {
    selectedRefereeTeamId.value = teamId
    unreadRefereeCount.value = 0
    if (isDrawerOpen.value) {
      await fetchHistory('REFEREE')
    }
  }

  /**
   * 展开/关闭抽屉
   */
  async function toggleDrawer(open?: boolean) {
    const nextState = open !== undefined ? open : !isDrawerOpen.value
    isDrawerOpen.value = nextState
    if (nextState) {
      clearLocalUnread(currentTarget.value)
      await fetchHistory(currentTarget.value)
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
    nextPageTokens.value = {}
    hasMoreHistory.value = {}
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
    nextPageTokens,
    hasMoreHistory,
    unreadTeamCount,
    unreadPrivateCount,
    unreadRefereeCount,
    unreadPrivateDetails,
    isDrawerOpen,
    loadingHistory,
    loadingMoreHistory,
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
    fetchHistoryIfNeeded,
    loadMoreHistory,
    fetchRefereeTeams,
    toggleDrawer,
    markRead,
    clearLocalUnread
  }
})
