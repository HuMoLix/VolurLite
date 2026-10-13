<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
        {{ t('admin.competitions.mounted_challenges', '已挂载赛题列表') }} ({{ mountedChallenges.length }})
      </h3>
      <VBtn
        clz="primary"
        size="medium"
        :left-icon="AddOutline"
        @click="openMountModal"
      >
        {{ t('admin.competitions.mount_challenge_btn', '挂载题目') }}
      </VBtn>
    </div>

    <VTable
      :columns="mountedChallengeColumns"
      :data="mountedChallenges"
      :loading="loadingChallenges"
      :pagination="false"
      size="small"
      :bordered="false"
    >
      <template #empty>
        <div class="py-12 text-center text-xs text-google-text-secondary dark:text-google-text-darkSecondary">
          暂无挂载赛题，请点击右上角挂载题库题目
        </div>
      </template>
    </VTable>

    <VModal
      v-model:show="showMountModal"
      :title="t('admin.competitions.mount_challenge_title', '挂载题库题目到竞赛')"
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
          />
        </div>
        <div class="grid grid-cols-2 gap-4">
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
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <VBtn clz="normal" size="medium" @click="showMountModal = false">
            {{ t('common.cancel', '取消') }}
          </VBtn>
          <VBtn clz="primary" size="medium" :loading="mounting" @click="handleConfirmMount">
            {{ t('admin.competitions.confirm_mount', '确认挂载') }}
          </VBtn>
        </div>
      </template>
    </VModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import { AddOutline, TrashOutline } from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VInput from '@/components/common/VInput.vue'
import VSelection from '@/components/common/VSelection.vue'
import VTable from '@/components/common/VTable.vue'
import VModal from '@/components/common/VModal.vue'
import type { CtfCompetitionChallengeVO } from '@/types/competition'
import {
  listMountedCtfChallengesApi,
  mountCtfChallengeApi,
  unmountCtfChallengeApi
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

const mountedChallenges = ref<CtfCompetitionChallengeVO[]>([])
const loadingChallenges = ref(false)
const showMountModal = ref(false)
const repoChallenges = ref<CtfChallengeListVO[]>([])
const mounting = ref(false)
const mountForm = ref({
  challengeId: '',
  initialPoints: '1000',
  minPoints: '100'
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
      return `${row.currentPoints} Pts`
    }
  },
  {
    title: '解题次数',
    key: 'solvesCount',
    width: 100,
    render(row) {
      return `${row.solvesCount || 0} 次`
    }
  },
  {
    title: t('common.actions', '操作'),
    key: 'actions',
    width: 100,
    align: 'center',
    render(row) {
      return h(
        VBtn,
        {
          clz: 'danger',
          size: 'small',
          leftIcon: TrashOutline,
          onClick: () => handleUnmountChallenge(row.id)
        },
        { default: () => '卸载' }
      )
    }
  }
])

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
      minPoints: parseInt(mountForm.value.minPoints, 10) || 100
    })
    message.success('赛题挂载成功')
    showMountModal.value = false
    await fetchMountedChallenges()
  } catch (err: any) {
    message.error(err.message || '挂载赛题失败')
  } finally {
    mounting.value = false
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
  fetchMountedChallenges()
})
</script>
