<template>
  <div
    class="v-table"
    :class="[
      bordered ? 'v-table-bordered' : 'v-table-borderless',
      `size-${size}`,
      effectiveTheme,
      {
        'theme-dark': effectiveTheme === 'dark',
        'is-round': round === true
      }
    ]"
    :style="customStyle"
  >
    <div v-if="$slots.header || $slots.toolbar" class="v-table-header">
      <slot name="header">
        <slot name="toolbar" />
      </slot>
    </div>

    <n-data-table
      ref="dataTableInstRef"
      class="v-data-table"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-key="rowKey"
      :bordered="resolvedBordered"
      :bottom-bordered="resolvedBottomBordered"
      :single-line="singleLine"
      :single-column="singleColumn"
      :size="size"
      :scroll-x="scrollX"
      :max-height="maxHeight"
      :min-height="minHeight"
      :virtual-scroll="virtualScroll"
      :striped="striped"
      :remote="remote"
      :checked-row-keys="checkedRowKeys"
      :theme-overrides="tableThemeOverrides"
      v-bind="$attrs"
      @update:page="(page: number) => emit('update:page', page)"
      @update:page-size="(pageSize: number) => emit('update:pageSize', pageSize)"
      @update:sorter="(sorter: any) => emit('update:sorter', sorter)"
      @update:filters="(filters: any) => emit('update:filters', filters)"
      @update:checked-row-keys="(keys: any[], rows: any[], meta: any) => emit('update:checkedRowKeys', keys, rows, meta)"
      @update:expanded-row-keys="(keys: any[]) => emit('update:expandedRowKeys', keys)"
      @scroll="(ev: Event) => emit('scroll', ev)"
    >
      <template v-if="$slots.empty" #empty>
        <slot name="empty" />
      </template>
      <template v-if="$slots.loading" #loading>
        <slot name="loading" />
      </template>
    </n-data-table>

    <div v-if="$slots.footer" class="v-table-footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts" generic="T extends object = any">
import { ref, computed } from 'vue'
import type { CSSProperties } from 'vue'
import { NDataTable } from 'naive-ui'
import type { DataTableInst } from 'naive-ui'
import { useAppStore } from '@/store/modules/app'
import type { VTableProps, DataTableThemeOverrides } from './types'

defineOptions({
  name: 'VTable',
  inheritAttrs: false
})

const props = withDefaults(defineProps<VTableProps<T>>(), {
  bordered: true,
  bottomBordered: undefined,
  backgroundColor: 'transparent',
  columns: () => [],
  data: () => [],
  loading: false,
  pagination: false,
  rowKey: undefined,
  size: 'medium',
  singleLine: true,
  singleColumn: false,
  striped: false,
  scrollX: undefined,
  maxHeight: undefined,
  minHeight: undefined,
  virtualScroll: false,
  checkedRowKeys: undefined,
  remote: false,
  round: false,
  radius: undefined,
  theme: undefined,
  themeOverrides: undefined
})

const emit = defineEmits<{
  (e: 'update:page', page: number): void
  (e: 'update:pageSize', pageSize: number): void
  (e: 'update:sorter', sorter: any): void
  (e: 'update:filters', filters: any): void
  (e: 'update:checkedRowKeys', keys: any[], rows: any[], meta: any): void
  (e: 'update:expandedRowKeys', keys: any[]): void
  (e: 'scroll', ev: Event): void
}>()

const appStore = useAppStore()
const dataTableInstRef = ref<DataTableInst | null>(null)

const effectiveTheme = computed<'light' | 'dark'>(() => {
  return props.theme || (appStore.theme === 'dark' ? 'dark' : 'light')
})

const resolvedBordered = computed<boolean>(() => {
  return false
})

const resolvedBottomBordered = computed<boolean>(() => {
  if (props.bottomBordered !== undefined) {
    return props.bottomBordered
  }
  return props.bordered
})

const resolvedBgColor = computed<string>(() => {
  return props.backgroundColor || 'transparent'
})

const resolvedRadius = computed<string>(() => {
  if (props.round) {
    return '16px'
  }
  if (props.radius !== undefined && props.radius !== null && props.radius !== '') {
    return typeof props.radius === 'number' ? `${props.radius}px` : String(props.radius)
  }
  return '8px'
})

const customStyle = computed<CSSProperties>(() => {
  return {
    '--v-table-radius': resolvedRadius.value,
    '--v-table-bg': resolvedBgColor.value
  }
})

const tableThemeOverrides = computed<DataTableThemeOverrides>(() => {
  const isDark = effectiveTheme.value === 'dark'
  const isBordered = props.bordered

  const baseOverrides: DataTableThemeOverrides = {
    borderRadius: resolvedRadius.value,
    borderColor: isBordered
      ? (isDark ? '#3c4043' : '#e0e2e6')
      : (isDark ? 'rgba(255, 255, 255, 0.06)' : 'rgba(0, 0, 0, 0.06)'),
    thColor: isBordered
      ? (isDark ? '#292a2d' : '#f8f9fa')
      : 'transparent',
    thTextColor: isDark ? '#e3e3e3' : '#202124',
    thFontWeight: '500',
    tdColor: resolvedBgColor.value,
    tdTextColor: isDark ? '#e8eaed' : '#3c4043',
    tdColorHover: isBordered
      ? (isDark ? '#303134' : '#f1f3f4')
      : (isDark ? 'rgba(255, 255, 255, 0.04)' : 'rgba(0, 0, 0, 0.03)'),
    tdColorStriped: isDark ? 'rgba(255, 255, 255, 0.03)' : 'rgba(0, 0, 0, 0.02)',
    loadingColor: isDark ? '#8ab4f8' : '#1a73e8',
    fontSizeMedium: '14px',
    fontSizeSmall: '13px',
    fontSizeLarge: '15px',
    lineHeight: '1.5'
  }

  return {
    ...baseOverrides,
    ...props.themeOverrides
  }
})

defineExpose({
  filter: (filters: any) => dataTableInstRef.value?.filter(filters),
  filters: (filters: any) => dataTableInstRef.value?.filters(filters),
  clearFilter: () => dataTableInstRef.value?.clearFilter(),
  clearFilters: () => dataTableInstRef.value?.clearFilters(),
  clearSorter: () => dataTableInstRef.value?.clearSorter(),
  page: (page: number) => dataTableInstRef.value?.page(page),
  sort: (columnKey: any, order: any) => dataTableInstRef.value?.sort(columnKey, order),
  scrollTo: (options: any) => dataTableInstRef.value?.scrollTo(options),
  downloadCsv: (options?: any) => dataTableInstRef.value?.downloadCsv(options),
  dataTableInst: () => dataTableInstRef.value
})
</script>

<style scoped lang="scss">
.v-table {
  width: 100%;
  box-sizing: border-box;
  font-family: Inter, sans-serif;
  background-color: var(--v-table-bg, transparent);
  transition: background-color 0.15s ease-in-out, border-color 0.15s ease-in-out;

  :deep(.n-data-table) {
    font-family: Inter, sans-serif;

    .n-data-table-wrapper,
    .n-data-table-table,
    .n-data-table-thead,
    .n-data-table-tbody,
    .n-data-table-th,
    .n-data-table-td {
      font-family: Inter, sans-serif;
    }

    .n-data-table-tr {
      background-color: transparent !important;
    }

    .n-data-table-th {
      font-size: 13px;
      color: #202124;
      font-weight: 500;

      .n-data-table-th__title-wrapper,
      .n-data-table-th__title {
        color: inherit;
        font-size: inherit;
        font-weight: inherit;
      }
    }

    .n-data-table-td {
      color: #3c4043;
      font-weight: 500;
    }

    .n-data-table-thead tr:not(:last-child) > .n-data-table-th,
    .n-data-table-th[colspan]:not([colspan="1"]),
    .v-table-header-l1,
    .header-l1 {
      font-size: 12px !important;
      font-weight: 700 !important;
      color: #202124;

      .n-data-table-th__title {
        font-size: 12px !important;
        font-weight: 700 !important;
      }
    }

    .n-data-table-thead tr:nth-child(n+2) > .n-data-table-th,
    .n-data-table-thead tr:last-child > .n-data-table-th:not([rowspan]),
    .v-table-header-l2,
    .header-l2 {
      font-size: 12px !important;
      font-weight: 500 !important;
      color: #202124;

      .n-data-table-th__title {
        font-size: 12px !important;
        font-weight: 500 !important;
      }
    }

    .v-table-header-l3,
    .header-l3,
    .scoreboard-score,
    .header-score,
    .sub-score {
      font-size: 10px !important;
      font-weight: 500 !important;
    }

    .redirectable {
      color: var(--color-v3-text-link) !important;
      font-weight: 500 !important;
      cursor: pointer;
      text-decoration: none;
      transition: text-decoration 0.15s ease-in-out, color 0.15s ease-in-out;

      &:hover {
        text-decoration: underline !important;
      }
    }
  }

  :deep(.redirectable) {
    color: var(--color-v3-text-link) !important;
    font-weight: 500 !important;
    cursor: pointer;
    text-decoration: none;
    transition: text-decoration 0.15s ease-in-out, color 0.15s ease-in-out;

    &:hover {
      text-decoration: underline !important;
    }
  }

  html.dark & {
    :deep(.n-data-table) {
      .n-data-table-th {
        color: #e3e3e3;
      }

      .n-data-table-td {
        color: #e8eaed;
      }

      .n-data-table-thead tr:not(:last-child) > .n-data-table-th,
      .n-data-table-th[colspan]:not([colspan="1"]),
      .v-table-header-l1,
      .header-l1 {
        color: #e3e3e3;
      }

      .n-data-table-thead tr:nth-child(n+2) > .n-data-table-th,
      .n-data-table-thead tr:last-child > .n-data-table-th:not([rowspan]),
      .v-table-header-l2,
      .header-l2 {
        color: #e3e3e3;
      }
    }
  }

  .v-table-header {
    margin-bottom: 12px;
  }

  .v-table-footer {
    margin-top: 12px;
  }

  &.v-table-bordered {
    border: 1px solid #e0e2e6;
    border-radius: var(--v-table-radius, 8px);
    overflow: hidden;
    background-color: var(--v-table-bg, transparent);

    html.dark & {
      border-color: #3c4043;
      background-color: var(--v-table-bg, transparent);
    }

    :deep(.n-data-table) {
      background-color: transparent;

      .n-data-table-wrapper {
        border-radius: inherit;
        background-color: transparent;
      }

      .n-data-table-table {
        background-color: transparent;
      }

      .n-data-table-th {
        background-color: #f8f9fa;
        color: #202124;
        font-size: 13px;
        font-weight: 500;
        border-bottom: 1px solid #e0e2e6;
      }

      .n-data-table-td {
        background-color: var(--v-table-bg, transparent);
        color: #3c4043;
        font-weight: 500;
        border-bottom: 1px solid #e0e2e6;
      }
    }

    html.dark & {
      :deep(.n-data-table) {
        .n-data-table-th {
          background-color: #292a2d;
          color: #e3e3e3;
          border-bottom: 1px solid #3c4043;
        }

        .n-data-table-td {
          background-color: var(--v-table-bg, transparent);
          color: #e8eaed;
          font-weight: 500;
          border-bottom: 1px solid #3c4043;
        }
      }
    }
  }

  &.v-table-borderless {
    border: none;
    border-radius: var(--v-table-radius, 8px);
    background-color: var(--v-table-bg, transparent);

    html.dark & {
      background-color: var(--v-table-bg, transparent);
    }

    :deep(.n-data-table) {
      background-color: transparent;

      .n-data-table-wrapper {
        border-radius: inherit;
        background-color: transparent;
      }

      .n-data-table-table {
        background-color: transparent;
      }

      .n-data-table-th {
        background-color: transparent;
        color: #202124;
        font-size: 13px;
        font-weight: 500;
        border-bottom: 1px solid rgba(0, 0, 0, 0.06);
      }

      .n-data-table-td {
        background-color: var(--v-table-bg, transparent);
        color: #3c4043;
        font-weight: 500;
        border-bottom: 1px solid rgba(0, 0, 0, 0.04);
      }
    }

    html.dark & {
      :deep(.n-data-table) {
        .n-data-table-th {
          background-color: transparent;
          color: #e3e3e3;
          border-bottom: 1px solid rgba(255, 255, 255, 0.08);
        }

        .n-data-table-td {
          background-color: var(--v-table-bg, transparent);
          color: #e8eaed;
          font-weight: 500;
          border-bottom: 1px solid rgba(255, 255, 255, 0.06);
        }
      }
    }
  }

  &.size-small {
    font-size: 13px;

    :deep(.n-data-table-th),
    :deep(.n-data-table-td) {
      padding: 8px 12px;
    }
  }

  &.size-medium {
    font-size: 14px;

    :deep(.n-data-table-th),
    :deep(.n-data-table-td) {
      padding: 12px 16px;
    }
  }

  &.size-large {
    font-size: 15px;

    :deep(.n-data-table-th),
    :deep(.n-data-table-td) {
      padding: 16px 20px;
    }
  }

  &.is-round {
    border-radius: 16px !important;

    :deep(.n-data-table-wrapper) {
      border-radius: 16px !important;
    }
  }
}
</style>
