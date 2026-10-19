<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
        {{ t('admin.competitions.announcements_list', '比赛专属公告列表') }}
      </h3>
      <VBtn
        clz="primary"
        size="medium"
        :left-icon="AddOutline"
        @click="openCreateAnnounceModal"
      >
        {{ t('admin.competitions.create_announce_btn', '发布公告') }}
      </VBtn>
    </div>

    <VTable
      :columns="announcementColumns"
      :data="announcements"
      :loading="loadingAnnouncements"
      :pagination="false"
      size="small"
      :bordered="false"
    >
      <template #empty>
        <div class="py-12 text-center text-xs text-google-text-secondary dark:text-google-text-darkSecondary">
          暂无比赛公告
        </div>
      </template>
    </VTable>

    <VModal
      v-model:show="showAnnounceModal"
      :title="t('admin.competitions.create_announce_title', '发布比赛公告')"
      :width="540"
    >
      <div class="space-y-4">
        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            公告标题 *
          </label>
          <VInput
            v-model:value="announceForm.title"
            placeholder="公告简述标题"
            clearable
          />
        </div>
        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            公告内容 *
          </label>
          <VInput
            v-model:value="announceForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入公告正文..."
          />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <VBtn clz="normal" size="medium" @click="showAnnounceModal = false">
            {{ t('common.cancel', '取消') }}
          </VBtn>
          <VBtn clz="primary" size="medium" :loading="publishingAnnounce" @click="handlePublishAnnounce">
            {{ t('common.confirm', '发布') }}
          </VBtn>
        </div>
      </template>
    </VModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessage, type DataTableColumns } from 'naive-ui'
import { AddOutline, TrashOutline } from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VInput from '@/components/common/VInput.vue'
import VTable from '@/components/common/VTable.vue'
import VModal from '@/components/common/VModal.vue'
import type { CompetitionAnnouncementVO } from '@/types/competition'
import {
  listAdminCompetitionAnnouncementsApi,
  createAdminCompetitionAnnouncementApi,
  deleteAdminCompetitionAnnouncementApi
} from '@/api/competition'

defineOptions({
  name: 'AdminCtfCompetitionAnnounceView'
})

const props = defineProps<{
  compId: string | number
}>()

const { t } = useI18n()
const message = useMessage()

const announcements = ref<CompetitionAnnouncementVO[]>([])
const loadingAnnouncements = ref(false)
const showAnnounceModal = ref(false)
const publishingAnnounce = ref(false)
const announceForm = ref({
  title: '',
  content: ''
})

const announcementColumns = computed<DataTableColumns<CompetitionAnnouncementVO>>(() => [
  {
    title: '公告标题',
    key: 'title',
    render(row) {
      return row.title
    }
  },
  {
    title: '内容简述',
    key: 'content',
    ellipsis: { tooltip: true },
    render(row) {
      return row.content
    }
  },
  {
    title: '发布时间',
    key: 'createdAt',
    width: 180,
    render(row) {
      return row.createdAt ? new Date(row.createdAt).toLocaleString() : '-'
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
          onClick: () => handleDeleteAnnounce(row.id)
        },
        { default: () => '删除' }
      )
    }
  }
])

async function fetchAnnouncements() {
  if (!props.compId) return
  loadingAnnouncements.value = true
  try {
    const res = await listAdminCompetitionAnnouncementsApi(props.compId)
    if (res.data) {
      announcements.value = res.data
    }
  } catch {
    announcements.value = []
  } finally {
    loadingAnnouncements.value = false
  }
}

function openCreateAnnounceModal() {
  announceForm.value = { title: '', content: '' }
  showAnnounceModal.value = true
}

async function handlePublishAnnounce() {
  if (!props.compId || !announceForm.value.title.trim() || !announceForm.value.content.trim()) {
    message.warning('请完整填写公告标题与内容')
    return
  }
  publishingAnnounce.value = true
  try {
    await createAdminCompetitionAnnouncementApi(props.compId, {
      title: announceForm.value.title.trim(),
      content: announceForm.value.content.trim()
    })
    message.success('公告发布成功')
    showAnnounceModal.value = false
    await fetchAnnouncements()
  } catch (err: any) {
    message.error(err.message || '发布公告失败')
  } finally {
    publishingAnnounce.value = false
  }
}

async function handleDeleteAnnounce(id: number | string) {
  if (!props.compId) return
  try {
    await deleteAdminCompetitionAnnouncementApi(props.compId, id)
    message.success('公告已删除')
    await fetchAnnouncements()
  } catch (err: any) {
    message.error(err.message || '删除公告失败')
  }
}

onMounted(() => {
  fetchAnnouncements()
})
</script>
