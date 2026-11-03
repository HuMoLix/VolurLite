let timer: ReturnType<typeof setInterval> | null = null

self.onmessage = (event: MessageEvent) => {
  const { action, interval = 5000 } = event.data || {}
  if (action === 'START') {
    if (timer) {
      clearInterval(timer)
    }
    timer = setInterval(() => {
      self.postMessage({ type: 'HEARTBEAT_TICK', timestamp: Date.now() })
    }, interval)
  } else if (action === 'STOP') {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }
}

export {}
