import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useProctorStore = defineStore('proctor', () => {
  const screenStream = ref<MediaStream | null>(null)
  const isPrecheckPassed = ref<boolean>(false)
  const competitionId = ref<string | number | null>(null)

  function setProctorReady(id: string | number, stream: MediaStream) {
    if (screenStream.value && screenStream.value !== stream) {
      screenStream.value.getTracks().forEach((track) => track.stop())
    }
    competitionId.value = id
    screenStream.value = stream
    isPrecheckPassed.value = true
  }

  function clearProctorState() {
    if (screenStream.value) {
      screenStream.value.getTracks().forEach((track) => track.stop())
      screenStream.value = null
    }
    isPrecheckPassed.value = false
    competitionId.value = null
  }

  return {
    screenStream,
    isPrecheckPassed,
    competitionId,
    setProctorReady,
    clearProctorState
  }
})
