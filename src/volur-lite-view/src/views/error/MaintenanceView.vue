<template>
  <div class="maintenance-view">
    <div class="maintenance-container">
      <div class="icon-wrapper">
        <n-icon size="64" class="maintenance-icon">
          <ConstructOutline />
        </n-icon>
      </div>

      <h1 class="maintenance-title">全站维护模式中</h1>

      <div class="notice-card">
        <div class="notice-header">
          <n-icon size="18" class="notice-icon">
            <AlertCircleOutline />
          </n-icon>
          <span class="notice-label">维护公告</span>
        </div>
        <p class="notice-content">
          {{ systemStore.maintenanceNotice }}
        </p>
      </div>

      <div class="actions">
        <n-button
          type="primary"
          class="theme-primary-btn"
          :loading="refreshing"
          @click="handleRefresh"
        >
          <template #icon>
            <n-icon><RefreshOutline /></n-icon>
          </template>
          刷新检测
        </n-button>

        <n-button secondary @click="handleAdminLogin">
          <template #icon>
            <n-icon><LogInOutline /></n-icon>
          </template>
          管理员通道
        </n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon, useMessage } from 'naive-ui'
import {
  ConstructOutline,
  AlertCircleOutline,
  RefreshOutline,
  LogInOutline
} from '@vicons/ionicons5'
import { useSystemStore } from '@/store/modules/system'

const router = useRouter()
const message = useMessage()
const systemStore = useSystemStore()
const refreshing = ref(false)

async function handleRefresh() {
  refreshing.value = true
  try {
    await systemStore.fetchPublicSettings(true)
    if (!systemStore.isMaintenanceMode) {
      message.success('系统已恢复正常服务，正在进入平台...')
      router.push('/')
    } else {
      message.info('系统仍在维护中，请稍后再试')
    }
  } catch {
    message.warning('无法连接到服务器，系统仍在维护中')
  } finally {
    refreshing.value = false
  }
}

function handleAdminLogin() {
  router.push('/auth/login')
}
</script>

<style scoped lang="scss">
.maintenance-view {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background-color: var(--n-color-embedded, #f8fafc);
}

.maintenance-container {
  max-width: 540px;
  width: 100%;
  padding: 40px 32px;
  background-color: var(--n-color, #ffffff);
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  border: 1px solid var(--v-border-base, #e2e8f0);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.icon-wrapper {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background-color: rgba(245, 158, 11, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;

  .maintenance-icon {
    color: #f59e0b;
  }
}

.maintenance-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--v-text-primary, #0f172a);
  margin: 0 0 16px 0;
}

.notice-card {
  width: 100%;
  background-color: rgba(245, 158, 11, 0.06);
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 28px;
  text-align: left;

  .notice-header {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 8px;
    color: #d97706;
    font-weight: 600;
    font-size: 13px;
  }

  .notice-content {
    margin: 0;
    font-size: 14px;
    line-height: 1.6;
    color: #444444;
    word-break: break-word;
  }
}

.actions {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  justify-content: center;
}

.theme-primary-btn {
  background-color: #000000;
  color: #ffffff;
  font-weight: 500;
  border-radius: 6px;

  &:hover {
    background-color: #222222;
  }
}
</style>
