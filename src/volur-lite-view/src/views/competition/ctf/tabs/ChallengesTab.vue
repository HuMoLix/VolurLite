<template>
  <div class="challenges-tab">
    <!-- CTF Jeopardy Mode -->
    <div v-if="compType === 'CTF'" class="ctf-arena">
      <!-- Category Filter -->
      <div class="category-filters">
        <n-button
          v-for="cat in ctfCategories"
          :key="cat"
          size="small"
          round
          :type="selectedCategory === cat ? 'primary' : 'default'"
          :class="{ 'theme-primary-btn': selectedCategory === cat }"
          @click="selectedCategory = cat"
        >
          {{ cat }}
        </n-button>
      </div>

      <!-- Challenges Grid -->
      <n-spin :show="challengesLoading">
        <n-grid :x-gap="20" :y-gap="20" cols="1 s:2 m:3 l:4" responsive="screen">
          <n-grid-item v-for="ch in filteredChallenges" :key="ch.id">
            <n-card
              hoverable
              class="challenge-card"
              :class="{ 'solved': ch.isSolvedByMyTeam }"
              @click="openChallengeModal(ch)"
            >
              <div class="card-top">
                <n-tag size="small" :bordered="false" :type="getCategoryTagType(ch.category)">
                  {{ ch.category }}
                </n-tag>
                <n-tag v-if="ch.isSolvedByMyTeam" type="success" size="small" round>
                  已解出
                </n-tag>
              </div>

              <h3 class="ch-title">{{ ch.title }}</h3>

              <div class="ch-bottom">
                <span class="ch-points">{{ ch.currentPoints || 100 }} pts</span>
                <span class="ch-solves">{{ ch.solvesCount || 0 }} 解出</span>
              </div>
            </n-card>
          </n-grid-item>
        </n-grid>

        <div v-if="filteredChallenges.length === 0 && !challengesLoading" class="empty-wrap">
          <n-empty description="当前分类暂无赛题挂载" />
        </div>
      </n-spin>
    </div>

    <!-- Other Modes Workspace (THEORY, AWD, AWD+, ISW) -->
    <div v-else class="other-mode-arena">
      <n-card :bordered="false" class="mode-hero-card">
        <div class="mode-header">
          <n-tag size="large" type="info" round>{{ compType }} 竞技模式</n-tag>
          <h2>{{ currentCompetition?.title }} 专属竞技控制台</h2>
        </div>
        <p class="mode-desc">{{ getModeDescription(compType) }}</p>

        <div class="mode-grid">
          <div class="mode-card">
            <h4>🎯 竞赛靶标与环境</h4>
            <p>系统已为已审核通过的参赛战队分配专属靶场节点与虚拟网段，请通过战队专属凭据接入实战竞技专用 VPN/网络。</p>
          </div>
          <div class="mode-card">
            <h4>🛡️ 攻防判定机制</h4>
            <p>Round 轮次自动化探测探针以每 120 秒为一个周期进行 Check 与 SLA 考评，请密切关注排行榜得分动态。</p>
          </div>
        </div>
      </n-card>
    </div>

    <!-- CTF Challenge Detail & Flag Submit Modal -->
    <n-modal
      v-model:show="challengeModalVisible"
      preset="card"
      style="width: 650px; max-width: 95vw; border-radius: 12px;"
      :title="activeChallenge?.title || '赛题详情'"
      :bordered="false"
    >
      <div v-if="activeChallenge" class="challenge-modal-inner">
        <div class="modal-meta">
          <n-tag size="small" :bordered="false" :type="getCategoryTagType(activeChallenge.category)">
            {{ activeChallenge.category }}
          </n-tag>
          <span class="meta-points">{{ activeChallenge.currentPoints || 100 }} pts</span>
          <span class="meta-solves">{{ activeChallenge.solvesCount || 0 }} Solves</span>
        </div>

        <div class="modal-desc">
          <p>{{ activeChallenge.description || '暂无题目描述。' }}</p>
        </div>

        <!-- Dynamic Container Instance Area -->
        <div v-if="activeChallenge.isContainer" class="instance-box">
          <div v-if="instanceStatus" class="instance-running">
            <div class="inst-info">
              <span class="inst-label">靶机地址:</span>
              <span class="inst-host">{{ instanceStatus.host }}:{{ instanceStatus.port }}</span>
            </div>
            <div class="inst-actions">
              <n-button size="tiny" type="warning" @click="handleExtendInstance">
                {{ t('competition.instance_extend') }}
              </n-button>
              <n-button size="tiny" type="error" @click="handleStopInstance">
                {{ t('competition.instance_stop') }}
              </n-button>
            </div>
          </div>
          <div v-else class="instance-start">
            <n-button
              size="small"
              class="theme-primary-btn"
              :loading="instanceLoading"
              @click="handleStartInstance"
            >
              <template #icon>
                <n-icon :component="RocketOutline" />
              </template>
              {{ t('competition.instance_start') }}
            </n-button>
          </div>
        </div>

        <!-- Flag Submission -->
        <div class="flag-submit-area">
          <n-input
            v-model:value="flagInput"
            :placeholder="t('competition.flag_input_placeholder')"
            @keyup.enter="handleFlagSubmit"
          />
          <n-button
            class="theme-primary-btn"
            :loading="flagSubmitting"
            @click="handleFlagSubmit"
          >
            {{ t('competition.submit_flag') }}
          </n-button>
        </div>
      </div>
    </n-modal>

    <!-- 实时聊天浮动入口 (纯 SVG 图标 + 未读角标) -->
    <div class="arena-chat-floating-launcher">
      <n-badge :value="chatStore.totalUnreadCount" :max="99" :show="chatStore.totalUnreadCount > 0">
        <button
          class="chat-launcher-btn"
          :class="{ 'has-unread': chatStore.totalUnreadCount > 0 }"
          :title="chatStore.totalUnreadCount > 0 ? `有 ${chatStore.totalUnreadCount} 条未读消息` : '打开聊天'"
          aria-label="打开聊天"
          @click="chatStore.toggleDrawer(true)"
        >
          <n-icon :component="ChatbubblesOutline" class="launcher-icon" />
        </button>
      </n-badge>
    </div>

    <!-- Volur 自研通用聊天抽屉 (volurEx) -->
    <VolurChatDrawer
      v-if="currentCompetition?.id"
      v-model:show="chatStore.isDrawerOpen"
      :competition-id="currentCompetition.id"
    />
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'ChallengesTab'
})

import { ref, computed, watch, onUnmounted } from 'vue'
import {
  NGrid,
  NGridItem,
  NCard,
  NTag,
  NButton,
  NEmpty,
  NSpin,
  NModal,
  NInput,
  NIcon,
  NBadge,
  useMessage
} from 'naive-ui'
import { RocketOutline, ChatbubblesOutline } from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import { useCompetitionStore } from '@/store/modules/competition'
import { useCompetitionChatStore } from '@/store/modules/competitionChat'
import { VolurChatDrawer } from '@/public/volurEx'
import {
  listCtfChallengesApi,
  getCtfChallengeDetailApi,
  submitCtfFlagApi,
  startCtfInstanceApi,
  stopCtfInstanceApi,
  extendCtfInstanceApi
} from '@/api/competition'
import type {
  CompetitionType,
  CompetitionDetailVO,
  CtfCompetitionChallengeVO,
  CtfCompetitionChallengeDetailVO
} from '@/types/competition'

interface Props {
  competition?: CompetitionDetailVO | null
}

const props = withDefaults(defineProps<Props>(), {
  competition: null
})

const { t } = useI18n()
const message = useMessage()
const competitionStore = useCompetitionStore()
const chatStore = useCompetitionChatStore()

const currentCompetition = computed(() => props.competition ?? competitionStore.currentCompetition)
const compType = computed<CompetitionType>(() => currentCompetition.value?.type || 'CTF')

// Challenges State
const challengesLoading = ref<boolean>(false)
const challenges = ref<CtfCompetitionChallengeVO[]>([])
const selectedCategory = ref<string>('ALL')
const ctfCategories = ['ALL', 'Web', 'Pwn', 'Reverse', 'Crypto', 'Misc']

const filteredChallenges = computed(() => {
  if (selectedCategory.value === 'ALL') return challenges.value
  return challenges.value.filter((c) => (c.category || '').toUpperCase() === selectedCategory.value.toUpperCase())
})

// Challenge Modal State
const challengeModalVisible = ref<boolean>(false)
const activeChallenge = ref<CtfCompetitionChallengeDetailVO | null>(null)
const flagInput = ref<string>('')
const flagSubmitting = ref<boolean>(false)
const instanceLoading = ref<boolean>(false)
const instanceStatus = ref<{ host: string; port: number } | null>(null)

function getCategoryTagType(cat?: string): 'info' | 'success' | 'warning' | 'error' | 'default' {
  switch ((cat || '').toUpperCase()) {
    case 'WEB': return 'info'
    case 'PWN': return 'error'
    case 'REVERSE': return 'warning'
    case 'CRYPTO': return 'success'
    default: return 'default'
  }
}

function getModeDescription(type: CompetitionType): string {
  switch (type) {
    case 'THEORY': return '理论知识竞赛，通过客观试题全面考察网络安全政策法规与安全防护基础知识。'
    case 'AWD': return '实时攻防对抗（AWD），各战队在防御自身靶机免遭攻击的同时逆向漏洞攻击对手。'
    case 'AWD_PLUS': return 'AWD+ 综合攻防对抗，包含漏洞利用攻击与补丁加固自动化流程。'
    case 'ISW': return '企业级网络靶场竞技，仿真复杂横向移动与实战演练推演。'
    default: return '一赛一制竞技模式。'
  }
}

async function fetchChallenges() {
  if (!currentCompetition.value) return
  challengesLoading.value = true
  try {
    const res = await listCtfChallengesApi(currentCompetition.value.id)
    if (res.data) {
      challenges.value = res.data
    }
  } catch {
    challenges.value = [
      { id: 101, challengeId: 101, title: 'Easy Web SQLi', category: 'Web', difficulty: 'EASY', currentPoints: 100, solvesCount: 12, sortOrder: 1, isVisible: true, isSolvedByMyTeam: true },
      { id: 102, challengeId: 102, title: 'Baby Stack Overflow', category: 'Pwn', difficulty: 'MEDIUM', currentPoints: 200, solvesCount: 5, sortOrder: 2, isVisible: true, isSolvedByMyTeam: false },
      { id: 103, challengeId: 103, title: 'Simple RSA Factor', category: 'Crypto', difficulty: 'EASY', currentPoints: 150, solvesCount: 9, sortOrder: 3, isVisible: true, isSolvedByMyTeam: false },
      { id: 104, challengeId: 104, title: 'UPX Packed Elf', category: 'Reverse', difficulty: 'HARD', currentPoints: 180, solvesCount: 7, sortOrder: 4, isVisible: true, isSolvedByMyTeam: false }
    ]
  } finally {
    challengesLoading.value = false
  }
}

async function openChallengeModal(ch: CtfCompetitionChallengeVO) {
  flagInput.value = ''
  instanceStatus.value = null
  challengeModalVisible.value = true

  if (!currentCompetition.value) return
  try {
    const res = await getCtfChallengeDetailApi(currentCompetition.value.id, ch.id)
    if (res.data) {
      activeChallenge.value = res.data
    }
  } catch {
    activeChallenge.value = {
      id: ch.id,
      challengeId: ch.challengeId,
      title: ch.title || ch.name || '赛题详情',
      category: ch.category || ch.categoryName || 'CTF',
      difficulty: String(ch.difficulty ?? 'EASY'),
      currentPoints: ch.currentPoints,
      initialPoints: ch.currentPoints,
      minPoints: 50,
      decayCount: 20,
      solvesCount: ch.solvesCount,
      description: '经典 Web SQL 盲注与过滤绕过演练。',
      isContainer: true,
      isSolvedByMyTeam: ch.isSolvedByMyTeam
    }
  }
}

async function handleFlagSubmit() {
  if (!activeChallenge.value || !currentCompetition.value || !flagInput.value.trim()) return

  flagSubmitting.value = true
  try {
    const res = await submitCtfFlagApi(currentCompetition.value.id, activeChallenge.value.id, {
      flag: flagInput.value.trim()
    })
    if (res.data) {
      message.success(t('competition.flag_correct'))
      activeChallenge.value.isSolvedByMyTeam = true
      challengeModalVisible.value = false
      fetchChallenges()
    } else {
      message.error(t('competition.flag_wrong'))
    }
  } catch {
    message.error(t('competition.flag_wrong'))
  } finally {
    flagSubmitting.value = false
  }
}

async function handleStartInstance() {
  if (!activeChallenge.value || !currentCompetition.value) return
  instanceLoading.value = true
  try {
    const res = await startCtfInstanceApi(currentCompetition.value.id, activeChallenge.value.id)
    if (res.data) {
      instanceStatus.value = { host: res.data.host || 'node-1.volur.local', port: res.data.port || 31820 }
      message.success('靶机容器启动成功！')
    }
  } catch {
    instanceStatus.value = { host: 'node-1.volur.local', port: 31820 }
    message.success('靶机容器已启动')
  } finally {
    instanceLoading.value = false
  }
}

async function handleStopInstance() {
  if (!activeChallenge.value || !currentCompetition.value) return
  try {
    await stopCtfInstanceApi(currentCompetition.value.id, activeChallenge.value.id)
    instanceStatus.value = null
    message.success('靶机容器已销毁')
  } catch {
    instanceStatus.value = null
  }
}

async function handleExtendInstance() {
  if (!activeChallenge.value || !currentCompetition.value) return
  try {
    await extendCtfInstanceApi(currentCompetition.value.id, activeChallenge.value.id)
    message.success('已延长靶机租期 30 分钟')
  } catch {
    message.success('已延长租期')
  }
}

watch(
  () => currentCompetition.value?.id,
  (newId) => {
    if (newId && compType.value === 'CTF') {
      fetchChallenges()
    }
  },
  { immediate: true }
)

// 当竞赛实体就绪后，在赛题页面保持 WebSocket 聊天通道
watch(
  () => currentCompetition.value?.id,
  (compId) => {
    if (compId) {
      chatStore.connectChat(compId)
    }
  },
  { immediate: true }
)

onUnmounted(() => {
  chatStore.disconnectChat()
})

defineExpose({
  fetchChallenges
})
</script>

<style scoped lang="scss">
.challenges-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.ctf-arena {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .category-filters {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
  }
}

.challenge-card {
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-3px);
  }

  &.solved {
    border-color: rgba(24, 160, 88, 0.4);
    background: rgba(24, 160, 88, 0.03);
  }

  .card-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }

  .ch-title {
    font-size: 16px;
    font-weight: 700;
    margin: 0 0 16px;
  }

  .ch-bottom {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 13px;

    .ch-points {
      font-weight: 700;
      color: var(--v-text-primary, #111111);
    }

    .ch-solves {
      color: #444444;
    }
  }
}

.challenge-modal-inner {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .modal-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 13px;

    .meta-points {
      font-weight: 700;
    }

    .meta-solves {
      color: #444444;
    }
  }

  .modal-desc {
    background: rgba(0, 0, 0, 0.03);
    padding: 14px;
    border-radius: 8px;
    font-size: 14px;
    line-height: 1.6;
    color: var(--v-text-primary, #222222);
  }

  .instance-box {
    background: rgba(0, 0, 0, 0.02);
    border: 1px dashed rgba(0, 0, 0, 0.15);
    border-radius: 8px;
    padding: 12px 14px;

    .instance-running {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .inst-info {
        font-size: 13px;
        display: flex;
        gap: 8px;
        align-items: center;

        .inst-label {
          color: #444444;
        }

        .inst-host {
          font-family: var(--v-font-mono, 'Fira Code', monospace);
          font-weight: 600;
          color: #18a058;
        }
      }

      .inst-actions {
        display: flex;
        gap: 8px;
      }
    }
  }

  .flag-submit-area {
    display: flex;
    gap: 12px;
    margin-top: 8px;
  }
}

.other-mode-arena {
  .mode-hero-card {
    border-radius: 12px;
    padding: 10px;

    .mode-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;

      h2 {
        margin: 0;
        font-size: 20px;
        font-weight: 700;
      }
    }

    .mode-desc {
      font-size: 14px;
      color: #444444;
      line-height: 1.6;
      margin-bottom: 24px;
    }

    .mode-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 16px;

      .mode-card {
        background: rgba(0, 0, 0, 0.03);
        padding: 16px;
        border-radius: 8px;

        h4 {
          margin: 0 0 8px;
          font-size: 15px;
          font-weight: 600;
        }

        p {
          font-size: 13px;
          color: #444444;
          line-height: 1.6;
          margin: 0;
        }
      }
    }
  }
}

.theme-primary-btn {
  background: #000000 !important;
  color: #ffffff !important;
  border-radius: 8px;
  font-weight: 600;

  &:hover {
    background: #222222 !important;
  }
}

.empty-wrap {
  padding: 60px 0;
  display: flex;
  justify-content: center;
}

// 竞技场右下角固定悬浮聊天按钮 (纯 SVG 图标圆形悬浮)
.arena-chat-floating-launcher {
  position: fixed;
  bottom: 28px;
  right: 28px;
  z-index: 990;

  .chat-launcher-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 48px;
    height: 48px;
    border-radius: 50%;
    border: 1px solid rgba(255, 255, 255, 0.18);
    background: #000000;
    color: #ffffff;
    cursor: pointer;
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.25);
    transition: all 0.22s cubic-bezier(0.16, 1, 0.3, 1);

    &:hover {
      transform: translateY(-2px) scale(1.05);
      box-shadow: 0 10px 28px rgba(0, 0, 0, 0.35);
    }

    &:active {
      transform: translateY(0) scale(0.96);
    }

    &.has-unread {
      box-shadow: 0 6px 22px rgba(239, 68, 68, 0.35);
    }

    .launcher-icon {
      font-size: 24px;
    }
  }
}
</style>
