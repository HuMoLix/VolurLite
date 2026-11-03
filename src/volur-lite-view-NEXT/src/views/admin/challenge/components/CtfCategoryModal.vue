<template>
  <VModal
    v-model:show="visible"
    :title="t('admin.challenges.category_manage_title', 'CTF 赛题方向分类管理')"
    :width="820"
    :trap-focus="false"
  >
    <div class="space-y-4">
      <div class="flex flex-wrap items-center gap-3 py-1">
        <div class="w-48">
          <VInput
            v-model:value="newCategoryName"
            :placeholder="t('admin.challenges.category_name_placeholder', '分类名称 (例: Web)')"
            clearable
            size="medium"
            @keyup.enter="handleCreateCategory"
          />
        </div>
        <div class="w-56">
          <VIconPicker
            v-model:value="newCategoryIcon"
            :placeholder="t('admin.challenges.category_icon_placeholder', '选择图标')"
            clearable
            size="medium"
          />
        </div>
        <div class="flex-1 min-w-[200px]">
          <VInput
            v-model:value="newCategoryDesc"
            :placeholder="t('admin.challenges.category_desc_placeholder', '简要说明 (可选)')"
            clearable
            size="medium"
            @keyup.enter="handleCreateCategory"
          />
        </div>
      </div>

      <VTable
        :columns="tableColumns"
        :data="categoryList"
        :loading="loadingCategories"
        :pagination="false"
        size="small"
        bordered
      />
    </div>

    <template #footer>
      <div class="flex justify-end">
        <VBtn
          clz="primary"
          size="medium"
          :left-icon="AddOutline"
          :loading="creatingCategory"
          @click="handleCreateCategory"
        >
          {{ t('admin.challenges.add_category_btn', '添加分类') }}
        </VBtn>
      </div>
    </template>
  </VModal>

  <VModal
    v-model:show="showEditModal"
    :title="t('admin.challenges.edit_category', '编辑分类')"
    :width="460"
    :trap-focus="false"
  >
    <div class="space-y-4">
      <div>
        <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
          {{ t('admin.challenges.category_name', '分类名称') }} *
        </label>
        <VInput
          v-model:value="editCategoryName"
          :placeholder="t('admin.challenges.category_name_placeholder', '分类名称 (例: Web)')"
          clearable
        />
      </div>
      <div>
        <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
          {{ t('admin.challenges.category_icon', '分类图标') }}
        </label>
        <VIconPicker
          v-model:value="editCategoryIcon"
          :placeholder="t('admin.challenges.category_icon_placeholder', '选择图标')"
          clearable
        />
      </div>
      <div>
        <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
          {{ t('admin.challenges.category_desc', '简要说明') }}
        </label>
        <VInput
          v-model:value="editCategoryDesc"
          type="textarea"
          :rows="3"
          :placeholder="t('admin.challenges.category_desc_placeholder', '简要说明 (可选)')"
        />
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3">
        <VBtn clz="normal" size="medium" @click="showEditModal = false">
          {{ t('common.cancel', '取消') }}
        </VBtn>
        <VBtn
          clz="primary"
          size="medium"
          :loading="updatingCategory"
          @click="handleSaveEditCategory"
        >
          {{ t('common.save', '保存') }}
        </VBtn>
      </div>
    </template>
  </VModal>
</template>

<script setup lang="ts">
import { ref, computed, watch, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import { AddOutline, CreateOutline, TrashOutline } from '@vicons/ionicons5'
import VModal from '@/components/common/VModal.vue'
import VBtn from '@/components/common/VBtn.vue'
import VInput from '@/components/common/VInput.vue'
import VTable from '@/components/common/VTable.vue'
import VIcon from '@/components/common/VIcon.vue'
import VIconPicker from '@/components/common/VIconPicker.vue'
import {
  getAdminCtfCategoriesApi,
  createAdminCtfCategoryApi,
  updateAdminCtfCategoryApi,
  deleteAdminCtfCategoryApi
} from '@/api/challenge'
import type { CtfCategoryVO } from '@/types/challenge'

const props = defineProps<{
  show: boolean
}>()

const emit = defineEmits<{
  (e: 'update:show', val: boolean): void
  (e: 'change'): void
}>()

const { t } = useI18n()
const message = useMessage()
const dialog = useDialog()

const visible = computed({
  get: () => props.show,
  set: (val) => emit('update:show', val)
})

const loadingCategories = ref(false)
const categoryList = ref<CtfCategoryVO[]>([])

const newCategoryName = ref('')
const newCategoryIcon = ref('')
const newCategoryDesc = ref('')
const creatingCategory = ref(false)

const showEditModal = ref(false)
const editCategoryId = ref<number | string | null>(null)
const editCategoryName = ref('')
const editCategoryIcon = ref('')
const editCategoryDesc = ref('')
const updatingCategory = ref(false)

const tableColumns = computed<DataTableColumns<CtfCategoryVO>>(() => [
  {
    title: t('admin.challenges.category_name', '分类名称'),
    key: 'name',
    width: 140,
    render(row) {
      return h('span', { class: 'font-semibold text-google-text-primary dark:text-google-text-darkPrimary' }, row.name)
    }
  },
  {
    title: t('admin.challenges.category_icon', '图标'),
    key: 'icon',
    width: 80,
    align: 'center',
    render(row) {
      if (!row.icon) {
        return '-'
      }
      return h('div', { class: 'flex items-center justify-center' }, [
        h(VIcon, { name: row.icon, size: 20 })
      ])
    }
  },
  {
    title: t('admin.challenges.category_desc', '说明'),
    key: 'description',
    render(row) {
      return h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary' }, row.description || '-')
    }
  },
  {
    title: t('common.actions', '操作'),
    key: 'actions',
    width: 150,
    align: 'center',
    render(row) {
      return h('div', { class: 'flex items-center justify-center gap-2' }, [
        h(
          VBtn,
          {
            clz: 'normal',
            size: 'small',
            leftIcon: CreateOutline,
            onClick: () => handleOpenEdit(row)
          },
          { default: () => t('common.edit', '编辑') }
        ),
        h(
          VBtn,
          {
            clz: 'danger',
            size: 'small',
            leftIcon: TrashOutline,
            onClick: () => handleDeleteCategory(row)
          },
          { default: () => t('common.delete', '删除') }
        )
      ])
    }
  }
])

async function fetchCategories() {
  loadingCategories.value = true
  try {
    const res = await getAdminCtfCategoriesApi()
    if (res.data) {
      categoryList.value = res.data
    }
  } catch {
  } finally {
    loadingCategories.value = false
  }
}

async function handleCreateCategory() {
  if (!newCategoryName.value.trim()) {
    message.warning(t('admin.challenges.category_name_required', '分类名称不能为空'))
    return
  }
  creatingCategory.value = true
  try {
    await createAdminCtfCategoryApi({
      name: newCategoryName.value.trim(),
      icon: newCategoryIcon.value.trim() || undefined,
      description: newCategoryDesc.value.trim() || undefined
    })
    message.success(t('admin.challenges.category_created', '分类创建成功'))
    newCategoryName.value = ''
    newCategoryIcon.value = ''
    newCategoryDesc.value = ''
    await fetchCategories()
    emit('change')
  } catch {
  } finally {
    creatingCategory.value = false
  }
}

function handleOpenEdit(row: CtfCategoryVO) {
  editCategoryId.value = row.id
  editCategoryName.value = row.name
  editCategoryIcon.value = row.icon || ''
  editCategoryDesc.value = row.description || ''
  showEditModal.value = true
}

async function handleSaveEditCategory() {
  if (!editCategoryId.value || !editCategoryName.value.trim()) {
    message.warning(t('admin.challenges.category_name_required', '分类名称不能为空'))
    return
  }
  updatingCategory.value = true
  try {
    await updateAdminCtfCategoryApi(editCategoryId.value, {
      name: editCategoryName.value.trim(),
      icon: editCategoryIcon.value.trim() || undefined,
      description: editCategoryDesc.value.trim() || undefined
    })
    message.success(t('admin.challenges.category_updated', '分类更新成功'))
    showEditModal.value = false
    await fetchCategories()
    emit('change')
  } catch {
  } finally {
    updatingCategory.value = false
  }
}

function handleDeleteCategory(row: CtfCategoryVO) {
  dialog.warning({
    title: t('admin.challenges.delete_category_confirm_title', '确认删除分类'),
    content: t('admin.challenges.delete_category_confirm_content', '删除分类后，该分类下的赛题分类关联将被清空，是否继续？'),
    positiveText: t('common.confirm', '确认删除'),
    negativeText: t('common.cancel', '取消'),
    onPositiveClick: async () => {
      try {
        await deleteAdminCtfCategoryApi(row.id)
        message.success(t('admin.challenges.category_deleted', '分类已删除'))
        await fetchCategories()
        emit('change')
      } catch {
      }
    }
  })
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
