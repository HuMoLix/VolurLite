import { ref, computed, onUnmounted } from 'vue'

export function useCountdown(defaultSeconds: number = 60) {
  const countdown = ref(0)
  const isCounting = computed(() => countdown.value > 0)
  let timer: ReturnType<typeof setInterval> | null = null

  function stop(): void {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  function reset(): void {
    stop()
    countdown.value = 0
  }

  function start(seconds: number = defaultSeconds): void {
    stop()
    countdown.value = seconds
    timer = setInterval(() => {
      if (countdown.value > 0) {
        countdown.value -= 1
      } else {
        stop()
      }
    }, 1000)
  }

  onUnmounted(() => {
    stop()
  })

  return {
    countdown,
    isCounting,
    start,
    reset,
    stop
  }
}
