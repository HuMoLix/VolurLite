<template>
  <div class="not-found-view">
    <n-result
      status="404"
      :title="t('error.not_found_title')"
      :description="t('error.not_found_desc')"
    >
      <template #footer>
        <n-button type="primary" @click="router.push('/')">{{ t('error.back_home') }}</n-button>
      </template>
    </n-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { NResult, NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const { t } = useI18n()
</script>

<style scoped lang="scss">
.not-found-view {
  min-height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
