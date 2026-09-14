<template>
  <AppTableCard v-bind="$attrs">
    <template v-for="(_, slotName) in $slots" #[slotName]="slotProps">
      <slot :name="slotName" v-bind="slotProps ?? {}" />
    </template>
  </AppTableCard>
</template>

<script setup lang="ts">
import AppTableCard from './AppTableCard.vue'

defineOptions({
  name: 'CommonTableCard',
  inheritAttrs: false
})
</script>
