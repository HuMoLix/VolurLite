<template>
  <div class="admin-user-view flex flex-col gap-4 sm:gap-6">
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-blue-50 dark:bg-blue-900/30 text-google-blue dark:text-google-blue-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <PeopleOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <PeopleOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            {{ t('admin.users.stat_total_users', '系统总用户') }}
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ statistics.totalUsers }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-amber-50 dark:bg-amber-900/30 text-google-yellow dark:text-google-yellow-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <ShieldCheckmarkOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <ShieldCheckmarkOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            {{ t('admin.users.stat_admin_count', '管理员数') }}
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ statistics.adminCount }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-green-50 dark:bg-green-900/30 text-google-green dark:text-google-green-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <CheckmarkCircleOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <CheckmarkCircleOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            {{ t('admin.users.stat_active_count', '正常启用') }}
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ statistics.activeCount }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-red-50 dark:bg-red-900/30 text-google-red dark:text-google-red-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <PersonRemoveOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <PersonRemoveOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            {{ t('admin.users.stat_banned_count', '已封禁') }}
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ statistics.bannedCount }}
          </div>
        </div>
      </div>
    </div>

    <div class="table-card p-3.5 sm:p-6 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark transition-colors">
      <div class="flex flex-col sm:flex-row sm:flex-wrap sm:items-center gap-2.5 sm:gap-3 mb-4 sm:mb-6">
        <VSearch
          v-model:value="searchKeyword"
          :placeholder="t('admin.users.search_placeholder', '搜索用户名 / 邮箱 / 组织...')"
          class="w-full sm:w-auto sm:flex-1 sm:min-w-[140px] sm:max-w-sm"
          @search="handleSearch"
          @clear="handleSearch"
          @keydown.enter="handleSearch"
        />

        <div class="grid grid-cols-2 gap-2.5 w-full sm:flex sm:w-auto shrink-0">
          <VSelection
            v-model:value="filterRole"
            :options="roleFilterOptions"
            :placeholder="t('admin.users.filter_role', '用户角色')"
            clearable
            class="w-full sm:w-36"
            @update:value="handleSearch"
          />

          <VSelection
            v-model:value="filterStatus"
            :options="statusFilterOptions"
            :placeholder="t('admin.users.filter_status', '账号状态')"
            clearable
            class="w-full sm:w-32"
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
            @click="fetchUsers"
          >
            {{ t('admin.users.refresh_btn', '刷新') }}
          </VBtn>
        </div>

        <div class="w-full sm:w-auto sm:ml-auto shrink-0">
          <VBtn
            clz="primary"
            size="medium"
            :left-icon="AddOutline"
            class="w-full sm:w-auto"
            @click="openCreateModal"
          >
            {{ t('admin.users.create_btn', '新增用户') }}
          </VBtn>
        </div>
      </div>

      <VTable
        :columns="columns"
        :data="userList"
        :loading="loading"
        :pagination="false"
        :bordered="false"
        :row-key="(row) => String(row.id)"
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

    <VModal
      v-model:show="isUserModalShow"
      :title="isEditMode ? t('admin.users.modal.title_edit', '编辑用户资料') : t('admin.users.modal.title_create', '创建新用户')"
      :width="560"
    >
      <div class="flex flex-col gap-4 py-2">
        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            {{ t('admin.users.modal.username_label', '用户名') }}
          </label>
          <VInput
            v-model:value="userForm.username"
            :disabled="isEditMode"
            :placeholder="t('admin.users.modal.username_placeholder', '请输入用户名（3-32位字符）')"
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            {{ t('admin.users.modal.email_label', '电子邮箱') }}
          </label>
          <VInput
            v-model:value="userForm.email"
            :placeholder="t('admin.users.modal.email_placeholder', '请输入有效的电子邮箱')"
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            {{ isEditMode ? t('admin.users.modal.password_label_edit', '新密码') : t('admin.users.modal.password_label_create', '登录密码') }}
          </label>
          <VInput
            v-model:value="userForm.password"
            type="password"
            :placeholder="isEditMode ? t('admin.users.modal.password_placeholder_edit', '留空则保持原密码不变') : t('admin.users.modal.password_placeholder_create', '请输入密码（至少6位）')"
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            {{ t('admin.users.modal.role_label', '用户角色') }}
          </label>
          <VSelection
            v-model:value="userForm.roleName"
            :options="roleModalOptions"
            :placeholder="t('admin.users.modal.role_placeholder', '请选择权限角色')"
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            {{ t('admin.users.modal.org_label', '所属组织') }}
          </label>
          <VInput
            v-model:value="userForm.org"
            :placeholder="t('admin.users.modal.org_placeholder', '选填，战队、公司或院校')"
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            {{ t('admin.users.modal.bio_label', '个人简介') }}
          </label>
          <VInput
            v-model:value="userForm.bio"
            type="textarea"
            :rows="2"
            :placeholder="t('admin.users.modal.bio_placeholder', '选填，个人简介或签名')"
          />
        </div>

        <div class="flex items-center justify-between p-3 rounded-xl bg-google-surface-light dark:bg-google-surface-darkElevated">
          <span class="text-sm font-medium text-google-text-primary dark:text-google-text-darkPrimary">
            {{ t('admin.users.modal.status_label', '账号状态') }}
          </span>
          <n-switch v-model:value="userForm.status" />
        </div>
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-3 w-full">
          <VBtn clz="normal" @click="closeUserModal">
            {{ t('admin.users.modal.cancel', '取消') }}
          </VBtn>
          <VBtn clz="primary" :loading="submitting" @click="handleSaveUser">
            {{ t('admin.users.modal.submit', '保存提交') }}
          </VBtn>
        </div>
      </template>
    </VModal>

    <VModal
      v-model:show="isDeleteModalShow"
      :title="t('admin.users.actions.delete_confirm_title', '确认删除用户')"
      :width="440"
    >
      <div class="py-3 text-sm text-google-text-desc dark:text-google-text-darkDesc">
        {{ t('admin.users.actions.delete_confirm', '确定删除该用户吗？此操作不可撤销。') }}
        <span v-if="userToDelete" class="font-semibold text-google-text-primary dark:text-google-text-darkPrimary block mt-1">
          {{ userToDelete.username }} ({{ userToDelete.email }})
        </span>
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-3 w-full">
          <VBtn clz="normal" @click="isDeleteModalShow = false">
            {{ t('admin.users.modal.cancel', '取消') }}
          </VBtn>
          <VBtn clz="warning" :loading="deleting" @click="confirmDeleteUser">
            {{ t('admin.users.actions.delete', '删除') }}
          </VBtn>
        </div>
      </template>
    </VModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NIcon,
  NSwitch,
  useMessage,
  type DataTableColumns
} from 'naive-ui'
import {
  PeopleOutline,
  ShieldCheckmarkOutline,
  CheckmarkCircleOutline,
  PersonRemoveOutline,
  RefreshOutline,
  AddOutline,
  SearchOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VTable from '@/components/common/VTable.vue'
import VModal from '@/components/common/VModal.vue'
import VInput from '@/components/common/VInput.vue'
import VSelection from '@/components/common/VSelection.vue'
import VSearch from '@/components/common/VSearch.vue'
import VPagination from '@/components/common/VPagination.vue'
import VAvatar from '@/components/sidebar/VAvatar.vue'
import {
  getAdminUserStatisticsApi,
  getAdminUserListApi,
  createAdminUserApi,
  updateAdminUserApi,
  deleteAdminUserApi,
  toggleAdminUserStatusApi
} from '@/api/user'
import type {
  AdminUserDetailsVO,
  AdminUserStatisticsVO,
  AdminUserPageRequestDTO
} from '@/types/user'

defineOptions({
  name: 'AdminUserView'
})

const { t } = useI18n()
const message = useMessage()

const loading = ref(false)
const submitting = ref(false)
const deleting = ref(false)

const searchKeyword = ref('')
const filterRole = ref<string | null>(null)
const filterStatus = ref<string | null>(null)

const userList = ref<AdminUserDetailsVO[]>([])
const statistics = reactive<AdminUserStatisticsVO>({
  totalUsers: 0,
  adminCount: 0,
  bannedCount: 0,
  activeCount: 0
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const isUserModalShow = ref(false)
const isEditMode = ref(false)
const editingUserId = ref<string | number | null>(null)

const userForm = reactive({
  username: '',
  email: '',
  password: '',
  roleName: 'USER',
  org: '',
  bio: '',
  status: true
})

const isDeleteModalShow = ref(false)
const userToDelete = ref<AdminUserDetailsVO | null>(null)

const roleFilterOptions = computed(() => [
  { label: t('admin.users.role_all', '全部角色'), value: '' },
  { label: t('admin.users.role_user', '普通用户'), value: 'USER' },
  { label: t('admin.users.role_admin', '管理员'), value: 'ADMIN' },
  { label: t('admin.users.role_super_admin', '超级管理员'), value: 'SUPER_ADMIN' }
])

const statusFilterOptions = computed(() => [
  { label: t('admin.users.status_all', '全部状态'), value: '' },
  { label: t('admin.users.status_active', '正常启用'), value: 'true' },
  { label: t('admin.users.status_banned', '已封禁'), value: 'false' }
])

const roleModalOptions = [
  { label: '普通用户 (USER)', value: 'USER' },
  { label: '管理员 (ADMIN)', value: 'ADMIN' },
  { label: '超级管理员 (SUPER_ADMIN)', value: 'SUPER_ADMIN' }
]

function formatDateTime(instantOrString: string | number | Date | null | undefined): string {
  if (!instantOrString) return '-'
  const date = new Date(instantOrString)
  if (isNaN(date.getTime())) return String(instantOrString)
  const Y = date.getFullYear()
  const M = String(date.getMonth() + 1).padStart(2, '0')
  const D = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const m = String(date.getMinutes()).padStart(2, '0')
  const s = String(date.getSeconds()).padStart(2, '0')
  return `${Y}-${M}-${D} ${h}:${m}:${s}`
}

const columns = computed<DataTableColumns<AdminUserDetailsVO>>(() => [
  {
    title: t('admin.users.columns.id', 'ID'),
    key: 'id',
    render(row) {
      return String(row.id)
    }
  },
  {
    title: t('admin.users.columns.user', '用户'),
    key: 'username',
    minWidth: 200,
    render(row) {
      return h('div', { class: 'flex items-center gap-3' }, [
        h(VAvatar, {
          src: row.avatarUrl || '',
          text: (row.username || '').charAt(0).toUpperCase(),
          size: 32,
          shape: 'circle'
        }),
        h('div', { class: 'flex flex-col min-w-0' }, [
          h('span', { class: 'text-sm font-medium text-google-text-primary dark:text-google-text-darkPrimary truncate' }, row.username),
          h('span', { class: 'text-xs text-google-text-muted dark:text-google-text-darkMuted truncate' }, row.email || '-')
        ])
      ])
    }
  },
  {
    title: t('admin.users.columns.org', '所属组织'),
    key: 'org',
    minWidth: 120,
    render(row) {
      return row.org || '-'
    }
  },
  {
    title: t('admin.users.columns.role', '角色身份'),
    key: 'role',
    width: 130,
    render(row) {
      const role = (row.role || '').toUpperCase()
      if (role.includes('SUPER')) {
        return t('admin.users.role_super_admin', '超级管理员')
      }
      if (role.includes('ADMIN')) {
        return t('admin.users.role_admin', '管理员')
      }
      return t('admin.users.role_user', '普通用户')
    }
  },
  {
    title: t('admin.users.columns.status', '账号状态'),
    key: 'status',
    width: 100,
    render(row) {
      return row.status !== false
        ? t('admin.users.status_active_short', '正常')
        : t('admin.users.status_banned_short', '封禁')
    }
  },
  {
    title: t('admin.users.columns.created_at', '注册时间'),
    key: 'createdAt',
    width: 170,
    render(row) {
      return h(
        'span',
        { class: 'whitespace-nowrap text-sm text-google-text-desc dark:text-google-text-darkDesc' },
        formatDateTime(row.createdAt)
      )
    }
  },
  {
    title: t('admin.users.columns.actions', '操作'),
    key: 'actions',
    width: 220,
    render(row) {
      return h('div', { class: 'flex items-center gap-2' }, [
        h(
          VBtn,
          {
            clz: 'normal',
            size: 'small',
            onClick: () => openEditModal(row)
          },
          { default: () => t('admin.users.actions.edit', '编辑') }
        ),
        h(
          VBtn,
          {
            clz: 'normal',
            size: 'small',
            onClick: () => handleToggleStatus(row)
          },
          { default: () => row.status ? t('admin.users.actions.disable', '封禁') : t('admin.users.actions.enable', '解封') }
        ),
        h(
          VBtn,
          {
            clz: 'warning',
            size: 'small',
            onClick: () => openDeleteModal(row)
          },
          { default: () => t('admin.users.actions.delete', '删除') }
        )
      ])
    }
  }
])

async function fetchStats() {
  try {
    const res = await getAdminUserStatisticsApi()
    if (res.data) {
      Object.assign(statistics, res.data)
    }
  } catch {}
}

async function fetchUsers() {
  loading.value = true
  try {
    const params: AdminUserPageRequestDTO = {
      page: pagination.page,
      size: pagination.pageSize
    }
    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
    }
    if (filterRole.value) {
      params.role = filterRole.value
    }
    if (filterStatus.value !== null && filterStatus.value !== '') {
      params.status = filterStatus.value === 'true'
    }

    const res = await getAdminUserListApi(params)
    if (res.data) {
      userList.value = res.data.list || []
      pagination.itemCount = res.data.total || 0
    }
  } catch {
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchUsers()
}

function handlePageChange(p: number) {
  pagination.page = p
  fetchUsers()
}

function handlePageSizeChange(ps: number) {
  pagination.pageSize = ps
  pagination.page = 1
  fetchUsers()
}

function openCreateModal() {
  isEditMode.value = false
  editingUserId.value = null
  userForm.username = ''
  userForm.email = ''
  userForm.password = ''
  userForm.roleName = 'USER'
  userForm.org = ''
  userForm.bio = ''
  userForm.status = true
  isUserModalShow.value = true
}

function openEditModal(row: AdminUserDetailsVO) {
  isEditMode.value = true
  editingUserId.value = row.id
  userForm.username = row.username
  userForm.email = row.email
  userForm.password = ''
  userForm.roleName = row.role || 'USER'
  userForm.org = row.org || ''
  userForm.bio = row.bio || ''
  userForm.status = row.status !== false
  isUserModalShow.value = true
}

function closeUserModal() {
  isUserModalShow.value = false
}

async function handleSaveUser() {
  if (!userForm.username.trim()) {
    message.error(t('admin.users.validation.username_required', '请输入用户名'))
    return
  }
  if (!userForm.email.trim()) {
    message.error(t('admin.users.validation.email_required', '请输入电子邮箱'))
    return
  }
  if (!isEditMode.value && !userForm.password.trim()) {
    message.error(t('admin.users.validation.password_required', '请输入登录密码'))
    return
  }

  submitting.value = true
  try {
    if (isEditMode.value && editingUserId.value !== null) {
      await updateAdminUserApi(editingUserId.value, {
        username: userForm.username,
        email: userForm.email,
        password: userForm.password ? userForm.password : undefined,
        roleName: userForm.roleName,
        org: userForm.org,
        bio: userForm.bio,
        status: userForm.status
      })
      message.success(t('admin.users.messages.update_success', '更新用户信息成功'))
    } else {
      await createAdminUserApi({
        username: userForm.username,
        password: userForm.password,
        email: userForm.email,
        roleName: userForm.roleName,
        org: userForm.org,
        bio: userForm.bio
      })
      message.success(t('admin.users.messages.create_success', '创建新用户成功'))
    }
    isUserModalShow.value = false
    await fetchUsers()
    await fetchStats()
  } catch {
  } finally {
    submitting.value = false
  }
}

async function handleToggleStatus(row: AdminUserDetailsVO) {
  const nextStatus = !row.status
  try {
    await toggleAdminUserStatusApi(row.id, nextStatus)
    message.success(t('admin.users.messages.toggle_status_success', '用户状态更新成功'))
    row.status = nextStatus
    await fetchStats()
  } catch {
  }
}

function openDeleteModal(row: AdminUserDetailsVO) {
  userToDelete.value = row
  isDeleteModalShow.value = true
}

async function confirmDeleteUser() {
  if (!userToDelete.value) return
  deleting.value = true
  try {
    await deleteAdminUserApi(userToDelete.value.id)
    message.success(t('admin.users.messages.delete_success', '用户已成功删除'))
    isDeleteModalShow.value = false
    userToDelete.value = null
    await fetchUsers()
    await fetchStats()
  } catch {
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  fetchStats()
  fetchUsers()
})
</script>
