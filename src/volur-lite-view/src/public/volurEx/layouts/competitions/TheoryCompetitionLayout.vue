<template>
  <competition-layout
    class="theory-competition-layout"
    main-class="theory-arena-main"
    mode="theory"
    user-role-fallback="Candidate"
    default-title="理论知识考核竞技场"
    :exit-text="t('competition.exit_arena', '退出考场')"
    :warning-threshold-minutes="15"
    :show-candidate-pill="true"
    :countdown-labels="{
      pending: '距离考核开始还有',
      running: '考核进行中 - 剩余时间',
      ended: '理论考核已结束'
    }"
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
  name: 'TheoryCompetitionLayout'
})

import { useI18n } from 'vue-i18n'
import CompetitionLayout from './CompetitionLayout.vue'

const { t } = useI18n()
</script>
