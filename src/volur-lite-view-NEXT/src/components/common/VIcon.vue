<template>
  <n-icon
    v-if="iconComponent"
    :size="size"
    :color="color"
    :depth="depth"
    class="v-icon"
  >
    <component :is="iconComponent" />
  </n-icon>
  <slot v-else name="fallback" />
</template>

<script lang="ts">
import type { Component } from 'vue'
import * as Ion5 from '@vicons/ionicons5'
import { libraryMaps, ICON_SETS } from './iconMap'

const ion5FullMap: Record<string, Component> = Ion5 as unknown as Record<string, Component>
const mergedIon5Keys = Object.keys(ion5FullMap).sort()

const combinedSets: Record<string, string[]> = {
  ...ICON_SETS,
  ionicons5: mergedIon5Keys
}

export { combinedSets as ICON_SETS }

const allLibrariesList: Record<string, Component>[] = [
  ion5FullMap,
  libraryMaps.fluent,
  libraryMaps.antd,
  libraryMaps.material,
  libraryMaps.tabler,
  libraryMaps.carbon,
  libraryMaps.fa,
  libraryMaps.ionicons4
]

export function getAllIconNames(): string[] {
  const merged = new Set<string>()
  for (const list of Object.values(combinedSets)) {
    for (const name of list) {
      merged.add(name)
    }
  }
  return Array.from(merged).sort()
}

export function resolveIcon(name?: string | null): Component | null {
  if (!name || typeof name !== 'string') return null
  const trimmed = name.trim()
  if (!trimmed) return null

  if (trimmed.includes(':')) {
    const colonIdx = trimmed.indexOf(':')
    const libKey = trimmed.slice(0, colonIdx).toLowerCase()
    const iconName = trimmed.slice(colonIdx + 1)
    if (libKey === 'ionicons5' || libKey === 'ion5') {
      if (ion5FullMap[iconName]) return ion5FullMap[iconName]
    }
    const targetMap = libraryMaps[libKey]
    if (targetMap && targetMap[iconName]) {
      return targetMap[iconName]
    }
  }

  for (const libMap of allLibrariesList) {
    if (libMap[trimmed]) {
      return libMap[trimmed]
    }
  }

  if (!trimmed.endsWith('Outline') && ion5FullMap[`${trimmed}Outline`]) {
    return ion5FullMap[`${trimmed}Outline`]
  }

  const lower = trimmed.toLowerCase()
  for (const libMap of allLibrariesList) {
    const found = Object.keys(libMap).find((k) => k.toLowerCase() === lower)
    if (found) {
      return libMap[found]
    }
  }

  return null
}
</script>

<script setup lang="ts">
import { computed } from 'vue'
import { NIcon } from 'naive-ui'
import type { VIconProps } from './types'

defineOptions({
  name: 'VIcon'
})

const props = withDefaults(defineProps<VIconProps>(), {
  name: undefined,
  size: 18,
  color: undefined,
  depth: undefined,
  component: null
})

const iconComponent = computed<Component | null>(() => {
  if (props.component) return props.component
  return resolveIcon(props.name)
})
</script>

<style scoped lang="scss">
.v-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  vertical-align: middle;
  flex-shrink: 0;
  transition: transform 0.15s ease;
}
</style>
