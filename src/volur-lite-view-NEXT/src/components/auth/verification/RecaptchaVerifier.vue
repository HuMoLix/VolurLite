<template>
  <div class="recaptcha-widget-container">
    <iframe
      ref="sandboxIframeRef"
      :srcdoc="sandboxHtml"
      class="recaptcha-sandbox-frame"
      sandbox="allow-scripts allow-same-origin"
      scrolling="no"
      frameborder="0"
      title="reCAPTCHA"
      @load="handleIframeLoaded"
    />
    <transition name="field-error-fade">
      <div v-if="displayErrorMsg" class="recaptcha-error-msg">
        <svg viewBox="0 0 24 24" class="w-3.5 h-3.5 fill-current shrink-0">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
        </svg>
        <span>{{ displayErrorMsg }}</span>
      </div>
    </transition>

    <RecaptchaModal
      v-model:visible="modalVisible"
      :challenge-type="modalChallengeType"
      :background-image="modalBgImage"
      :puzzle-image="modalPuzzleImage"
      :puzzle-y="modalPuzzleY"
      :captcha-image="modalCaptchaImage"
      :prompt-text="modalPromptText"
      :loading="modalLoading"
      :error-msg="modalErrorMsg"
      @close="handleModalClose"
      @refresh="handleModalRefresh"
      @submit="handleModalSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAppStore } from '@/store/modules/app'
import RecaptchaModal from './RecaptchaModal.vue'
import { getRecaptchaChallengeApi, verifyRecaptchaChallengeApi, type RecaptchaChallengeVO } from '@/api/captcha'
import { encryptPayload } from '@/utils/captcha-crypto'

defineOptions({
  name: 'Recaptcha'
})

const { t } = useI18n()

interface TrajectoryPoint {
  x: number
  y: number
  t: number
}

const props = withDefaults(
  defineProps<{
    modelValue?: boolean
    passed?: boolean
    token?: string
    ticket?: string
    dsh?: string
    tl?: string
    theme?: 'light' | 'dark' | 'auto'
    errorMsg?: string
  }>(),
  {
    modelValue: false,
    passed: false,
    token: '',
    ticket: '',
    dsh: '',
    tl: '',
    theme: 'auto',
    errorMsg: ''
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'update:passed', value: boolean): void
  (e: 'update:token', value: string): void
  (e: 'update:ticket', value: string): void
  (e: 'update:errorMsg', value: string): void
  (e: 'success', payload: { token: string; ticket: string; metrics?: Record<string, unknown> }): void
  (e: 'error', error: string): void
  (e: 'change', value: boolean): void
}>()

const appStore = useAppStore()
const sandboxIframeRef = ref<HTMLIFrameElement | null>(null)
const isIframeReady = ref(false)
const trajectoryBuffer = ref<TrajectoryPoint[]>([])
const internalError = ref('')
const displayErrorMsg = computed(() => props.errorMsg || internalError.value)
const MAX_BUFFER_SIZE = 120

const modalVisible = ref(false)
const modalChallengeType = ref<'CLICK' | 'PUZZLE' | 'SLIDER' | 'IMAGE' | 'SELECT_IMAGE'>('PUZZLE')
const modalBgImage = ref('')
const modalPuzzleImage = ref('')
const modalPuzzleY = ref(0)
const modalCaptchaImage = ref('')
const modalPromptText = ref('')
const modalLoading = ref(false)
const modalErrorMsg = ref('')

let activeChallengeData: RecaptchaChallengeVO | null = null
let pendingMetrics: Record<string, unknown> = {}
let pendingToken = ''

const effectiveTheme = computed<'light' | 'dark'>(() => {
  if (props.theme && props.theme !== 'auto') {
    return props.theme
  }
  return appStore.theme === 'dark' ? 'dark' : 'light'
})

function handleGlobalMouseMove(event: MouseEvent): void {
  const point: TrajectoryPoint = {
    x: Math.round(event.clientX),
    y: Math.round(event.clientY),
    t: Math.round(performance.now())
  }
  trajectoryBuffer.value.push(point)
  if (trajectoryBuffer.value.length > MAX_BUFFER_SIZE) {
    trajectoryBuffer.value.shift()
  }
}

function sendToSandbox(payload: Record<string, unknown>): void {
  if (!sandboxIframeRef.value || !sandboxIframeRef.value.contentWindow) return
  sandboxIframeRef.value.contentWindow.postMessage(payload, '*')
}

function handleIframeLoaded(): void {
  isIframeReady.value = true
  sendToSandbox({
    type: 'VM_CONFIG_SYNC',
    theme: effectiveTheme.value,
    passed: props.passed || props.modelValue,
    hasBackendSession: Boolean(props.dsh && props.tl),
    initialTrajectory: trajectoryBuffer.value.slice(-30)
  })
}

async function withMinimumDelay<T>(action: () => Promise<T>, minDurationMs = 1500): Promise<T> {
  const startTime = Date.now()
  try {
    return await action()
  } finally {
    const elapsed = Date.now() - startTime
    if (elapsed < minDurationMs) {
      await new Promise(resolve => setTimeout(resolve, minDurationMs - elapsed))
    }
  }
}

async function handleBackendChallengeFlow(metrics: Record<string, unknown>) {
  if (!props.dsh || !props.tl) {
    const msg = t('captcha.recaptcha.missing_credentials')
    internalError.value = msg
    emit('update:errorMsg', msg)
    sendToSandbox({ type: 'VM_SET_ERROR' })
    emit('error', msg)
    return
  }

  pendingMetrics = metrics
  try {
    let challengeData: RecaptchaChallengeVO | null = null
    let ticketResult = ''
    let isClickPass = false

    await withMinimumDelay(async () => {
      const res = await getRecaptchaChallengeApi(props.dsh, props.tl)
      const challenge = res.data
      activeChallengeData = challenge
      pendingToken = challenge.recaptchaToken
      challengeData = challenge

      if (challenge.challengeType === 'CLICK') {
        const payload = encryptPayload(metrics, challenge.publicKey)
        const verifyRes = await verifyRecaptchaChallengeApi(props.dsh, props.tl, {
          recaptchaToken: challenge.recaptchaToken,
          payload
        })
        ticketResult = verifyRes.data.recaptchaTicket
        isClickPass = true
      }
    }, 1500)

    if (isClickPass && challengeData) {
      sendToSandbox({ type: 'VM_CONFIRM_PASS' })
      emit('update:modelValue', true)
      emit('update:passed', true)
      emit('update:token', (challengeData as RecaptchaChallengeVO).recaptchaToken)
      emit('update:ticket', ticketResult)
      emit('change', true)
      emit('success', { token: (challengeData as RecaptchaChallengeVO).recaptchaToken, ticket: ticketResult, metrics })
    } else if (challengeData) {
      const challenge = challengeData as RecaptchaChallengeVO
      modalChallengeType.value = challenge.challengeType
      modalBgImage.value = challenge.backgroundImage || ''
      modalPuzzleImage.value = challenge.puzzleImage || ''
      modalPuzzleY.value = challenge.puzzleY || 0
      modalCaptchaImage.value = challenge.captchaImage || ''
      modalPromptText.value = challenge.promptText || ''
      modalErrorMsg.value = ''
      modalVisible.value = true
    }
  } catch (err: any) {
    const msg = err?.message || t('captcha.recaptcha.failed')
    internalError.value = msg
    emit('update:errorMsg', msg)
    sendToSandbox({ type: 'VM_SET_ERROR' })
    emit('error', msg)
  }
}

async function handleModalRefresh() {
  if (!props.dsh || !props.tl) return
  modalLoading.value = true
  modalErrorMsg.value = ''
  try {
    const res = await getRecaptchaChallengeApi(props.dsh, props.tl)
    activeChallengeData = res.data
    modalChallengeType.value = res.data.challengeType
    modalBgImage.value = res.data.backgroundImage || ''
    modalPuzzleImage.value = res.data.puzzleImage || ''
    modalPuzzleY.value = res.data.puzzleY || 0
    modalCaptchaImage.value = res.data.captchaImage || ''
    modalPromptText.value = res.data.promptText || ''
  } catch (err: any) {
    modalErrorMsg.value = err?.message || t('captcha.recaptcha.refresh_failed')
  } finally {
    modalLoading.value = false
  }
}

function handleModalClose() {
  modalVisible.value = false
  modalErrorMsg.value = ''
  sendToSandbox({ type: 'VM_RESET' })
}

async function handleModalSubmit(result: { x?: number; code?: string; userInput?: string }) {
  if (!activeChallengeData || !props.dsh || !props.tl) return
  modalLoading.value = true
  modalErrorMsg.value = ''
  try {
    const submitPayload = {
      ...result,
      telemetry: pendingMetrics
    }
    const payload = encryptPayload(submitPayload, activeChallengeData.publicKey)
    const verifyRes = await verifyRecaptchaChallengeApi(props.dsh, props.tl, {
      recaptchaToken: activeChallengeData.recaptchaToken,
      payload
    })
    const ticket = verifyRes.data.recaptchaTicket
    modalVisible.value = false
    sendToSandbox({ type: 'VM_CONFIRM_PASS' })
    emit('update:modelValue', true)
    emit('update:passed', true)
    emit('update:token', pendingToken)
    emit('update:ticket', ticket)
    emit('change', true)
    emit('success', { token: pendingToken, ticket, metrics: pendingMetrics })
  } catch (err: any) {
    modalErrorMsg.value = err?.message || t('captcha.recaptcha.verify_failed')
    handleModalRefresh()
  } finally {
    modalLoading.value = false
  }
}

function handleWindowMessage(event: MessageEvent): void {
  if (!sandboxIframeRef.value || event.source !== sandboxIframeRef.value.contentWindow) {
    return
  }

  const data = event.data
  if (!data || typeof data !== 'object') return

  if (data.type === 'VM_PULL_TRAJECTORY') {
    sendToSandbox({
      type: 'VM_PUSH_TRAJECTORY',
      points: trajectoryBuffer.value.slice(-60)
    })
    return
  }

  if (data.type === 'VM_CHALLENGE_START') {
    internalError.value = ''
    emit('update:errorMsg', '')
    return
  }

  if (data.type === 'VM_CHALLENGE_SUBMIT' || data.type === 'VM_CHALLENGE_SUCCESS') {
    const metrics = (data.metrics && typeof data.metrics === 'object') ? data.metrics : {}
    internalError.value = ''
    emit('update:errorMsg', '')
    handleBackendChallengeFlow(metrics)
    return
  }

  if (data.type === 'VM_CHALLENGE_ERROR') {
    const errText = typeof data.error === 'string' ? data.error : t('captcha.recaptcha.failed')
    internalError.value = errText
    emit('update:errorMsg', errText)
    emit('update:modelValue', false)
    emit('update:passed', false)
    emit('change', false)
    emit('error', errText)
  }
}

watch(effectiveTheme, (newTheme) => {
  sendToSandbox({
    type: 'VM_THEME_CHANGE',
    theme: newTheme
  })
})

watch(
  () => appStore.locale,
  () => {
    sendToSandbox({
      type: 'VM_LOCALE_CHANGE',
      notRobot: t('captcha.recaptcha.not_robot'),
      verified: t('captcha.recaptcha.verified')
    })
  }
)

watch(() => props.passed, (val) => {
  if (!val) {
    reset()
  }
})

watch(() => props.errorMsg, (newVal, oldVal) => {
  if (newVal && newVal !== oldVal) {
    sendToSandbox({
      type: 'VM_SET_ERROR'
    })
  }
})

function reset(): void {
  internalError.value = ''
  emit('update:errorMsg', '')
  trajectoryBuffer.value = []
  sendToSandbox({
    type: 'VM_RESET'
  })
}

onMounted(() => {
  window.addEventListener('mousemove', handleGlobalMouseMove, { passive: true })
  window.addEventListener('message', handleWindowMessage)
})

onUnmounted(() => {
  window.removeEventListener('mousemove', handleGlobalMouseMove)
  window.removeEventListener('message', handleWindowMessage)
})

defineExpose({
  reset
})

const sandboxHtml = computed(() => {
  const isDark = effectiveTheme.value === 'dark'
  const htmlClass = isDark ? 'dark' : ''
  const currentLang = appStore.locale || 'zh-CN'
  const notRobotLabel = t('captcha.recaptcha.not_robot')
  const privacyLabel = t('common.privacy')
  const termsLabel = t('common.terms')
  const verifiedLabel = t('captcha.recaptcha.verified')
  return `<!DOCTYPE html>
<html lang="${currentLang}" class="${htmlClass}">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <style>
    *, *::before, *::after {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      user-select: none;
      -webkit-user-select: none;
    }
    html, body {
      width: 100%;
      height: 100%;
      overflow: hidden;
      background: transparent;
      font-family: Roboto, -apple-system, BlinkMacSystemFont, "Segoe UI", Arial, sans-serif;
    }
    .rc-anchor {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 302px;
      height: 76px;
      padding: 0 12px;
      border-radius: 3px;
      border: 1px solid #dadce0;
      background-color: #f9f9f9;
      box-sizing: border-box;
      transition: background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
    }
    html.dark .rc-anchor {
      border-color: #3c4043;
      background-color: #292a2d;
    }
    .rc-anchor.rc-shake {
      animation: rcShake 0.4s cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
      border-color: #ea4335 !important;
    }
    html.dark .rc-anchor.rc-shake {
      border-color: #f28b82 !important;
    }
    @keyframes rcShake {
      10%, 90% { transform: translate3d(-1px, 0, 0); }
      20%, 80% { transform: translate3d(2px, 0, 0); }
      30%, 50%, 70% { transform: translate3d(-3px, 0, 0); }
      40%, 60% { transform: translate3d(3px, 0, 0); }
    }
    .rc-anchor-content {
      display: flex;
      align-items: center;
      gap: 12px;
      cursor: pointer;
      outline: none;
    }
    .rc-checkbox-wrap {
      width: 28px;
      height: 28px;
      border-radius: 2px;
      border: 2px solid #c1c1c1;
      background-color: #ffffff;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
      transition: border-color 0.15s ease, background-color 0.15s ease;
      cursor: pointer;
    }
    .rc-anchor-content:hover .rc-checkbox-wrap:not(.rc-active):not(.rc-passed) {
      border-color: #b2b2b2;
    }
    html.dark .rc-checkbox-wrap {
      border-color: #5f6368;
      background-color: #202124;
    }
    html.dark .rc-anchor-content:hover .rc-checkbox-wrap:not(.rc-active):not(.rc-passed) {
      border-color: #80868b;
    }
    .rc-checkbox-wrap.rc-active,
    .rc-checkbox-wrap.rc-passed {
      border-color: transparent !important;
      background-color: transparent !important;
    }
    .rc-spinner {
      width: 22px;
      height: 22px;
      border: 2.5px solid #1a73e8;
      border-top-color: transparent;
      border-radius: 50%;
      animation: rcSpin 0.75s linear infinite;
    }
    html.dark .rc-spinner {
      border-color: #8ab4f8;
      border-top-color: transparent;
    }
    @keyframes rcSpin {
      to { transform: rotate(360deg); }
    }
    .rc-checkmark-icon {
      width: 28px;
      height: 28px;
      color: #34a853;
      animation: rcZoom 0.22s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    }
    html.dark .rc-checkmark-icon {
      color: #81c995;
    }
    @keyframes rcZoom {
      0% { transform: scale(0.4); opacity: 0; }
      100% { transform: scale(1); opacity: 1; }
    }
    .rc-label {
      font-size: 14px;
      font-weight: 500;
      color: #202124;
      line-height: 1;
      letter-spacing: 0.1px;
    }
    html.dark .rc-label {
      color: #e8eaed;
    }
    .rc-brand {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;
      margin-left: auto;
    }
    .rc-brand-logo {
      width: 28px;
      height: 28px;
      object-fit: contain;
      opacity: 0.7;
    }
    .rc-brand-title {
      font-size: 10px;
      font-weight: 600;
      color: #5f6368;
      margin-top: 2px;
    }
    html.dark .rc-brand-title {
      color: #9aa0a6;
    }
    .rc-brand-legal {
      font-size: 8px;
      color: #5f6368;
      display: flex;
      gap: 3px;
      margin-top: 1px;
    }
    html.dark .rc-brand-legal {
      color: #9aa0a6;
    }
    .rc-brand-legal a {
      color: inherit;
      text-decoration: none;
    }
    .rc-brand-legal a:hover {
      text-decoration: underline;
    }
  </style>
</head>
<body>
  <div id="rc-anchor-elem" class="rc-anchor">
    <div id="rc-action-target" class="rc-anchor-content" role="checkbox" aria-checked="false" tabindex="0">
      <div id="rc-check-box" class="rc-checkbox-wrap">
        <div id="rc-slot"></div>
      </div>
      <span id="rc-text-label" class="rc-label">${notRobotLabel}</span>
    </div>
    <div class="rc-brand">
      <img src="/r/static/svg/favicon-simple.svg" alt="reCAPTCHA" class="rc-brand-logo" />
      <span class="rc-brand-title">reCAPTCHA</span>
      <div class="rc-brand-legal">
        <a href="https://policies.google.com/privacy" target="_blank" rel="noopener noreferrer">${privacyLabel}</a>
        <span>-</span>
        <a href="https://policies.google.com/terms" target="_blank" rel="noopener noreferrer">${termsLabel}</a>
      </div>
    </div>
  </div>

  <script>
    (function() {
      var state = {
        isProcessing: false,
        isVerified: false,
        downTimestamp: 0,
        upTimestamp: 0,
        downIsTrusted: false,
        upIsTrusted: false,
        clickIsTrusted: false,
        landingCoords: { x: 0, y: 0 },
        localPoints: [],
        remotePoints: [],
        storageUsage: -1,
        idbCount: -1
      };

      var anchorElem = document.getElementById('rc-anchor-elem');
      var actionTarget = document.getElementById('rc-action-target');
      var checkBox = document.getElementById('rc-check-box');
      var slotElem = document.getElementById('rc-slot');
      var labelElem = document.getElementById('rc-text-label');
      var I18N = {
        notRobot: ${JSON.stringify(notRobotLabel)},
        verified: ${JSON.stringify(verifiedLabel)}
      };

      function recordLocalMovement(e) {
        state.localPoints.push({
          x: Math.round(e.clientX),
          y: Math.round(e.clientY),
          t: Math.round(performance.now())
        });
        if (state.localPoints.length > 80) {
          state.localPoints.shift();
        }
      }

      window.addEventListener('mousemove', recordLocalMovement, { passive: true });
      document.addEventListener('pointerenter', function() {
        window.parent.postMessage({ type: 'VM_PULL_TRAJECTORY' }, '*');
      });

      function inspectStorageEcosystem() {
        if (navigator.storage && navigator.storage.estimate) {
          navigator.storage.estimate().then(function(est) {
            state.storageUsage = est.usage || 0;
          }).catch(function() {});
        }
        if (window.indexedDB && window.indexedDB.databases) {
          window.indexedDB.databases().then(function(dbs) {
            state.idbCount = dbs ? dbs.length : 0;
          }).catch(function() {});
        }
      }
      inspectStorageEcosystem();

      function vmDetectAutomation() {
        var isWd = Boolean(navigator.webdriver);
        var cdpSignatures = [
          'cdc_adoQpoasnfa76pfcZLmcfl_Array',
          'cdc_adoQpoasnfa76pfcZLmcfl_Promise',
          'cdc_adoQpoasnfa76pfcZLmcfl_Symbol',
          '__webdriver_evaluate',
          '__selenium_evaluate',
          '__webdriver_script_fn',
          '__driver_evaluate',
          '__playwright',
          '__puppeteer_evaluation_script__',
          'domAutomation',
          'domAutomationController'
        ];
        var cdpDetected = false;
        for (var i = 0; i < cdpSignatures.length; i++) {
          if (cdpSignatures[i] in window || cdpSignatures[i] in document) {
            cdpDetected = true;
            break;
          }
        }
        for (var k in window) {
          if (k.indexOf('cdc_') === 0 || k.indexOf('__webdriver_') === 0) {
            cdpDetected = true;
            break;
          }
        }

        var protoTampered = false;
        try {
          var desc = Object.getOwnPropertyDescriptor(navigator, 'webdriver');
          if (desc && desc.configurable && !desc.get) {
            protoTampered = true;
          }
        } catch(e) {}

        var zeroOuter = (window.outerWidth === 0 && window.outerHeight === 0);
        var headlessUa = /HeadlessChrome|PhantomJS|Electron/i.test(navigator.userAgent);

        return {
          isWebdriver: isWd,
          cdpDetected: cdpDetected,
          protoTampered: protoTampered,
          zeroOuter: zeroOuter,
          headlessUa: headlessUa
        };
      }

      function vmDetectHardware() {
        var webglSoftware = false;
        var webglVendor = '';
        var webglRenderer = '';
        try {
          var cvs = document.createElement('canvas');
          var gl = cvs.getContext('webgl') || cvs.getContext('experimental-webgl');
          if (gl) {
            var ext = gl.getExtension('WEBGL_debug_renderer_info');
            if (ext) {
              webglVendor = gl.getParameter(ext.UNMASKED_VENDOR_WEBGL) || '';
              webglRenderer = gl.getParameter(ext.UNMASKED_RENDERER_WEBGL) || '';
              if (/SwiftShader|llvmpipe|Mesa Offscreen|VirtualBox|VMware|Software Rasterizer/i.test(webglRenderer)) {
                webglSoftware = true;
              }
            }
          }
        } catch(e) {}

        var canvasHash = 0;
        try {
          var c2d = document.createElement('canvas');
          c2d.width = 120;
          c2d.height = 30;
          var ctx = c2d.getContext('2d');
          if (ctx) {
            ctx.textBaseline = 'alphabetic';
            ctx.fillStyle = '#1a73e8';
            ctx.fillRect(10, 2, 50, 20);
            ctx.fillStyle = '#ea4335';
            ctx.fillText('v4.Google.reCAPTCHA,m%*@', 4, 16);
            var str = c2d.toDataURL();
            for (var i = 0; i < str.length; i++) {
              canvasHash = ((canvasHash << 5) - canvasHash + str.charCodeAt(i)) | 0;
            }
          }
        } catch(e) {}

        var audioHash = 0;
        try {
          var AudioContextClass = window.OfflineAudioContext || window.webkitOfflineAudioContext;
          if (AudioContextClass) {
            var actx = new AudioContextClass(1, 44100, 44100);
            var osc = actx.createOscillator();
            osc.type = 'triangle';
            osc.frequency.setValueAtTime(10000, actx.currentTime);
            var comp = actx.createDynamicsCompressor();
            comp.threshold.setValueAtTime(-50, actx.currentTime);
            comp.knee.setValueAtTime(40, actx.currentTime);
            comp.ratio.setValueAtTime(12, actx.currentTime);
            comp.attack.setValueAtTime(0, actx.currentTime);
            comp.release.setValueAtTime(0.25, actx.currentTime);
            osc.connect(comp);
            comp.connect(actx.destination);
            osc.start(0);
            actx.startRendering().then(function(buf) {
              var ch = buf.getChannelData(0);
              var sum = 0;
              for (var j = 4500; j < 5000; j++) sum += Math.abs(ch[j]);
              audioHash = (sum * 1000000) | 0;
            }).catch(function() {});
          }
        } catch(e) {}

        var screenInconsistent = (window.screen.availWidth > window.screen.width) ||
                                (window.screen.availHeight > window.screen.height);
        var cores = navigator.hardwareConcurrency || 0;
        var mem = navigator.deviceMemory || 0;
        var hardwareSpoofed = (cores === 0) || (cores === 1 && mem > 8);

        return {
          webglSoftware: webglSoftware,
          webglVendor: webglVendor,
          webglRenderer: webglRenderer,
          canvasHash: canvasHash,
          audioHash: audioHash,
          screenInconsistent: screenInconsistent,
          cores: cores,
          mem: mem,
          hardwareSpoofed: hardwareSpoofed
        };
      }

      function vmDetectNativeHooks() {
        var coreHook = false;
        var extHookCount = 0;

        function checkNative(fn) {
          if (typeof fn !== 'function') return false;
          try {
            var fnStr = Function.prototype.toString.call(fn);
            if (!/^function\s*[\w$]*\s*\(\)\s*\{\s*\[native code\]\s*\}$/.test(fnStr)) {
              return false;
            }
            if (Object.prototype.hasOwnProperty.call(fn, 'toString')) {
              return false;
            }
            return true;
          } catch(e) {
            return false;
          }
        }

        if (!checkNative(Function.prototype.toString)) coreHook = true;
        if (!checkNative(document.createElement)) extHookCount++;
        if (!checkNative(window.setTimeout)) extHookCount++;
        if (window.CanvasRenderingContext2D && !checkNative(CanvasRenderingContext2D.prototype.getImageData)) extHookCount++;
        if (window.WebGLRenderingContext && !checkNative(WebGLRenderingContext.prototype.getParameter)) extHookCount++;

        return {
          coreHook: coreHook,
          extHookCount: extHookCount,
          apiTampered: coreHook || (extHookCount >= 3)
        };
      }

      function vmDetectEcosystem() {
        var histLen = (window.history && window.history.length) ? window.history.length : 1;
        var navTimingZero = false;
        try {
          var navEntries = performance.getEntriesByType('navigation');
          if (navEntries && navEntries.length > 0) {
            var n = navEntries[0];
            if (n.duration === 0 && n.domComplete === 0) {
              navTimingZero = true;
            }
          }
        } catch(e) {}

        return {
          historyLength: histLen,
          storageUsage: state.storageUsage,
          idbCount: state.idbCount,
          navTimingZero: navTimingZero
        };
      }

      function analyzeTrajectory(points) {
        var res = {
          count: points.length,
          accVariance: 0,
          curvVariance: 0,
          bezierSmoothness: 1,
          isStraightLine: false,
          isTeleport: false,
          isNoMovement: false
        };

        if (points.length < 3) {
          res.isNoMovement = true;
          return res;
        }

        var velocities = [];
        var totalDist = 0;
        for (var i = 1; i < points.length; i++) {
          var dx = points[i].x - points[i - 1].x;
          var dy = points[i].y - points[i - 1].y;
          var dt = Math.max(1, points[i].t - points[i - 1].t);
          var dist = Math.sqrt(dx * dx + dy * dy);
          totalDist += dist;
          if (dt < 8 && dist > 50) {
            res.isTeleport = true;
          }
          velocities.push(dist / dt);
        }

        if (totalDist < 5) {
          res.isNoMovement = true;
          return res;
        }

        var accels = [];
        for (var j = 1; j < velocities.length; j++) {
          var dtime = Math.max(1, points[j + 1].t - points[j].t);
          accels.push((velocities[j] - velocities[j - 1]) / dtime);
        }

        var accSum = 0;
        for (var a = 0; a < accels.length; a++) accSum += accels[a];
        var accMean = accSum / (accels.length || 1);
        var accVarSum = 0;
        for (var b = 0; b < accels.length; b++) {
          accVarSum += Math.pow(accels[b] - accMean, 2);
        }
        res.accVariance = accVarSum / (accels.length || 1);

        var curvatures = [];
        for (var k = 1; k < points.length - 1; k++) {
          var ux = points[k].x - points[k - 1].x;
          var uy = points[k].y - points[k - 1].y;
          var vx = points[k + 1].x - points[k].x;
          var vy = points[k + 1].y - points[k].y;
          var segDist = Math.max(1, Math.sqrt(vx * vx + vy * vy));
          var th1 = Math.atan2(uy, ux);
          var th2 = Math.atan2(vy, vx);
          var diff = Math.abs(th2 - th1);
          if (diff > Math.PI) diff = 2 * Math.PI - diff;
          curvatures.push(diff / segDist);
        }

        var curvSum = 0;
        for (var c = 0; c < curvatures.length; c++) curvSum += curvatures[c];
        var curvMean = curvSum / (curvatures.length || 1);
        var curvVarSum = 0;
        for (var d = 0; d < curvatures.length; d++) {
          curvVarSum += Math.pow(curvatures[d] - curvMean, 2);
        }
        res.curvVariance = curvVarSum / (curvatures.length || 1);

        var p0 = points[0];
        var pn = points[points.length - 1];
        var chord = Math.sqrt(Math.pow(pn.x - p0.x, 2) + Math.pow(pn.y - p0.y, 2));
        if (chord > 20) {
          var maxDev = 0;
          for (var p = 1; p < points.length - 1; p++) {
            var dev = Math.abs((pn.y - p0.y) * points[p].x - (pn.x - p0.x) * points[p].y + pn.x * p0.y - pn.y * p0.x) / chord;
            if (dev > maxDev) maxDev = dev;
          }
          if (maxDev < 0.65 && res.curvVariance < 0.00015) {
            res.isStraightLine = true;
          }
          res.bezierSmoothness = Math.min(1, maxDev / 12);
        }

        return res;
      }

      function evaluateClickDistribution(clickPos, checkRect, actionRect) {
        var centerX = checkRect.left + checkRect.width / 2;
        var centerY = checkRect.top + checkRect.height / 2;
        var r = Math.sqrt(Math.pow(clickPos.x - centerX, 2) + Math.pow(clickPos.y - centerY, 2));
        var isCenterTarget = r < 0.28;

        var inActionBounds = (
          clickPos.x >= actionRect.left - 2 &&
          clickPos.x <= actionRect.right + 2 &&
          clickPos.y >= actionRect.top - 2 &&
          clickPos.y <= actionRect.bottom + 2
        );

        return {
          radius: r,
          isCenterTarget: isCenterTarget,
          inActionBounds: inActionBounds
        };
      }

      function executeVirtualVerificationPipeline() {
        if (state.isProcessing || state.isVerified) return;
        state.isProcessing = true;

        window.parent.postMessage({
          type: 'VM_CHALLENGE_START'
        }, '*');

        anchorElem.classList.remove('rc-shake');
        checkBox.className = 'rc-checkbox-wrap rc-active';
        slotElem.innerHTML = '<div class="rc-spinner"></div>';

        var autoRes = vmDetectAutomation();
        var hwRes = vmDetectHardware();
        var hookRes = vmDetectNativeHooks();
        var ecoRes = vmDetectEcosystem();

        var mergedPoints = state.remotePoints.concat(state.localPoints);
        var trajectoryRes = analyzeTrajectory(mergedPoints);

        var checkRect = checkBox.getBoundingClientRect();
        var actionRect = actionTarget.getBoundingClientRect();
        var landingRes = evaluateClickDistribution(state.landingCoords, checkRect, actionRect);
        var clickDuration = Math.max(1, Math.round(state.upTimestamp - state.downTimestamp));
        var isTrusted = Boolean(state.downIsTrusted && state.upIsTrusted && state.clickIsTrusted);
        checkBox.className = 'rc-checkbox-wrap rc-active';
        slotElem.innerHTML = '<div class="rc-spinner"></div>';

        var payload = {
          v: 4,
          isTrusted: isTrusted,
          bio: {
            accVar: trajectoryRes.accVariance,
            curvVar: trajectoryRes.curvVariance,
            smooth: trajectoryRes.bezierSmoothness,
            pts: trajectoryRes.count,
            dur: clickDuration,
            radius: Number(landingRes.radius.toFixed(2)),
            isCenterTarget: landingRes.isCenterTarget,
            inActionBounds: landingRes.inActionBounds,
            isNoMovement: trajectoryRes.isNoMovement,
            isStraightLine: trajectoryRes.isStraightLine,
            isTeleport: trajectoryRes.isTeleport
          },
          env: {
            wd: autoRes.isWebdriver,
            cdp: autoRes.cdpDetected,
            protoTampered: autoRes.protoTampered,
            headless: autoRes.headlessUa,
            zeroOuter: autoRes.zeroOuter,
            webglSoftware: hwRes.webglSoftware,
            coreHook: hookRes.coreHook,
            extHookCount: hookRes.extHookCount,
            screenInconsistent: hwRes.screenInconsistent,
            hardwareSpoofed: hwRes.hardwareSpoofed,
            gpu: hwRes.webglRenderer.substring(0, 32),
            cores: hwRes.cores,
            mem: hwRes.mem
          },
          eco: {
            hist: ecoRes.historyLength,
            usage: ecoRes.storageUsage,
            idb: ecoRes.idbCount,
            navTimingZero: ecoRes.navTimingZero
          },
          ts: Date.now(),
          nonce: Math.random().toString(36).substring(2, 10)
        };

        window.parent.postMessage({
          type: 'VM_CHALLENGE_SUBMIT',
          metrics: payload
        }, '*');
      }

      actionTarget.addEventListener('mousedown', function(e) {
        state.downTimestamp = performance.now();
        state.downIsTrusted = e.isTrusted;
      });

      actionTarget.addEventListener('mouseup', function(e) {
        state.upTimestamp = performance.now();
        state.upIsTrusted = e.isTrusted;
      });

      actionTarget.addEventListener('click', function(e) {
        state.clickIsTrusted = e.isTrusted;
        state.landingCoords = { x: e.clientX, y: e.clientY };
        executeVirtualVerificationPipeline();
      });

      actionTarget.addEventListener('keydown', function(e) {
        if (e.key === ' ' || e.key === 'Enter') {
          e.preventDefault();
          state.downTimestamp = performance.now();
          state.upTimestamp = performance.now() + 85;
          state.downIsTrusted = e.isTrusted;
          state.upIsTrusted = e.isTrusted;
          state.clickIsTrusted = e.isTrusted;
          var rect = checkBox.getBoundingClientRect();
          state.landingCoords = { x: rect.left + rect.width / 2 + 1, y: rect.top + rect.height / 2 + 1 };
          executeVirtualVerificationPipeline();
        }
      });

      window.addEventListener('message', function(event) {
        var msg = event.data;
        if (!msg || typeof msg !== 'object') return;

        if (msg.type === 'VM_CONFIG_SYNC') {
          if (typeof msg.hasBackendSession === 'boolean') {
            state.hasBackendSession = msg.hasBackendSession;
          }
          if (msg.theme === 'dark') {
            document.documentElement.classList.add('dark');
          } else {
            document.documentElement.classList.remove('dark');
          }
          if (Array.isArray(msg.initialTrajectory)) {
            state.remotePoints = msg.initialTrajectory;
          }
          if (msg.passed) {
            state.isVerified = true;
            checkBox.className = 'rc-checkbox-wrap rc-passed';
            slotElem.innerHTML = '<svg class="rc-checkmark-icon" viewBox="0 0 24 24" fill="currentColor"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>';
            labelElem.textContent = I18N.verified;
          }
        } else if (msg.type === 'VM_CONFIRM_PASS') {
          state.isVerified = true;
          state.isProcessing = false;
          checkBox.className = 'rc-checkbox-wrap rc-passed';
          slotElem.innerHTML = '<svg class="rc-checkmark-icon" viewBox="0 0 24 24" fill="currentColor"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>';
          labelElem.textContent = I18N.verified;
          anchorElem.classList.remove('rc-shake');
        } else if (msg.type === 'VM_THEME_CHANGE') {
          if (msg.theme === 'dark') {
            document.documentElement.classList.add('dark');
          } else {
            document.documentElement.classList.remove('dark');
          }
        } else if (msg.type === 'VM_PUSH_TRAJECTORY') {
          if (Array.isArray(msg.points)) {
            state.remotePoints = msg.points;
          }
        } else if (msg.type === 'VM_RESET') {
          state.isVerified = false;
          state.isProcessing = false;
          state.localPoints = [];
          state.remotePoints = [];
          checkBox.className = 'rc-checkbox-wrap';
          slotElem.innerHTML = '';
          labelElem.textContent = I18N.notRobot;
          anchorElem.classList.remove('rc-shake');
        } else if (msg.type === 'VM_SET_ERROR') {
          state.isVerified = false;
          state.isProcessing = false;
          state.localPoints = [];
          state.remotePoints = [];
          checkBox.className = 'rc-checkbox-wrap';
          slotElem.innerHTML = '';
          labelElem.textContent = I18N.notRobot;
          anchorElem.classList.add('rc-shake');
          setTimeout(function() {
            anchorElem.classList.remove('rc-shake');
          }, 500);
        } else if (msg.type === 'VM_LOCALE_CHANGE') {
          if (msg.notRobot) I18N.notRobot = msg.notRobot;
          if (msg.verified) I18N.verified = msg.verified;
          if (state.isVerified) {
            labelElem.textContent = I18N.verified;
          } else if (!state.isProcessing) {
            labelElem.textContent = I18N.notRobot;
          }
        }
      });
    })();
  <` + `/script>
</body>
</html>`
})
</script>

<style scoped lang="scss">
.recaptcha-widget-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 304px;
  position: relative;
  user-select: none;
}

.recaptcha-sandbox-frame {
  width: 100%;
  height: 78px;
  border: none;
  overflow: hidden;
  display: block;
}

.recaptcha-error-msg {
  margin-top: 6px;
  margin-left: 0;
  padding-left: 0;
  font-size: 12px;
  line-height: 16px;
  color: #ea4335;
  display: flex;
  align-items: flex-start;
  gap: 6px;

  svg {
    margin-top: 1px;
  }
}

html.dark .recaptcha-error-msg {
  color: #f28b82;
}

.field-error-fade-enter-active,
.field-error-fade-leave-active {
  transition: opacity 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}

.field-error-fade-enter-from,
.field-error-fade-leave-to {
  opacity: 0;
}
</style>
