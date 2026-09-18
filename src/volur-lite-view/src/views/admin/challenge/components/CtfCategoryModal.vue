<template>
  <n-modal
    v-model:show="visible"
    preset="card"
    title="CTF 赛题方向分类管理"
    style="width: 820px; max-width: 95vw;"
    class="ctf-category-modal"
  >
    <VolurTableCard
      :bordered="true"
      :table-bordered="true"
      :scroll-x="780"
      :columns="tableColumns"
      :data="categoryList"
      :loading="loadingCategories"
      size="small"
      table-class="category-inner-table"
      :pagination="false"
      :expand-length="false"
      :max-height="420"
    >
      <template #filter-left>
        <div class="category-add-toolbar">
          <n-input
            v-model:value="newCategoryName"
            placeholder="分类名称 (例: Web)"
            clearable
            style="width: 150px;"
            @keyup.enter="handleCreateCategory"
          />
          <VolurSvgPicker
            v-model:value="newCategoryIcon"
            placeholder="选择分类图标"
            style="width: 170px;"
          />
          <n-input
            v-model:value="newCategoryDesc"
            placeholder="简要说明 (可选)"
            clearable
            style="width: 220px;"
            @keyup.enter="handleCreateCategory"
          />
          <n-button
            type="primary"
            class="theme-primary-btn"
            :loading="creatingCategory"
            @click="handleCreateCategory"
          >
            <template #icon>
              <n-icon :component="AddOutline" />
            </template>
            添加分类
          </n-button>
        </div>
      </template>
    </VolurTableCard>

    <!-- Edit Category Modal -->
    <n-modal
      v-model:show="showEditModal"
      preset="card"
      title="编辑分类"
      style="width: 460px; max-width: 90vw;"
      :mask-closable="false"
    >
      <n-form label-placement="left" label-width="80">
        <n-form-item label="分类名称" required>
          <n-input
            v-model:value="editCategoryName"
            placeholder="分类名称 (例: Web)"
            maxlength="64"
            clearable
            @keyup.enter="handleSaveEditCategory"
          />
        </n-form-item>
        <n-form-item label="分类图标">
          <VolurSvgPicker
            v-model:value="editCategoryIcon"
            placeholder="选择分类图标"
            style="width: 100%;"
          />
        </n-form-item>
        <n-form-item label="简要说明">
          <n-input
            v-model:value="editCategoryDesc"
            type="textarea"
            :rows="3"
            placeholder="简要说明 (可选)"
            maxlength="255"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <div class="edit-modal-footer">
          <n-button @click="showEditModal = false">取消</n-button>
          <n-button
            type="primary"
            class="theme-primary-btn"
            :loading="updatingCategory"
            @click="handleSaveEditCategory"
          >
            保存
          </n-button>
        </div>
      </template>
    </n-modal>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch, h } from 'vue'
import {
  NModal,
  NInput,
  NButton,
  NIcon,
  NPopconfirm,
  NForm,
  NFormItem,
  useMessage,
  type DataTableColumns
} from 'naive-ui'
import { VolurSvgPicker, VolurIcon, VolurTableCard } from '@/public/volurEx'
import { AddOutline } from '@vicons/ionicons5'
import {
  getAdminCtfCategoriesApi,
  createAdminCtfCategoryApi,
  updateAdminCtfCategoryApi,
  deleteAdminCtfCategoryApi
} from '@/api/challenge'
import type { CtfCategoryVO } from '@/types/challenge'

interface Props {
  show: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'change'): void
}>()

const message = useMessage()

const visible = computed({
  get: () => props.show,
  set: (val: boolean) => emit('update:show', val)
})

const loadingCategories = ref(false)
const creatingCategory = ref(false)
const categoryList = ref<CtfCategoryVO[]>([])

const newCategoryName = ref('')
const newCategoryIcon = ref<string | null>(null)
const newCategoryDesc = ref('')

// Edit Modal State
const showEditModal = ref(false)
const updatingCategory = ref(false)
const editCategoryId = ref<number | string | null>(null)
const editCategoryName = ref('')
const editCategoryIcon = ref<string | null>(null)
const editCategoryDesc = ref('')

const tableColumns: DataTableColumns<CtfCategoryVO> = [
  {
    title: 'ID',
    key: 'id',
    width: 190,
    render(row) {
      return h('span', { class: 'mono-text font-bold' }, String(row.id))
    }
  },
  {
    title: '图标',
    key: 'icon',
    width: 130,
    render(row) {
      if (!row.icon) {
        return h('span', { class: 'text-muted' }, '-')
      }
      return h('div', { class: 'icon-cell' }, [
        h(VolurIcon, { name: row.icon, size: 20 }),
        h('span', { class: 'mono-text text-xs' }, row.icon)
      ])
    }
  },
  {
    title: '名称',
    key: 'name',
    width: 120,
    render(row) {
      return h('span', { style: 'font-weight: 500;' }, row.name)
    }
  },
  {
    title: '描述',
    key: 'description',
    minWidth: 160,
    ellipsis: { tooltip: true },
    render(row) {
      return row.description || h('span', { class: 'text-muted' }, '-')
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 130,
    fixed: 'right',
    render(row) {
      return h('div', { class: 'table-actions' }, [
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            type: 'primary',
            onClick: () => handleOpenEditModal(row)
          },
          { default: () => '编辑' }
        ),
        h(
          NPopconfirm,
          {
            onPositiveClick: () => handleDeleteCategory(row.id)
          },
          {
            trigger: () =>
              h(
                NButton,
                { size: 'tiny', quaternary: true, type: 'error' },
                { default: () => '删除' }
              ),
            default: () => `确定删除分类 "${row.name}" 吗？`
          }
        )
      ])
    }
  }
]

async function fetchCategories() {
  loadingCategories.value = true
  try {
    const res = await getAdminCtfCategoriesApi()
    if (res.success && res.data) {
      categoryList.value = res.data
    }
  } catch (err) {
    console.error('Failed to load categories in modal:', err)
  } finally {
    loadingCategories.value = false
  }
}

async function handleCreateCategory() {
  if (!newCategoryName.value.trim()) {
    message.warning('请填写分类名称')
    return
  }
  creatingCategory.value = true
  try {
    const res = await createAdminCtfCategoryApi({
      name: newCategoryName.value.trim(),
      icon: newCategoryIcon.value?.trim() || undefined,
      description: newCategoryDesc.value.trim() || undefined
    })
    if (res.success) {
      message.success('分类创建成功')
      newCategoryName.value = ''
      newCategoryIcon.value = null
      newCategoryDesc.value = ''
      fetchCategories()
      emit('change')
    }
  } catch (err) {
    console.error('Failed to create category:', err)
  } finally {
    creatingCategory.value = false
  }
}

function handleOpenEditModal(row: CtfCategoryVO) {
  editCategoryId.value = row.id
  editCategoryName.value = row.name
  editCategoryIcon.value = row.icon || null
  editCategoryDesc.value = row.description || ''
  showEditModal.value = true
}

async function handleSaveEditCategory() {
  if (!editCategoryId.value) return
  if (!editCategoryName.value.trim()) {
    message.warning('请填写分类名称')
    return
  }
  updatingCategory.value = true
  try {
    const res = await updateAdminCtfCategoryApi(editCategoryId.value, {
      name: editCategoryName.value.trim(),
      icon: editCategoryIcon.value?.trim() || undefined,
      description: editCategoryDesc.value.trim() || undefined
    })
    if (res.success) {
      message.success('分类更新成功')
      showEditModal.value = false
      fetchCategories()
      emit('change')
    }
  } catch (err) {
    console.error('Failed to update category:', err)
  } finally {
    updatingCategory.value = false
  }
}

async function handleDeleteCategory(id: number | string) {
  try {
    const res = await deleteAdminCtfCategoryApi(id)
    if (res.success) {
      message.success('分类已删除')
      fetchCategories()
      emit('change')
    }
  } catch (err) {
    console.error('Failed to delete category:', err)
  }
}

watch(
  () => props.show,
  (val) => {
    if (val) {
      fetchCategories()
    }
  }
)
</script>

<style scoped lang="scss">
.ctf-category-modal {
  :deep(.n-card__content) {
    padding: 10px 16px 16px 16px;
  }
}

.category-add-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.edit-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.icon-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

:deep(.theme-primary-btn) {
  background-color: var(--v-btn-primary-bg, #000000) !important;
  color: var(--v-btn-primary-text, #ffffff) !important;
  border: none !important;

  &:hover {
    background-color: var(--v-btn-primary-hover, #222222) !important;
  }

  &:active {
    background-color: var(--v-btn-primary-active, #333333) !important;
  }
}

.mono-text {
  font-family: 'Fira Code', monospace;
  font-size: 13px;
}

.text-xs {
  font-size: 11px;
}

.font-bold {
  font-weight: 600;
}

.text-muted {
  color: #888888;
  font-size: 12px;
}
</style>
