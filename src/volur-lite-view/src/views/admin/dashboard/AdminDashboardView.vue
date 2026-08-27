<template>
  <div class="admin-dashboard-view">
    <div class="page-header">
      <h2 class="page-title">{{ t('admin.dashboard.title') }}</h2>
      <p class="page-desc">{{ t('admin.dashboard.desc') }}</p>
    </div>

    <n-grid cols="1 s:2 m:4" :x-gap="16" :y-gap="16" class="stat-grid">
      <n-grid-item>
        <n-card class="stat-card" hoverable>
          <div class="stat-inner">
            <div class="stat-info">
              <span class="stat-label">{{ t('admin.dashboard.stat_competitions') }}</span>
              <span class="stat-value">--</span>
            </div>
            <n-icon size="36" class="stat-icon" color="#6366f1">
              <TrophyOutline />
            </n-icon>
          </div>
        </n-card>
      </n-grid-item>

      <n-grid-item>
        <n-card class="stat-card" hoverable>
          <div class="stat-inner">
            <div class="stat-info">
              <span class="stat-label">{{ t('admin.dashboard.stat_challenges') }}</span>
              <span class="stat-value">--</span>
            </div>
            <n-icon size="36" class="stat-icon" color="#10b981">
              <LibraryOutline />
            </n-icon>
          </div>
        </n-card>
      </n-grid-item>

      <n-grid-item>
        <n-card class="stat-card" hoverable>
          <div class="stat-inner">
            <div class="stat-info">
              <span class="stat-label">{{ t('admin.dashboard.stat_users') }}</span>
              <span class="stat-value">--</span>
            </div>
            <n-icon size="36" class="stat-icon" color="#f59e0b">
              <PeopleOutline />
            </n-icon>
          </div>
        </n-card>
      </n-grid-item>

      <n-grid-item>
        <n-card class="stat-card" hoverable>
          <div class="stat-inner">
            <div class="stat-info">
              <span class="stat-label">{{ t('admin.dashboard.stat_containers') }}</span>
              <span class="stat-value">--</span>
            </div>
            <n-icon size="36" class="stat-icon" color="#ec4899">
              <CubeOutline />
            </n-icon>
          </div>
        </n-card>
      </n-grid-item>
    </n-grid>
  </div>
</template>

<script setup lang="ts">
import { NGrid, NGridItem, NCard, NIcon } from 'naive-ui'
import {
  TrophyOutline,
  LibraryOutline,
  PeopleOutline,
  CubeOutline
} from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<style scoped lang="scss">
.admin-dashboard-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  .page-title {
    font-size: 22px;
    font-weight: 600;
    margin: 0;
    color: var(--v-text-primary, #111111);
  }

  .page-desc {
    font-size: 13px;
    color: #444444;
    margin-top: 4px;
  }
}

.stat-card {
  border-radius: 10px;

  .stat-inner {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .stat-info {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .stat-label {
        font-size: 13px;
        color: #444444;
      }

      .stat-value {
        font-size: 24px;
        font-weight: 700;
        color: var(--v-text-primary, #111111);
      }
    }
  }
}
</style>
