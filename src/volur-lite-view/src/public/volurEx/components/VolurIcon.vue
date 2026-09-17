<template>
  <n-icon
    v-if="iconComponent"
    :size="size"
    :color="color"
    :depth="depth"
    class="volur-icon"
  >
    <component :is="iconComponent" />
  </n-icon>
  <slot v-else name="fallback" />
</template>

<script lang="ts">
import type { Component } from 'vue'
import * as IonIcons from '@vicons/ionicons5'

export interface VolurIconProps {
  name?: string | null
  size?: number | string
  color?: string
  depth?: 1 | 2 | 3 | 4 | 5
  component?: Component | null
}

const iconsMap: Record<string, Component> = IonIcons as unknown as Record<string, Component>

/**
 * 根据图标名称动态解析 @vicons/ionicons5 对应的 Vue 图标组件
 */
export function resolveIonIcon(name?: string | null): Component | null {
  if (!name || typeof name !== 'string') return null
  const trimmed = name.trim()
  if (!trimmed) return null

  // 1. 精准匹配
  if (iconsMap[trimmed]) {
    return iconsMap[trimmed]
  }

  // 2. 尝试添加/去除 Outline 后缀匹配
  if (!trimmed.endsWith('Outline') && iconsMap[`${trimmed}Outline`]) {
    return iconsMap[`${trimmed}Outline`]
  }

  // 3. 大小写不敏感查找
  const lower = trimmed.toLowerCase()
  const foundKey = Object.keys(iconsMap).find(k => k.toLowerCase() === lower)
  if (foundKey) {
    return iconsMap[foundKey]
  }

  return null
}

/**
 * 获取所有可用的 @vicons/ionicons5 图标名称集合
 */
export function getAllIconNames(): string[] {
  return Object.keys(iconsMap).sort()
}

/**
 * 平台推荐/精选图标分类集合
 */
export interface CuratedIconCategory {
  key: string
  name: string
  icons: string[]
}

export const CURATED_ICON_CATEGORIES: CuratedIconCategory[] = [
  {
    key: 'security_ctf',
    name: '安全赛题',
    icons: [
      'GlobeOutline',
      'ShieldCheckmarkOutline',
      'ShieldOutline',
      'LockClosedOutline',
      'LockOpenOutline',
      'KeyOutline',
      'TerminalOutline',
      'CodeSlashOutline',
      'BugOutline',
      'SkullOutline',
      'FingerPrintOutline',
      'EyeOutline',
      'CloudOutline',
      'ServerOutline',
      'HardwareChipOutline',
      'FlashOutline',
      'RadioOutline',
      'SearchOutline',
      'DocumentTextOutline',
      'AlertCircleOutline',
      'CompassOutline',
      'LayersOutline',
      'SparklesOutline',
      'ConstructOutline',
      'FolderOutline',
      'CubeOutline',
      'WifiOutline',
      'FlameOutline',
      'RocketOutline',
      'NuclearOutline',
      'FishOutline',
      'InfiniteOutline',
      'DiceOutline',
      'PulseOutline',
      'MagnetOutline',
      'TrophyOutline',
      'RibbonOutline',
      'MedalOutline',
      'FlagOutline',
      'TimerOutline',
      'SpeedometerOutline',
      'HammerOutline',
      'BuildOutline',
      'WarningOutline',
      'HelpCircleOutline'
    ]
  },
  {
    key: 'dev_tech',
    name: '开发技术',
    icons: [
      'CodeOutline',
      'CodeSlashOutline',
      'CodeWorkingOutline',
      'TerminalOutline',
      'GitBranchOutline',
      'GitCommitOutline',
      'GitMergeOutline',
      'GitNetworkOutline',
      'GitPullRequestOutline',
      'HardwareChipOutline',
      'ServerOutline',
      'DesktopOutline',
      'LaptopOutline',
      'SaveOutline',
      'CogOutline',
      'OptionsOutline',
      'LogoDocker',
      'LogoGithub',
      'LogoPython',
      'LogoJavascript',
      'LogoHtml5',
      'LogoCss3',
      'FileTrayFullOutline'
    ]
  },
  {
    key: 'storage_data',
    name: '存储数据',
    icons: [
      'ServerOutline',
      'CloudOutline',
      'CloudUploadOutline',
      'CloudDownloadOutline',
      'FolderOutline',
      'FolderOpenOutline',
      'DocumentOutline',
      'DocumentTextOutline',
      'AttachOutline',
      'ArchiveOutline',
      'LayersOutline',
      'AlbumsOutline',
      'PieChartOutline',
      'BarChartOutline',
      'AnalyticsOutline',
      'StatsChartOutline'
    ]
  },
  {
    key: 'comm_action',
    name: '交互操作',
    icons: [
      'MailOutline',
      'ChatbubbleOutline',
      'ChatbubblesOutline',
      'NotificationsOutline',
      'MegaphoneOutline',
      'VolumeMediumOutline',
      'VideocamOutline',
      'CameraOutline',
      'ImageOutline',
      'AddOutline',
      'TrashOutline',
      'CreateOutline',
      'RefreshOutline',
      'SyncOutline',
      'CheckmarkCircleOutline',
      'CloseCircleOutline',
      'StarOutline',
      'BookmarkOutline'
    ]
  }
]
</script>

<script setup lang="ts">
import { computed } from 'vue'
import { NIcon } from 'naive-ui'

const props = withDefaults(defineProps<VolurIconProps>(), {
  name: undefined,
  size: 18,
  color: undefined,
  depth: undefined,
  component: null
})

const iconComponent = computed<Component | null>(() => {
  if (props.component) return props.component
  return resolveIonIcon(props.name)
})
</script>

<style scoped lang="scss">
.volur-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  vertical-align: middle;
  transition: transform var(--v-transition-fast, 0.15s ease);
}
</style>
