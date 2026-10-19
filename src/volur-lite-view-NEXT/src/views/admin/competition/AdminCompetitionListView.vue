<template>
  <div class="admin-competition-list-view flex flex-col gap-4 sm:gap-6">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.competitions.title', '竞赛管理') }}
        </h2>
        <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
          {{ t('admin.competitions.desc', '管理全平台赛事排程、赛制规则、赛题挂载、战队审核与天梯榜单') }}
        </p>
      </div>
    </div>

    <div class="table-card p-3.5 sm:p-6 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark transition-colors">
      <div class="flex flex-col sm:flex-row sm:flex-wrap sm:items-center gap-2.5 sm:gap-3 mb-4 sm:mb-6">
        <VSearch
          v-model:value="searchKeyword"
          :placeholder="t('admin.competitions.search_placeholder', '搜索竞赛标题')"
          class="w-full sm:w-auto sm:flex-1 sm:min-w-[140px] sm:max-w-sm"
          @search="handleSearch"
          @clear="handleReset"
          @keydown.enter="handleSearch"
        />

        <div class="grid grid-cols-2 gap-2.5 w-full sm:flex sm:w-auto shrink-0">
          <VSelection
            v-model:value="filterType"
            :options="typeSelectOptions"
            :placeholder="t('competition.type_all', '全部赛制')"
            clearable
            class="w-full sm:w-40"
            @update:value="handleSearch"
          />

          <VSelection
            v-model:value="filterStatus"
            :options="statusSelectOptions"
            :placeholder="t('competition.status_all', '全部状态')"
            clearable
            class="w-full sm:w-36"
            @update:value="handleSearch"
          />
        </div>

        <div class="flex items-center gap-2 w-full sm:w-auto shrink-0">
          <VBtn
            clz="normal"
            size="medium"
            :left-icon="SearchOutline"
            class="flex-1 sm:flex-none"
            @click="handleSearch"
          >
            {{ t('common.search', '搜索') }}
          </VBtn>

          <VBtn
            clz="normal"
            size="medium"
            :left-icon="RefreshOutline"
            class="flex-1 sm:flex-none"
            @click="handleReset"
          >
            {{ t('common.reset', '重置') }}
          </VBtn>
        </div>

        <div class="w-full sm:w-auto sm:ml-auto shrink-0">
          <VBtn
            clz="primary"
            size="medium"
            :left-icon="AddOutline"
            class="w-full sm:w-auto"
            @click="router.push('/admin/competitions/create')"
          >
            {{ t('admin.competitions.create_btn', '创建新竞赛') }}
          </VBtn>
        </div>
      </div>

      <VTable
        :columns="tableColumns"
        :data="tableData"
        :loading="loading"
        :pagination="false"
        :bordered="false"
        :row-key="(row: AdminCompetitionListVO) => row.id"
        :scroll-x="1050"
      />

      <div v-if="pagination.itemCount > 0" class="flex flex-col sm:flex-row items-center justify-between gap-3 pt-4 border-t border-google-border-light/70 dark:border-google-border-dark/70">
        <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted text-center sm:text-left">
          显示第 {{ (pagination.page - 1) * pagination.pageSize + 1 }} - {{ Math.min(pagination.page * pagination.pageSize, pagination.itemCount) }} 条，共 {{ pagination.itemCount }} 条
        </div>
        <div class="w-full sm:w-auto overflow-x-auto flex justify-center sm:justify-end">
          <VPagination
            v-model:page="pagination.page"
            v-model:page-size="pagination.pageSize"
            :item-count="pagination.itemCount"
            :page-sizes="pagination.pageSizes"
            show-size-picker
            show-quick-jumper
            size="small"
            @update:page="handlePageChange"
            @update:page-size="handlePageSizeChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import {
  SearchOutline,
  RefreshOutline,
  AddOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VTable from '@/components/common/VTable.vue'
import VSearch from '@/components/common/VSearch.vue'
import VSelection from '@/components/common/VSelection.vue'
import VPagination from '@/components/common/VPagination.vue'
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

defineOptions({
  name: 'AdminCompetitionListView'
})

const { t } = useI18n()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const searchKeyword = ref('')
const filterType = ref<string | null>(null)
const filterStatus = ref<string | null>(null)
const tableData = ref<AdminCompetitionListVO[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const typeSelectOptions = computed(() => [
  { label: t('competition.type_all', '全部赛制'), value: '' },
  { label: t('competition.type_ctf', 'CTF 解题赛'), value: 'CTF' },
  { label: t('competition.type_theory', '理论答题赛'), value: 'THEORY' },
  { label: t('competition.type_awd', 'AWD 攻防对抗'), value: 'AWD' },
  { label: t('competition.type_awd_plus', 'AWD+ 综合攻防'), value: 'AWD_PLUS' },
  { label: t('competition.type_isw', 'ISW 靶场实战'), value: 'ISW' }
])

const statusSelectOptions = computed(() => [
  { label: t('competition.status_all', '全部状态'), value: '' },
  { label: t('competition.status_draft', '草稿'), value: 'DRAFT' },
  { label: t('competition.status_published', '已发布'), value: 'PUBLISHED' },
  { label: t('competition.status_registration', '报名中'), value: 'REGISTRATION' },
  { label: t('competition.status_running', '进行中'), value: 'RUNNING' },
  { label: t('competition.status_frozen', '已封榜'), value: 'FROZEN' },
  { label: t('competition.status_ended', '已结束'), value: 'ENDED' },
  { label: t('competition.status_archived', '已归档'), value: 'ARCHIVED' },
  { label: t('competition.status_hidden', '隐藏'), value: 'HIDDEN' }
])

function getStatusText(status: CompetitionStatus): string {
  switch (status) {
    case 'RUNNING':
      return t('competition.status_running', '进行中')
    case 'REGISTRATION':
      return t('competition.status_registration', '报名中')
    case 'PUBLISHED':
      return t('competition.status_published', '已发布')
    case 'FROZEN':
      return t('competition.status_frozen', '已封榜')
    case 'ENDED':
      return t('competition.status_ended', '已结束')
    case 'ARCHIVED':
      return t('competition.status_archived', '已归档')
    case 'HIDDEN':
      return t('competition.status_hidden', '隐藏')
    default:
      return t('competition.status_draft', '草稿')
  }
}

const tableColumns = computed<DataTableColumns<AdminCompetitionListVO>>(() => [
  {
    title: t('admin.competitions.col_title', '竞赛名称'),
    key: 'title',
    minWidth: 200,
    render(row) {
      return h('div', { class: 'space-y-0.5' }, [
        h(
          'div',
          {
            class: 'redirectable',
            style: { fontWeight: '500' },
            onClick: () => {
              if (row.type === 'THEORY') {
                router.push(`/admin/competitions/theory/${row.id}/edit`)
              } else {
                router.push(`/admin/competitions/ctf/${row.id}/edit`)
              }
            }
          },
          row.title
        ),
        h(
          'div',
          {
            class: 'text-[12px]',
            style: {
              color: 'var(--color-v3-text-var)',
              fontSize: '12px'
            }
          },
          row.slug
        )
      ])
    }
  },
  {
    title: t('admin.competitions.col_type', '赛制'),
    key: 'type',
    width: 120,
    align: 'center',
    render(row) {
      return row.type || '-'
    }
  },
  {
    title: t('admin.competitions.col_status', '状态'),
    key: 'status',
    width: 100,
    align: 'center',
    render(row) {
      return getStatusText(row.status)
    }
  },
  {
    title: t('admin.competitions.col_time', '赛程时间'),
    key: 'time',
    width: 260,
    render(row) {
      return h('div', { class: 'space-y-0.5' }, [
        h('div', `起: ${row.startTime ? new Date(row.startTime).toLocaleString() : '-'}`),
        h('div', `止: ${row.endTime ? new Date(row.endTime).toLocaleString() : '-'}`)
      ])
    }
  },
  {
    title: t('admin.competitions.col_scale', '规模'),
    key: 'scale',
    width: 130,
    render(row) {
      return `${row.teamCount ?? 0} 队 / ${row.participantCount ?? 0} 人`
    }
  },
  {
    title: t('common.actions', '操作'),
    key: 'actions',
    width: 200,
    align: 'center',
    render(row) {
      return h('div', { class: 'flex items-center justify-center gap-1.5' }, [
        h(
          VBtn,
          {
            clz: 'normal',
            size: 'small',
            onClick: () => {
              if (row.type === 'THEORY') {
                router.push(`/admin/competitions/theory/${row.id}/edit`)
              } else {
                router.push(`/admin/competitions/ctf/${row.id}/edit`)
              }
            }
          },
          { default: () => t('common.edit', '管理') }
        ),
        row.status === 'ARCHIVED'
          ? h(
              VBtn,
              {
                clz: 'normal',
                size: 'small',
                onClick: () => handleUnarchive(row.id)
              },
              { default: () => t('admin.competitions.action_unarchive', '解归档') }
            )
          : h(
              VBtn,
              {
                clz: 'normal',
                size: 'small',
                onClick: () => handleArchive(row.id)
              },
              { default: () => t('admin.competitions.action_archive', '归档') }
            ),
        h(
          VBtn,
          {
            clz: 'danger',
            size: 'small',
            onClick: () => handleDelete(row.id)
          },
          { default: () => t('common.delete', '删除') }
        )
      ])
    }
  }
])

async function fetchData() {
  loading.value = true
  try {
    const res = await getAdminCompetitionListApi({
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchKeyword.value.trim() || undefined
    })
    if (res.data) {
      let list = res.data.list ?? []
      if (filterType.value) {
        list = list.filter((item: AdminCompetitionListVO) => item.type === (filterType.value as CompetitionType))
      }
      if (filterStatus.value) {
        list = list.filter((item: AdminCompetitionListVO) => item.status === (filterStatus.value as CompetitionStatus))
      }
      tableData.value = list
      pagination.itemCount = Number(res.data.total) || 0
    }
  } catch (err: any) {
    message.error(err.message || t('admin.competitions.fetch_failed', '获取竞赛列表失败'))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleReset() {
  filterType.value = null
  filterStatus.value = null
  searchKeyword.value = ''
  pagination.page = 1
  fetchData()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchData()
}

function handlePageSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  fetchData()
}

function handleArchive(id: number | string) {
  dialog.warning({
    title: t('admin.competitions.archive_confirm_title', '确认归档竞赛'),
    content: t('admin.competitions.archive_confirm_content', '归档后竞赛将对普通选手封存只读，是否继续？'),
    positiveText: t('common.confirm', '确认归档'),
    negativeText: t('common.cancel', '取消'),
    onPositiveClick: async () => {
      try {
        await archiveCompetitionApi(id)
        message.success(t('admin.competitions.archived_success', '竞赛已归档'))
        await fetchData()
      } catch (err: any) {
        message.error(err.message || t('admin.competitions.archive_failed', '归档失败'))
      }
    }
  })
}

function handleUnarchive(id: number | string) {
  dialog.warning({
    title: t('admin.competitions.unarchive_confirm_title', '确认解除归档'),
    content: t('admin.competitions.unarchive_confirm_content', '解档后该比赛将恢复正常展示，是否继续？'),
    positiveText: t('common.confirm', '确认解档'),
    negativeText: t('common.cancel', '取消'),
    onPositiveClick: async () => {
      try {
        await unarchiveCompetitionApi(id)
        message.success(t('admin.competitions.unarchived_success', '竞赛已解档'))
        await fetchData()
      } catch (err: any) {
        message.error(err.message || t('admin.competitions.unarchive_failed', '解档失败'))
      }
    }
  })
}

function handleDelete(id: number | string) {
  dialog.warning({
    title: t('admin.competitions.delete_confirm_title', '确认删除竞赛'),
    content: t('admin.competitions.delete_confirm_content', '删除后该竞赛全部挂载赛题、报名战队与天梯数据将彻底清除，此操作不可逆！'),
    positiveText: t('common.confirm', '彻底删除'),
    negativeText: t('common.cancel', '取消'),
    onPositiveClick: async () => {
      try {
        await deleteCompetitionApi(id)
        message.success(t('admin.competitions.deleted_success', '竞赛已删除'))
        await fetchData()
      } catch (err: any) {
        message.error(err.message || t('admin.competitions.delete_failed', '删除竞赛失败'))
      }
    }
  })
}

onMounted(() => {
  fetchData()
})
</script>
