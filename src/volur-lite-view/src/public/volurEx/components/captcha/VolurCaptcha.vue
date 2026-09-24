<template>
  <div class="volur-captcha" :class="{ 'is-headless': headless }">
    <!-- 1. 字符图形验证码 (IMAGE) -->
    <ImageCaptcha
      v-if="!headless && captchaType === 'IMAGE'"
      v-model="captchaCode"
      :captcha-image="captchaImage"
      @refresh="fetchCaptcha"
      @enter="emit('enter')"
    />

    <!-- 2. 行为式滑块拼图 (SLIDER) -->
    <SlidePuzzleCaptcha
      v-else-if="!headless && captchaType === 'SLIDER'"
      ref="inlineSliderRef"
      :scene="scene"
      :ticket="captchaTicket"
      @success="handleInlineSliderSuccess"
    />

    <!-- 3. 极高风控 / 带外邮箱动态安全码 (EMAIL) -->
    <EmailCaptcha
      v-else-if="!headless && captchaType === 'EMAIL'"
      v-model="otpCode"
      :account="account"
      :countdown="otpCountdown"
      :loading="isSendingOtp"
      @send="handleSendOtpClick"
      @enter="emit('enter')"
    />

    <!-- 4. 极高风控 / 短信动态安全码 (SMS) -->
    <SmsCaptcha
      v-else-if="!headless && captchaType === 'SMS'"
      v-model="otpCode"
      :countdown="otpCountdown"
      :loading="isSendingOtp"
      @send="handleSendOtpClick"
      @enter="emit('enter')"
    />

    <!-- 5. 动态行为式滑块拼图弹窗 (仅在唤起时异步挂载，支持 headless 场景与动态 OTP 拦截) -->
    <SlidePuzzleCaptcha
      v-if="showBehaviorModal"
      ref="behaviorSliderRef"
      :show="showBehaviorModal"
      :scene="behaviorScene"
      headless
      @update:show="handleBehaviorModalUpdateShow"
      @success="handleBehaviorSuccess"
      @close="handleBehaviorModalClose"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed, defineAsyncComponent, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useMessage } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { getCaptchaApi, sendCaptchaOtpApi } from '@/api/verification'
import { encryptCaptchaScene, fetchAndCacheCaptchaPublicKey } from '@/utils/captcha-crypto'
import { getDeviceFingerprint } from '@/utils/fingerprint'
import type {
  CaptchaDisplayType,
  VolurCaptchaProps,
  CaptchaTicketVO,
  CaptchaVerifyRequestDTO
} from './types'

// 按需异步懒加载子验证码模块：仅在需要具体模式或弹窗唤起时由 Vite 请求，阻断首屏瀑布
const ImageCaptcha = defineAsyncComponent(() => import('./ImageCaptcha.vue'))
const SlidePuzzleCaptcha = defineAsyncComponent(() => import('./SlidePuzzleCaptcha.vue'))
const EmailCaptcha = defineAsyncComponent(() => import('./EmailCaptcha.vue'))
const SmsCaptcha = defineAsyncComponent(() => import('./SmsCaptcha.vue'))

const props = withDefaults(defineProps<VolurCaptchaProps>(), {
  scene: 'LOGIN',
  sceneId: '',
  account: '',
  type: 'ADAPTIVE',
  autoLoad: true,
  headless: false
})

const emit = defineEmits<{
  (e: 'enter'): void
  (e: 'change', payload: CaptchaVerifyRequestDTO): void
  (e: 'success', ticketVO: CaptchaTicketVO): void
}>()

const route = useRoute()
const { t } = useI18n()
const message = useMessage()

const captchaType = ref<CaptchaDisplayType>(
  props.type !== 'ADAPTIVE' ? props.type : 'IMAGE'
)
const captchaId = ref<string>('')
const captchaImage = ref<string>('')
const captchaCode = ref<string>('')
const captchaTicket = ref<string>('')
const otpCode = ref<string>('')

const otpCountdown = ref<number>(0)
const isSendingOtp = ref<boolean>(false)

// 行为式滑块弹窗状态
const showBehaviorModal = ref<boolean>(false)
const behaviorScene = ref<string>(props.scene)
const inlineSliderRef = ref<any>(null)
const behaviorSliderRef = ref<any>(null)

let behaviorPromiseResolve: ((ticket: CaptchaTicketVO) => void) | null = null
let behaviorPromiseReject: ((reason?: any) => void) | null = null

// 兼容既有代码对 mode 属性的读取
const mode = computed<CaptchaDisplayType>(() => captchaType.value)

watch(
  () => props.type,
  (newType) => {
    if (newType && newType !== 'ADAPTIVE') {
      captchaType.value = newType
    }
  }
)

watch([captchaCode, captchaTicket, otpCode], () => {
  emit('change', buildCurrentPayload())
})

function buildCurrentPayload(): CaptchaVerifyRequestDTO {
  const fp = getDeviceFingerprint()
  return {
    captchaId: captchaType.value === 'IMAGE' ? captchaId.value : undefined,
    captchaCode: captchaType.value === 'IMAGE' ? captchaCode.value.trim() : undefined,
    captchaTicket: captchaType.value === 'SLIDER' || captchaTicket.value ? captchaTicket.value : undefined,
    otpCode:
      captchaType.value === 'EMAIL' || captchaType.value === 'SMS' ? otpCode.value.trim() : undefined,
    scene: props.scene,
    account: props.account,
    deviceFingerprint: fp.deviceHash,
    browserFamily: fp.browserFamily
  }
}

/**
 * 基于魔改 RC4 + RSA 加密向后端发起 POST /api/v1/captcha 请求获取自适应挑战
 */
async function fetchCaptcha() {
  if (props.headless || props.type !== 'ADAPTIVE') return

  try {
    // 优先拉取/预热服务端最新的 RSA 公钥
    await fetchAndCacheCaptchaPublicKey()

    const rawPath = props.sceneId || (route && route.path ? route.path : '') || '/auth/login'
    const encryptedSceneId = encryptCaptchaScene(rawPath)

    const res = await getCaptchaApi({
      captchaSceneId: encryptedSceneId,
      account: props.account ? props.account.trim() : undefined
    })

    if (res.data) {
      const type = (res.data.captchaType as CaptchaDisplayType) || 'IMAGE'
      captchaType.value = type

      if (type === 'IMAGE') {
        captchaId.value = res.data.captchaId || ''
        captchaImage.value = res.data.imageBase64 || ''
        captchaCode.value = ''
      }
    }
  } catch (err) {
    console.warn('[VolurCaptcha] 获取自适应人机验证挑战失败，保持图形验证码模式', err)
  }
}

function handleInlineSliderSuccess(ticketVO: CaptchaTicketVO) {
  captchaTicket.value = ticketVO.ticket
  emit('success', ticketVO)
}

function handleBehaviorModalUpdateShow(val: boolean) {
  showBehaviorModal.value = val
  if (!val && behaviorPromiseReject) {
    behaviorPromiseReject(new Error('User cancelled verification'))
    behaviorPromiseResolve = null
    behaviorPromiseReject = null
  }
}

function handleBehaviorSuccess(ticketVO: CaptchaTicketVO) {
  captchaTicket.value = ticketVO.ticket
  emit('success', ticketVO)
  if (behaviorPromiseResolve) {
    behaviorPromiseResolve(ticketVO)
    behaviorPromiseResolve = null
    behaviorPromiseReject = null
  }
  showBehaviorModal.value = false
}

function handleBehaviorModalClose() {
  showBehaviorModal.value = false
}

/**
 * 核心对外方法：编程式拉起人机行为滑块核验
 * 外部无需实例化或挂载任何 Modal，直接调用此方法即可获取核验通过凭证
 */
async function verifyBehavior(customScene?: string): Promise<CaptchaTicketVO> {
  const targetScene = customScene || props.scene
  behaviorScene.value = targetScene

  // 1. 若当前处于内联 SLIDER 模式且组件已挂载，优先直接复用内联实例拉起弹窗
  if (inlineSliderRef.value?.openModal) {
    const ticketVO = await inlineSliderRef.value.openModal(targetScene)
    captchaTicket.value = ticketVO.ticket
    emit('success', ticketVO)
    return ticketVO
  }

  // 2. 否则通过全局 behaviorSliderRef 唤起弹窗
  return new Promise<CaptchaTicketVO>((resolve, reject) => {
    behaviorPromiseResolve = resolve
    behaviorPromiseReject = reject
    showBehaviorModal.value = true

    nextTick(() => {
      if (behaviorSliderRef.value?.openModal) {
        behaviorSliderRef.value.openModal(targetScene).catch((err: any) => {
          if (behaviorPromiseReject) {
            behaviorPromiseReject(err)
            behaviorPromiseResolve = null
            behaviorPromiseReject = null
          }
        })
      }
    })
  })
}

async function handleSendOtpClick() {
  if (!props.account) {
    message.warning(t('auth.enter_username'))
    return
  }

  try {
    // 发送带外动态安全码前，强制要求完成一次滑块行为核验，防轰炸
    const ticketVO = await verifyBehavior('OTP')
    await doSendOtp(ticketVO.ticket)
  } catch {
    // 用户取消或滑块验证中断
  }
}

async function doSendOtp(ticket: string) {
  isSendingOtp.value = true
  try {
    await sendCaptchaOtpApi({
      account: props.account.trim(),
      captchaTicket: ticket
    })
    message.success(t('auth.verification_code_sent'))
    otpCountdown.value = 60
    const timer = setInterval(() => {
      otpCountdown.value--
      if (otpCountdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (err: any) {
    message.error(err.response?.data?.message || '动态安全码发送失败')
  } finally {
    isSendingOtp.value = false
  }
}

/**
 * 外部表单提交前统一调用：校验当前验证项并返回标准化 CaptchaVerifyRequestDTO
 */
async function getPayload(): Promise<CaptchaVerifyRequestDTO> {
  if (captchaType.value === 'NONE') {
    return buildCurrentPayload()
  }

  if (captchaType.value === 'IMAGE') {
    if (!captchaCode.value.trim()) {
      throw new Error(t('auth.code_required') || '请输入验证码')
    }
    return buildCurrentPayload()
  }

  if (captchaType.value === 'SLIDER') {
    if (captchaTicket.value) {
      return buildCurrentPayload()
    }
    // 未完成滑块验证时主动拉起弹窗并等待完成
    await verifyBehavior(props.scene)
    return buildCurrentPayload()
  }

  if (captchaType.value === 'EMAIL' || captchaType.value === 'SMS') {
    if (!otpCode.value.trim()) {
      throw new Error('请输入动态安全码')
    }
    return buildCurrentPayload()
  }

  return buildCurrentPayload()
}

/**
 * 外部业务请求失败回调：自适应跃迁风控防御状态
 */
function handleFailure(err: any) {
  const errorMsg = err?.response?.data?.message || err?.message || ''

  if (errorMsg.includes('滑块') || errorMsg.includes('拼图') || err?.response?.data?.data?.requireBehaviorVerify) {
    captchaType.value = 'SLIDER'
    captchaTicket.value = ''
    verifyBehavior(props.scene).catch(() => {})
  } else if (errorMsg.includes('动态码') || errorMsg.includes('邮箱') || err?.response?.data?.data?.requireOtp) {
    captchaType.value = 'EMAIL'
    otpCode.value = ''
  } else if (errorMsg.includes('短信')) {
    captchaType.value = 'SMS'
    otpCode.value = ''
  } else {
    // 普通失败刷新图形验证码
    fetchCaptcha()
  }
}

function reset() {
  captchaCode.value = ''
  captchaTicket.value = ''
  otpCode.value = ''
  if (props.type === 'ADAPTIVE' && !props.headless) {
    fetchCaptcha()
  }
}

onMounted(() => {
  if (props.autoLoad && !props.headless && props.type === 'ADAPTIVE') {
    fetchCaptcha()
  }
})

defineExpose({
  getPayload,
  verifyBehavior,
  handleFailure,
  reset,
  refresh: fetchCaptcha,
  mode,
  captchaType
})
</script>

<style lang="scss" scoped>
.volur-captcha {
  width: 100%;

  &.is-headless {
    display: contents;
  }
}
</style>
