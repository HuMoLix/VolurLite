<template>
  <footer
    :class="[
      'volur-footer',
      {
        'volur-footer--transparent': transparent,
        'volur-footer--bordered': bordered
      },
      footerClass
    ]"
    :style="footerStyle"
  >
    <div
      class="volur-footer__content"
      :class="{ 'is-column': direction === 'column' }"
    >
      <slot>
        <span class="copyright-text">
          <slot name="text">{{ displayText }}</slot>
        </span>
        <slot name="icp">
          <a
            v-if="showIcp && systemStore.icpNumber"
            href="https://beian.miit.gov.cn/"
            target="_blank"
            rel="noopener noreferrer"
            class="icp-link"
          >
            <img
              v-if="showIcpIcon"
              src="/beian.png"
              alt="工信部备案图标"
              class="icp-icon"
            />
            <span>{{ systemStore.icpNumber }}</span>
          </a>
        </slot>
        <slot name="extra" />
      </slot>
    </div>
  </footer>
</template>

<script setup lang="ts">
defineOptions({
  name: 'VolurFooter'
})

import { computed } from 'vue'
import { useSystemStore } from '@/store/modules/system'

export interface VolurFooterProps {
  /** 显示模式：app | admin | auth | competition | custom */
  mode?: 'app' | 'admin' | 'auth' | 'competition' | 'custom'
  /** 自定义文本，传此项时优先于根据 mode 计算的默认文本 */
  text?: string
  /** 排版方向：'row' | 'column'，默认 'row' */
  direction?: 'row' | 'column'
  /** 是否显示 ICP 备案信息，默认 true */
  showIcp?: boolean
  /** 是否显示 ICP 备案图标，默认 true */
  showIcpIcon?: boolean
  /** 是否带有上边框，默认 false（遵循透明无边框规范） */
  bordered?: boolean
  /** 是否透明背景，默认 true */
  transparent?: boolean
  /** 自定义类名 */
  footerClass?: string
  /** 自定义行内样式 */
  footerStyle?: Record<string, string | number> | string
}

const props = withDefaults(defineProps<VolurFooterProps>(), {
  mode: 'app',
  text: undefined,
  direction: 'row',
  showIcp: true,
  showIcpIcon: true,
  bordered: false,
  transparent: true,
  footerClass: '',
  footerStyle: undefined
})

const systemStore = useSystemStore()
const currentYear = new Date().getFullYear()

const displayText = computed(() => {
  if (props.text) return props.text

  const name = systemStore.platformName || 'Volur Lite'

  switch (props.mode) {
    case 'admin':
      return `${name} Management Control System © ${currentYear}`
    case 'auth':
      return `© ${currentYear} ${name}. All rights reserved.`
    case 'competition':
      return `© ${currentYear} ${name}.${systemStore.platformDesc ? ' ' + systemStore.platformDesc : ' All rights reserved.'}`
    case 'app':
    default:
      return `© ${currentYear} ${name}.${systemStore.platformDesc ? ' ' + systemStore.platformDesc : ''}`
  }
})
</script>

<style scoped lang="scss">
.volur-footer {
  width: 100%;
  box-sizing: border-box;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: transparent;
  border: none;

  &--bordered {
    border-top: 1px solid var(--v-border-base, #efeff5);
  }

  &__content {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: center;
    gap: 16px;
    font-size: 13px;
    line-height: 1.5;
    color: var(--v-text-desc, #444444);
    transition: color 0.25s ease;

    &.is-column {
      flex-direction: column;
      gap: 6px;
    }
  }

  .copyright-text {
    margin: 0;
    display: inline-flex;
    align-items: center;
  }

  .icp-link {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: var(--v-text-desc, #444444);
    text-decoration: none;
    transition: color 0.2s ease;

    &:hover {
      color: var(--v-text-primary, #000000);
      text-decoration: underline;
    }
  }

  .icp-icon {
    width: 14px;
    height: 14px;
    vertical-align: middle;
    object-fit: contain;
    flex-shrink: 0;
  }

  @include theme-dark {
    .volur-footer__content {
      color: var(--v-text-desc, #94a3b8);
    }
    .icp-link {
      color: var(--v-text-desc, #94a3b8);
      &:hover {
        color: var(--v-text-primary, #f8fafc);
      }
    }
  }

  .dark & {
    .volur-footer__content {
      color: var(--v-text-desc, #94a3b8);
    }
    .icp-link {
      color: var(--v-text-desc, #94a3b8);
      &:hover {
        color: var(--v-text-primary, #f8fafc);
      }
    }
  }
}
</style>
