<template>
  <competition-layout
    class="ctf-competition-layout"
    main-class="ctf-arena-main"
    mode="ctf"
    user-role-fallback="Player"
    default-title="网络安全竞技场"
    :warning-threshold-minutes="30"
    :show-candidate-pill="false"
  >
    <template #default>
      <slot>
        <router-view v-slot="{ Component }">
          <transition name="arena-page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </slot>
    </template>
    <template v-if="$slots['header-left']" #header-left="slotProps">
      <slot name="header-left" v-bind="slotProps" />
    </template>
    <template v-if="$slots['header-center']" #header-center="slotProps">
      <slot name="header-center" v-bind="slotProps" />
    </template>
    <template v-if="$slots['header-right']" #header-right="slotProps">
      <slot name="header-right" v-bind="slotProps" />
    </template>
    <template v-if="$slots['header-team']" #header-team="slotProps">
      <slot name="header-team" v-bind="slotProps" />
    </template>
    <template v-if="$slots['footer']" #footer="slotProps">
      <slot name="footer" v-bind="slotProps" />
    </template>
  </competition-layout>
</template>

<script setup lang="ts">
defineOptions({
  name: 'CtfCompetitionLayout'
})

import CompetitionLayout from './CompetitionLayout.vue'
</script>
