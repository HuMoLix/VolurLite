export function getDefaultRtcConfiguration(): RTCConfiguration {
  return {
    iceServers: [
      { urls: 'stun:stun.aliyun.com:3478' },
      { urls: 'stun:stun.cloud.tencent.com:3478' },
      { urls: 'stun:stun.qq.com:3478' },
      { urls: 'stun:stun.miwifi.com:3478' },
      { urls: 'stun:stun.l.google.com:19302' }
    ],
    iceCandidatePoolSize: 2
  }
}
