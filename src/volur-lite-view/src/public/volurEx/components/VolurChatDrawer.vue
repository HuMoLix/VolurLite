<template>
  <n-drawer
    :show="show"
    :width="drawerWidth"
    :placement="placement"
    class="volur-chat-drawer"
    :class="[appStore.theme]"
    :trap-focus="false"
    :block-scroll="false"
    @update:show="handleUpdateShow"
  >
    <n-drawer-content :native-scrollbar="false" body-content-class="volur-chat-body-content">
      <!-- 1. 抽屉顶栏 (Header: 无上滑切换，保留标题与连接状态) -->
      <template #header>
        <div class="chat-header-bar">
          <div class="header-left">
            <n-icon :component="ChatbubblesOutline" class="header-main-icon" />
            <span class="header-title">{{ title }}</span>
            <div class="connection-status-pill" :class="chatStore.status.toLowerCase()" :title="statusTooltip">
              <span class="status-dot" />
              <span class="status-label">{{ statusText }}</span>
            </div>
          </div>
          <div class="header-right">
            <n-button
              quaternary
              circle
              size="small"
              class="close-btn"
              @click="handleClose"
            >
              <template #icon>
                <n-icon :component="CloseOutline" />
              </template>
            </n-button>
          </div>
        </div>
      </template>

      <!-- 2. 消息流主展示区 (Message Feed) -->
      <div ref="messageFeedRef" class="chat-message-feed" @scroll="handleFeedScroll">
        <div v-if="chatStore.loadingHistory" class="history-loading-indicator">
          <n-spin size="small" />
          <span>正在同步历史记录...</span>
        </div>

        <!-- 空白提示 -->
        <div v-if="displayMessages.length === 0 && !chatStore.loadingHistory" class="chat-empty-box">
          <n-empty :description="emptyDescription">
            <template #icon>
              <n-icon :component="ChatbubblesOutline" :size="48" />
            </template>
          </n-empty>
        </div>

        <!-- 消息气泡列表 -->
        <div
          v-for="msg in displayMessages"
          :key="String(msg.id)"
          class="chat-message-item"
          :class="{
            'is-self': isSelfMessage(msg),
            'is-referee': isRefereeMessage(msg),
            'is-private': msg.channelType === 'PRIVATE',
            'is-mentioned-me': isMentionedMe(msg)
          }"
        >
          <!-- 头像（点击可在队伍频道中快捷 @ 队员） -->
          <div
            class="message-avatar-wrap"
            :title="canMention && !isSelfMessage(msg) && !isRefereeMessage(msg) ? `点击 @ ${msg.senderName}` : undefined"
            @click="handleAvatarClick(msg)"
          >
            <AppAvatar
              :src="msg.senderAvatar || undefined"
              :name="msg.senderName"
              :username="msg.senderName"
              size="small"
            />
          </div>

          <!-- 消息体 -->
          <div class="message-body-wrap">
            <div class="message-meta">
              <span
                class="sender-name"
                :class="{ clickable: canMention && !isSelfMessage(msg) && !isRefereeMessage(msg) }"
                @click="handleAvatarClick(msg)"
              >
                {{ msg.senderName }}
              </span>

              <!-- 角色标识（取消“队长”及普通队员标识，仅保留裁判组等官方标识） -->
              <span
                v-if="formatRoleName(msg.senderRole)"
                class="role-pill"
                :class="getRolePillClass(msg.senderRole)"
              >
                {{ formatRoleName(msg.senderRole) }}
              </span>

              <!-- 私信标记 -->
              <span v-if="msg.channelType === 'PRIVATE'" class="private-tag">
                <n-icon :component="LockClosedOutline" />
                <span>私信</span>
              </span>

              <!-- 被提及通知标记 -->
              <span v-if="isMentionedMe(msg)" class="mentioned-tag">
                @提及了我
              </span>

              <span class="message-time">{{ formatTime(msg.createdAt) }}</span>
            </div>

            <!-- 消息气泡 -->
            <div class="message-bubble" :class="[msg.msgType.toLowerCase()]">
              <!-- 文本消息：高亮 @提及 的队员并保留换行 -->
              <div class="text-message-content">
                <template v-for="(segment, idx) in renderMessageSegments(msg.content)" :key="idx">
                  <span v-if="segment.isMention" class="mention-highlight">{{ segment.text }}</span>
                  <template v-else>{{ segment.text }}</template>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 快捷回到底部悬浮药丸 -->
      <transition name="fade">
        <button v-if="showScrollToBottom" class="scroll-bottom-pill" @click="scrollToBottom(true)">
          <n-icon :component="ArrowDownOutline" />
          <span>回到底部</span>
        </button>
      </transition>

      <!-- 3. 底部发信与目标选择工具栏 (Input Footer) -->
      <template #footer>
        <div class="chat-input-toolbar">
          <!-- 上方控制条：沟通对象触发器 (名称 + 小箭头，无输入框) -->
          <div class="toolbar-target-row">
            <div class="target-selector-wrap">
              <span class="selector-prefix-label">发送给:</span>
              <n-dropdown
                trigger="click"
                :options="targetDropdownOptions"
                placement="top-start"
                @select="handleTargetDropdownSelect"
              >
                <button class="target-dropdown-trigger-btn" type="button" title="点击切换沟通对象">
                  <span class="target-display-name">{{ currentTargetDisplayName }}</span>
                  <n-icon :component="ChevronDownOutline" class="target-dropdown-arrow" />
                </button>
              </n-dropdown>
            </div>
          </div>

          <!-- 调整后尺寸的输入区域 (集成输入 @ 自动弹出队员小列表的 NMention) -->
          <div class="input-box-wrapper">
            <n-mention
              ref="mentionRef"
              v-model:value="inputContent"
              type="textarea"
              :options="canMention ? mentionCandidateOptions : []"
              :autosize="{ minRows: 3, maxRows: 5 }"
              :placeholder="inputPlaceholder"
              :disabled="!isChatReady"
              class="chat-mention-textarea"
              @keydown="handleKeyDown"
            />

            <button
              class="volur-chat-send-btn"
              :disabled="!canSend"
              @click="handleSend"
            >
              <n-icon :component="PaperPlaneOutline" />
              <span>发送</span>
            </button>
          </div>

          <div class="input-bottom-tips">
            <span class="hint-text">按 Enter 发送 / Shift+Enter 换行</span>
            <span v-if="canMention" class="hint-text mention-hint">输入 @ 可弹出队员列表</span>
          </div>
        </div>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup lang="ts">
defineOptions({
  name: 'VolurChatDrawer'
})

import { ref, computed, watch, nextTick, onMounted } from 'vue'
import {
  NDrawer,
  NDrawerContent,
  NButton,
  NIcon,
  NMention,
  NDropdown,
  NEmpty,
  NSpin,
  useMessage,
  type DropdownOption,
  type MentionOption
} from 'naive-ui'
import {
  ChatbubblesOutline,
  CloseOutline,
  LockClosedOutline,
  PaperPlaneOutline,
  ArrowDownOutline,
  ChevronDownOutline
} from '@vicons/ionicons5'
import AppAvatar from '@/components/common/AppAvatar.vue'
import { isSelfMemberIdentity, getCurrentAuthCandidateIdentities } from '@/utils/auth'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { useCompetitionStore } from '@/store/modules/competition'
import { useCompetitionChatStore } from '@/store/modules/competitionChat'
import type { ChatMessageVO } from '@/types/competition/chat'

export interface VolurChatDrawerProps {
  /** 控制抽屉显示 */
  show: boolean
  /** 当前竞赛唯一标识 ID 或 slug */
  competitionId: number | string
  /** 抽屉宽度 */
  width?: number | string
  /** 抽屉标题 */
  title?: string
  /** 弹出方位 */
  placement?: 'right' | 'left'
}

const props = withDefaults(defineProps<VolurChatDrawerProps>(), {
  width: 500,
  title: '聊天',
  placement: 'right'
})

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'messageSent', message: ChatMessageVO): void
}>()

const message = useMessage()
const appStore = useAppStore()
const userStore = useUserStore()
const competitionStore = useCompetitionStore()
const chatStore = useCompetitionChatStore()

// 严格判定目标成员是否为当前选手本人 (聚合 Token, LocalStorage, UserStore, ReadyData 多源全量 ID 与用户名比对)
function isSelfMember(target: { userId?: string | number | null; id?: string | number | null; username?: string | null } | null | undefined): boolean {
  return isSelfMemberIdentity(target, userStore.user, chatStore.readyData)
}

// 响应式状态
const messageFeedRef = ref<HTMLElement | null>(null)
const mentionRef = ref<InstanceType<typeof NMention> | null>(null)
const inputContent = ref<string>('')
const showScrollToBottom = ref<boolean>(false)
const userScrolledUp = ref<boolean>(false)

// 抽屉宽度响应式计算
const drawerWidth = computed(() => {
  if (typeof window !== 'undefined' && window.innerWidth <= 640) {
    return '100%'
  }
  return props.width
})

// 连接状态描述文案
const statusText = computed(() => {
  switch (chatStore.status) {
    case 'CONNECTED':
      return '实时在线'
    case 'CONNECTING':
      return '正在连接...'
    case 'RECONNECTING':
      return '重连中...'
    case 'CLOSING':
      return '断开中...'
    case 'DISCONNECTED':
    default:
      return '未连接'
  }
})

const statusTooltip = computed(() => {
  if (chatStore.status === 'CONNECTED') {
    return 'WebSocket 通道通畅，消息毫秒级推送'
  }
  return '连接已断开，系统将根据指数退避策略自动重连'
})

// 当前沟通目标展示名称 (纯文本无图标)
const currentTargetDisplayName = computed(() => {
  const target = chatStore.currentTarget
  const teamName = chatStore.readyData?.teamName || competitionStore.myTeam?.teamName || '战队公屏'

  if (target === 'TEAM') {
    return teamName
  }
  if (target.startsWith('USER_')) {
    const targetUserId = target.replace('USER_', '')
    const member = chatStore.teammateList.find((m) => String(m.userId) === targetUserId)
    return member ? member.username : '队员'
  }
  if (target.startsWith('REFEREE')) {
    if (chatStore.isReferee && chatStore.selectedRefereeTeamId) {
      const team = chatStore.refereeTeams.find(
        (t) => String(t.teamId) === String(chatStore.selectedRefereeTeamId)
      )
      return team ? team.teamName : '裁判'
    }
    return '裁判'
  }
  return teamName
})

// 沟通对象下拉选项构建 (彻底移除所有图标，仅纯文本显示：队伍名称、用户名称、裁判)
const targetDropdownOptions = computed<DropdownOption[]>(() => {
  const options: DropdownOption[] = []
  const teamName = chatStore.readyData?.teamName || competitionStore.myTeam?.teamName || '战队公屏'

  // 1. 队伍名称
  const unreadTeam = chatStore.unreadTeamCount > 0 ? ` (${chatStore.unreadTeamCount})` : ''
  options.push({
    key: 'TEAM',
    label: `${teamName}${unreadTeam}`
  })

  // 2. 用户名称 (同队其他队员，必须严格排除自己)
  if (chatStore.teammateList.length > 0) {
    chatStore.teammateList.forEach((m) => {
      if (isSelfMember(m)) {
        return
      }
      const unreadCount = chatStore.unreadPrivateDetails[`USER_${m.userId}`] || 0
      const unreadTag = unreadCount > 0 ? ` (${unreadCount})` : ''
      options.push({
        key: `USER_${m.userId}`,
        label: `${m.username}${unreadTag}`
      })
    })
  }

  // 3. 裁判申诉工单 (普通选手与裁判沟通)
  const unreadReferee = chatStore.unreadRefereeCount > 0 ? ` (${chatStore.unreadRefereeCount})` : ''
  options.push({
    key: 'REFEREE',
    label: `裁判${unreadReferee}`
  })

  return options
})

// 是否可在当前频道 @ 队员（仅在战队公屏生效）
const canMention = computed(() => {
  return chatStore.currentTarget === 'TEAM'
})

// 输入 @ 弹出的小列表候选项 (纯文本用户名，严格排除自己)
const mentionCandidateOptions = computed<MentionOption[]>(() => {
  return chatStore.teammateList
    .filter((m) => !isSelfMember(m))
    .map((m) => ({
      label: m.username,
      value: m.username
    }))
})

// 当前展示的消息列表
const displayMessages = computed(() => chatStore.currentMessages)

// 输入框 placeholder
const inputPlaceholder = computed(() => {
  const target = chatStore.currentTarget
  if (target === 'TEAM') {
    return '在战队公屏发言，所有队友可见（支持输入 @ 提及队友）...'
  }
  if (target.startsWith('USER_')) {
    const targetUserId = target.replace('USER_', '')
    const member = chatStore.teammateList.find((m) => String(m.userId) === targetUserId)
    return `正在私信 @${member?.username || '队友'}，仅双方可见...`
  }
  if (target.startsWith('REFEREE')) {
    return chatStore.isReferee
      ? '回复该队伍提问/申诉，按 Enter 发送...'
      : '向竞赛裁判组发起提问或申诉，裁判将实时收到...'
  }
  return '输入消息内容，按 Enter 发送...'
})

// 发送可用性
const isChatReady = computed(() => {
  return chatStore.status === 'CONNECTED' || true
})

const canSend = computed(() => {
  return isChatReady.value && inputContent.value.trim().length > 0
})

const emptyDescription = computed(() => {
  const target = chatStore.currentTarget
  if (target === 'TEAM') {
    return '战队公屏暂无消息，开始与队员打个招呼吧！'
  }
  if (target.startsWith('USER_')) {
    return '暂无私信记录，发送一条消息开始单独沟通'
  }
  if (target.startsWith('REFEREE')) {
    return chatStore.isReferee
      ? '暂无该队伍申诉记录'
      : '暂无裁判沟通记录。若遇题目争议或环境故障，可在此联系裁判组'
  }
  return '暂无消息记录'
})

// ==========================================
// 交互逻辑
// ==========================================

function handleUpdateShow(val: boolean) {
  emit('update:show', val)
  chatStore.toggleDrawer(val)
}

function handleClose() {
  handleUpdateShow(false)
}

function handleTargetDropdownSelect(key: string | number) {
  const val = String(key)
  if (val.startsWith('USER_')) {
    const targetUserId = val.replace('USER_', '')
    const targetMember = chatStore.teammateList.find((m) => String(m.userId) === targetUserId)
    if (isSelfMember({ userId: targetUserId, username: targetMember?.username })) {
      message.warning('不能向自己发起私信')
      return
    }
  }
  if (val.startsWith('REFEREE_TEAM_')) {
    const teamId = val.replace('REFEREE_TEAM_', '')
    chatStore.selectRefereeTeam(teamId)
    chatStore.switchTarget('REFEREE')
  } else {
    chatStore.switchTarget(val)
  }
  nextTick(() => {
    scrollToBottom(false)
  })
}

function handleAvatarClick(msg: ChatMessageVO) {
  // 如果是自己或裁判，不响应（队伍不能在队伍频道中 @ 裁判）
  if (isSelfMessage(msg) || isRefereeMessage(msg)) return

  if (canMention.value) {
    insertMentionText(msg.senderName)
  }
}

function insertMentionText(username: string) {
  if (!username) return
  const mentionToken = `@${username} `
  inputContent.value = `${inputContent.value}${mentionToken}`
  nextTick(() => {
    mentionRef.value?.focus()
  })
}

function handleKeyDown(e: KeyboardEvent) {
  // 如果已经被 NMention 下拉菜单捕获确认，不触发发送
  if (e.defaultPrevented) return

  // Enter 发送，Shift+Enter 或 Ctrl+Enter 换行
  if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handleSend()
  }
}

async function handleSend() {
  if (!canSend.value) return

  // 校验私信目标不能为自己
  if (chatStore.currentTarget.startsWith('USER_')) {
    const targetUserId = chatStore.currentTarget.replace('USER_', '')
    const targetMember = chatStore.teammateList.find((m) => String(m.userId) === targetUserId)
    if (isSelfMember({ userId: targetUserId, username: targetMember?.username })) {
      message.warning('不能向自己发送私信')
      return
    }
  }

  const rawText = inputContent.value
  inputContent.value = ''

  // 解析被 @ 提及的队员 ID
  let mentionIds: (string | number)[] | undefined = undefined
  if (canMention.value && chatStore.teammateList.length > 0) {
    const ids: (string | number)[] = []
    chatStore.teammateList.forEach((m) => {
      if (rawText.includes(`@${m.username}`)) {
        ids.push(m.userId)
      }
    })
    if (ids.length > 0) {
      mentionIds = ids
    }
  }

  try {
    await chatStore.sendMessage(rawText, 'TEXT', mentionIds)
    nextTick(() => {
      scrollToBottom(true)
    })
  } catch (err: any) {
    message.error(err.message || '消息发送失败')
    inputContent.value = rawText // 失败时恢复内容
  }
}

function isSelfMessage(msg: ChatMessageVO): boolean {
  if (!msg) return false
  return isSelfMember({
    userId: msg.senderId,
    id: msg.senderId,
    username: msg.senderName
  })
}

function isRefereeMessage(msg: ChatMessageVO): boolean {
  const r = (msg.senderRole || '').toUpperCase()
  return r === 'REFEREE' || r === 'ADMIN' || r === 'SUPER_ADMIN'
}

function isMentionedMe(msg: ChatMessageVO): boolean {
  if (!msg.mentionUserIds || isSelfMessage(msg)) return false
  const { candidateIds, candidateUsernames } = getCurrentAuthCandidateIdentities(userStore.user, chatStore.readyData)
  const mentioned = msg.mentionUserIds.split(',').map((s) => s.trim().toLowerCase())
  for (const cid of candidateIds) {
    if (cid && mentioned.includes(cid.toLowerCase())) {
      return true
    }
  }
  for (const cname of candidateUsernames) {
    if (cname && mentioned.includes(cname)) {
      return true
    }
  }
  return false
}

function formatRoleName(role?: string): string {
  if (!role) return ''
  const upper = role.toUpperCase()
  if (upper === 'REFEREE' || upper === 'ADMIN' || upper === 'SUPER_ADMIN') {
    return '裁判组'
  }
  // 取消聊天框中的“队长”与普通队员标识，仅裁判组展示官方标识
  return ''
}

function getRolePillClass(role?: string): string {
  if (!role) return ''
  const upper = role.toUpperCase()
  if (upper === 'REFEREE' || upper === 'ADMIN' || upper === 'SUPER_ADMIN') {
    return 'role-referee'
  }
  return ''
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
    return isoStr
  }
}

/**
 * 将带有 @username 的消息文本切分成段落，以便高亮渲染
 */
function renderMessageSegments(content: string) {
  if (!content) return []
  const regex = /(@[^\s@]+)/g
  const segments: { text: string; isMention: boolean }[] = []
  let lastIndex = 0
  let match: RegExpExecArray | null

  while ((match = regex.exec(content)) !== null) {
    if (match.index > lastIndex) {
      segments.push({
        text: content.substring(lastIndex, match.index),
        isMention: false
      })
    }
    segments.push({
      text: match[0],
      isMention: true
    })
    lastIndex = regex.lastIndex
  }

  if (lastIndex < content.length) {
    segments.push({
      text: content.substring(lastIndex),
      isMention: false
    })
  }

  return segments
}

function handleFeedScroll() {
  if (!messageFeedRef.value) return
  const el = messageFeedRef.value
  const distanceFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight
  userScrolledUp.value = distanceFromBottom > 120
  showScrollToBottom.value = distanceFromBottom > 180
}

function scrollToBottom(smooth = false) {
  if (!messageFeedRef.value) return
  const el = messageFeedRef.value
  if (smooth) {
    el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
  } else {
    el.scrollTop = el.scrollHeight
  }
  userScrolledUp.value = false
  showScrollToBottom.value = false
}

// 监听新消息到达：如果未主动往上翻，自动平滑滚动到底部
watch(
  () => displayMessages.value.length,
  () => {
    if (!userScrolledUp.value) {
      nextTick(() => {
        scrollToBottom(true)
      })
    }
  }
)

// 抽屉打开时联动初始化
watch(
  () => props.show,
  (isOpen) => {
    if (isOpen && props.competitionId) {
      chatStore.connectChat(props.competitionId)
      nextTick(() => {
        scrollToBottom(false)
      })
    }
  },
  { immediate: true }
)

// 防御性纠正：若当前私信目标刚好为自己，立即重置回全队公屏
watch(
  [() => chatStore.currentTarget, () => chatStore.readyData, () => userStore.user],
  ([target]) => {
    if (target.startsWith('USER_')) {
      const targetUserId = target.replace('USER_', '')
      const targetMember = chatStore.teammateList.find((m) => String(m.userId) === targetUserId)
      if (isSelfMember({ userId: targetUserId, username: targetMember?.username })) {
        chatStore.switchTarget('TEAM')
      }
    }
  },
  { immediate: true }
)

onMounted(() => {
  if (props.show && props.competitionId) {
    chatStore.connectChat(props.competitionId)
  }
})
</script>

<style scoped lang="scss">
.volur-chat-drawer {
  :deep(.n-drawer-header) {
    padding: 14px 20px;
    border-bottom: 1px solid var(--v-border-subtle, rgba(0, 0, 0, 0.08));
  }

  :deep(.n-drawer-footer) {
    padding: 12px 18px 16px;
    border-top: 1px solid var(--v-border-subtle, rgba(0, 0, 0, 0.08));
    background: var(--v-bg-card, #ffffff);
  }
}

// 顶栏
.chat-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;

  .header-left {
    display: flex;
    align-items: center;
    gap: 8px;

    .header-main-icon {
      font-size: 20px;
      color: var(--v-primary, #000000);
    }

    .header-title {
      font-size: 16px;
      font-weight: 700;
      color: var(--v-text-primary, #111827);
    }

    .connection-status-pill {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 2px 7px;
      border-radius: 999px;
      font-size: 11px;
      font-weight: 500;
      background: rgba(0, 0, 0, 0.04);
      color: #666;

      .status-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: #9ca3af;
      }

      &.connected {
        background: rgba(16, 185, 129, 0.1);
        color: #059669;

        .status-dot {
          background: #10b981;
          box-shadow: 0 0 6px rgba(16, 185, 129, 0.6);
        }
      }

      &.connecting,
      &.reconnecting {
        background: rgba(245, 158, 11, 0.1);
        color: #d97706;

        .status-dot {
          background: #f59e0b;
          animation: pulse-dot 1.5s infinite;
        }
      }

      &.disconnected {
        background: rgba(239, 68, 68, 0.1);
        color: #dc2626;

        .status-dot {
          background: #ef4444;
        }
      }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 8px;

    .close-btn {
      color: #666;
      &:hover {
        color: #000;
      }
    }
  }
}

// 消息流展示区
.chat-message-feed {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 250px;

  .history-loading-indicator {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    font-size: 12px;
    color: #888;
    padding: 6px 0;
  }

  .chat-empty-box {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    min-height: 260px;
    color: #999;
  }

  .chat-message-item {
    display: flex;
    gap: 10px;
    max-width: 86%;

    .message-avatar-wrap {
      flex-shrink: 0;
      padding-top: 2px;
      cursor: pointer;
    }

    .message-body-wrap {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .message-meta {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 12px;

        .sender-name {
          font-weight: 600;
          color: #333333;

          &.clickable {
            cursor: pointer;
            &:hover {
              text-decoration: underline;
              color: #2563eb;
            }
          }
        }

        .role-pill {
          font-size: 10px;
          padding: 1px 6px;
          border-radius: 4px;
          font-weight: 600;

          &.role-captain {
            background: rgba(245, 158, 11, 0.12);
            color: #b45309;
          }

          &.role-referee {
            background: rgba(59, 130, 246, 0.12);
            color: #1d4ed8;
          }

          &.role-leader {
            background: rgba(16, 185, 129, 0.12);
            color: #047857;
          }

          &.role-member {
            background: rgba(0, 0, 0, 0.05);
            color: #666;
          }
        }

        .private-tag {
          display: inline-flex;
          align-items: center;
          gap: 2px;
          font-size: 10px;
          padding: 1px 5px;
          border-radius: 4px;
          background: rgba(139, 92, 246, 0.12);
          color: #7c3aed;
          font-weight: 600;
        }

        .mentioned-tag {
          font-size: 10px;
          padding: 1px 5px;
          border-radius: 4px;
          background: #ef4444;
          color: #ffffff;
          font-weight: 700;
        }

        .message-time {
          font-size: 11px;
          color: #999999;
        }
      }

      .message-bubble {
        padding: 10px 14px;
        border-radius: 12px;
        border-top-left-radius: 2px;
        font-size: 13.5px;
        line-height: 1.5;
        word-break: break-word;
        background: #f3f4f6;
        color: #1f2937;
        box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);

        .text-message-content {
          white-space: pre-wrap;

          .mention-highlight {
            color: #2563eb;
            font-weight: 600;
            background: rgba(37, 99, 235, 0.08);
            padding: 1px 4px;
            border-radius: 3px;
          }
        }
      }
    }

    // 自己发送的消息：右对齐
    &.is-self {
      align-self: flex-end;
      flex-direction: row-reverse;

      .message-body-wrap {
        align-items: flex-end;

        .message-meta {
          flex-direction: row-reverse;
        }

        .message-bubble {
          border-top-left-radius: 12px;
          border-top-right-radius: 2px;
          // Volur 视觉铁律：纯黑底色纯白字
          background: #000000;
          color: #ffffff;

          .text-message-content {
            color: #ffffff;

            .mention-highlight {
              color: #93c5fd;
              background: rgba(255, 255, 255, 0.15);
            }
          }
        }
      }
    }

    // 被 @ 提及的消息：特别金色/红色左边框提示
    &.is-mentioned-me:not(.is-self) {
      .message-body-wrap .message-bubble {
        border-left: 3px solid #ef4444;
        background: #fff5f5;
      }
    }

    // 裁判组消息：突出蓝调
    &.is-referee:not(.is-self) {
      .message-body-wrap .message-bubble {
        background: #eff6ff;
        border: 1px solid rgba(59, 130, 246, 0.2);
        color: #1e3a8a;
      }
    }
  }
}

// 悬浮回到底部药丸
.scroll-bottom-pill {
  position: absolute;
  bottom: 150px;
  right: 24px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  background: #000000;
  color: #ffffff;
  border: none;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.25);
  }
}

// 底部发信区域 (包含目标选择器与拉高的输入框)
.chat-input-toolbar {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;

  .toolbar-target-row {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 8px;

    .target-selector-wrap {
      display: flex;
      align-items: center;
      gap: 6px;

      .selector-prefix-label {
        font-size: 12px;
        font-weight: 500;
        color: #666666;
        white-space: nowrap;
      }

      .target-dropdown-trigger-btn {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        padding: 2px 8px;
        border-radius: 6px;
        border: 1px solid transparent;
        background: transparent;
        cursor: pointer;
        transition: all 0.16s ease;

        &:hover {
          background: rgba(0, 0, 0, 0.05);
        }

        .target-display-name {
          font-size: 12.5px;
          font-weight: 400;
          color: #000000;
        }

        .target-dropdown-arrow {
          font-size: 13px;
          color: #666666;
          transition: transform 0.2s ease;
        }
      }
    }
  }

  // 尺寸调整后的输入框布局 (NMention + 紧凑发送按钮)
  .input-box-wrapper {
    display: flex;
    gap: 10px;
    align-items: flex-end;

    .chat-mention-textarea {
      flex: 1;
      border-radius: 8px;

      :deep(.n-input) {
        border-radius: 8px;
      }

      :deep(.n-input__textarea-el) {
        min-height: 76px;
        line-height: 1.5;
        font-size: 13.5px;
      }
    }

    .volur-chat-send-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      padding: 0 16px;
      height: 38px;
      border-radius: 8px;
      border: none;
      // 视觉铁律：纯黑底色纯白字
      background: #000000;
      color: #ffffff;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.15s ease;
      white-space: nowrap;

      &:hover:not(:disabled) {
        opacity: 0.88;
        transform: translateY(-1px);
      }

      &:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }
    }
  }

  .input-bottom-tips {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 11px;
    color: #888888;

    .mention-hint {
      color: #2563eb;
    }
  }
}

// 黑夜模式沉浸式适配
.volur-chat-drawer.dark {
  .chat-header-bar {
    .header-left {
      .header-title {
        color: #ffffff;
      }
    }

  }

  .chat-message-feed {
    .chat-message-item {
      .message-body-wrap {
        .message-meta {
          .sender-name {
            color: #eeeeee;
          }
          .message-time {
            color: #777777;
          }
          .role-pill.role-member {
            background: rgba(255, 255, 255, 0.08);
            color: #bbb;
          }
        }

        .message-bubble {
          background: #242429;
          color: #e5e5e5;

          .text-message-content .mention-highlight {
            color: #60a5fa;
            background: rgba(96, 165, 250, 0.12);
          }
        }
      }

      &.is-self .message-body-wrap .message-bubble {
        background: #ffffff;
        color: #000000;

        .text-message-content {
          color: #000000;

          .mention-highlight {
            color: #1d4ed8;
            background: rgba(0, 0, 0, 0.08);
          }
        }
      }

      &.is-mentioned-me:not(.is-self) .message-body-wrap .message-bubble {
        background: #2b1d1d;
        border-left-color: #f87171;
      }

      &.is-referee:not(.is-self) .message-body-wrap .message-bubble {
        background: #1e293b;
        border-color: rgba(96, 165, 250, 0.3);
        color: #93c5fd;
      }
    }
  }

  .chat-input-toolbar {
    .toolbar-target-row {
      .target-selector-wrap {
        .selector-prefix-label {
          color: #888888;
        }
        .target-dropdown-trigger-btn {
          &:hover {
            background: rgba(255, 255, 255, 0.08);
          }
          .target-display-name {
            font-weight: 400;
            color: #ffffff;
          }
          .target-dropdown-arrow {
            color: #aaaaaa;
          }
        }
      }
    }

    .input-box-wrapper .volur-chat-send-btn {
      background: #ffffff;
      color: #000000;
    }
  }
}

@keyframes pulse-dot {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.7);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 6px rgba(245, 158, 11, 0);
  }
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(245, 158, 11, 0);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
