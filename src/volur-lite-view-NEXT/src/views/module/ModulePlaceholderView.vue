<template>
  <div class="module-placeholder-view flex-1 flex flex-col min-w-0">
    <VViewlayout>
      <template #header>
        <header class="module-header">
          <h2 class="module-title">
            {{ config.title }}
          </h2>
          <p class="module-subtitle">
            {{ config.description }}
          </p>
        </header>
      </template>

      <div class="flex-1 flex items-center justify-center py-6">
        <div class="module-card">
          <div class="module-icon-halo">
            <n-icon :size="40" :component="config.icon" />
          </div>

          <div class="module-badge">
            <n-icon :size="12" :component="ConstructOutline" />
            <span>{{ t('modules.badge', '建设中') }}</span>
          </div>

          <h3 class="module-card-title">
            {{ t('modules.coming_soon_title', '该模块正在建设中') }}
          </h3>
          <p class="module-card-desc">
            {{ t('modules.coming_soon_desc', '我们正在打磨内容与交互，上线后会第一时间在这里与你见面。') }}
          </p>

          <div class="module-features">
            <div v-for="feature in config.features" :key="feature" class="module-feature">
              <n-icon :size="15" :component="CheckmarkCircleOutline" class="shrink-0" />
              <span>{{ feature }}</span>
            </div>
          </div>

          <div class="module-actions">
            <VBtn clz="primary" @click="goTo('/challenges')">
              {{ t('modules.go_training', '去题目训练') }}
            </VBtn>
            <VBtn clz="normal" @click="goTo('/competitions')">
              {{ t('modules.go_competitions', '去赛事大厅') }}
            </VBtn>
          </div>
        </div>
      </div>
    </VViewlayout>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import {
  CheckmarkCircleOutline,
  ConstructOutline,
  FlaskOutline,
  HelpCircleOutline,
  LibraryOutline,
  SchoolOutline
} from '@vicons/ionicons5'
import { VBtn } from '@/components/common'
import { VViewlayout } from '@/volur-ui'

defineOptions({
  name: 'ModulePlaceholderView'
})

const MODULE_ICONS: Record<string, Component> = {
  learning: SchoolOutline,
  resources: LibraryOutline,
  experimental: FlaskOutline,
  help: HelpCircleOutline
}

const MODULE_FALLBACKS: Record<string, { title: string; description: string; features: string[] }> = {
  learning: {
    title: '学习中心',
    description: '系统化的学习路径与实战课程，从入门到进阶',
    features: ['体系化学习路径', '配套练习与讲义', '学习进度跟踪']
  },
  resources: {
    title: '资源中心',
    description: '工具、镜像与文档资料，一站获取',
    features: ['常用工具与镜像', '官方文档与规范', '资源检索与收藏']
  },
  experimental: {
    title: '实验性功能',
    description: '正在孵化中的新能力，抢先体验',
    features: ['新功能预览', '实验性工具集', '意见反馈直达']
  },
  help: {
    title: '帮助中心',
    description: '常见问题、使用指南与支持渠道',
    features: ['常见问题解答', '平台使用指南', '联系技术支持']
  }
}

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const moduleKey = computed<string>(() => {
  const raw = route.meta.moduleKey
  return typeof raw === 'string' && raw in MODULE_FALLBACKS ? raw : 'learning'
})

const fallback = computed(() => MODULE_FALLBACKS[moduleKey.value])

const config = computed<{ icon: Component; title: string; description: string; features: string[] }>(() => {
  const key = moduleKey.value
  return {
    icon: MODULE_ICONS[key] || ConstructOutline,
    title: t(`modules.${key}.title`, fallback.value.title),
    description: t(`modules.${key}.desc`, fallback.value.description),
    features: [
      t(`modules.${key}.feature1`, fallback.value.features[0]),
      t(`modules.${key}.feature2`, fallback.value.features[1]),
      t(`modules.${key}.feature3`, fallback.value.features[2])
    ]
  }
})

function goTo(path: string) {
  router.push(path)
}
</script>

<style scoped lang="scss">
.module-placeholder-view {
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.module-header {
  padding-bottom: 0;
}

.module-title {
  margin: 0;
  color: #1b1b1e;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.25;

  html.dark & {
    color: #e8eaed;
  }
}

.module-subtitle {
  margin: 8px 0 24px;
  font-family: Inter, sans-serif;
  font-size: 13px;
  line-height: 18px;
  color: #5f6368;

  html.dark & {
    color: #9aa0a6;
  }
}

.module-card {
  width: 100%;
  max-width: 620px;
  padding: 40px 36px 32px;
  border-radius: 24px;
  text-align: center;
  background: #fff;
  border: 1px solid #d7d8da80;

  html.dark & {
    background: #0e0e0e;
    border-color: rgba(60, 64, 67, 0.6);
  }
}

.module-icon-halo {
  width: 84px;
  height: 84px;
  margin: 0 auto 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 24px;
  color: #1a73e8;
  background: radial-gradient(circle at 30% 25%, rgba(26, 115, 232, 0.16), rgba(26, 115, 232, 0.06));
  border: 1px solid rgba(26, 115, 232, 0.18);

  html.dark & {
    color: #a8c7fa;
    background: radial-gradient(circle at 30% 25%, rgba(168, 199, 250, 0.16), rgba(168, 199, 250, 0.05));
    border-color: rgba(168, 199, 250, 0.22);
  }
}

.module-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 9px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
  color: #1a73e8;
  background: rgba(26, 115, 232, 0.08);
  border: 1px solid rgba(26, 115, 232, 0.2);

  html.dark & {
    color: #a8c7fa;
    background: rgba(168, 199, 250, 0.1);
    border-color: rgba(168, 199, 250, 0.24);
  }
}

.module-card-title {
  margin: 14px 0 8px;
  font-size: 18px;
  font-weight: 700;
  line-height: 26px;
  color: #202124;

  html.dark & {
    color: #e3e3e3;
  }
}

.module-card-desc {
  max-width: 420px;
  margin: 0 auto;
  font-size: 13px;
  line-height: 21px;
  color: #5f6368;

  html.dark & {
    color: #9aa0a6;
  }
}

.module-features {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 320px;
  margin: 24px auto 0;
  text-align: left;
}

.module-feature {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 14px;
  border-radius: 12px;
  font-size: 12px;
  line-height: 18px;
  color: #3c4043;
  background: #f8f9fa;
  border: 1px solid rgba(218, 220, 224, 0.55);

  :deep(svg) {
    color: #34a853;
  }

  html.dark & {
    color: #bdc1c6;
    background: #1e1f20;
    border-color: rgba(60, 64, 67, 0.6);
  }
}

.module-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 28px;
}
</style>
