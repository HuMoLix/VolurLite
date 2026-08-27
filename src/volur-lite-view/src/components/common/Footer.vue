<template>
  <footer class="app-footer">
    <div class="app-footer__content">
      <p class="copyright-text">
        © {{ currentYear }} {{ systemStore.platformName }}. All rights reserved.
      </p>
      <p v-if="systemStore.icpNumber" class="icp-text">
        <a
          href="https://beian.miit.gov.cn/"
          target="_blank"
          rel="noopener noreferrer"
          class="icp-link"
        >
          {{ systemStore.icpNumber }}
        </a>
      </p>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useSystemStore } from '@/store/modules/system'

const currentYear = ref(new Date().getFullYear())
const systemStore = useSystemStore()
</script>

<style scoped lang="scss">
.app-footer {
  width: 100%;
  box-sizing: border-box;
  padding: 16px 32px;
  display: flex;
  justify-content: center;
  align-items: center;

  &__content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;

    .copyright-text {
      margin: 0;
      font-size: 13px;
      color: #444444;
    }

    .icp-text {
      margin: 0;
      font-size: 12px;

      .icp-link {
        color: #444444;
        text-decoration: none;
        transition: color 0.2s ease;

        &:hover {
          color: #000000;
          text-decoration: underline;
        }
      }
    }
  }
}
</style>
