<template>
  <div class="ticket-tab">
    <div class="ticket-layout-container">
      <!-- 1. 左侧队伍与工单列表栏 (Teams & Tickets Sidebar) -->
      <div class="ticket-sidebar-panel">
        <div class="sidebar-header">
          <div class="header-title-row">
            <span class="sidebar-title">参赛队伍列表</span>
            <n-button quaternary circle size="tiny" title="刷新队伍与工单" :loading="loadingTeams" @click="loadData">
              <template #icon>
                <n-icon :component="RefreshOutline" />
              </template>
            </n-button>
          </div>
          <div class="sidebar-search-wrap">
            <n-input
              v-model:value="searchKeyword"
              size="small"
              placeholder="搜索队伍名称或队长..."
              clearable
            >
              <template #prefix>
                <n-icon :component="SearchOutline" />
              </template>
            </n-input>
          </div>
          <div class="sidebar-filter-tabs">
            <button
              type="button"
              class="filter-pill"
              :class="{ active: listFilterMode === 'ALL' }"
              @click="listFilterMode = 'ALL'"
            >
              全部队伍 ({{ filteredTeams.length }})
            </button>
            <button
              type="button"
              class="filter-pill"
              :class="{ active: listFilterMode === 'TICKETS' }"
              @click="listFilterMode = 'TICKETS'"
            >
              有工单 ({{ ticketTeamsCount }})
            </button>
          </div>
        </div>

        <div class="sidebar-teams-list">
          <div v-if="loadingTeams" class="loading-state">
            <n-spin size="small" />
            <span>正在加载队伍工单列表...</span>
          </div>

          <div v-else-if="filteredTeams.length === 0" class="empty-state">
            <n-empty description="暂无符合条件的参赛队伍" />
          </div>

          <div
            v-for="team in filteredTeams"
            v-else
            :key="String(team.id)"
            class="team-item-card"
            :class="{ active: selectedTeam && String(selectedTeam.id) === String(team.id) }"
            @click="handleSelectTeam(team)"
          >
            <div class="team-avatar-col">
              <AppAvatar
                :name="team.teamName"
                :username="team.teamName"
                size="medium"
              />
            </div>
            <div class="team-meta-col">
              <div class="team-title-row">
                <span class="team-name" :title="team.teamName">{{ team.teamName }}</span>
                <span v-if="team.hasTickets" class="ticket-tag">工单</span>
              </div>
              <div class="team-sub-row">
                <span class="captain-desc">队长: {{ team.captainUsername || '未知' }}</span>
                <span v-if="team.lastTicketTime" class="time-desc">{{ team.lastTicketTime }}</span>
              </div>
              <div v-if="team.lastTicketContent" class="team-last-msg" :title="team.lastTicketContent">
                {{ team.lastTicketContent }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 2. 右侧主工作区：工单与双向交流 (Interaction Workspace) -->
      <div class="ticket-workspace-panel">
        <div v-if="!selectedTeam" class="no-selection-box">
          <div class="empty-guide-content">
            <n-icon :component="ChatbubbleEllipsesOutline" size="64" class="guide-icon" />
            <h4 class="guide-title">请从左侧选择参赛队伍</h4>
            <p class="guide-desc">
              裁判可在此与各战队开展官方答疑工单交互，亦可进入队伍内部频道发布裁判通报或与指定参赛成员发送点对点私信。
            </p>
          </div>
        </div>

        <div v-else class="workspace-active-chat">
          <!-- 工作区顶栏 (Workspace Header) -->
          <div class="workspace-top-bar">
            <div class="top-bar-team-info">
              <div class="team-headline">
                <span class="active-team-name">【{{ selectedTeam.teamName }}】</span>
                <n-tag size="tiny" :bordered="false" type="info">ID: {{ selectedTeam.id }}</n-tag>
                <span class="member-count-hint">成员: {{ teamMembers.length }} 人</span>
              </div>
              <div class="active-channel-pills">
                <button
                  type="button"
                  class="channel-pill"
                  :class="{ active: currentChannel === 'REFEREE' }"
                  @click="switchChannel('REFEREE')"
                >
                  <n-icon :component="ShieldCheckmarkOutline" />
                  <span>裁判答疑工单</span>
                </button>
                <button
                  type="button"
                  class="channel-pill"
                  :class="{ active: currentChannel === 'TEAM' }"
                  @click="switchChannel('TEAM')"
                >
                  <n-icon :component="RadioOutline" />
                  <span>队伍公屏广播</span>
                </button>
                <button
                  type="button"
                  class="channel-pill"
                  :class="{ active: currentChannel === 'PRIVATE' }"
                  @click="switchChannel('PRIVATE')"
                >
                  <n-icon :component="PersonOutline" />
                  <span>队员点对点私信</span>
                </button>
              </div>
            </div>

            <div class="top-bar-actions">
              <n-button quaternary size="small" :loading="loadingHistory" @click="fetchHistory">
                <template #icon>
                  <n-icon :component="RefreshOutline" />
                </template>
                刷新记录
              </n-button>
            </div>
          </div>

          <!-- 私信目标成员选择条 (仅在 PRIVATE 模式展示) -->
          <div v-if="currentChannel === 'PRIVATE'" class="private-member-select-bar">
            <span class="bar-label">私信目标成员:</span>
            <n-select
              v-model:value="selectedMemberId"
              :options="memberSelectOptions"
              placeholder="请选择接收私信的队员..."
              size="small"
              class="member-select"
              @update:value="handleMemberChange"
            />
          </div>

          <!-- 消息展示流 (Message Feed) -->
          <div ref="messageFeedRef" class="workspace-feed-box">
            <div v-if="loadingHistory" class="feed-loading-wrap">
              <n-spin size="small" />
              <span>正在拉取历史沟通记录...</span>
            </div>

            <div v-else-if="messages.length === 0" class="feed-empty-wrap">
              <n-empty :description="channelEmptyDesc" />
            </div>

            <div
              v-for="msg in messages"
              :key="String(msg.id)"
              class="admin-chat-message-row"
              :class="{
                'is-referee-sender': isRefereeMessage(msg),
                'is-competitor-sender': !isRefereeMessage(msg)
              }"
            >
              <div class="msg-avatar-col">
                <AppAvatar
                  :src="msg.senderAvatar || undefined"
                  :name="msg.senderName"
                  :username="msg.senderName"
                  size="small"
                />
              </div>
              <div class="msg-content-col">
                <div class="msg-meta-row">
                  <span class="sender-username">{{ msg.senderName }}</span>
                  <span v-if="isRefereeMessage(msg)" class="role-pill referee">裁判组</span>
                  <span v-else class="role-pill member">参赛队员</span>
                  <span class="msg-time">{{ formatTime(msg.createdAt) }}</span>
                </div>
                <div class="msg-bubble" :class="{ 'referee-bubble': isRefereeMessage(msg) }">
                  <span class="bubble-text">{{ msg.content }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 底部输入与发送控制台 (Send Box) -->
          <div class="workspace-input-console">
            <div class="input-target-notice">
              <span class="notice-prefix">当前发送目标：</span>
              <span class="notice-badge">{{ currentTargetNoticeText }}</span>
              <span v-if="currentChannel === 'TEAM'" class="notice-tip">
                (裁判直接向该队伍频道广播，全队队员可见；队伍无法在公屏中 @ 裁判)
              </span>
            </div>

            <div class="input-textarea-row">
              <n-input
                v-model:value="inputMessageText"
                type="textarea"
                :autosize="{ minRows: 3, maxRows: 5 }"
                :placeholder="inputPlaceholderText"
                class="admin-ticket-input"
                @keydown="handleKeyDown"
              />
              <n-button
                type="primary"
                class="theme-send-btn"
                :loading="sending"
                :disabled="!canSend"
                @click="handleSend"
              >
                <template #icon>
                  <n-icon :component="PaperPlaneOutline" />
                </template>
                发送
              </n-button>
            </div>
            <div class="input-bottom-hints">
              <span class="hint">按 Enter 发送 / Shift+Enter 换行</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'TicketTab'
})

import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import {
  NInput,
  NButton,
  NIcon,
  NTag,
  NSpin,
  NEmpty,
  NSelect,
  useMessage,
  type SelectOption
} from 'naive-ui'
import {
  ChatbubbleEllipsesOutline,
  PaperPlaneOutline,
  RefreshOutline,
  SearchOutline,
  PersonOutline,
  ShieldCheckmarkOutline,
  RadioOutline
} from '@vicons/ionicons5'
import AppAvatar from '@/components/common/AppAvatar.vue'
import { getAccessToken } from '@/utils/auth'
import { ChatWebSocketClient } from '@/utils/chatWebSocket'
import {
  getRefereeTeamsApi,
  getChatHistoryApi,
  sendChatMessageApi
} from '@/api/competition/chat'
import { listAdminTeamsApi, getAdminTeamDetailApi } from '@/api/competition'
import type {
  ChatMessageVO,
  ChatChannelType,
  RefereeTeamSummaryVO
} from '@/types/competition/chat'
import type {
  AdminTeamListVO,
  CompetitionMemberVO
} from '@/types/competition'

interface Props {
  compId: number | string
}

const props = defineProps<Props>()
const message = useMessage()

// 状态管理
const loadingTeams = ref<boolean>(false)
const searchKeyword = ref<string>('')
const listFilterMode = ref<'ALL' | 'TICKETS'>('ALL')

interface TeamWithTicketItem extends AdminTeamListVO {
  hasTickets?: boolean
  lastTicketContent?: string
  lastTicketTime?: string
  lastSenderName?: string
}

const allTeams = ref<TeamWithTicketItem[]>([])
const refereeSummaries = ref<RefereeTeamSummaryVO[]>([])

// 当前选中的队伍
const selectedTeam = ref<TeamWithTicketItem | null>(null)
const teamMembers = ref<CompetitionMemberVO[]>([])
const selectedMemberId = ref<string | number | null>(null)

// 频道选择：'REFEREE' (工单), 'TEAM' (队伍公屏广播), 'PRIVATE' (队员私信)
const currentChannel = ref<ChatChannelType>('REFEREE')

// 消息列表与加载
const messages = ref<ChatMessageVO[]>([])
const loadingHistory = ref<boolean>(false)
const messageFeedRef = ref<HTMLElement | null>(null)

// 输入与发送
const inputMessageText = ref<string>('')
const sending = ref<boolean>(false)

// WebSocket 长连接实例（管理端专属）
let wsClient: ChatWebSocketClient | null = null

// 计算筛选后的队伍列表
const filteredTeams = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  return allTeams.value.filter((t) => {
    if (listFilterMode.value === 'TICKETS' && !t.hasTickets) {
      return false
    }
    if (!kw) return true
    const matchName = t.teamName?.toLowerCase().includes(kw)
    const matchCaptain = t.captainUsername?.toLowerCase().includes(kw)
    return matchName || matchCaptain
  })
})

const ticketTeamsCount = computed(() => {
  return allTeams.value.filter((t) => t.hasTickets).length
})

const memberSelectOptions = computed<SelectOption[]>(() => {
  return teamMembers.value.map((m) => ({
    label: `${m.username} (${m.role === 'CAPTAIN' ? '队长' : '队员'})`,
    value: m.userId
  }))
})

const canSend = computed(() => {
  if (!inputMessageText.value.trim() || sending.value || !selectedTeam.value) {
    return false
  }
  if (currentChannel.value === 'PRIVATE' && !selectedMemberId.value) {
    return false
  }
  return true
})

const currentTargetNoticeText = computed(() => {
  if (!selectedTeam.value) return ''
  if (currentChannel.value === 'REFEREE') {
    return `【${selectedTeam.value.teamName}】申诉答疑工单通道`
  }
  if (currentChannel.value === 'TEAM') {
    return `【${selectedTeam.value.teamName}】队伍公屏频道`
  }
  if (currentChannel.value === 'PRIVATE') {
    const mem = teamMembers.value.find((m) => String(m.userId) === String(selectedMemberId.value))
    return mem ? `队员【${mem.username}】个人私信` : '请选择目标队员'
  }
  return ''
})

const inputPlaceholderText = computed(() => {
  if (!selectedTeam.value) return ''
  if (currentChannel.value === 'REFEREE') {
    return `以裁判身份向【${selectedTeam.value.teamName}】回复答疑工单...`
  }
  if (currentChannel.value === 'TEAM') {
    return `以裁判身份向【${selectedTeam.value.teamName}】队伍频道广播文字（所有队员均可见）...`
  }
  if (currentChannel.value === 'PRIVATE') {
    return '以裁判身份向该成员发送私信交流...'
  }
  return '请输入消息内容...'
})

const channelEmptyDesc = computed(() => {
  if (currentChannel.value === 'REFEREE') {
    return '该队伍暂无向裁判组发起的申诉或提问工单'
  }
  if (currentChannel.value === 'TEAM') {
    return '该队伍公屏暂无交流消息，裁判可在此主动发起通报或警告'
  }
  return '暂无与该队员的私信记录'
})

// 生命周期
onMounted(() => {
  loadData()
  initAdminWebSocket()
})

onUnmounted(() => {
  if (wsClient) {
    wsClient.disconnect()
    wsClient = null
  }
})

watch(
  () => props.compId,
  (newId) => {
    if (newId) {
      loadData()
      initAdminWebSocket()
    }
  }
)

/**
 * 初始化管理员专属聊天 WebSocket 客户端，实现实时双向消息推达
 */
function initAdminWebSocket() {
  if (wsClient) {
    wsClient.disconnect()
    wsClient = null
  }

  if (!props.compId) return

  wsClient = new ChatWebSocketClient({
    competitionId: props.compId,
    getToken: () => getAccessToken(),
    autoConnect: true
  })

  wsClient.onChat('chatMessage', (incomingMsg) => {
    handleIncomingSocketMessage(incomingMsg)
  })
}

/**
 * 处理实时推达的数据帧
 */
function handleIncomingSocketMessage(incomingMsg: ChatMessageVO) {
  // 1. 同步更新左侧队伍工单概要
  if (incomingMsg.channelType === 'REFEREE') {
    const targetTeam = allTeams.value.find((t) => String(t.id) === String(incomingMsg.teamId))
    if (targetTeam) {
      targetTeam.hasTickets = true
      targetTeam.lastTicketContent = incomingMsg.content
      targetTeam.lastTicketTime = formatTime(incomingMsg.createdAt)
      targetTeam.lastSenderName = incomingMsg.senderName
    }
  }

  // 2. 若推送的消息恰好属于当前激活的队伍和频道，追加展示
  if (!selectedTeam.value) return
  if (String(incomingMsg.teamId) !== String(selectedTeam.value.id)) return

  if (incomingMsg.channelType === currentChannel.value) {
    if (currentChannel.value === 'PRIVATE') {
      if (
        String(incomingMsg.senderId) === String(selectedMemberId.value) ||
        String(incomingMsg.receiverId) === String(selectedMemberId.value)
      ) {
        appendMessageIfNotExist(incomingMsg)
      }
    } else {
      appendMessageIfNotExist(incomingMsg)
    }
  }
}

function appendMessageIfNotExist(incomingMsg: ChatMessageVO) {
  if (!messages.value.some((m) => String(m.id) === String(incomingMsg.id))) {
    messages.value.push(incomingMsg)
    nextTick(() => {
      scrollToBottom()
    })
  }
}

/**
 * 加载全部参赛队伍及裁判工单概览
 */
async function loadData() {
  if (!props.compId) return
  loadingTeams.value = true

  try {
    const [teamsRes, refereeRes] = await Promise.all([
      listAdminTeamsApi(props.compId, { size: 200 }),
      getRefereeTeamsApi(props.compId).catch(() => ({ data: [] }))
    ])

    const ticketMap = new Map<string, RefereeTeamSummaryVO>()
    if (refereeRes?.data) {
      refereeSummaries.value = refereeRes.data
      refereeRes.data.forEach((item) => {
        ticketMap.set(String(item.teamId), item)
      })
    }

    if (teamsRes.data?.list) {
      allTeams.value = teamsRes.data.list.map((team: AdminTeamListVO) => {
        const ticket = ticketMap.get(String(team.id))
        return {
          ...team,
          hasTickets: !!ticket,
          lastTicketContent: ticket?.lastContent || '',
          lastTicketTime: ticket?.lastCreatedAt ? formatTime(ticket.lastCreatedAt) : '',
          lastSenderName: ticket?.lastSenderName || ''
        }
      })
    }

    // 若当前未选中队伍但有队伍列表，默认激活首位队伍
    if (!selectedTeam.value && allTeams.value.length > 0) {
      const firstTicketTeam = allTeams.value.find((t) => t.hasTickets)
      handleSelectTeam(firstTicketTeam || allTeams.value[0])
    }
  } catch (err: any) {
    message.error(err.message || '加载参赛队伍工单列表失败')
  } finally {
    loadingTeams.value = false
  }
}

/**
 * 选中指定战队
 */
async function handleSelectTeam(team: TeamWithTicketItem) {
  selectedTeam.value = team
  // 默认进入工单频道
  currentChannel.value = team.hasTickets ? 'REFEREE' : 'TEAM'

  // 加载该战队成员详情
  try {
    const detailRes = await getAdminTeamDetailApi(props.compId, team.id)
    if (detailRes.data?.members) {
      teamMembers.value = detailRes.data.members
      if (detailRes.data.members.length > 0) {
        selectedMemberId.value = detailRes.data.members[0].userId
      }
    }
  } catch {
    teamMembers.value = []
  }

  fetchHistory()
}

/**
 * 切换交流频道 (工单 / 队伍公屏 / 私信)
 */
function switchChannel(channel: ChatChannelType) {
  currentChannel.value = channel
  fetchHistory()
}

function handleMemberChange() {
  if (currentChannel.value === 'PRIVATE') {
    fetchHistory()
  }
}

/**
 * 获取当前频道历史消息
 */
async function fetchHistory() {
  if (!props.compId || !selectedTeam.value) return
  loadingHistory.value = true

  try {
    const res = await getChatHistoryApi(props.compId, {
      channelType: currentChannel.value,
      teamId: selectedTeam.value.id,
      targetUserId: currentChannel.value === 'PRIVATE' ? (selectedMemberId.value ?? undefined) : undefined,
      size: 100
    })

    if (res.data?.list) {
      // 倒序列表恢复为正序排列
      messages.value = [...res.data.list].reverse()
      nextTick(() => {
        scrollToBottom()
      })
    } else {
      messages.value = []
    }
  } catch (err: any) {
    message.error(err.message || '获取历史消息失败')
    messages.value = []
  } finally {
    loadingHistory.value = false
  }
}

/**
 * 发送消息（支持向工单回复、向队伍公屏广播、向队员私信）
 */
async function handleSend() {
  if (!canSend.value || !selectedTeam.value) return

  const text = inputMessageText.value.trim()
  inputMessageText.value = ''
  sending.value = true

  try {
    const res = await sendChatMessageApi(props.compId, {
      channelType: currentChannel.value,
      teamId: selectedTeam.value.id,
      receiverId: currentChannel.value === 'PRIVATE' ? (selectedMemberId.value ?? undefined) : undefined,
      content: text,
      msgType: 'TEXT'
    })

    if (res.data) {
      appendMessageIfNotExist(res.data)
    }
  } catch (err: any) {
    message.error(err.message || '消息发送失败')
    inputMessageText.value = text // 恢复原文字
  } finally {
    sending.value = false
  }
}

function handleKeyDown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handleSend()
  }
}

function isRefereeMessage(msg: ChatMessageVO): boolean {
  const r = (msg.senderRole || '').toUpperCase()
  return r === 'REFEREE' || r === 'ADMIN' || r === 'SUPER_ADMIN'
}

function scrollToBottom() {
  if (messageFeedRef.value) {
    messageFeedRef.value.scrollTop = messageFeedRef.value.scrollHeight
  }
}

function formatTime(isoStr?: string): string {
  if (!isoStr) return ''
  try {
    const d = new Date(isoStr)
    const hours = String(d.getHours()).padStart(2, '0')
    const minutes = String(d.getMinutes()).padStart(2, '0')
    const seconds = String(d.getSeconds()).padStart(2, '0')
    return `${hours}:${minutes}:${seconds}`
  } catch {
    return ''
  }
}
</script>

<style scoped lang="scss">
.ticket-tab {
  display: flex;
  flex-direction: column;
  height: 720px;
  max-height: 80vh;
  box-sizing: border-box;
}

.ticket-layout-container {
  display: flex;
  flex: 1;
  height: 100%;
  border: 1px solid var(--v-border-subtle, rgba(0, 0, 0, 0.08));
  border-radius: 8px;
  overflow: hidden;
  background: var(--v-card-bg, #ffffff);
}

// 1. 左侧队伍侧边栏
.ticket-sidebar-panel {
  width: 320px;
  min-width: 280px;
  border-right: 1px solid var(--v-border-subtle, rgba(0, 0, 0, 0.08));
  display: flex;
  flex-direction: column;
  background: var(--v-card-bg-subtle, #f9fafb);

  .sidebar-header {
    padding: 14px;
    border-bottom: 1px solid var(--v-border-subtle, rgba(0, 0, 0, 0.06));
    display: flex;
    flex-direction: column;
    gap: 10px;

    .header-title-row {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .sidebar-title {
        font-size: 14px;
        font-weight: 700;
        color: var(--v-text-primary, #111827);
      }
    }

    .sidebar-filter-tabs {
      display: flex;
      gap: 6px;

      .filter-pill {
        flex: 1;
        padding: 4px 8px;
        font-size: 11px;
        font-weight: 500;
        border: 1px solid var(--v-border-subtle, rgba(0, 0, 0, 0.08));
        border-radius: 4px;
        background: #ffffff;
        color: #444444;
        cursor: pointer;
        transition: all 0.15s ease;

        &:hover {
          background: rgba(0, 0, 0, 0.04);
        }

        &.active {
          background: #000000;
          color: #ffffff;
          border-color: #000000;
        }
      }
    }
  }

  .sidebar-teams-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px;
    display: flex;
    flex-direction: column;
    gap: 4px;

    .loading-state,
    .empty-state {
      padding: 40px 16px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 8px;
      color: #666666;
      font-size: 12px;
    }

    .team-item-card {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px 12px;
      border-radius: 6px;
      cursor: pointer;
      background: #ffffff;
      border: 1px solid var(--v-border-subtle, rgba(0, 0, 0, 0.05));
      transition: all 0.15s ease;

      &:hover {
        background: rgba(0, 0, 0, 0.02);
      }

      &.active {
        background: rgba(0, 0, 0, 0.06);
        border-color: #000000;
      }

      .team-meta-col {
        flex: 1;
        min-width: 0;

        .team-title-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 2px;

          .team-name {
            font-size: 13px;
            font-weight: 600;
            color: var(--v-text-primary, #111827);
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }

          .ticket-tag {
            font-size: 10px;
            padding: 1px 5px;
            border-radius: 3px;
            background: rgba(59, 130, 246, 0.12);
            color: #1d4ed8;
            font-weight: 600;
            flex-shrink: 0;
          }
        }

        .team-sub-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          font-size: 11px;
          color: #444444;

          .time-desc {
            font-size: 10px;
            color: #888888;
          }
        }

        .team-last-msg {
          font-size: 11px;
          color: #666666;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          margin-top: 2px;
        }
      }
    }
  }
}

// 2. 右侧主工作区
.ticket-workspace-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--v-card-bg, #ffffff);

  .no-selection-box {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 32px;

    .empty-guide-content {
      max-width: 360px;
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;

      .guide-icon {
        color: #9ca3af;
        margin-bottom: 12px;
      }

      .guide-title {
        font-size: 16px;
        font-weight: 600;
        color: var(--v-text-primary, #111827);
        margin: 0 0 8px 0;
      }

      .guide-desc {
        font-size: 13px;
        color: #444444;
        line-height: 1.5;
        margin: 0;
      }
    }
  }

  .workspace-active-chat {
    flex: 1;
    display: flex;
    flex-direction: column;
    height: 100%;

    // 顶栏
    .workspace-top-bar {
      padding: 12px 18px;
      border-bottom: 1px solid var(--v-border-subtle, rgba(0, 0, 0, 0.08));
      display: flex;
      align-items: center;
      justify-content: space-between;
      background: var(--v-card-bg, #ffffff);

      .top-bar-team-info {
        display: flex;
        flex-direction: column;
        gap: 6px;

        .team-headline {
          display: flex;
          align-items: center;
          gap: 8px;

          .active-team-name {
            font-size: 15px;
            font-weight: 700;
            color: var(--v-text-primary, #111827);
          }

          .member-count-hint {
            font-size: 12px;
            color: #444444;
          }
        }

        .active-channel-pills {
          display: flex;
          gap: 6px;

          .channel-pill {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 3px 10px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: 500;
            border: 1px solid var(--v-border-subtle, rgba(0, 0, 0, 0.1));
            background: #ffffff;
            color: #444444;
            cursor: pointer;
            transition: all 0.15s ease;

            &:hover {
              background: rgba(0, 0, 0, 0.03);
            }

            &.active {
              background: #000000;
              color: #ffffff;
              border-color: #000000;
            }
          }
        }
      }
    }

    // 私信队员选择条
    .private-member-select-bar {
      padding: 8px 18px;
      background: rgba(0, 0, 0, 0.02);
      border-bottom: 1px solid var(--v-border-subtle, rgba(0, 0, 0, 0.06));
      display: flex;
      align-items: center;
      gap: 10px;

      .bar-label {
        font-size: 12px;
        font-weight: 600;
        color: #444444;
      }

      .member-select {
        width: 240px;
      }
    }

    // 消息列表
    .workspace-feed-box {
      flex: 1;
      overflow-y: auto;
      padding: 16px 20px;
      display: flex;
      flex-direction: column;
      gap: 14px;
      background: var(--v-feed-bg, #fafafa);

      .feed-loading-wrap,
      .feed-empty-wrap {
        margin: auto;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 8px;
        color: #666666;
        font-size: 13px;
      }

      .admin-chat-message-row {
        display: flex;
        gap: 10px;
        align-items: flex-start;

        .msg-content-col {
          display: flex;
          flex-direction: column;
          gap: 4px;
          max-width: 75%;

          .msg-meta-row {
            display: flex;
            align-items: center;
            gap: 6px;

            .sender-username {
              font-size: 12px;
              font-weight: 600;
              color: #111827;
            }

            .role-pill {
              font-size: 10px;
              padding: 1px 6px;
              border-radius: 3px;
              font-weight: 600;

              &.referee {
                background: rgba(59, 130, 246, 0.12);
                color: #1d4ed8;
              }

              &.member {
                background: rgba(0, 0, 0, 0.06);
                color: #666666;
              }
            }

            .msg-time {
              font-size: 11px;
              color: #888888;
            }
          }

          .msg-bubble {
            padding: 8px 12px;
            border-radius: 6px;
            font-size: 13px;
            line-height: 1.5;
            word-break: break-word;
            background: #ffffff;
            border: 1px solid var(--v-border-subtle, rgba(0, 0, 0, 0.08));
            color: #1f2937;

            &.referee-bubble {
              background: #000000;
              color: #ffffff;
              border-color: #000000;
            }
          }
        }

        &.is-referee-sender {
          flex-direction: row-reverse;

          .msg-content-col {
            align-items: flex-end;

            .msg-meta-row {
              flex-direction: row-reverse;
            }
          }
        }
      }
    }

    // 控制台输入框
    .workspace-input-console {
      padding: 12px 18px;
      border-top: 1px solid var(--v-border-subtle, rgba(0, 0, 0, 0.08));
      background: var(--v-card-bg, #ffffff);
      display: flex;
      flex-direction: column;
      gap: 8px;

      .input-target-notice {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 12px;

        .notice-prefix {
          color: #444444;
        }

        .notice-badge {
          font-weight: 600;
          color: #111827;
        }

        .notice-tip {
          color: #888888;
          font-size: 11px;
        }
      }

      .input-textarea-row {
        display: flex;
        gap: 10px;
        align-items: flex-end;

        .admin-ticket-input {
          flex: 1;
        }

        .theme-send-btn {
          background: #000000;
          color: #ffffff;
          border: none;
          height: 38px;
          padding: 0 16px;
          font-weight: 500;

          &:hover {
            opacity: 0.85;
          }
        }
      }

      .input-bottom-hints {
        display: flex;
        justify-content: space-between;

        .hint {
          font-size: 11px;
          color: #444444;
        }
      }
    }
  }
}
</style>
