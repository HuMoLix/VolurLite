<template>
  <div class="announce-tab">
    <!-- Announcement Table Card -->
    <AppTableCard
      :bordered="true"
      :table-bordered="true"
      :columns="columns"
      :data="announcements"
      :loading="loading"
      :scroll-x="920"
      :row-key="(row) => row.id"
      table-class="announcements-table"
    >
      <template #table-header>
        <!-- Intro Alert -->
        <n-alert type="info" :bordered="false" class="tab-intro-alert" style="margin-bottom: 16px;">
          <strong>比赛专属实时通报：</strong>本模块发布的公告仅向参与当前比赛的选手进行推送与展示，区别于全站公开公告，适用于赛题解题提示 (Hint)、赛程变动、靶机维护与紧急规则澄清。
        </n-alert>
      </template>

      <template #toolbar>
        <div class="tab-top-actions">
          <div class="actions-left">
            <span class="tab-subtitle">已发布赛事公告 ({{ announcements.length }})</span>
          </div>
          <div class="actions-right">
            <n-button quaternary :loading="loading" @click="fetchAnnouncements">
              <template #icon>
                <n-icon :component="RefreshOutline" />
              </template>
              刷新
            </n-button>
            <n-button type="primary" class="theme-primary-btn" @click="openCreateModal">
              <template #icon>
                <n-icon :component="AddOutline" />
              </template>
              发布比赛公告
            </n-button>
          </div>
        </div>
      </template>

      <template #empty>
        <div style="padding: 24px; text-align: center; color: #888;">
          暂无比赛专属公告，点击右上角发布新公告
        </div>
      </template>
    </AppTableCard>

    <!-- Create/Edit Modal -->
    <n-modal
      v-model:show="showModal"
      preset="card"
      :title="isEditMode ? '编辑比赛公告' : '发布比赛专属公告'"
      style="width: 680px;"
      :mask-closable="false"
    >
      <n-form
        ref="modalFormRef"
        :model="formModel"
        :rules="formRules"
        label-placement="top"
      >
        <n-grid :x-gap="16" cols="1 s:2" responsive="screen">
          <n-grid-item :span="2">
            <n-form-item label="公告标题" path="title">
              <n-input
                v-model:value="formModel.title"
                placeholder="请输入公告标题，例如：【提示】Web01 题目环境更新通知"
                maxlength="128"
                show-count
              />
            </n-form-item>
          </n-grid-item>

          <n-grid-item :span="1">
            <n-form-item label="通报类型 / 级别" path="level">
              <n-select
                v-model:value="formModel.level"
                :options="levelOptions"
              />
            </n-form-item>
          </n-grid-item>

          <n-grid-item :span="1">
            <n-form-item label="置顶显示">
              <div class="switch-row">
                <n-switch v-model:value="formModel.isPinned" />
                <span class="field-hint">
                  {{ formModel.isPinned ? '置顶顶部' : '普通排序' }}
                </span>
              </div>
            </n-form-item>
          </n-grid-item>

          <n-grid-item :span="2">
            <n-form-item label="关联赛题名称 (可选，若为题目 Hint 提示可填)">
              <n-input
                v-model:value="formModel.challengeName"
                placeholder="例如：SQLi-Master"
                clearable
              />
            </n-form-item>
          </n-grid-item>

          <n-grid-item :span="2">
            <n-form-item label="公告详细内容 (支持 Markdown)" path="content">
              <VolurMarkdownEditor
                v-model:value="formModel.content"
                placeholder="请输入通报详情与提示正文，支持加粗、代码块、列表等排版"
                :rows="6"
                min-height="160px"
              />
            </n-form-item>
          </n-grid-item>
        </n-grid>

        <div class="modal-actions">
          <n-button @click="showModal = false">取消</n-button>
          <n-button class="theme-primary-btn" :loading="modalSubmitting" @click="handleSaveAnnouncement">
            确认发布
          </n-button>
        </div>
      </n-form>
    </n-modal>

    <!-- Detail View Modal -->
    <n-modal
      v-model:show="showDetailModal"
      preset="card"
      :title="selectedAnnouncement?.title || '公告详情'"
      style="width: 600px;"
    >
      <div v-if="selectedAnnouncement" class="announcement-detail-content">
        <div class="meta-line">
          <n-tag size="small" :type="getLevelTagType(selectedAnnouncement.level)" round>
            {{ getLevelLabel(selectedAnnouncement.level) }}
          </n-tag>
          <n-tag v-if="selectedAnnouncement.isPinned" size="small" type="warning" round>置顶</n-tag>
          <span v-if="selectedAnnouncement.challengeName" class="chal-badge">
            赛题：{{ selectedAnnouncement.challengeName }}
          </span>
          <span class="time-lbl">{{ new Date(selectedAnnouncement.createdAt).toLocaleString() }}</span>
        </div>
        <div class="volur-markdown-body content-body" v-html="renderMarkdown(selectedAnnouncement.content)" />
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import {
  NAlert,
  NButton,
  NIcon,
  NModal,
  NForm,
  NFormItem,
  NInput,
  NSelect,
  NSwitch,
  NTag,
  NGrid,
  NGridItem,
  useMessage,
  useDialog,
  type DataTableColumns,
  type FormInst,
  type FormRules
} from 'naive-ui'
import AppTableCard from '@/components/common/AppTableCard.vue'
import {
  RefreshOutline,
  AddOutline
} from '@vicons/ionicons5'
import type {
  CompetitionAnnouncementVO,
  CompetitionAnnouncementDTO
} from '@/types/competition'
import {
  listAdminCompetitionAnnouncementsApi,
  createAdminCompetitionAnnouncementApi,
  updateAdminCompetitionAnnouncementApi,
  togglePinAdminCompetitionAnnouncementApi,
  deleteAdminCompetitionAnnouncementApi
} from '@/api/competition'
import { renderMarkdown } from '@/utils/markdown'
import { VolurMarkdownEditor } from '@/public/volurEx'

interface Props {
  compId: string | number
}

const props = defineProps<Props>()
const message = useMessage()
const dialog = useDialog()

const loading = ref<boolean>(false)
const announcements = ref<CompetitionAnnouncementVO[]>([])
const showModal = ref<boolean>(false)
const isEditMode = ref<boolean>(false)
const editingId = ref<number | string | null>(null)
const modalSubmitting = ref<boolean>(false)
const modalFormRef = ref<FormInst | null>(null)

const showDetailModal = ref<boolean>(false)
const selectedAnnouncement = ref<CompetitionAnnouncementVO | null>(null)

const formModel = reactive<{
  title: string
  content: string
  level: string
  isPinned: boolean
  challengeName: string
}>({
  title: '',
  content: '',
  level: 'INFO',
  isPinned: false,
  challengeName: ''
})

const formRules: FormRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

const levelOptions = [
  { label: '普通通知 (INFO)', value: 'INFO' },
  { label: '题目提示 (HINT)', value: 'HINT' },
  { label: '紧急预警 (WARNING)', value: 'WARNING' },
  { label: '重要变更 (CRITICAL)', value: 'CRITICAL' }
]

const columns: DataTableColumns<CompetitionAnnouncementVO> = [
  {
    title: '标题',
    key: 'title',
    render(row) {
      return h('div', { style: 'display: flex; align-items: center; gap: 8px;' }, [
        row.isPinned
          ? h(NTag, { size: 'small', type: 'warning', round: true }, { default: () => '置顶' })
          : null,
        h(NTag, { size: 'small', type: getLevelTagType(row.level), round: true }, {
          default: () => getLevelLabel(row.level)
        }),
        h('span', {
          style: 'font-weight: 500; cursor: pointer; color: var(--v-text-primary, #111);',
          onClick: () => openDetailModal(row)
        }, row.title)
      ])
    }
  },
  {
    title: '关联赛题',
    key: 'challengeName',
    width: 140,
    render(row) {
      return row.challengeName
        ? h(NTag, { size: 'small', type: 'info', bordered: false }, { default: () => row.challengeName })
        : h('span', { style: 'color: #999; font-size: 12px;' }, '-')
    }
  },
  {
    title: '发布者',
    key: 'createdByName',
    width: 120,
    render(row) {
      return row.createdByName || '管理员'
    }
  },
  {
    title: '发布时间',
    key: 'createdAt',
    width: 180,
    render(row) {
      return new Date(row.createdAt).toLocaleString()
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    fixed: 'right',
    render(row) {
      return h('div', { style: 'display: flex; gap: 8px;' }, [
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            onClick: () => handleTogglePin(row)
          },
          {
            default: () => (row.isPinned ? '取消置顶' : '置顶')
          }
        ),
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            onClick: () => openEditModal(row)
          },
          {
            default: () => '编辑'
          }
        ),
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            type: 'error',
            onClick: () => handleDelete(row)
          },
          {
            default: () => '删除'
          }
        )
      ])
    }
  }
]

onMounted(() => {
  if (props.compId) {
    fetchAnnouncements()
  }
})

async function fetchAnnouncements() {
  loading.value = true
  try {
    const res = await listAdminCompetitionAnnouncementsApi(props.compId)
    announcements.value = res.data || []
  } catch (error: any) {
    console.error('Fetch announcements error:', error)
    message.error(error.message || '获取比赛公告列表失败')
  } finally {
    loading.value = false
  }
}

function openCreateModal() {
  isEditMode.value = false
  editingId.value = null
  formModel.title = ''
  formModel.content = ''
  formModel.level = 'INFO'
  formModel.isPinned = false
  formModel.challengeName = ''
  showModal.value = true
}

function openEditModal(row: CompetitionAnnouncementVO) {
  isEditMode.value = true
  editingId.value = row.id
  formModel.title = row.title
  formModel.content = row.content
  formModel.level = row.level || 'INFO'
  formModel.isPinned = row.isPinned || false
  formModel.challengeName = row.challengeName || ''
  showModal.value = true
}

function openDetailModal(row: CompetitionAnnouncementVO) {
  selectedAnnouncement.value = row
  showDetailModal.value = true
}

async function handleSaveAnnouncement() {
  if (!modalFormRef.value) return
  await modalFormRef.value.validate()

  modalSubmitting.value = true
  try {
    const dto: CompetitionAnnouncementDTO = {
      title: formModel.title.trim(),
      content: formModel.content,
      level: formModel.level,
      isPinned: formModel.isPinned,
      challengeName: formModel.challengeName ? formModel.challengeName.trim() : undefined
    }

    if (isEditMode.value && editingId.value) {
      await updateAdminCompetitionAnnouncementApi(props.compId, editingId.value, dto)
      message.success('更新比赛公告成功')
    } else {
      await createAdminCompetitionAnnouncementApi(props.compId, dto)
      message.success('发布比赛公告成功')
    }
    showModal.value = false
    fetchAnnouncements()
  } catch (error: any) {
    console.error('Save announcement error:', error)
    message.error(error.message || '保存公告失败')
  } finally {
    modalSubmitting.value = false
  }
}

async function handleTogglePin(row: CompetitionAnnouncementVO) {
  try {
    const targetState = !row.isPinned
    await togglePinAdminCompetitionAnnouncementApi(props.compId, row.id, targetState)
    message.success(targetState ? '公告已置顶' : '已取消置顶')
    fetchAnnouncements()
  } catch (error: any) {
    message.error(error.message || '操作失败')
  }
}

function handleDelete(row: CompetitionAnnouncementVO) {
  dialog.warning({
    title: '确认删除公告',
    content: `确定要删除公告「${row.title}」吗？删除后正在参赛的选手将不再看到此通告。`,
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteAdminCompetitionAnnouncementApi(props.compId, row.id)
        message.success('公告已删除')
        fetchAnnouncements()
      } catch (error: any) {
        message.error(error.message || '删除失败')
      }
    }
  })
}

function getLevelTagType(level?: string) {
  switch (level?.toUpperCase()) {
    case 'HINT':
      return 'primary'
    case 'WARNING':
      return 'warning'
    case 'CRITICAL':
      return 'error'
    case 'INFO':
    default:
      return 'info'
  }
}

function getLevelLabel(level?: string) {
  switch (level?.toUpperCase()) {
    case 'HINT':
      return '题目提示'
    case 'WARNING':
      return '紧急提醒'
    case 'CRITICAL':
      return '重要变更'
    case 'INFO':
    default:
      return '一般通知'
  }
}
</script>

<style scoped lang="scss">
.announce-tab {
  .tab-intro-alert {
    margin-bottom: 20px;
    font-size: 13px;
    line-height: 1.6;
  }

  .tab-top-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 16px;
    margin-bottom: 20px;
    border-bottom: 1px solid var(--v-border-base, #e5e4e7);

    .tab-subtitle {
      font-size: 15px;
      font-weight: 600;
      color: var(--v-text-primary, #111111);
    }

    .actions-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  @media (max-width: 768px) {
    .tab-top-actions {
      flex-direction: column;
      align-items: stretch;
      gap: 10px;

      .actions-right {
        display: flex;
        width: 100%;
        gap: 10px;

        .n-button {
          flex: 1;
          justify-content: center;
        }
      }
    }
  }

  .modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 20px;
  }
}

.announcement-detail-content {
  .meta-line {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--v-border-base, #e5e4e7);

    .chal-badge {
      font-size: 12px;
      color: #444444;
      background: var(--v-bg-muted, #f8f8f9);
      padding: 2px 8px;
      border-radius: 4px;
    }

    .time-lbl {
      margin-left: auto;
      font-size: 12px;
      color: var(--v-text-muted, #999999);
    }
  }

  .content-body {
    font-size: 14px;
    line-height: 1.8;
  }
}

.switch-row {
  display: flex;
  align-items: center;

  .field-hint {
    margin-left: 12px;
    font-size: 12px;
    color: #444444;
  }
}
</style>
