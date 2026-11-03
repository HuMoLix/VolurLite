<template>
  <div class="space-y-6">
    <div>
      <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
        {{ t('admin.challenges.awd_title', 'AWD 题库') }}
      </h2>
      <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
        {{ t('admin.challenges.awd_desc', '攻防兼备模式 AWD 靶机镜像与防御 Check 规则库') }}
      </p>
    </div>

    <div class="p-16 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark flex flex-col items-center justify-center text-center">
      <div class="w-16 h-16 rounded-full bg-google-blue/10 dark:bg-google-blue-dark/20 text-google-blue dark:text-google-blue-dark flex items-center justify-center mb-4">
        <ShieldOutline class="w-8 h-8" />
      </div>
      <h3 class="text-base font-semibold text-google-text-primary dark:text-google-text-darkPrimary mb-1">
        {{ t('admin.challenges.awd_title', 'AWD 题库') }}
      </h3>
      <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary max-w-sm">
        {{ t('admin.challenges.awd_placeholder', 'AWD 靶机题目库与服务检查器正在建设中，敬请期待...') }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { ShieldOutline } from '@vicons/ionicons5'

const { t } = useI18n()
</script>
