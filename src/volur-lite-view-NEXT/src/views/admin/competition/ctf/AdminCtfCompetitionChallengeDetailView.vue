<template>
  <div class="space-y-6">
    <div class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-4">
      <div class="flex items-center justify-between border-b border-google-border-light dark:border-google-border-dark pb-3">
        <div>
          <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
            赛题配置
          </h3>
          <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">
            配置本场竞赛题目解题得分规则与一二三血奖励机制
          </p>
        </div>
        <VBtn
          clz="primary"
          size="medium"
          :left-icon="SaveOutline"
          :loading="savingConfig"
          @click="handleSaveConfig"
        >
          保存配置
        </VBtn>
      </div>

      <div class="space-y-4">
        <div class="flex items-center justify-between py-1">
          <div>
            <div class="text-xs font-medium text-google-text-primary dark:text-google-text-darkPrimary">
              一二三血奖励机制
            </div>
            <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">
              开启后，每道赛题前三名解出的队伍将分别获得额外奖励积分
            </div>
          </div>
          <NSwitch v-model:value="bloodConfig.enableBloodBonus" />
        </div>

        <div v-if="bloodConfig.enableBloodBonus" class="grid grid-cols-1 md:grid-cols-3 gap-4 pt-2 border-t border-google-border-light dark:border-google-border-dark">
          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              一血奖励比例 (%) *
            </label>
            <VInput
              v-model:value="bloodConfig.firstBloodBonusPercentage"
              placeholder="10"
              clearable
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              二血奖励比例 (%) *
            </label>
            <VInput
              v-model:value="bloodConfig.secondBloodBonusPercentage"
              placeholder="5"
              clearable
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              三血奖励比例 (%) *
            </label>
            <VInput
              v-model:value="bloodConfig.thirdBloodBonusPercentage"
              placeholder="2"
              clearable
            />
          </div>
        </div>
      </div>
    </div>

    <div class="space-y-4">
      <div class="flex items-center justify-between">
        <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.competitions.mounted_challenges', '赛题列表') }}
        </h3>
        <VBtn
          clz="primary"
          size="medium"
          :left-icon="AddOutline"
          @click="openMountModal"
        >
          {{ t('admin.competitions.mount_challenge_btn', '添加题目') }}
        </VBtn>
      </div>

      <VTable
        :columns="mountedChallengeColumns"
        :data="paginatedChallenges"
        :loading="loadingChallenges"
        :pagination="false"
        size="small"
        :bordered="false"
      >
        <template #empty>
          <div class="py-12 text-center text-xs text-google-text-secondary dark:text-google-text-darkSecondary">
            暂无挂载赛题，请点击右上角添加题目
          </div>
        </template>
      </VTable>

      <div v-if="mountedChallenges.length > 0" class="flex justify-end pt-2">
        <VPagination
          v-model:page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :item-count="mountedChallenges.length"
          :page-sizes="[10, 20, 50]"
          show-size-picker
        />
      </div>
    </div>

    <VModal
      v-model:show="showMountModal"
      :title="t('admin.competitions.mount_challenge_title', '添加题目至竞赛')"
      :width="540"
    >
      <div class="space-y-4">
        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            选择题库赛题 *
          </label>
          <VSelection
            v-model:value="mountForm.challengeId"
            :options="repoChallengeOptions"
            placeholder="请选择题库中的赛题"
            to="body"
            :z-index="3000"
          />
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              初始分数 (Pts)
            </label>
            <VInput
              v-model:value="mountForm.initialPoints"
              placeholder="1000"
              clearable
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              衰减底分 (Min)
            </label>
            <VInput
              v-model:value="mountForm.minPoints"
              placeholder="100"
              clearable
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              衰减梯度 (Decay)
            </label>
            <VInput
              v-model:value="mountForm.decayCount"
              placeholder="30"
              clearable
            />
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <VBtn clz="primary" size="medium" :loading="mounting" @click="handleConfirmMount">
            {{ t('admin.competitions.add', '添加') }}
          </VBtn>
        </div>
      </template>
    </VModal>

    <VModal
      v-model:show="showEditModal"
      title="编辑赛题分值配置"
      :width="540"
    >
      <div class="space-y-4">
        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            题目名称
          </label>
          <div class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary py-1">
            {{ editForm.challengeName }}
          </div>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              初始分数 (Pts) *
            </label>
            <VInput
              v-model:value="editForm.initialPoints"
              placeholder="1000"
              clearable
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              衰减底分 (Min) *
            </label>
            <VInput
              v-model:value="editForm.minPoints"
              placeholder="100"
              clearable
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              衰减梯度 (Decay) *
            </label>
            <VInput
              v-model:value="editForm.decayCount"
              placeholder="30"
              clearable
            />
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <VBtn clz="primary" size="medium" :loading="updatingChallenge" @click="handleConfirmEdit">
            {{ t('common.save', '保存') }}
          </VBtn>
        </div>
      </template>
    </VModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessage, useDialog, NSwitch, type DataTableColumns } from 'naive-ui'
import { AddOutline, TrashOutline, PencilOutline, SaveOutline } from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VInput from '@/components/common/VInput.vue'
import VSelection from '@/components/common/VSelection.vue'
import VTable from '@/components/common/VTable.vue'
import VModal from '@/components/common/VModal.vue'
import VPagination from '@/components/common/VPagination.vue'
import type { CtfCompetitionChallengeVO } from '@/types/competition'
import {
  listMountedCtfChallengesApi,
  mountCtfChallengeApi,
  updateMountedCtfChallengeApi,
  unmountCtfChallengeApi,
  getAdminCompetitionByIdApi,
  updateCompetitionApi
} from '@/api/competition'
import { getAdminCtfChallengeListApi } from '@/api/challenge'
import type { CtfChallengeListVO } from '@/types/challenge'

defineOptions({
  name: 'AdminCtfCompetitionChallengeDetailView'
})

const props = defineProps<{
  compId: string | number
}>()

const { t } = useI18n()
const message = useMessage()
const dialog = useDialog()

const bloodConfig = ref({
  enableBloodBonus: true,
  firstBloodBonusPercentage: '10',
  secondBloodBonusPercentage: '5',
  thirdBloodBonusPercentage: '2'
})
const savingConfig = ref(false)

const mountedChallenges = ref<CtfCompetitionChallengeVO[]>([])
const loadingChallenges = ref(false)
const showMountModal = ref(false)
const repoChallenges = ref<CtfChallengeListVO[]>([])
const mounting = ref(false)
const mountForm = ref({
  challengeId: '',
  initialPoints: '1000',
  minPoints: '100',
  decayCount: '30'
})

const showEditModal = ref(false)
const updatingChallenge = ref(false)
const editForm = ref({
  id: '' as string | number,
  challengeId: '' as string | number,
  challengeName: '',
  initialPoints: '1000',
  minPoints: '100',
  decayCount: '30'
})

const pagination = ref({
  page: 1,
  pageSize: 10
})

const paginatedChallenges = computed(() => {
  const start = (pagination.value.page - 1) * pagination.value.pageSize
  return mountedChallenges.value.slice(start, start + pagination.value.pageSize)
})

const repoChallengeOptions = computed(() => {
  return repoChallenges.value.map(c => ({
    label: `[${c.categoryName || 'Misc'}] ${c.name}`,
    value: String(c.id)
  }))
})

const mountedChallengeColumns = computed<DataTableColumns<CtfCompetitionChallengeVO>>(() => [
  {
    title: t('admin.challenges.name', '题目名称'),
    key: 'title',
    render(row) {
      return h('span', { class: 'font-semibold text-google-text-primary dark:text-google-text-darkPrimary' }, row.title || row.name || '-')
    }
  },
  {
    title: t('admin.challenges.category', '方向分类'),
    key: 'category',
    width: 120,
    render(row) {
      return h('span', { class: 'px-2 py-0.5 text-xs rounded-full bg-google-blue/10 text-google-blue dark:text-google-blue-dark' }, row.category || row.categoryName || '-')
    }
  },
  {
    title: '当前分值',
    key: 'currentPoints',
    width: 120,
    render(row) {
      return `${row.currentPoints ?? 0}`
    }
  },
  {
    title: '衰减梯度',
    key: 'decayCount',
    width: 100,
    render(row) {
      return `${row.decayCount ?? 30}`
    }
  },
  {
    title: '解题次数',
    key: 'solvesCount',
    width: 100,
    render(row) {
      return `${row.solvesCount || 0}`
    }
  },
  {
    title: t('common.actions', '操作'),
    key: 'actions',
    width: 160,
    align: 'center',
    render(row) {
      return h('div', { class: 'flex items-center justify-center gap-2' }, [
        h(
          VBtn,
          {
            clz: 'normal',
            size: 'small',
            leftIcon: PencilOutline,
            onClick: () => openEditModal(row)
          },
          { default: () => '编辑' }
        ),
        h(
          VBtn,
          {
            clz: 'danger',
            size: 'small',
            leftIcon: TrashOutline,
            onClick: () => handleUnmountChallenge(row.id)
          },
          { default: () => '卸载' }
        )
      ])
    }
  }
])

async function fetchCompetitionConfig() {
  if (!props.compId) return
  try {
    const res = await getAdminCompetitionByIdApi(props.compId)
    if (res.data) {
      bloodConfig.value.enableBloodBonus = res.data.enableBloodBonus ?? true
      bloodConfig.value.firstBloodBonusPercentage = String(res.data.firstBloodBonusPercentage ?? 10)
      bloodConfig.value.secondBloodBonusPercentage = String(res.data.secondBloodBonusPercentage ?? 5)
      bloodConfig.value.thirdBloodBonusPercentage = String(res.data.thirdBloodBonusPercentage ?? 2)
    }
  } catch {
    // keep defaults
  }
}

async function handleSaveConfig() {
  if (!props.compId) return
  savingConfig.value = true
  try {
    await updateCompetitionApi(props.compId, {
      config: {
        enableBloodBonus: bloodConfig.value.enableBloodBonus,
        firstBloodBonusPercentage: parseInt(bloodConfig.value.firstBloodBonusPercentage, 10) || 0,
        secondBloodBonusPercentage: parseInt(bloodConfig.value.secondBloodBonusPercentage, 10) || 0,
        thirdBloodBonusPercentage: parseInt(bloodConfig.value.thirdBloodBonusPercentage, 10) || 0
      }
    })
    message.success('赛题配置已保存')
  } catch (err: any) {
    message.error(err.message || '保存赛题配置失败')
  } finally {
    savingConfig.value = false
  }
}

async function fetchMountedChallenges() {
  if (!props.compId) return
  loadingChallenges.value = true
  try {
    const res = await listMountedCtfChallengesApi(props.compId)
    if (res.data) {
      mountedChallenges.value = res.data
    }
  } catch {
    mountedChallenges.value = []
  } finally {
    loadingChallenges.value = false
  }
}

async function openMountModal() {
  showMountModal.value = true
  try {
    const res = await getAdminCtfChallengeListApi({ page: 1, size: 100 })
    if (res.data) {
      repoChallenges.value = res.data.list ?? []
    }
  } catch {
    repoChallenges.value = []
  }
}

async function handleConfirmMount() {
  if (!props.compId || !mountForm.value.challengeId) {
    message.warning('请选择赛题')
    return
  }
  mounting.value = true
  try {
    await mountCtfChallengeApi(props.compId, {
      challengeId: mountForm.value.challengeId,
      initialPoints: parseInt(mountForm.value.initialPoints, 10) || 1000,
      minPoints: parseInt(mountForm.value.minPoints, 10) || 100,
      decayCount: parseInt(mountForm.value.decayCount, 10) || 30
    })
    message.success('赛题添加成功')
    showMountModal.value = false
    await fetchMountedChallenges()
  } catch (err: any) {
    message.error(err.message || '添加赛题失败')
  } finally {
    mounting.value = false
  }
}

function openEditModal(row: CtfCompetitionChallengeVO) {
  editForm.value = {
    id: row.id,
    challengeId: row.challengeId || '',
    challengeName: row.title || row.name || '',
    initialPoints: String(row.initialPoints ?? row.currentPoints ?? 1000),
    minPoints: String(row.minPoints ?? 100),
    decayCount: String(row.decayCount ?? 30)
  }
  showEditModal.value = true
}

async function handleConfirmEdit() {
  if (!props.compId || !editForm.value.id) return
  updatingChallenge.value = true
  try {
    await updateMountedCtfChallengeApi(props.compId, editForm.value.id, {
      challengeId: Number(editForm.value.challengeId),
      initialPoints: parseInt(editForm.value.initialPoints, 10) || 1000,
      minPoints: parseInt(editForm.value.minPoints, 10) || 100,
      decayCount: parseInt(editForm.value.decayCount, 10) || 30
    })
    message.success('赛题配置已更新')
    showEditModal.value = false
    await fetchMountedChallenges()
  } catch (err: any) {
    message.error(err.message || '更新赛题配置失败')
  } finally {
    updatingChallenge.value = false
  }
}

async function handleUnmountChallenge(id: number | string) {
  if (!props.compId) return
  dialog.warning({
    title: '确认卸载赛题',
    content: '卸载后该题目将不再向参赛选手展示，是否继续？',
    positiveText: '确认卸载',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await unmountCtfChallengeApi(props.compId, id)
        message.success('赛题已卸载')
        await fetchMountedChallenges()
      } catch (err: any) {
        message.error(err.message || '卸载赛题失败')
      }
    }
  })
}

onMounted(() => {
  fetchCompetitionConfig()
  fetchMountedChallenges()
})
</script>
