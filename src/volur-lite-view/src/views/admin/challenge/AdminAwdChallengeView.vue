<template>
  <div class="admin-awd-challenge-view">
    <div class="page-header">
      <h2 class="page-title">{{ t('admin.challenges.awd_title') }}</h2>
      <p class="page-desc">{{ t('admin.challenges.awd_desc') }}</p>
    </div>

    <n-card class="content-card" :bordered="false">
      <div class="placeholder-content">
        <n-empty :description="t('admin.challenges.awd_placeholder')" />
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { NCard, NEmpty } from 'naive-ui'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<style scoped lang="scss">
.admin-awd-challenge-view {
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

.content-card {
  border-radius: 10px;
  min-height: 400px;
}

.placeholder-content {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}
</style>
