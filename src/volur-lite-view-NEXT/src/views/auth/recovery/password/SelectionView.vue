<template>
  <AuthCardShell
    :title="t('auth.recovery.password_title')"
    :subtitle="t('auth.recovery.password_subtitle')"
  >
    <div class="flex flex-col">
      <div v-if="errorMsg" class="mb-4 text-sm text-google-red-light dark:text-google-red-dark">
        {{ errorMsg }}
      </div>

      <AuthMethodSelector
        :items="mappedMethods"
        :loading="isFetching"
        :empty-tip="t('auth.recovery.password_disabled_tip')"
        :disabled="loading"
        @select="handleSelectMethod"
      />
    </div>

    <AuthActionBar
      :secondary-text="t('auth.back_to_signin')"
      :secondary-disabled="loading"
      :show-secondary="true"
      @secondary="handleBackToSignin"
    />
  </AuthCardShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useSigninFlowStore } from '@/store/modules/flow'
import { getPasswordRecoveryOptionsApi, selectPasswordRecoveryMethodApi } from '@/api/auth'
import type { RecoveryMethodVO } from '@/types/auth/recovery'
import { AuthCardShell, AuthMethodSelector, AuthActionBar, type AuthMethodOption } from '@/components/auth'
import { useFormError } from '@/composables/useFormError'

defineOptions({
  name: 'PasswordSelectionView'
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()
const { errorMsg, setErrorKey, setRawError } = useFormError()

const loading = computed(() => signinFlowStore.loading)
const isFetching = ref(true)
const methods = ref<RecoveryMethodVO[]>([])
const maskedEmail = ref('')
const maskedPhone = ref('')

function getMethodTitle(item: RecoveryMethodVO): string {
  if (item.method === 'EMAIL') {
    return t('auth.recovery.method_email')
  }
  if (item.method === 'PHONE') {
    return t('auth.recovery.method_phone')
  }
  return item.name || item.method
}

function getMethodDesc(item: RecoveryMethodVO): string {
  if (item.method === 'EMAIL' && maskedEmail.value) {
    return t('auth.recovery.emailotp_desc', { email: maskedEmail.value })
  }
  if (item.method === 'PHONE' && maskedPhone.value) {
    return t('auth.signin.two_step_sms_tip')
  }
  return item.description || ''
}

const mappedMethods = computed<AuthMethodOption[]>(() => {
  return methods.value.map((item) => ({
    ...item,
    type: item.method,
    title: getMethodTitle(item),
    subtitle: getMethodDesc(item)
  }))
})

async function loadOptions(): Promise<void> {
  if (!signinFlowStore.dsh) {
    await router.push({
      path: '/v1/signin/identifier',
      query: signinFlowStore.getQueryParams()
    })
    return
  }
  isFetching.value = true
  try {
    const res = await getPasswordRecoveryOptionsApi(signinFlowStore.dsh)
    if (res.data?.methods) {
      methods.value = res.data.methods
    } else {
      methods.value = []
    }
    maskedEmail.value = res.data?.maskedEmail || ''
    maskedPhone.value = res.data?.maskedPhone || ''
  } catch (err: unknown) {
    if (err && typeof err === 'object' && 'message' in err && typeof (err as { message: unknown }).message === 'string') {
      setRawError((err as { message: string }).message)
    } else {
      setErrorKey('auth.recovery.password_options_failed')
    }
  } finally {
    isFetching.value = false
  }
}

onMounted(async () => {
  await signinFlowStore.ensureFlowParams(route, router)
  await loadOptions()
})

async function handleSelectMethod(item: AuthMethodOption): Promise<void> {
  if (loading.value) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  try {
    const res = await selectPasswordRecoveryMethodApi({
      dsh: signinFlowStore.dsh,
      tl: signinFlowStore.tl,
      cid: signinFlowStore.cid,
      method: item.method || item.type || ''
    })
    if (res.data?.tl) {
      signinFlowStore.tl = res.data.tl
    }
    if (typeof res.data?.cid === 'number') {
      signinFlowStore.cid = res.data.cid
    }
  } catch (err: unknown) {
    if (err && typeof err === 'object' && 'message' in err && typeof (err as { message: unknown }).message === 'string') {
      setRawError((err as { message: string }).message)
    }
    return
  }

  await router.push({
    path: '/v1/recovery/challenge/recaptcha',
    query: signinFlowStore.getQueryParams({
      recoveryType: 'password',
      recoveryMethod: item.method || item.type || ''
    })
  })
}

async function handleBackToSignin(): Promise<void> {
  if (loading.value) return
  signinFlowStore.tl = ''
  signinFlowStore.cid = 0
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signin/identifier',
    query: signinFlowStore.getQueryParams()
  })
}
</script>
