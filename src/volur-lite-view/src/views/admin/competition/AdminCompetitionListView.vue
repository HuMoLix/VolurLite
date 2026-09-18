<template>
  <div class="admin-competition-list-view">
    <!-- Page Header -->
    <div class="page-header">
      <h2 class="page-title">{{ t('admin.competitions.title') }}</h2>
      <p class="page-desc">{{ t('admin.competitions.desc') }}</p>
    </div>

    <!-- Main Content Table Card -->
    <VolurTableCard
      :bordered="true"
      :table-bordered="true"
      :scroll-x="1210"
      remote
      :columns="columns"
      :data="tableData"
      :loading="loading"
      :pagination="pagination"
      :row-key="(row: AdminCompetitionListVO) => row.id"
      table-class="competition-data-table"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    >
      <template #filter-left>
        <div class="competition-filter-left">
          <n-input
            v-model:value="searchKeyword"
            :placeholder="t('admin.competitions.search_placeholder')"
            clearable
            class="filter-input-search"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>

          <n-select
            v-model:value="filterType"
            :placeholder="t('competition.type_all')"
            clearable
            :options="typeSelectOptions"
            class="filter-select-type"
            @update:value="handleSearch"
          />

          <n-button class="theme-primary-btn search-btn" @click="handleSearch">
            <template #icon>
              <n-icon :component="SearchOutline" />
            </template>
            搜索
          </n-button>
        </div>
      </template>

      <template #filter-right>
        <div class="competition-filter-right">
          <n-button class="theme-primary-btn action-create-btn" @click="openCreateView">
            <template #icon>
              <n-icon :component="AddOutline" />
            </template>
            {{ t('admin.competitions.create_btn') }}
          </n-button>
        </div>
      </template>
    </VolurTableCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  NInput,
  NSelect,
  NButton,
  NTag,
  NIcon,
  NPopconfirm,
  useMessage,
  type DataTableColumns
} from 'naive-ui'
import { VolurTableCard } from '@/public/volurEx'
import {
  SearchOutline,
  AddOutline
} from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import type {
  AdminCompetitionListVO,
  CompetitionType,
  CompetitionStatus
} from '@/types/competition'
import {
  getAdminCompetitionListApi,
  deleteCompetitionApi,
  archiveCompetitionApi,
  unarchiveCompetitionApi
} from '@/api/competition'

const { t } = useI18n()
const router = useRouter()
const message = useMessage()

// State
const loading = ref<boolean>(false)
const searchKeyword = ref<string>('')
const filterType = ref<CompetitionType | null>(null)
const tableData = ref<AdminCompetitionListVO[]>([])

// Pagination
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  showQuickJumper: true,
  pageSizes: [10, 20, 50],
  onChange: (page: number) => {
    pagination.page = page
    fetchData()
  },
  onUpdatePageSize: (pageSize: number) => {
    pagination.pageSize = pageSize
    pagination.page = 1
    fetchData()
  }
})

// Options
const typeSelectOptions = [
  { label: 'CTF 解题赛 (Jeopardy)', value: 'CTF' },
  { label: '理论知识赛 (Theory Exam)', value: 'THEORY' },
  { label: 'AWD 攻防对抗 (Attack & Defense)', value: 'AWD' },
  { label: 'AWD+ 综合攻防 (Attack & Fix)', value: 'AWD_PLUS' },
  { label: 'ISW 靶场实战 (Cyber Range)', value: 'ISW' }
]

// Columns
const columns: DataTableColumns<AdminCompetitionListVO> = [
  {
    title: t('admin.competitions.col_id'),
    key: 'id',
    width: 180,
    render(row) {
      return h('span', { class: 'id-text' }, String(row.id || '-'))
    }
  },
  {
    title: t('admin.competitions.col_title'),
    key: 'title',
    minWidth: 200,
    ellipsis: { tooltip: true },
    render(row) {
      return h(
        'span',
        {
          style: 'font-weight: 600; cursor: pointer;',
          onClick: () => openEditView(row)
        },
        row.title
      )
    }
  },
  {
    title: t('admin.competitions.col_type'),
    key: 'type',
    width: 120,
    align: 'center',
    ellipsis: { tooltip: true },
    render(row) {
      const typeMap: Record<CompetitionType, { label: string; type: 'info' | 'success' | 'warning' | 'error' | 'default' }> = {
        CTF: { label: 'CTF 解题', type: 'info' },
        THEORY: { label: '理论答题', type: 'success' },
        AWD: { label: 'AWD 对抗', type: 'warning' },
        AWD_PLUS: { label: 'AWD+ 综合', type: 'error' },
        ISW: { label: 'ISW 靶场', type: 'default' }
      }
      const item = typeMap[row.type] || { label: row.type, type: 'default' }
      return h(NTag, { type: item.type, size: 'small', round: true }, { default: () => item.label })
    }
  },
  {
    title: t('admin.competitions.col_status'),
    key: 'status',
    width: 100,
    align: 'center',
    ellipsis: { tooltip: true },
    render(row) {
      const statusMap: Record<CompetitionStatus, { label: string; type: 'warning' | 'success' | 'info' | 'default' }> = {
        DRAFT: { label: t('competition.status_draft'), type: 'default' },
        PUBLISHED: { label: t('competition.status_published'), type: 'info' },
        REGISTRATION: { label: t('competition.status_registration'), type: 'info' },
        RUNNING: { label: t('competition.status_running'), type: 'success' },
        FROZEN: { label: t('competition.status_frozen'), type: 'warning' },
        ENDED: { label: t('competition.status_ended'), type: 'default' },
        ARCHIVED: { label: t('competition.status_archived'), type: 'default' }
      }
      const item = statusMap[row.status] || { label: row.status, type: 'default' }
      return h(NTag, { type: item.type, size: 'small' }, { default: () => item.label })
    }
  },
  {
    title: t('admin.competitions.col_time'),
    key: 'time',
    width: 260,
    ellipsis: { tooltip: true },
    render(row) {
      return h('div', { style: 'font-size: 12px; line-height: 1.5;' }, [
        h('div', { style: 'overflow: hidden; text-overflow: ellipsis; white-space: nowrap;' }, `起: ${row.startTime || '-'}`),
        h('div', { style: 'overflow: hidden; text-overflow: ellipsis; white-space: nowrap;' }, `止: ${row.endTime || '-'}`)
      ])
    }
  },
  {
    title: t('admin.competitions.col_scale'),
    key: 'scale',
    width: 130,
    ellipsis: { tooltip: true },
    render(row) {
      return `${row.teamCount ?? 0} 队 / ${row.participantCount ?? 0} 人`
    }
  },
  {
    title: t('admin.competitions.col_actions'),
    key: 'actions',
    width: 220,
    fixed: 'right',
    render(row) {
      return h('div', { style: 'display: flex; gap: 8px;' }, [
        h(
          NButton,
          {
            size: 'tiny',
            onClick: () => openEditView(row)
          },
          { default: () => t('admin.competitions.action_edit') }
        ),
        row.status === 'ARCHIVED'
          ? h(
              NPopconfirm,
              {
                onPositiveClick: () => handleUnarchive(row.id)
              },
              {
                trigger: () =>
                  h(
                    NButton,
                    { size: 'tiny', secondary: true, type: 'warning' },
                    { default: () => t('admin.competitions.action_unarchive') }
                  ),
                default: () => t('admin.competitions.unarchive_confirm')
              }
            )
          : h(
              NPopconfirm,
              {
                onPositiveClick: () => handleArchive(row.id)
              },
              {
                trigger: () =>
                  h(
                    NButton,
                    { size: 'tiny', quaternary: true },
                    { default: () => t('admin.competitions.action_archive') }
                  ),
                default: () => t('admin.competitions.archive_confirm')
              }
            ),
        h(
          NPopconfirm,
          {
            onPositiveClick: () => handleDelete(row.id)
          },
          {
            trigger: () =>
              h(
                NButton,
                { size: 'tiny', type: 'error' },
                { default: () => t('admin.competitions.action_delete') }
              ),
            default: () => t('admin.competitions.action_delete_confirm')
          }
        )
      ])
    }
  }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getAdminCompetitionListApi({
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchKeyword.value || undefined
    })
    if (res.data) {
      tableData.value = filterType.value
        ? res.data.list.filter((item) => item.type === filterType.value)
        : res.data.list
      pagination.itemCount = res.data.total
    }
  } catch {
    // fallback sample data if backend unreachable
    tableData.value = [
      {
        id: 1,
        title: '2026 全国高校网络安全精英挑战赛 (CTF)',
        slug: 'national-ctf-2026',
        type: 'CTF',
        status: 'RUNNING',
        startTime: '2026-09-10 09:00:00',
        endTime: '2026-09-12 18:00:00',
        teamCount: 64,
        participantCount: 192,
        createdAt: '2026-08-01 10:00:00'
      },
      {
        id: 2,
        title: '全国网络空间安全攻防 AWD 巅峰对抗赛',
        slug: 'awd-championship-2026',
        type: 'AWD',
        status: 'REGISTRATION',
        startTime: '2026-09-20 10:00:00',
        endTime: '2026-09-21 17:00:00',
        teamCount: 32,
        participantCount: 96,
        createdAt: '2026-08-15 14:30:00'
      }
    ]
    pagination.itemCount = tableData.value.length
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchData()
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  fetchData()
}

/**
 * 跳转到独立 SPA 页面进行新建竞赛
 */
function openCreateView() {
  router.push('/admin/competitions/create')
}

/**
 * 跳转到独立 SPA 页面进行编辑竞赛
 */
function openEditView(row: AdminCompetitionListVO) {
  router.push(`/admin/competitions/${row.id}/edit`)
}

async function handleDelete(id: number | string) {
  try {
    await deleteCompetitionApi(id)
    message.success(t('admin.competitions.delete_success'))
    fetchData()
  } catch {
    message.error('删除失败')
  }
}

async function handleArchive(id: number | string) {
  try {
    await archiveCompetitionApi(id)
    message.success(t('admin.competitions.archive_success'))
    fetchData()
  } catch (error: any) {
    message.error(error.message || '归档失败')
  }
}

async function handleUnarchive(id: number | string) {
  try {
    await unarchiveCompetitionApi(id)
    message.success(t('admin.competitions.unarchive_success'))
    fetchData()
  } catch (error: any) {
    message.error(error.message || '取消归档失败')
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.admin-competition-list-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  .page-title {
    font-size: 22px;
    font-weight: 600;
    margin: 0;
    color: var(--v-text-primary, #111111);
  }

  .page-desc {
    font-size: 13px;
    color: var(--v-text-desc, #444444);
    margin-top: 4px;
  }
}

.competition-filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;

  .filter-input-search {
    width: 220px;
    min-width: 160px;
  }

  .filter-select-type {
    width: 150px;
    flex-shrink: 0;
  }
}

.competition-filter-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 768px) {
  :deep(.filter-header) {
    flex-direction: column;
    align-items: stretch !important;
    gap: 12px;

    .filter-left,
    .filter-right {
      width: 100%;
    }
  }

  .competition-filter-left {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 10px;

    .filter-input-search,
    .filter-select-type {
      width: 100% !important;
    }

    .search-btn {
      width: 100%;
      justify-content: center;
    }
  }

  .competition-filter-right {
    width: 100%;
    display: flex;
    gap: 10px;

    .action-create-btn {
      width: 100%;
      justify-content: center;
    }
  }
}
</style>
