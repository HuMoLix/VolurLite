<template>
  <div class="admin-user-view">
    <!-- Top section: Compact Metrics Statistics Cards in a single row -->
    <div class="stat-ribbon">
      <div class="stat-mini-card">
        <div class="stat-icon-wrapper primary">
          <n-icon size="18">
            <PeopleOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.totalUsers }}</div>
          <div class="stat-label">系统总用户</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper warning">
          <n-icon size="18">
            <ShieldCheckmarkOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.adminCount }}</div>
          <div class="stat-label">管理员数</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper success">
          <n-icon size="18">
            <CheckmarkCircleOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.activeCount }}</div>
          <div class="stat-label">正常启用</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper error">
          <n-icon size="18">
            <PersonRemoveOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.bannedCount }}</div>
          <div class="stat-label">已封禁</div>
        </div>
      </div>
    </div>

    <!-- Bottom section: User Management Table Card -->
    <AppTableCard
      :bordered="true"
      :table-bordered="true"
      :scroll-x="1170"
      remote
      :columns="columns"
      :data="userList"
      :loading="loading"
      :pagination="pagination"
      :row-key="(row: AdminUserDetailsVO) => String(row.id)"
      table-class="user-data-table"
      @update:sorter="handleSorterChange"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    >
      <template #filter-left>
        <div class="user-filter-left">
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索用户名 / 邮箱 / 组织"
            clearable
            class="filter-input-search"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>

          <n-select
            v-model:value="filterRole"
            placeholder="用户角色"
            clearable
            :options="roleSelectOptions"
            class="filter-select-role"
            @update:value="handleSearch"
          />

          <n-select
            v-model:value="filterStatus"
            placeholder="账号状态"
            clearable
            :options="statusSelectOptions"
            class="filter-select-status"
            @update:value="handleSearch"
          />

          <n-date-picker
            v-model:value="dateRange"
            type="datetimerange"
            clearable
            format="yyyy-MM-dd HH:mm"
            start-placeholder="注册起始时间"
            end-placeholder="注册截止时间"
            class="filter-date-picker"
            @update:value="handleSearch"
          />

          <div class="filter-action-buttons">
            <n-button type="primary" @click="handleSearch" class="theme-primary-btn">
              <template #icon>
                <n-icon :component="SearchOutline" />
              </template>
              查询
            </n-button>

            <n-button quaternary @click="handleReset">
              <template #icon>
                <n-icon :component="RefreshOutline" />
              </template>
              重置
            </n-button>
          </div>
        </div>
      </template>

      <template #filter-right>
        <div class="user-filter-right">
          <n-button type="primary" class="theme-primary-btn action-create-btn" @click="openCreateModal">
            <template #icon>
              <n-icon :component="AddOutline" />
            </template>
            新增用户
          </n-button>
        </div>
      </template>
    </AppTableCard>

    <!-- Create / Edit User Modal -->
    <n-modal
      v-model:show="showUserModal"
      preset="card"
      :title="isEditMode ? '编辑用户资料' : '创建新用户'"
      style="width: 540px;"
      :mask-closable="false"
    >
      <n-form
        ref="userFormRef"
        :model="userFormData"
        :rules="userFormRules"
        label-placement="left"
        label-width="90"
        require-mark-placement="right-hanging"
      >
        <n-form-item label="用户名" path="username">
          <n-input
            v-model:value="userFormData.username"
            placeholder="请输入用户名（3-32位字符）"
            :disabled="isEditMode && userFormData.username === 'VolurSuperAdmin'"
          />
        </n-form-item>

        <n-form-item label="电子邮箱" path="email">
          <n-input
            v-model:value="userFormData.email"
            placeholder="请输入有效的电子邮箱"
          />
        </n-form-item>

        <n-form-item
          :label="isEditMode ? '新密码' : '登录密码'"
          path="password"
        >
          <n-input
            v-model:value="userFormData.password"
            type="password"
            show-password-on="click"
            :placeholder="isEditMode ? '留空则保持原密码不变' : '请输入密码（至少6位）'"
          />
        </n-form-item>

        <n-form-item label="用户角色" path="roleName">
          <n-select
            v-model:value="userFormData.roleName"
            :options="roleModalOptions"
            placeholder="请选择用户权限角色"
            :disabled="isEditMode && userFormData.username === 'VolurSuperAdmin'"
          />
        </n-form-item>

        <n-form-item label="所属组织" path="org">
          <n-input
            v-model:value="userFormData.org"
            placeholder="选填，所属战队、公司或院校"
          />
        </n-form-item>

        <n-form-item label="个人简介" path="bio">
          <n-input
            v-model:value="userFormData.bio"
            type="textarea"
            :rows="3"
            placeholder="选填，个人简介或签名"
          />
        </n-form-item>

        <n-form-item v-if="isEditMode" label="账号状态" path="status">
          <div class="switch-row">
            <n-switch
              v-model:value="userFormData.status"
              :disabled="userFormData.username === 'VolurSuperAdmin'"
            />
            <span class="field-hint">
              {{ userFormData.status ? '正常启用' : '封禁禁用' }}
            </span>
          </div>
        </n-form-item>
      </n-form>

      <template #footer>
        <div class="modal-footer">
          <n-button @click="showUserModal = false">取消</n-button>
          <n-button
            type="primary"
            :loading="submitLoading"
            class="theme-primary-btn"
            @click="handleSubmitUserForm"
          >
            保存提交
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import {
  NIcon,
  NInput,
  NSelect,
  NDatePicker,
  NButton,
  NModal,
  NForm,
  NFormItem,
  NSwitch,
  NTag,
  NPopconfirm,
  NSpace,
  useMessage,
  type DataTableColumns,
  type FormInst,
  type FormRules,
  type SelectOption
} from 'naive-ui'
import AppAvatar from '@/components/common/AppAvatar.vue'
import AppTableCard from '@/components/common/AppTableCard.vue'
import {
  PeopleOutline,
  ShieldCheckmarkOutline,
  CheckmarkCircleOutline,
  PersonRemoveOutline,
  SearchOutline,
  RefreshOutline,
  AddOutline,
  CreateOutline,
  TrashOutline
} from '@vicons/ionicons5'
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
  AdminUserPageRequestDTO,
  AdminUserCreateDTO,
  AdminUserUpdateDTO
} from '@/types/user'

const message = useMessage()

// Statistics State
const statistics = reactive<AdminUserStatisticsVO>({
  totalUsers: 0,
  adminCount: 0,
  bannedCount: 0,
  activeCount: 0
})

// Query & Filter State
const searchKeyword = ref('')
const filterRole = ref<string | null>(null)
const filterStatus = ref<string | null>(null)
const dateRange = ref<[number, number] | null>(null)
const sortBy = ref<string>('id')
const sortOrder = ref<'asc' | 'desc'>('desc')

const roleSelectOptions: SelectOption[] = [
  { label: '全部角色', value: '' },
  { label: '普通用户 (USER)', value: 'USER' },
  { label: '管理员 (ADMIN)', value: 'ADMIN' },
  { label: '超级管理员 (SUPER_ADMIN)', value: 'SUPER_ADMIN' }
]

const statusSelectOptions: SelectOption[] = [
  { label: '全部状态', value: '' },
  { label: '正常启用', value: 'true' },
  { label: '已封禁', value: 'false' }
]

const roleModalOptions: SelectOption[] = [
  { label: '普通用户 (USER)', value: 'USER' },
  { label: '管理员 (ADMIN)', value: 'ADMIN' }
]

// Table & Pagination State
const loading = ref(false)
const userList = ref<AdminUserDetailsVO[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  prefix: (info: { itemCount?: number }) => `共 ${info.itemCount || 0} 条用户记录`
})

// Table Columns Definition
const columns: DataTableColumns<AdminUserDetailsVO> = [
  {
    title: 'ID',
    key: 'id',
    width: 180,
    sorter: true,
    render(row) {
      return h('span', { class: 'id-text' }, String(row.id))
    }
  },
  {
    title: '用户',
    key: 'username',
    minWidth: 260,
    sorter: true,
    render(row) {
      return h(
        'div',
        {
          class: 'user-cell',
          style: {
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            minWidth: '0'
          }
        },
        [
          h(AppAvatar, {
            round: true,
            size: 34,
            src: row.avatarUrl,
            name: row.username
          }),
          h(
            'div',
            {
              class: 'user-info',
              style: {
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'center',
                gap: '2px',
                minWidth: '0',
                flex: '1',
                overflow: 'hidden'
              }
            },
            [
              h(
                'span',
                {
                  class: 'user-name',
                  title: row.username,
                  style: {
                    fontSize: '14px',
                    fontWeight: '600',
                    color: 'var(--v-text-primary, #111111)',
                    lineHeight: '1.3',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap'
                  }
                },
                row.username
              ),
              row.email
                ? h(
                    'span',
                    {
                      class: 'user-email',
                      title: row.email,
                      style: {
                        fontSize: '12px',
                        color: 'var(--v-text-desc, #444444)',
                        lineHeight: '1.3',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap'
                      }
                    },
                    row.email
                  )
                : h(
                    'span',
                    {
                      class: 'user-email-empty',
                      style: {
                        fontSize: '12px',
                        color: 'var(--v-text-disabled, #999999)',
                        lineHeight: '1.3',
                        fontStyle: 'italic',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap'
                      }
                    },
                    '未设置邮箱'
                  )
            ]
          )
        ]
      )
    }
  },
  {
    title: '所属组织',
    key: 'org',
    width: 150,
    render(row) {
      return row.org || h('span', { class: 'text-muted' }, '-')
    }
  },
  {
    title: '角色身份',
    key: 'role',
    width: 130,
    sorter: true,
    render(row) {
      const role = (row.role || 'USER').toUpperCase()
      if (role === 'SUPER_ADMIN') {
        return h(NTag, { type: 'warning', size: 'small', round: true }, { default: () => '超级管理员' })
      } else if (role === 'ADMIN') {
        return h(NTag, { type: 'info', size: 'small', round: true }, { default: () => '管理员' })
      }
      return h(NTag, { size: 'small', round: true }, { default: () => '普通用户' })
    }
  },
  {
    title: '账号状态',
    key: 'status',
    width: 130,
    render(row) {
      return h(
        'div',
        {
          class: 'switch-row',
          style: 'display: flex; align-items: center; gap: 8px;'
        },
        [
          h(NSwitch, {
            value: row.status,
            size: 'small',
            disabled: row.username === 'VolurSuperAdmin',
            'onUpdate:value': (val: boolean) => handleToggleStatus(row, val)
          }),
          h(
            'span',
            {
              class: 'field-hint',
              style: 'font-size: 12px; color: #444444;'
            },
            row.status ? '正常' : '封禁'
          )
        ]
      )
    }
  },
  {
    title: '注册时间',
    key: 'createdAt',
    width: 180,
    sorter: true,
    render(row) {
      if (!row.createdAt) return '-'
      const date = new Date(row.createdAt)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    fixed: 'right',
    render(row) {
      return h(NSpace, { size: 'small' }, {
        default: () => [
          h(
            NButton,
            {
              quaternary: true,
              size: 'tiny',
              type: 'primary',
              onClick: () => handleEditUser(row)
            },
            {
              icon: () => h(NIcon, null, { default: () => h(CreateOutline) }),
              default: () => '编辑'
            }
          ),
          row.username !== 'VolurSuperAdmin'
            ? h(
                NPopconfirm,
                {
                  onPositiveClick: () => handleDeleteUser(row)
                },
                {
                  trigger: () =>
                    h(
                      NButton,
                      {
                        quaternary: true,
                        size: 'tiny',
                        type: 'error'
                      },
                      {
                        icon: () => h(NIcon, null, { default: () => h(TrashOutline) }),
                        default: () => '删除'
                      }
                    ),
                  default: () => `确定删除用户 "${row.username}" 吗？此操作不可逆。`
                }
              )
            : null
        ]
      })
    }
  }
]

// Modal & Form State
const showUserModal = ref(false)
const isEditMode = ref(false)
const currentEditId = ref<string | number | null>(null)
const submitLoading = ref(false)
const userFormRef = ref<FormInst | null>(null)

const userFormData = reactive<{
  username: string
  email: string
  password?: string
  roleName: string
  org?: string
  bio?: string
  status?: boolean
}>({
  username: '',
  email: '',
  password: '',
  roleName: 'USER',
  org: '',
  bio: '',
  status: true
})

const userFormRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度需在 3 到 32 个字符之间', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入电子邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: ['input', 'blur'] }
  ],
  password: [
    {
      validator(_rule, value) {
        if (!isEditMode.value && (!value || value.length < 6)) {
          return new Error('新增用户登录密码长度至少为 6 位')
        }
        if (isEditMode.value && value && value.length < 6) {
          return new Error('修改密码时长度至少为 6 位')
        }
        return true
      },
      trigger: 'blur'
    }
  ]
}

// Data Fetching Methods
async function fetchStatistics() {
  try {
    const res = await getAdminUserStatisticsApi()
    if (res.data) {
      Object.assign(statistics, res.data)
    }
  } catch (error) {
    console.error('Failed to fetch user statistics:', error)
  }
}

async function fetchUserList() {
  loading.value = true
  try {
    const params: AdminUserPageRequestDTO = {
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchKeyword.value.trim() || undefined,
      role: filterRole.value || undefined,
      status: filterStatus.value === 'true' ? true : filterStatus.value === 'false' ? false : undefined,
      sortBy: sortBy.value,
      sortOrder: sortOrder.value
    }

    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = new Date(dateRange.value[0]).toISOString()
      params.endTime = new Date(dateRange.value[1]).toISOString()
    }

    const res = await getAdminUserListApi(params)
    if (res.data) {
      userList.value = res.data.list || []
      pagination.itemCount = res.data.total || 0
    }
  } catch (error) {
    console.error('Failed to fetch admin users:', error)
  } finally {
    loading.value = false
  }
}

// Filter Actions
function handleSearch() {
  pagination.page = 1
  fetchUserList()
}

function handleReset() {
  searchKeyword.value = ''
  filterRole.value = null
  filterStatus.value = null
  dateRange.value = null
  sortBy.value = 'id'
  sortOrder.value = 'desc'
  pagination.page = 1
  fetchUserList()
}

function handleSorterChange(sorter: { columnKey: string; order: 'ascend' | 'descend' | false } | null) {
  if (sorter && sorter.order) {
    sortBy.value = sorter.columnKey
    sortOrder.value = sorter.order === 'ascend' ? 'asc' : 'desc'
  } else {
    sortBy.value = 'id'
    sortOrder.value = 'desc'
  }
  fetchUserList()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchUserList()
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  fetchUserList()
}

// User Actions
async function handleToggleStatus(row: AdminUserDetailsVO, newStatus: boolean) {
  try {
    await toggleAdminUserStatusApi(row.id, newStatus)
    row.status = newStatus
    message.success(`用户 "${row.username}" 状态已${newStatus ? '启用' : '禁用'}`)
    fetchStatistics()
  } catch (error: any) {
    message.error(error.message || '切换状态失败')
  }
}

function openCreateModal() {
  isEditMode.value = false
  currentEditId.value = null
  userFormData.username = ''
  userFormData.email = ''
  userFormData.password = ''
  userFormData.roleName = 'USER'
  userFormData.org = ''
  userFormData.bio = ''
  userFormData.status = true
  showUserModal.value = true
}

function handleEditUser(row: AdminUserDetailsVO) {
  isEditMode.value = true
  currentEditId.value = row.id
  userFormData.username = row.username
  userFormData.email = row.email
  userFormData.password = ''
  userFormData.roleName = row.role || 'USER'
  userFormData.org = row.org || ''
  userFormData.bio = row.bio || ''
  userFormData.status = row.status
  showUserModal.value = true
}

async function handleSubmitUserForm() {
  userFormRef.value?.validate(async (errors) => {
    if (errors) return

    submitLoading.value = true
    try {
      if (isEditMode.value && currentEditId.value) {
        const updatePayload: AdminUserUpdateDTO = {
          username: userFormData.username,
          email: userFormData.email,
          roleName: userFormData.roleName,
          org: userFormData.org,
          bio: userFormData.bio,
          status: userFormData.status
        }
        if (userFormData.password && userFormData.password.trim()) {
          updatePayload.password = userFormData.password.trim()
        }
        await updateAdminUserApi(currentEditId.value, updatePayload)
        message.success('更新用户信息成功')
      } else {
        const createPayload: AdminUserCreateDTO = {
          username: userFormData.username.trim(),
          email: userFormData.email.trim(),
          password: userFormData.password!.trim(),
          roleName: userFormData.roleName,
          org: userFormData.org?.trim(),
          bio: userFormData.bio?.trim()
        }
        await createAdminUserApi(createPayload)
        message.success('创建新用户成功')
      }

      showUserModal.value = false
      fetchUserList()
      fetchStatistics()
    } catch (error: any) {
      message.error(error.message || '操作失败')
    } finally {
      submitLoading.value = false
    }
  })
}

async function handleDeleteUser(row: AdminUserDetailsVO) {
  try {
    await deleteAdminUserApi(row.id)
    message.success(`用户 "${row.username}" 已删除`)
    fetchUserList()
    fetchStatistics()
  } catch (error: any) {
    message.error(error.message || '删除用户失败')
  }
}

onMounted(() => {
  fetchStatistics()
  fetchUserList()
})
</script>

<style scoped lang="scss">
.admin-user-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-ribbon {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;

  @media (max-width: 992px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 576px) {
    grid-template-columns: 1fr;
  }
}

.stat-mini-card {
  background-color: var(--n-color, #ffffff);
  border-radius: 8px;
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
  transition: all 0.2s ease-in-out;

  &:hover {
    border-color: rgba(0, 0, 0, 0.12);
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
  }

  .stat-icon-wrapper {
    width: 38px;
    height: 38px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &.primary {
      background-color: rgba(99, 102, 241, 0.1);
      color: #6366f1;
    }

    &.warning {
      background-color: rgba(245, 158, 11, 0.1);
      color: #f59e0b;
    }

    &.success {
      background-color: rgba(16, 185, 129, 0.1);
      color: #10b981;
    }

    &.error {
      background-color: rgba(239, 68, 68, 0.1);
      color: #ef4444;
    }
  }

  .stat-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;

    .stat-value {
      font-size: 18px;
      font-weight: 700;
      color: var(--v-text-primary, #111111);
      line-height: 1.2;
    }

    .stat-label {
      font-size: 12px;
      color: #444444;
      font-weight: 500;
      white-space: nowrap;
    }
  }
}

:deep(.user-cell) {
  display: flex !important;
  align-items: center !important;
  gap: 10px;
  min-width: 0;
  padding: 2px 0;

  .user-info {
    display: flex !important;
    flex-direction: column !important;
    justify-content: center !important;
    gap: 2px;
    min-width: 0;
    flex: 1;
    overflow: hidden;

    .user-name {
      font-size: 14px;
      font-weight: 600;
      color: var(--v-text-primary, #111111);
      line-height: 1.3;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      letter-spacing: -0.01em;
    }

    .user-email {
      font-size: 12px;
      color: var(--v-text-desc, #444444);
      line-height: 1.3;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      transition: color 0.2s ease;

      &:hover {
        color: var(--v-text-primary, #111111);
      }
    }

    .user-email-empty {
      font-size: 12px;
      color: var(--v-text-disabled, #999999);
      line-height: 1.3;
      font-style: italic;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.text-muted {
  color: #999999;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.user-filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;

  .filter-input-search {
    width: 200px;
    min-width: 150px;
  }

  .filter-select-role,
  .filter-select-status {
    width: 125px;
    flex-shrink: 0;
  }

  .filter-date-picker {
    width: 290px;
  }

  .filter-action-buttons {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.user-filter-right {
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

  .user-filter-left {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 10px;

    .filter-input-search,
    .filter-select-role,
    .filter-select-status,
    .filter-date-picker {
      width: 100% !important;
    }

    .filter-action-buttons {
      width: 100%;
      display: flex;
      gap: 10px;

      :deep(.n-button) {
        flex: 1;
        justify-content: center;
      }
    }
  }

  .user-filter-right {
    width: 100%;
    display: flex;
    gap: 10px;

    :deep(.n-button) {
      flex: 1;
      justify-content: center;
    }
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
