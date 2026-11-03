<template>
  <div class="v-view-layout flex-1 flex flex-col min-w-0 w-full" :class="layoutClass">
    <header v-if="$slots.header" class="v-view-layout-header w-full">
      <slot name="header" />
    </header>

    <main class="v-view-layout-main flex-1 flex flex-col min-w-0 w-full" :class="contentClass">
      <slot />
    </main>

    <footer v-if="showFooter" class="v-view-layout-footer w-full mt-auto">
      <slot name="footer">
        <VFooter
          :copyright="copyright"
          :icp-text="icpText"
          :icp-url="icpUrl"
        />
      </slot>
    </footer>
  </div>
</template>

<script setup lang="ts">
import VFooter from './VFooter.vue'

defineOptions({
  name: 'VViewlayout'
})

interface Props {
  showFooter?: boolean
  copyright?: string
  icpText?: string
  icpUrl?: string
  layoutClass?: string
  contentClass?: string
}

withDefaults(defineProps<Props>(), {
  showFooter: true,
  copyright: undefined,
  icpText: undefined,
  icpUrl: 'https://beian.miit.gov.cn/',
  layoutClass: '',
  contentClass: ''
})
</script>
