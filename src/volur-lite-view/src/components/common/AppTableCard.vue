<template>
  <div
    :class="[
      'app-table-card',
      'table-card',
      bordered ? 'table-card--bordered' : 'table-card--borderless',
      embedded ? 'table-card--embedded' : '',
      cardClass
    ]"
    :style="cardContentStyle"
  >
    <!-- 1. 顶部过滤与操作工具栏 (Toolbar / Filter Header) -->
    <div v-if="$slots.toolbar" class="table-toolbar-custom">
      <slot name="toolbar" />
    </div>
    <div
      v-else-if="showFilter && ($slots['filter-left'] || $slots.filter || $slots['filter-right'])"
      class="filter-header"
    >
      <div class="filter-left">
        <slot name="filter-left">
          <slot name="filter" />
        </slot>
      </div>
      <div v-if="$slots['filter-right']" class="filter-right">
        <slot name="filter-right" />
      </div>
    </div>

    <!-- 2. 表格前置区域 -->
    <div v-if="$slots['table-header']" class="table-header-extra">
      <slot name="table-header" />
    </div>

    <!-- 3. 主体内容区：默认插槽存在时渲染自定义列表（如卡片网格），否则渲染标准 n-data-table -->
    <div
      class="table-body-container"
      :style="{ minHeight: typeof tableMinHeight === 'number' ? `${tableMinHeight}px` : tableMinHeight }"
    >
      <slot v-if="$slots.default" />
      <n-data-table
        v-else
        :key="tableKey"
        :columns="columns"
        :data="data"
        :loading="loading"
        :remote="remote"
        :pagination="tablePagination"
        :row-key="rowKey"
        :checked-row-keys="checkedRowKeys"
        :bordered="tableBordered"
        :bottom-bordered="tableBottomBordered"
        :size="size"
        :single-column="singleColumn"
        :single-line="singleLine"
        :max-height="maxHeight"
        :min-height="tableMinHeight"
        :scroll-x="scrollX"
        :class="['app-data-table', tableClass]"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
        @update:sorter="handleSorterChange"
        @update:filters="handleFiltersChange"
        @update:checked-row-keys="handleCheckedRowKeysChange"
      >
        <template v-if="$slots.empty" #empty>
          <slot name="empty" />
        </template>
      </n-data-table>
    </div>

    <!-- 4. 表格后置区域 -->
    <div v-if="$slots['table-footer']" class="table-footer-extra">
      <slot name="table-footer" />
    </div>

    <!-- 5. 独立底部分页区 (支持通过插槽自定义或由组件接管外部分页) -->
    <slot v-if="$slots.pagination" name="pagination" />
    <div
      v-else-if="standalonePagination && (Number(itemCount) || 0) > 0"
      class="pagination-bar"
    >
      <n-pagination
        :key="`standalone-pagination-${page}-${pageSize}-${Number(itemCount) || 0}`"
        :page="page"
        :page-size="pageSize"
        :item-count="Number(itemCount) || 0"
        :page-sizes="pageSizes || [10, 20, 50]"
        show-size-picker
        show-quick-jumper
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, type CSSProperties } from 'vue'
import { NDataTable, NPagination, type DataTableColumns, type PaginationProps } from 'naive-ui'

export interface AppTableCardProps {
  /**
   * 是否带卡片描边
   * @default false
   */
  bordered?: boolean

  /**
   * 是否内嵌模式（保留属性兼容性）
   * @default false
   */
  embedded?: boolean

  /**
   * 卡片自定义附加 CSS 类名
   */
  cardClass?: string

  /**
   * n-card 内容区自定义样式
   */
  cardContentStyle?: CSSProperties | string

  /**
   * 是否渲染顶部筛选与工具栏
   * @default true
   */
  showFilter?: boolean

  /**
   * Naive UI 数据表格列配置
   */
  columns?: DataTableColumns<any>

  /**
   * 表格数据源
   */
  data?: any[]

  /**
   * 加载中状态
   * @default false
   */
  loading?: boolean

  /**
   * 是否为服务端受控异步分页模式
   * @default false
   */
  remote?: boolean

  /**
   * n-data-table 内置受控分页配置（若为 false 或空则禁用内置分页）
   */
  pagination?: PaginationProps | false

  /**
   * 表格每行数据唯一键提取函数
   */
  rowKey?: (row: any) => string | number

  /**
   * 选中的行主键数组
   */
  checkedRowKeys?: Array<string | number>

  /**
   * 数据表格内部边框（默认为 false，取消实线划分）
   * @default false
   */
  tableBordered?: boolean

  /**
   * 数据表格底部边框（默认为 false，取消实线划分）
   * @default false
   */
  tableBottomBordered?: boolean

  /**
   * 表格尺寸
   * @default 'medium'
   */
  size?: 'small' | 'medium' | 'large'

  /**
   * 是否取消行分割实线（默认为 true，取消内部实线划分）
   * @default true
   */
  singleColumn?: boolean

  /**
   * 单行分割线（默认为 true，避免纵向列实线）
   * @default true
   */
  singleLine?: boolean

  /**
   * 表格最大高度
   */
  maxHeight?: number | string

  /**
   * 表格最小高度
   */
  minHeight?: number | string

  /**
   * 表格最小保证展示的行数（防止末页数据行数较少时组件高度缩减跳动，最小固定至该行数高度）
   * @default 10
   */
  minRows?: number

  /**
   * 横向滚动最小宽度
   */
  scrollX?: number | string

  /**
   * 附加到 n-data-table 上的类名
   */
  tableClass?: string

  /**
   * 是否启用独立底部分页条（如外挂 n-pagination 模式）
   * @default false
   */
  standalonePagination?: boolean

  /**
   * 当前页码（配合独立底部分页使用）
   */
  page?: number

  /**
   * 每页条数（配合独立底部分页使用）
   */
  pageSize?: number

  /**
   * 数据总条数（配合独立底部分页使用）
   */
  itemCount?: number | string

  /**
   * 分页可选每页条数数组
   * @default [10, 20, 50]
   */
  pageSizes?: number[]
}

const props = withDefaults(defineProps<AppTableCardProps>(), {
  bordered: true,
  embedded: false,
  cardClass: '',
  showFilter: true,
  loading: false,
  remote: false,
  pagination: false,
  tableBordered: true,
  tableBottomBordered: false,
  size: 'medium',
  singleColumn: true,
  singleLine: true,
  standalonePagination: false,
  minRows: 10,
  page: 1,
  pageSize: 10,
  itemCount: 0
})

const emit = defineEmits<{
  (e: 'update:page', page: number): void
  (e: 'update:pageSize', pageSize: number): void
  (e: 'update:sorter', sorter: any): void
  (e: 'update:filters', filters: any): void
  (e: 'update:checkedRowKeys', keys: Array<string | number>): void
}>()

const defaultRowHeightMap: Record<'small' | 'medium' | 'large', number> = {
  small: 38,
  medium: 48,
  large: 56
}

const tableMinHeight = computed<string | number>(() => {
  if (props.minHeight !== undefined && props.minHeight !== null && props.minHeight !== '') {
    return props.minHeight
  }
  const baseH = defaultRowHeightMap[props.size || 'medium'] || 48
  const rows = props.minRows && props.minRows > 0 ? props.minRows : 10
  return baseH * rows
})

const tablePagination = computed(() => {
  if (props.pagination === false) return false
  if (props.pagination && typeof props.pagination === 'object') {
    const paginationConfig: Record<string, any> = { ...props.pagination }
    if (paginationConfig.itemCount !== undefined && paginationConfig.itemCount !== null) {
      paginationConfig.itemCount = Number(paginationConfig.itemCount) || 0
    }
    const p = paginationConfig.page || 1
    const s = paginationConfig.pageSize || 10
    const c = paginationConfig.itemCount || 0
    paginationConfig.key = `pagination-${p}-${s}-${c}`
    return paginationConfig as PaginationProps
  }
  return props.pagination
})

const tableKey = computed(() => {
  const p = tablePagination.value && typeof tablePagination.value === 'object' ? tablePagination.value.page : 1
  const s = tablePagination.value && typeof tablePagination.value === 'object' ? tablePagination.value.pageSize : 10
  const c = tablePagination.value && typeof tablePagination.value === 'object' ? tablePagination.value.itemCount : 0
  const firstId = props.data && props.data.length > 0
    ? (props.rowKey ? props.rowKey(props.data[0]) : props.data[0]?.id || 0)
    : 'empty'
  return `table-${p}-${s}-${c}-${firstId}`
})

function handlePageChange(page: number) {
  emit('update:page', page)
}

function handlePageSizeChange(pageSize: number) {
  emit('update:pageSize', pageSize)
}

function handleSorterChange(sorter: any) {
  emit('update:sorter', sorter)
}

function handleFiltersChange(filters: any) {
  emit('update:filters', filters)
}

function handleCheckedRowKeysChange(keys: Array<string | number>) {
  emit('update:checkedRowKeys', keys)
}
</script>

<style scoped lang="scss">
.app-table-card {
  box-sizing: border-box;
  width: 100%;
  border-radius: 10px;
  background-color: var(--v-bg-card, #ffffff);
  color: var(--v-text-primary, inherit);
  transition: background-color 0.3s ease, border-color 0.3s ease;

  &.table-card--bordered {
    border: 1px solid var(--v-border-base, #e5e4e7);
  }

  &.table-card--borderless {
    border: none;
  }

  &.table-card--embedded {
    padding: 0;
    background: transparent;
    border: none;
  }

  &:not(.table-card--embedded) {
    padding: 20px 24px;
  }

  // 列表数据表格外层边框与圆角启用，内部消除实线划分
  :deep(.n-data-table) {
    .n-data-table-wrapper {
      border: 1px solid var(--n-border-color, var(--n-merged-border-color, #e0e0e6)) !important;
      border-radius: 8px !important;
      overflow: hidden !important;
      box-shadow: none !important;
      display: flex;
      flex-direction: column;
    }

    .n-data-table-base-table-body {
      box-sizing: border-box;
    }

    .n-data-table-table {
      height: auto !important;

      &::after,
      &::before {
        border: none !important;
        display: none !important;
      }
    }

    // 消除表头实线下边框
    .n-data-table-th {
      border: none !important;
      border-bottom: none !important;
      border-right: none !important;
    }

    // 消除数据行内部实线横向/纵向分割线
    .n-data-table-td {
      border: none !important;
      border-bottom: none !important;
      border-right: none !important;

      &::after,
      &::before {
        border: none !important;
        display: none !important;
      }
    }
  }
}

.table-toolbar-custom {
  margin-bottom: 16px;
}

.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;

  .filter-left {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;
  }

  .filter-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

.table-header-extra {
  margin-bottom: 12px;
}

.table-body-container {
  width: 100%;
}

.table-footer-extra {
  margin-top: 12px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
