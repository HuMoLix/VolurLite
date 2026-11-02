<template>
  <div class="blank-layout min-h-[100dvh] w-full flex flex-col bg-google-surface-light dark:bg-google-surface-dark transition-colors duration-300">
    <main class="flex-1 flex flex-col min-w-0 w-full">
      <slot>
        <router-view />
      </slot>
    </main>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'BlankLayout'
})
</script>
