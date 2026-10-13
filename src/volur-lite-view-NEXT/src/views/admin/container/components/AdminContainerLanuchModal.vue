<template>
  <AdminContainerLanuchModal
    :show="show"
    @update:show="handleUpdateShow"
    @success="handleSuccess"
  />
</template>

<script setup lang="ts">
import AdminContainerLanuchModal from '../AdminContainerLanuchModal.vue'

defineOptions({
  name: 'AdminContainerLanuchModalWrapper'
})

defineProps<{
  show: boolean
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
  success: []
}>()

function handleUpdateShow(val: boolean) {
  emit('update:show', val)
}

function handleSuccess() {
  emit('success')
}
</script>
