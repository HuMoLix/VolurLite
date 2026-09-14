<template>
  <n-layout-sider
    ref="siderRef"
    :bordered="bordered"
    :collapse-mode="collapseMode"
    :collapsed-width="Number(collapsedWidth)"
    :width="Number(width)"
    :collapsed="isCollapsed"
    :show-trigger="showTrigger"
    :content-style="contentStyle"
    :class="['volur-sidebar', siderClass]"
    @update:collapsed="handleUpdateCollapsed"
    @collapse="handleCollapse"
    @expand="handleExpand"
    @scroll="handleSidebarScroll"
  >
    <div class="sidebar-content-wrapper">
      <!-- 顶部 Logo 区域 (Sticky + 滑动一定px后吸顶毛玻璃质感) -->
      <div
        v-if="showLogo"
        class="logo-container"
        :class="{ 'is-sticky': isSticky }"
        @click="handleLogoClick"
      >
        <slot name="logo" :collapsed="isCollapsed" :theme="currentTheme">
          <transition name="logo-fade" mode="out-in">
            <!-- 展开态 Logo -->
            <img
              v-if="!isCollapsed"
              :key="`expanded-${currentTheme}`"
              :src="expandedLogoSrc"
              :alt="logoAlt"
              class="logo-img-expanded"
            />

            <!-- 折叠态 Logo (Favicon) -->
            <img
              v-else
              :key="`collapsed-${currentTheme}`"
              :src="collapsedLogoSrc"
              :alt="logoAlt"
              class="logo-img-collapsed"
            />
          </transition>
        </slot>
      </div>

      <!-- 菜单上部扩展插槽 -->
      <slot name="menu-top" />

      <!-- 主导航菜单 -->
      <slot
        name="menu"
        :collapsed="isCollapsed"
        :options="menuOptions"
        :value="currentValue"
      >
        <n-menu
          :collapsed="isCollapsed"
          :collapsed-width="Number(collapsedWidth)"
          :collapsed-icon-size="collapsedIconSize"
          :options="menuOptions"
          :value="currentValue"
          :accordion="accordion"
          class="volur-menu"
          @update:value="handleMenuSelect"
        />
      </slot>

      <!-- 菜单下部扩展插槽 -->
      <slot name="menu-bottom" />

      <!-- 底部操作区域 (未被多级菜单挤压时沉底，展开超过时自然顺延最后) -->
      <div v-if="$slots.bottom || showBottomAction" class="sidebar-bottom">
        <slot name="bottom" :collapsed="isCollapsed">
          <transition name="btn-fade" mode="out-in">
            <!-- 折叠态圆形图标悬浮提示按钮 -->
            <div v-if="isCollapsed" key="collapsed" class="bottom-action-collapsed">
              <n-tooltip placement="right" trigger="hover">
                <template #trigger>
                  <n-button
                    quaternary
                    circle
                    size="large"
                    class="action-btn-collapsed"
                    @click="handleBottomActionClick"
                  >
                    <template #icon>
                      <n-icon :size="20">
                        <component :is="bottomActionIcon" v-if="bottomActionIcon" />
                      </n-icon>
                    </template>
                  </n-button>
                </template>
                {{ bottomActionCollapsedText || bottomActionText }}
              </n-tooltip>
            </div>

            <!-- 展开态通栏按钮 -->
            <div v-else key="expanded" class="bottom-action-expanded">
              <n-button
                quaternary
                block
                size="medium"
                class="action-btn-expanded"
                @click="handleBottomActionClick"
              >
                <template #icon>
                  <n-icon :size="18">
                    <component :is="bottomActionIcon" v-if="bottomActionIcon" />
                  </n-icon>
                </template>
                {{ bottomActionText }}
              </n-button>
            </div>
          </transition>
        </slot>
      </div>
    </div>
  </n-layout-sider>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, type Component, type CSSProperties } from 'vue'
import { useRouter } from 'vue-router'
import {
  NLayoutSider,
  NMenu,
  NTooltip,
  NButton,
  NIcon,
  type MenuOption
} from 'naive-ui'
import { useAppStore } from '@/store/modules/app'

defineOptions({
  name: 'VolurSidebar'
})

export interface VolurSidebarProps {
  /**
   * 侧边栏展开宽度（以 AdminLayout 为基准，默认 230px）
   * @default 230
   */
  width?: number | string
  /**
   * 侧边栏折叠后的宽度
   * @default 64
   */
  collapsedWidth?: number | string
  /**
   * 是否折叠（可通过 v-model:collapsed 绑定）
   */
  collapsed?: boolean
  /**
   * 是否显示折叠触发器箭头
   * @default true
   */
  showTrigger?: boolean | 'bar' | 'arrow-circle'
  /**
   * 是否显示右侧分割边框
   * @default true
   */
  bordered?: boolean
  /**
   * 折叠模式
   * @default 'width'
   */
  collapseMode?: 'width' | 'transform'
  /**
   * 导航菜单数据项
   * @default () => []
   */
  menuOptions?: MenuOption[]
  /**
   * 当前激活选中的菜单 key（可通过 v-model:value 绑定）
   */
  value?: string
  /**
   * 菜单是否启用手风琴模式
   * @default false
   */
  accordion?: boolean
  /**
   * 折叠态图标尺寸
   * @default 22
   */
  collapsedIconSize?: number
  /**
   * 是否显示顶部 Logo 容器
   * @default true
   */
  showLogo?: boolean
  /**
   * 点击 Logo 跳转的目标路由
   */
  logoRoute?: string | null
  /**
   * 展开态 Logo 图片地址（不传则自适应系统亮暗主题）
   */
  logoExpandedSrc?: string
  /**
   * 折叠态 Logo 图片地址（不传则自适应系统亮暗主题 Favicon）
   */
  logoCollapsedSrc?: string
  /**
   * Logo alt 属性
   * @default 'Volur Lite'
   */
  logoAlt?: string
  /**
   * 主题（'light' | 'dark'，不传则自动响应全局 appStore.theme）
   */
  theme?: 'light' | 'dark'
  /**
   * 是否显示底部操作按钮区（在未自定义 #bottom 插槽时生效）
   * @default false
   */
  showBottomAction?: boolean
  /**
   * 底部操作按钮展开态文案
   */
  bottomActionText?: string
  /**
   * 底部操作按钮折叠态提示文案（缺省时复用 bottomActionText）
   */
  bottomActionCollapsedText?: string
  /**
   * 底部操作按钮图标组件
   */
  bottomActionIcon?: Component
  /**
   * 底部操作按钮点击跳转的目标路由（若未自定义 @bottom-action-click 事件则触发自动路由跳转）
   */
  bottomActionRoute?: string
  /**
   * 触发吸顶毛玻璃质感所需的滚动距离阈值（单位：px）
   * @default 10
   */
  stickyScrollThreshold?: number
  /**
   * 侧边栏内部容器样式
   * @default ''
   */
  contentStyle?: string | CSSProperties
  /**
   * 附加到 Sider 根节点的自定义样式类
   */
  siderClass?: string
}

const props = withDefaults(defineProps<VolurSidebarProps>(), {
  width: 230,
  collapsedWidth: 64,
  collapsed: undefined,
  showTrigger: true,
  bordered: true,
  collapseMode: 'width',
  menuOptions: () => [],
  value: undefined,
  accordion: false,
  collapsedIconSize: 22,
  showLogo: true,
  logoRoute: null,
  logoExpandedSrc: '',
  logoCollapsedSrc: '',
  logoAlt: 'Volur Lite',
  theme: undefined,
  showBottomAction: false,
  bottomActionText: '',
  bottomActionCollapsedText: '',
  bottomActionIcon: undefined,
  bottomActionRoute: '',
  stickyScrollThreshold: 10,
  contentStyle: '',
  siderClass: ''
})

const emit = defineEmits<{
  (e: 'update:collapsed', value: boolean): void
  (e: 'update:value', key: string): void
  (e: 'select', key: string, item: MenuOption): void
  (e: 'collapse'): void
  (e: 'expand'): void
  (e: 'logo-click', ev: MouseEvent): void
  (e: 'bottom-action-click', ev: MouseEvent): void
}>()

const modelCollapsed = defineModel<boolean>('collapsed')
const modelValue = defineModel<string>('value')

const router = useRouter()
const appStore = useAppStore()

// 双向响应式折叠状态
const isCollapsed = computed<boolean>({
  get() {
    if (modelCollapsed.value !== undefined) {
      return modelCollapsed.value
    }
    if (props.collapsed !== undefined) {
      return props.collapsed
    }
    return appStore.isSidebarCollapsed
  },
  set(val: boolean) {
    if (modelCollapsed.value !== undefined) {
      modelCollapsed.value = val
    }
    appStore.isSidebarCollapsed = val
    emit('update:collapsed', val)
  }
})

// 双向响应式菜单选中项
const currentValue = computed<string | undefined>({
  get() {
    if (modelValue.value !== undefined) {
      return modelValue.value
    }
    return props.value
  },
  set(val: string | undefined) {
    if (val !== undefined) {
      if (modelValue.value !== undefined) {
        modelValue.value = val
      }
      emit('update:value', val)
    }
  }
})

// 响应式主题计算
const currentTheme = computed<'light' | 'dark'>(() => {
  return props.theme || (appStore.theme === 'dark' ? 'dark' : 'light')
})

// 展开态 Logo 地址
const expandedLogoSrc = computed<string>(() => {
  if (props.logoExpandedSrc) return props.logoExpandedSrc
  return currentTheme.value === 'dark' ? '/long-logo-dark.svg' : '/long-logo.svg'
})

// 折叠态 Logo (Favicon) 地址
const collapsedLogoSrc = computed<string>(() => {
  if (props.logoCollapsedSrc) return props.logoCollapsedSrc
  return currentTheme.value === 'dark' ? '/favicon-dark.svg' : '/favicon.svg'
})

function handleUpdateCollapsed(val: boolean) {
  isCollapsed.value = val
}

function handleCollapse() {
  isCollapsed.value = true
  emit('collapse')
}

function handleExpand() {
  isCollapsed.value = false
  emit('expand')
}

function handleLogoClick(ev: MouseEvent) {
  emit('logo-click', ev)
  if (props.logoRoute) {
    router.push(props.logoRoute).catch(() => {})
  }
}

function handleMenuSelect(key: string, item: MenuOption) {
  currentValue.value = key
  emit('select', key, item)
}

function handleBottomActionClick(ev: MouseEvent) {
  emit('bottom-action-click', ev)
  if (props.bottomActionRoute) {
    router.push(props.bottomActionRoute).catch(() => {})
  }
}

// Sider 实例与吸顶滚动状态标记
const siderRef = ref<InstanceType<typeof NLayoutSider> | null>(null)
const isSticky = ref<boolean>(false)
// 向后兼容 isScrolled
const isScrolled = isSticky

// 计算属性向后兼容暴露内部滚动容器 DOM
const scrollContainerRef = computed<HTMLElement | null>(() => {
  return siderRef.value?.$el?.querySelector('.n-layout-sider-scroll-container') || null
})

function handleSidebarScroll(ev: Event) {
  const target = ev.target as HTMLElement | null
  if (target) {
    isSticky.value = target.scrollTop > props.stickyScrollThreshold
  }
}

// 挂载原生滚动监听，双重保障滚动事件精准捕获
onMounted(() => {
  const el = siderRef.value?.$el?.querySelector('.n-layout-sider-scroll-container') as HTMLElement | null
  if (el) {
    el.addEventListener('scroll', handleSidebarScroll, { passive: true })
  }
})

onBeforeUnmount(() => {
  const el = siderRef.value?.$el?.querySelector('.n-layout-sider-scroll-container') as HTMLElement | null
  if (el) {
    el.removeEventListener('scroll', handleSidebarScroll)
  }
})

function scrollToTop(options?: ScrollToOptions) {
  if (siderRef.value) {
    siderRef.value.scrollTo({ top: 0, behavior: 'smooth', ...options })
  }
}

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
}

function expand() {
  isCollapsed.value = false
}

function collapse() {
  isCollapsed.value = true
}

defineExpose({
  isCollapsed,
  isSticky,
  isScrolled,
  siderRef,
  scrollContainerRef,
  scrollToTop,
  toggleCollapse,
  expand,
  collapse
})
</script>

<style scoped lang="scss">
.volur-sidebar {
  background-color: var(--n-color, #ffffff);
  z-index: 10;

  :deep(.n-layout-sider-scroll-container) {
    overflow-y: auto !important;
    overflow-x: hidden !important;
    overflow-anchor: none;

    // 细致优雅的高性能滚动条样式
    scrollbar-width: thin;
    scrollbar-color: rgba(0, 0, 0, 0.15) transparent;

    &::-webkit-scrollbar {
      width: 4px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }

    &::-webkit-scrollbar-thumb {
      background: rgba(0, 0, 0, 0.15);
      border-radius: 4px;

      &:hover {
        background: rgba(0, 0, 0, 0.28);
      }
    }

    @include theme-dark {
      scrollbar-color: rgba(255, 255, 255, 0.18) transparent;

      &::-webkit-scrollbar-thumb {
        background: rgba(255, 255, 255, 0.18);

        &:hover {
          background: rgba(255, 255, 255, 0.32);
        }
      }
    }
  }

  .sidebar-content-wrapper {
    min-height: 100%;
    display: flex;
    flex-direction: column;
    box-sizing: border-box;
  }

  .logo-container {
    position: sticky;
    top: 0;
    z-index: 10;
    height: 64px;
    width: 100%;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 16px;
    cursor: pointer;
    user-select: none;
    overflow: hidden;
    box-sizing: border-box;

    // 默认常态（未滚动或未达吸顶阈值）：纯色背景融合，标准分割线，无毛玻璃与阴影
    background-color: var(--n-color, #ffffff);
    border-bottom: 1px solid var(--v-border-base, #efeff5);
    border-right: 1px solid var(--v-border-base, #efeff5);
    transition: background-color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease, backdrop-filter 0.25s ease;

    // 滑动一定px后激活吸顶状态 (is-sticky)：半透明毛玻璃质感、半透明分割线与弥散阴影，完整保留右侧分割线
    &.is-sticky {
      background-color: rgba(255, 255, 255, 0.82);
      backdrop-filter: blur(12px) saturate(180%);
      -webkit-backdrop-filter: blur(12px) saturate(180%);
      border-bottom: 1px solid rgba(0, 0, 0, 0.08);
      border-right: 1px solid var(--v-border-base, #efeff5);
      box-shadow: 0 4px 16px -2px rgba(0, 0, 0, 0.06);
    }

    @include theme-dark {
      background-color: var(--n-color, #121826);
      border-bottom: 1px solid var(--v-border-base, #2e303a);
      border-right: 1px solid var(--v-border-base, #2e303a);

      &.is-sticky {
        background-color: rgba(18, 24, 38, 0.82);
        border-bottom: 1px solid rgba(255, 255, 255, 0.12);
        border-right: 1px solid var(--v-border-base, #2e303a);
        box-shadow: 0 4px 16px -2px rgba(0, 0, 0, 0.35);
      }
    }

    .logo-img-expanded {
      height: 28px;
      width: auto;
      max-width: 160px;
      object-fit: contain;
    }

    .logo-img-collapsed {
      width: 36px;
      height: 36px;
      object-fit: contain;
    }
  }

  .volur-menu {
    user-select: none;
    flex-shrink: 0;
    overflow: visible;
  }

  .sidebar-bottom {
    margin-top: auto;
    flex-shrink: 0;
    padding: 14px 12px 20px;
    border-top: 1px solid var(--v-border-base, #efeff5);
    display: flex;
    justify-content: center;
    box-sizing: border-box;

    @include theme-dark {
      border-top-color: var(--v-border-base, #2e303a);
    }

    .bottom-action-expanded {
      width: 100%;

      .action-btn-expanded {
        display: flex;
        justify-content: flex-start;
        align-items: center;
        gap: 8px;
        font-size: 13px;
        font-weight: 500;
        color: var(--v-text-desc, #444444);
        border-radius: 6px;
        transition: color 0.2s ease, background-color 0.2s ease;

        &:hover {
          color: var(--v-text-primary, #000000);
          background-color: rgba(0, 0, 0, 0.05);
        }

        @include theme-dark {
          &:hover {
            color: var(--v-text-primary, #ffffff);
            background-color: rgba(255, 255, 255, 0.08);
          }
        }
      }
    }

    .bottom-action-collapsed {
      display: flex;
      justify-content: center;

      .action-btn-collapsed {
        color: var(--v-text-desc, #444444);
        transition: color 0.2s ease, background-color 0.2s ease;

        &:hover {
          color: var(--v-text-primary, #000000);
          background-color: rgba(0, 0, 0, 0.05);
        }

        @include theme-dark {
          &:hover {
            color: var(--v-text-primary, #ffffff);
            background-color: rgba(255, 255, 255, 0.08);
          }
        }
      }
    }
  }
}

.logo-fade-enter-active,
.logo-fade-leave-active,
.btn-fade-enter-active,
.btn-fade-leave-active {
  transition: opacity 0.22s cubic-bezier(0.4, 0, 0.2, 1), transform 0.22s cubic-bezier(0.4, 0, 0.2, 1);
}

.logo-fade-enter-from,
.btn-fade-enter-from {
  opacity: 0;
  transform: scale(0.88);
}

.logo-fade-leave-to,
.btn-fade-leave-to {
  opacity: 0;
  transform: scale(0.88);
}
</style>
