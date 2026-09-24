<template>
  <n-layout class="app-layout" has-sider>
    <volur-sidebar
      v-model:collapsed="appStore.isSidebarCollapsed"
      :menu-options="menuOptions"
      :value="activeKey"
      logo-route="/competitions"
      :show-bottom-action="userStore.isAdmin"
      :bottom-action-text="t('layout.admin_panel')"
      :bottom-action-icon="ShieldCheckmarkOutline"
      bottom-action-route="/admin/dashboard"
      @select="handleMenuSelect"
    />

    <n-layout class="app-main-layout">
      <volur-header mode="app" show-notification />

      <n-layout-content content-style="padding: 24px;" class="app-content">
        <router-view v-slot="{ Component, route: currentRoute }">
          <transition name="fade-fast">
            <component :is="Component" :key="currentRoute.path" />
          </transition>
        </router-view>
      </n-layout-content>

      <volur-footer mode="app" />
    </n-layout>
  </n-layout>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NLayout,
  NLayoutContent,
  NIcon,
  type MenuOption
} from 'naive-ui'
import VolurSidebar from '../components/VolurSidebar.vue'
import VolurHeader from '../components/VolurHeader.vue'
import VolurFooter from '../components/VolurFooter.vue'
import {
  TrophyOutline,
  MegaphoneOutline,
  ShieldCheckmarkOutline
} from '@vicons/ionicons5'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const appStore = useAppStore()
const userStore = useUserStore()

const activeKey = computed(() => (route.name as string) || 'Competitions')

const menuOptions = computed<MenuOption[]>(() => [
  {
    label: () => t('layout.competitions'),
    key: 'Competitions',
    icon: () => h(NIcon, null, { default: () => h(TrophyOutline) })
  },
  {
    label: () => t('layout.announcements'),
    key: 'Announcements',
    icon: () => h(NIcon, null, { default: () => h(MegaphoneOutline) })
  }
])

function handleMenuSelect(key: string) {
  if (route.name === key) return
  router.push({ name: key }).catch(() => {})
}
</script>

<style scoped lang="scss">
.app-layout {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background-color: var(--v-bg-base);
  transition: background-color 0.25s ease;
}

.app-main-layout {
  background-color: var(--v-bg-base);
  transition: background-color 0.25s ease;
}

.app-content {
  min-height: calc(100vh - 64px - 48px);
  background-color: var(--v-bg-base);
  overflow-y: auto;
  transition: background-color 0.25s ease;
}



.fade-fast-enter-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.fade-fast-enter-from {
  opacity: 0;
  transform: translateY(4px);
}
</style>
