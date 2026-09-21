<template>
  <div class="profile-view-container">
    <div v-if="isLoading" class="loading-wrapper">
      <n-spin size="large" :description="t('common.loading')" />
    </div>

    <template v-else-if="profile">
      <!-- Top Hero Card with Avatar and Basic Summary -->
      <n-card class="user-hero-card" :bordered="false">
        <div class="hero-content">
          <UserAvatar
            :size="88"
            :src="avatarDisplayUrl"
            :username="profile.username"
            editable
            :overlay-text="t('profile.change_avatar')"
            @edit="showCropperModal = true"
          />

          <div class="hero-info">
            <div class="username-row">
              <h1 class="username-text">{{ profile.username }}</h1>
            </div>
            <div class="hero-meta">
              <span class="meta-item">
                <n-icon size="15" class="meta-icon"><MailOutline /></n-icon>
                {{ profile.email }}
              </span>
              <span v-if="profile.phone" class="meta-item">
                <n-icon size="15" class="meta-icon"><PhonePortraitOutline /></n-icon>
                {{ profile.phone }}
              </span>
              <span class="meta-item">
                <n-icon size="15" class="meta-icon"><CalendarOutline /></n-icon>
                {{ t('profile.joined_at') }}: {{ formatDate(profile.createdAt) }}
              </span>
            </div>
          </div>
        </div>
      </n-card>

      <!-- Main Tabs Container -->
      <n-card class="profile-tabs-card" :bordered="false">
        <n-tabs v-model:value="activeTab" type="line" animated class="profile-tabs">
          <!-- Tab 1: Personal Info -->
          <n-tab-pane name="info" :tab="t('profile.tab_personal_info')">
            <template #tab>
              <div class="tab-label">
                <n-icon size="17"><PersonOutline /></n-icon>
                <span>{{ t('profile.tab_personal_info') }}</span>
              </div>
            </template>

            <div class="tab-pane-content">
              <n-form
                ref="infoFormRef"
                :model="infoForm"
                label-placement="top"
                class="info-form"
              >
                <n-grid :x-gap="24" :y-gap="12" cols="1 s:2 m:2">
                  <n-grid-item>
                    <n-form-item :label="t('profile.gender_label')" path="gender">
                      <n-select
                        v-model:value="infoForm.gender"
                        :options="genderOptions"
                        size="large"
                      />
                    </n-form-item>
                  </n-grid-item>

                  <n-grid-item>
                    <n-form-item :label="t('profile.birthday_label')" path="birthday">
                      <n-date-picker
                        v-model:formatted-value="infoForm.birthday"
                        value-format="yyyy-MM-dd"
                        type="date"
                        size="large"
                        style="width: 100%"
                      />
                    </n-form-item>
                  </n-grid-item>

                  <n-grid-item>
                    <n-form-item :label="t('profile.org_label')" path="org">
                      <n-input
                        v-model:value="infoForm.org"
                        :placeholder="t('profile.org_placeholder')"
                        size="large"
                        maxlength="128"
                      />
                    </n-form-item>
                  </n-grid-item>

                  <n-grid-item>
                    <n-form-item :label="t('profile.major_label')" path="major">
                      <n-input
                        v-model:value="infoForm.major"
                        :placeholder="t('profile.major_placeholder')"
                        size="large"
                        maxlength="128"
                      />
                    </n-form-item>
                  </n-grid-item>
                </n-grid>

                <n-form-item :label="t('profile.bio_label')" path="bio">
                  <n-input
                    v-model:value="infoForm.bio"
                    type="textarea"
                    :placeholder="t('profile.bio_placeholder')"
                    :autosize="{ minRows: 3, maxRows: 6 }"
                    maxlength="500"
                    show-count
                    size="large"
                  />
                </n-form-item>

                <div class="form-action-row">
                  <n-button
                    type="primary"
                    size="large"
                    class="save-btn"
                    :loading="isSavingInfo"
                    @click="handleSaveInfo"
                  >
                    {{ t('profile.save_profile') }}
                  </n-button>
                </div>
              </n-form>
            </div>
          </n-tab-pane>

          <!-- Tab 2: Privacy Settings -->
          <n-tab-pane name="privacy" :tab="t('profile.tab_privacy_settings')">
            <template #tab>
              <div class="tab-label">
                <n-icon size="17"><ShieldCheckmarkOutline /></n-icon>
                <span>{{ t('profile.tab_privacy_settings') }}</span>
              </div>
            </template>

            <div class="tab-pane-content">
              <div class="privacy-items-list">
                <div class="privacy-item">
                  <div class="privacy-info">
                    <span class="privacy-title">{{ t('profile.privacy_email_title') }}</span>
                    <span class="privacy-desc">{{ t('profile.privacy_email_desc') }}</span>
                  </div>
                  <n-switch
                    v-model:value="privacyForm.emailPrivacy"
                    checked-value="PUBLIC"
                    unchecked-value="PRIVATE"
                    size="large"
                  />
                </div>

                <div class="privacy-item">
                  <div class="privacy-info">
                    <span class="privacy-title">{{ t('profile.privacy_phone_title') }}</span>
                    <span class="privacy-desc">{{ t('profile.privacy_phone_desc') }}</span>
                  </div>
                  <n-switch
                    v-model:value="privacyForm.phonePrivacy"
                    checked-value="PUBLIC"
                    unchecked-value="PRIVATE"
                    size="large"
                  />
                </div>

                <div class="privacy-item">
                  <div class="privacy-info">
                    <span class="privacy-title">{{ t('profile.privacy_org_title') }}</span>
                    <span class="privacy-desc">{{ t('profile.privacy_org_desc') }}</span>
                  </div>
                  <n-switch
                    v-model:value="privacyForm.orgPrivacy"
                    checked-value="PUBLIC"
                    unchecked-value="PRIVATE"
                    size="large"
                  />
                </div>
              </div>

              <div class="form-action-row">
                <n-button
                  type="primary"
                  size="large"
                  class="save-btn"
                  :loading="isSavingPrivacy"
                  @click="handleSavePrivacy"
                >
                  {{ t('profile.save_privacy') }}
                </n-button>
              </div>
            </div>
          </n-tab-pane>

          <!-- Tab 3: Account Security -->
          <n-tab-pane name="security" :tab="t('profile.tab_account_security')">
            <template #tab>
              <div class="tab-label">
                <n-icon size="17"><LockClosedOutline /></n-icon>
                <span>{{ t('profile.tab_account_security') }}</span>
              </div>
            </template>

            <div class="tab-pane-content">
              <!-- Password Card with 2FA Email Verification + Old Password -->
              <div class="security-card">
                <div class="security-card-header">
                  <div class="security-header-title">
                    <n-icon size="18" class="sec-icon"><KeyOutline /></n-icon>
                    <span>{{ t('profile.change_password_title') }}</span>
                  </div>
                  <span class="security-header-desc">{{ t('profile.change_password_desc_2fa') }}</span>
                </div>

                <n-form
                  ref="pwdFormRef"
                  :model="pwdForm"
                  :rules="pwdRules"
                  label-placement="top"
                  class="password-form"
                >
                  <n-grid :x-gap="24" :y-gap="12" cols="1 s:2 m:2">
                    <n-grid-item>
                      <n-form-item :label="t('profile.bound_email_label')">
                        <n-input
                          :value="profile.email"
                          disabled
                          size="large"
                        >
                          <template #prefix>
                            <n-icon><MailOutline /></n-icon>
                          </template>
                        </n-input>
                      </n-form-item>
                    </n-grid-item>

                    <n-grid-item>
                      <n-form-item :label="t('profile.email_code_label')" path="code">
                        <div class="code-input-group">
                          <n-input
                            v-model:value="pwdForm.code"
                            :placeholder="t('profile.email_code_placeholder')"
                            size="large"
                            maxlength="6"
                          />
                          <n-button
                            size="large"
                            secondary
                            :disabled="pwdCodeCountdown > 0 || isSendingPwdCode"
                            :loading="isSendingPwdCode"
                            class="send-code-btn"
                            @click="handleSendPwdCode"
                          >
                            {{ pwdCodeCountdown > 0 ? `${pwdCodeCountdown}s` : t('profile.send_code') }}
                          </n-button>
                        </div>
                      </n-form-item>
                    </n-grid-item>

                    <n-grid-item>
                      <n-form-item :label="t('profile.old_password_label')" path="oldPassword">
                        <n-input
                          v-model:value="pwdForm.oldPassword"
                          type="password"
                          show-password-on="click"
                          :placeholder="t('profile.old_password_placeholder')"
                          size="large"
                        />
                      </n-form-item>
                    </n-grid-item>

                    <n-grid-item>
                      <n-form-item :label="t('profile.new_password_label')" path="newPassword">
                        <n-input
                          v-model:value="pwdForm.newPassword"
                          type="password"
                          show-password-on="click"
                          :placeholder="t('profile.new_password_placeholder')"
                          size="large"
                        />
                      </n-form-item>
                    </n-grid-item>

                    <n-grid-item :span="2">
                      <n-form-item :label="t('profile.confirm_password_label')" path="confirmPassword">
                        <n-input
                          v-model:value="pwdForm.confirmPassword"
                          type="password"
                          show-password-on="click"
                          :placeholder="t('profile.confirm_password_placeholder')"
                          size="large"
                        />
                      </n-form-item>
                    </n-grid-item>
                  </n-grid>

                  <div class="form-action-row" style="margin-top: 12px;">
                    <n-button
                      type="primary"
                      size="large"
                      class="save-btn"
                      :loading="isSavingPassword"
                      @click="handleChangePassword"
                    >
                      {{ t('profile.submit_change_password') }}
                    </n-button>
                  </div>
                </n-form>
              </div>

              <!-- Danger Zone: Account Deletion with 2FA -->
              <div class="danger-zone-card">
                <div class="danger-zone-header">
                  <div class="danger-title">
                    <n-icon size="18" color="#d03050"><AlertCircleOutline /></n-icon>
                    <span>{{ t('profile.danger_zone_title') }}</span>
                  </div>
                  <span class="danger-desc">{{ t('profile.delete_account_warning') }}</span>
                </div>
                <div class="danger-zone-action">
                  <n-button
                    type="error"
                    size="large"
                    class="delete-account-btn"
                    @click="openDeleteModal"
                  >
                    {{ t('profile.delete_account_button') }}
                  </n-button>
                </div>
              </div>
            </div>
          </n-tab-pane>

          <!-- Tab 4: Notifications (站内消息) -->
          <n-tab-pane name="notifications">
            <template #tab>
              <div class="tab-label">
                <n-badge :value="notifUnreadCount" :max="99" :show="notifUnreadCount > 0" :offset="[6, -2]">
                  <n-icon size="17"><NotificationsOutline /></n-icon>
                </n-badge>
                <span>{{ t('profile.tab_notifications') }}</span>
              </div>
            </template>

            <div class="tab-pane-content notif-pane-content">
              <div class="section-header notif-header-info">
                <span class="section-title">{{ t('profile.notifications_title') }}</span>
                <span class="section-desc">{{ t('profile.notifications_desc') }}</span>
              </div>

              <!-- Notif Search & Status Filter Toolbar (aligned with AdminBroadcastView) -->
              <div class="filter-toolbar">
                <div class="filter-left">
                  <n-input
                    v-model:value="notifSearchKeyword"
                    :placeholder="t('profile.notifications_search_placeholder')"
                    clearable
                    style="width: 260px;"
                    @keyup.enter="handleNotifSearch"
                  >
                    <template #prefix>
                      <n-icon :component="SearchOutline" />
                    </template>
                  </n-input>

                  <n-select
                    v-model:value="notifStatusFilter"
                    :placeholder="t('profile.notifications_status_all')"
                    :options="notifStatusOptions"
                    style="width: 140px;"
                    @update:value="handleNotifSearch"
                  />

                  <n-button type="primary" class="theme-primary-btn" @click="handleNotifSearch">
                    <template #icon>
                      <n-icon :component="SearchOutline" />
                    </template>
                    {{ t('profile.notifications_query_btn') }}
                  </n-button>

                  <n-button quaternary @click="handleNotifReset">
                    <template #icon>
                      <n-icon :component="RefreshOutline" />
                    </template>
                    {{ t('profile.notifications_reset_btn') }}
                  </n-button>
                </div>

                <div class="filter-right">
                  <n-button
                    secondary
                    size="medium"
                    :disabled="notifUnreadCount === 0"
                    @click="handleProfileMarkAllRead"
                  >
                    {{ t('profile.notifications_mark_all_read') }}
                  </n-button>
                </div>
              </div>

              <!-- Notif List Container -->
              <div class="notif-list-wrapper" :class="{ 'is-loading': isNotifLoading }">
                <div v-if="isNotifLoading" class="notif-loading-box">
                  <n-spin size="medium" />
                </div>
                <div v-else-if="notifList.length === 0" class="notif-empty-box">
                  <n-empty :description="t('profile.notifications_empty')" size="medium" />
                </div>
                <div v-else class="notif-items-list">
                  <div
                    v-for="item in notifList"
                    :key="item.id"
                    class="notif-row-card"
                    :class="{ 'is-unread': item.status === 'UNREAD' }"
                    @click="openNotifDetail(item)"
                  >
                    <div class="notif-type-icon" :class="item.type?.toLowerCase() || 'system'">
                      <n-icon size="18">
                        <component :is="getNotifIcon(item.type)" />
                      </n-icon>
                    </div>
                    <div class="notif-main-info">
                      <div class="notif-row-top">
                        <span class="notif-title-text">{{ item.title }}</span>
                        <div class="notif-badges">
                          <n-tag size="tiny" round :type="getNotifTagType(item.type)">
                            {{ getNotifTypeLabel(item.type) }}
                          </n-tag>
                          <span class="notif-time-text">{{ formatNotifTime(item.createdAt) }}</span>
                        </div>
                      </div>
                      <div class="notif-content-preview">{{ item.content }}</div>
                    </div>
                    <div class="notif-row-actions">
                      <span v-if="item.status === 'UNREAD'" class="notif-unread-dot" />
                      <n-button
                        quaternary
                        circle
                        size="small"
                        class="notif-del-btn"
                        @click.stop="handleProfileDeleteNotif(item)"
                      >
                        <template #icon>
                          <n-icon :component="TrashOutline" size="14" />
                        </template>
                      </n-button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Pagination -->
              <div v-if="notifTotal > 0" class="notif-pagination-row">
                <n-pagination
                  v-model:page="notifPage"
                  v-model:page-size="notifPageSize"
                  :item-count="notifTotal"
                  :page-sizes="[10, 20, 50]"
                  show-size-picker
                  @update:page="handleNotifPageChange"
                  @update:page-size="handleNotifPageSizeChange"
                />
              </div>
            </div>
          </n-tab-pane>
        </n-tabs>
      </n-card>
    </template>

    <!-- Profile Notification Detail Modal -->
    <n-modal
      v-model:show="showNotifDetailModal"
      preset="card"
      :title="t('profile.notifications_detail_title')"
      :bordered="false"
      style="width: 520px; max-width: 95vw;"
    >
      <div v-if="selectedNotif" class="notif-detail-body">
        <div class="notif-detail-header">
          <div class="notif-detail-title">{{ selectedNotif.title }}</div>
          <div class="notif-detail-meta">
            <n-tag size="small" round :type="getNotifTagType(selectedNotif.type)">
              {{ getNotifTypeLabel(selectedNotif.type) }}
            </n-tag>
            <span class="notif-sender">{{ t('profile.notifications_sender') }}: {{ selectedNotif.senderUsername || '系统通知' }}</span>
            <span class="notif-time">{{ formatDate(selectedNotif.createdAt) }}</span>
          </div>
        </div>
        <div class="notif-detail-content">
          {{ selectedNotif.content }}
        </div>
      </div>
      <template #footer>
        <div class="modal-footer">
          <n-button @click="showNotifDetailModal = false">关闭</n-button>
        </div>
      </template>
    </n-modal>

    <!-- Avatar Cropper Modal -->
    <AvatarCropperModal
      v-model:show="showCropperModal"
      :current-avatar="profile?.avatarUrl"
      @success="handleAvatarCropSuccess"
    />

    <!-- Account Delete Confirmation Modal with 2FA -->
    <n-modal
      v-model:show="showDeleteModal"
      preset="card"
      :title="t('profile.delete_modal_title')"
      :bordered="false"
      :style="{ width: '520px', maxWidth: '95vw' }"
      @after-leave="resetDeleteModalState"
    >
      <div class="delete-modal-body">
        <n-alert type="error" :show-icon="true" class="delete-alert">
          {{ t('profile.delete_modal_alert') }}
        </n-alert>

        <div class="delete-2fa-form">
          <div class="field-item">
            <span class="field-label">{{ t('profile.bound_email_label') }}:</span>
            <span class="field-value">{{ profile?.email }}</span>
          </div>

          <div class="field-item">
            <span class="field-label">{{ t('profile.email_code_label') }}:</span>
            <div class="code-input-group">
              <n-input
                v-model:value="deleteForm.code"
                :placeholder="t('profile.email_code_placeholder')"
                size="large"
                maxlength="6"
              />
              <n-button
                size="large"
                secondary
                :disabled="delCodeCountdown > 0 || isSendingDelCode"
                :loading="isSendingDelCode"
                class="send-code-btn"
                @click="handleSendDeleteCode"
              >
                {{ delCodeCountdown > 0 ? `${delCodeCountdown}s` : t('profile.send_code') }}
              </n-button>
            </div>
          </div>

          <div class="field-item">
            <span class="field-label">{{ t('profile.delete_modal_prompt') }}</span>
            <div class="target-username-badge">{{ profile?.username }}</div>
            <n-input
              v-model:value="deleteForm.username"
              :placeholder="profile?.username"
              size="large"
              @keyup.enter="handleConfirmDeleteAccount"
            />
          </div>
        </div>

        <n-button
          type="error"
          block
          size="large"
          :disabled="deleteForm.username !== profile?.username || !deleteForm.code"
          :loading="isDeletingAccount"
          class="final-delete-btn"
          @click="handleConfirmDeleteAccount"
        >
          {{ t('profile.confirm_delete_account_button') }}
        </n-button>
      </div>
    </n-modal>

    <!-- Slide Puzzle Captcha Modal for 2FA Verification -->
    <SlidePuzzleModal
      v-model:show="showPuzzleModal"
      :scene="puzzleScene"
      @success="handlePuzzleSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  NCard,
  NTag,
  NIcon,
  NTabs,
  NTabPane,
  NForm,
  NFormItem,
  NGrid,
  NGridItem,
  NInput,
  NDatePicker,
  NSelect,
  NSwitch,
  NButton,
  NSpin,
  NModal,
  NAlert,
  NPagination,
  NEmpty,
  NBadge,
  useMessage,
  type FormInst,
  type FormRules
} from 'naive-ui'
import {
  MailOutline,
  PhonePortraitOutline,
  CalendarOutline,
  PersonOutline,
  ShieldCheckmarkOutline,
  LockClosedOutline,
  KeyOutline,
  AlertCircleOutline,
  NotificationsOutline,
  RefreshOutline,
  TrashOutline,
  TrophyOutline,
  PeopleOutline,
  RibbonOutline,
  SearchOutline
} from '@vicons/ionicons5'
import { useUserStore } from '@/store/modules/user'
import { useSystemStore } from '@/store/modules/system'
import {
  getCurrentUserProfileApi,
  updateUserProfileApi
} from '@/api/user'
import {
  sendChangePasswordEmailCodeApi,
  verifyChangePasswordCodeApi,
  changePasswordApi,
  sendDeleteAccountEmailCodeApi,
  deleteAccountWith2FaApi
} from '@/api/auth'
import {
  getNotificationListApi,
  getNotificationDetailApi,
  getUnreadNotificationCountApi,
  markAllNotificationsReadApi,
  deleteNotificationApi
} from '@/api/notification'
import { formatAvatarUrl } from '@/utils/auth'
import type { UserProfileResponseVO, Gender, PrivacyType } from '@/types/user'
import type { NotificationVO } from '@/types/notification'
import type { CaptchaTicketVO } from '@/types/verification'
import UserAvatar from '@/components/common/UserAvatar.vue'
import AvatarCropperModal from '@/components/user/AvatarCropperModal.vue'
import SlidePuzzleModal from '@/components/verification/SlidePuzzleModal.vue'

const { t } = useI18n()
const message = useMessage()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const systemStore = useSystemStore()

const isLoading = ref(true)
const validTabs = ['info', 'privacy', 'security', 'notifications']
const initialTab = typeof route.query.tab === 'string' && validTabs.includes(route.query.tab) ? route.query.tab : 'info'
const activeTab = ref(initialTab)
const profile = ref<UserProfileResponseVO | null>(null)

// Avatar Display URL with formatAvatarUrl resolution
const avatarDisplayUrl = computed(() => {
  const rawUrl = profile.value?.avatarUrl || userStore.user?.avatarUrl
  return formatAvatarUrl(rawUrl)
})

// Avatar Modal
const showCropperModal = ref(false)

// Captcha Modal State
const showPuzzleModal = ref(false)
const puzzleScene = ref<string>('CHANGE_PASSWORD')
const pendingCaptchaAction = ref<'CHANGE_PASSWORD_CODE' | 'DELETE_ACCOUNT_CODE' | null>(null)

// Personal Info Form
const infoFormRef = ref<FormInst | null>(null)
const isSavingInfo = ref(false)
const infoForm = reactive({
  gender: 'UNKNOWN' as Gender,
  birthday: null as string | null,
  org: '',
  major: '',
  bio: ''
})

const genderOptions = computed(() => [
  { label: t('profile.gender_male'), value: 'MALE' },
  { label: t('profile.gender_female'), value: 'FEMALE' },
  { label: t('profile.gender_unknown'), value: 'UNKNOWN' }
])

// Privacy Form
const isSavingPrivacy = ref(false)
const privacyForm = reactive({
  emailPrivacy: 'PRIVATE' as PrivacyType,
  phonePrivacy: 'PRIVATE' as PrivacyType,
  orgPrivacy: 'PUBLIC' as PrivacyType
})

// Password Form (Old Password + 2FA)
const pwdFormRef = ref<FormInst | null>(null)
const isSavingPassword = ref(false)
const isSendingPwdCode = ref(false)
const pwdCodeCountdown = ref(0)
let pwdTimer: number | null = null

const pwdForm = reactive({
  oldPassword: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules: FormRules = {
  oldPassword: [
    { required: true, message: () => t('profile.old_password_required'), trigger: 'blur' }
  ],
  code: [
    { required: true, message: () => t('profile.email_code_required'), trigger: 'blur' },
    { len: 6, message: () => t('profile.email_code_length_err'), trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: () => t('profile.new_password_required'), trigger: 'blur' },
    { min: 6, max: 32, message: () => t('profile.password_length_err'), trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*?&._#^&+=])[A-Za-z\d@$!%*?&._#^&+=]{6,32}$/,
      message: () => t('profile.password_pattern_err'),
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: () => t('profile.confirm_password_required'), trigger: 'blur' },
    {
      validator: (_rule, value) => value === pwdForm.newPassword,
      message: () => t('profile.passwords_mismatch'),
      trigger: ['blur', 'input']
    }
  ]
}

// Delete Account Modal (2FA)
const showDeleteModal = ref(false)
const isDeletingAccount = ref(false)
const isSendingDelCode = ref(false)
const delCodeCountdown = ref(0)
let delTimer: number | null = null

const deleteForm = reactive({
  code: '',
  username: ''
})

function formatDate(dateStr?: string) {
  if (!dateStr) return '-'
  try {
    return new Date(dateStr).toLocaleDateString()
  } catch {
    return dateStr
  }
}

async function loadProfile() {
  isLoading.value = true
  try {
    const res = await getCurrentUserProfileApi()
    profile.value = res.data
    initFormData(res.data)
  } catch (err: any) {
    message.error(err.message || t('profile.fetch_profile_failed'))
  } finally {
    isLoading.value = false
  }
}

function initFormData(data: UserProfileResponseVO) {
  infoForm.gender = data.gender || 'UNKNOWN'
  infoForm.birthday = data.birthday || null
  infoForm.org = data.org || ''
  infoForm.major = data.major || ''
  infoForm.bio = data.bio || ''

  privacyForm.emailPrivacy = data.emailPrivacy || 'PRIVATE'
  privacyForm.phonePrivacy = data.phonePrivacy || 'PRIVATE'
  privacyForm.orgPrivacy = data.orgPrivacy || 'PUBLIC'
}

function handleAvatarCropSuccess(newAvatarUrl: string) {
  if (profile.value) {
    profile.value = { ...profile.value, avatarUrl: newAvatarUrl }
  }
  userStore.updateUserAvatar(newAvatarUrl)
}

async function handleSaveInfo() {
  isSavingInfo.value = true
  try {
    const res = await updateUserProfileApi({
      gender: infoForm.gender,
      birthday: infoForm.birthday || undefined,
      org: infoForm.org.trim(),
      major: infoForm.major.trim(),
      bio: infoForm.bio.trim()
    })
    profile.value = res.data
    userStore.updateUserInfo({
      org: res.data.org,
      bio: res.data.bio
    })
    message.success(t('profile.profile_save_success'))
  } catch {
    // Error notice handled globally by request interceptor
  } finally {
    isSavingInfo.value = false
  }
}

async function handleSavePrivacy() {
  isSavingPrivacy.value = true
  try {
    const res = await updateUserProfileApi({
      emailPrivacy: privacyForm.emailPrivacy,
      phonePrivacy: privacyForm.phonePrivacy,
      orgPrivacy: privacyForm.orgPrivacy
    })
    profile.value = res.data
    message.success(t('profile.privacy_save_success'))
  } catch (err: any) {
    message.error(err.message || t('profile.privacy_save_failed'))
  } finally {
    isSavingPrivacy.value = false
  }
}

function handleSendPwdCode() {
  if (!profile.value?.email) return
  if (pwdCodeCountdown.value > 0) return

  if (systemStore.isCaptchaEnabled) {
    puzzleScene.value = 'CHANGE_PASSWORD'
    pendingCaptchaAction.value = 'CHANGE_PASSWORD_CODE'
    showPuzzleModal.value = true
  } else {
    doSendPwdCode()
  }
}

async function doSendPwdCode(ticket?: string) {
  isSendingPwdCode.value = true
  try {
    await sendChangePasswordEmailCodeApi({
      email: profile.value!.email,
      username: profile.value!.username,
      captchaTicket: ticket || undefined
    })
    message.success(t('profile.code_sent_success'))
    pwdCodeCountdown.value = 60
    if (pwdTimer) clearInterval(pwdTimer)
    pwdTimer = window.setInterval(() => {
      if (pwdCodeCountdown.value > 0) {
        pwdCodeCountdown.value--
      } else if (pwdTimer) {
        clearInterval(pwdTimer)
        pwdTimer = null
      }
    }, 1000)
  } catch {
    // Error notice handled globally by request interceptor
  } finally {
    isSendingPwdCode.value = false
  }
}

function handleChangePassword() {
  pwdFormRef.value?.validate(async (errors) => {
    if (errors) return
    if (!profile.value?.email) return

    isSavingPassword.value = true
    try {
      // 1. Verify code and get token
      const tokenRes = await verifyChangePasswordCodeApi({
        email: profile.value.email,
        username: profile.value.username,
        code: pwdForm.code.trim()
      })
      const token = tokenRes.data

      // 2. Change password with oldPassword, token and newPassword
      await changePasswordApi({
        oldPassword: pwdForm.oldPassword,
        token,
        newPassword: pwdForm.newPassword
      })

      message.success(t('profile.password_change_success_relogin'))
      pwdForm.oldPassword = ''
      pwdForm.code = ''
      pwdForm.newPassword = ''
      pwdForm.confirmPassword = ''

      // Clear session and redirect to login
      await userStore.logout()
      await router.replace('/auth/login')
    } catch {
      // Error notice handled globally by request interceptor
    } finally {
      isSavingPassword.value = false
    }
  })
}

function openDeleteModal() {
  showDeleteModal.value = true
  deleteForm.code = ''
  deleteForm.username = ''
}

function resetDeleteModalState() {
  deleteForm.code = ''
  deleteForm.username = ''
  if (delTimer) {
    clearInterval(delTimer)
    delTimer = null
  }
  delCodeCountdown.value = 0
}

function handleSendDeleteCode() {
  if (!profile.value?.email) return
  if (delCodeCountdown.value > 0) return

  if (systemStore.isCaptchaEnabled) {
    puzzleScene.value = 'CHANGE_PASSWORD'
    pendingCaptchaAction.value = 'DELETE_ACCOUNT_CODE'
    showPuzzleModal.value = true
  } else {
    doSendDeleteCode()
  }
}

async function doSendDeleteCode(ticket?: string) {
  isSendingDelCode.value = true
  try {
    await sendDeleteAccountEmailCodeApi({
      email: profile.value!.email,
      username: profile.value!.username,
      captchaTicket: ticket || undefined
    })
    message.success(t('profile.code_sent_success'))
    delCodeCountdown.value = 60
    if (delTimer) clearInterval(delTimer)
    delTimer = window.setInterval(() => {
      if (delCodeCountdown.value > 0) {
        delCodeCountdown.value--
      } else if (delTimer) {
        clearInterval(delTimer)
        delTimer = null
      }
    }, 1000)
  } catch {
    // Error notice handled globally by request interceptor
  } finally {
    isSendingDelCode.value = false
  }
}

async function handlePuzzleSuccess(ticketVO: CaptchaTicketVO) {
  showPuzzleModal.value = false
  if (pendingCaptchaAction.value === 'CHANGE_PASSWORD_CODE') {
    await doSendPwdCode(ticketVO.ticket)
  } else if (pendingCaptchaAction.value === 'DELETE_ACCOUNT_CODE') {
    await doSendDeleteCode(ticketVO.ticket)
  }
  pendingCaptchaAction.value = null
}

async function handleConfirmDeleteAccount() {
  if (deleteForm.username !== profile.value?.username) {
    message.error(t('profile.username_mismatch'))
    return
  }
  if (!deleteForm.code || deleteForm.code.trim().length !== 6) {
    message.error(t('profile.email_code_required'))
    return
  }

  isDeletingAccount.value = true
  try {
    await deleteAccountWith2FaApi({
      username: deleteForm.username.trim(),
      code: deleteForm.code.trim()
    })
    message.success(t('profile.account_deleted_success'))
    showDeleteModal.value = false
    await userStore.logout()
    await router.replace('/auth/login')
  } catch {
    // Error notice handled globally by request interceptor
  } finally {
    isDeletingAccount.value = false
  }
}

// Notifications Tab State
const notifSearchKeyword = ref('')
const notifStatusFilter = ref<'ALL' | 'UNREAD' | 'READ'>('ALL')
const notifList = ref<NotificationVO[]>([])
const notifTotal = ref(0)
const notifPage = ref(1)
const notifPageSize = ref(10)
const isNotifLoading = ref(false)
const notifUnreadCount = ref(0)

const showNotifDetailModal = ref(false)
const selectedNotif = ref<NotificationVO | null>(null)

const notifStatusOptions = computed(() => [
  { label: t('profile.notifications_status_all'), value: 'ALL' },
  { label: t('profile.notifications_status_unread'), value: 'UNREAD' },
  { label: t('profile.notifications_status_read'), value: 'READ' }
])

function getNotifIcon(type?: string) {
  switch (type) {
    case 'COMPETITION':
      return TrophyOutline
    case 'TEAM':
      return PeopleOutline
    case 'AWARD':
      return RibbonOutline
    default:
      return MailOutline
  }
}

function getNotifTypeLabel(type?: string) {
  switch (type) {
    case 'COMPETITION':
      return t('profile.notifications_competition')
    case 'TEAM':
      return t('profile.notifications_team')
    case 'AWARD':
      return t('profile.notifications_award')
    default:
      return t('profile.notifications_system')
  }
}

function getNotifTagType(type?: string): 'info' | 'success' | 'warning' | 'default' {
  switch (type) {
    case 'COMPETITION':
      return 'warning'
    case 'TEAM':
      return 'info'
    case 'AWARD':
      return 'success'
    default:
      return 'default'
  }
}

function formatNotifTime(timeStr?: string) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  if (diffMins < 1) return '刚刚'
  if (diffMins < 60) return `${diffMins} 分钟前`
  const diffHours = Math.floor(diffMins / 60)
  if (diffHours < 24) return `${diffHours} 小时前`
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

async function fetchProfileUnreadCount() {
  try {
    const res = await getUnreadNotificationCountApi()
    if (res.data !== undefined) {
      notifUnreadCount.value = Number(res.data)
    }
  } catch (err) {
    console.error('Failed to fetch unread count in profile:', err)
  }
}

async function fetchProfileNotifications() {
  isNotifLoading.value = true
  try {
    const params: {
      page: number
      size: number
      keyword?: string
      status?: string
    } = {
      page: notifPage.value,
      size: notifPageSize.value
    }
    if (notifSearchKeyword.value.trim()) {
      params.keyword = notifSearchKeyword.value.trim()
    }
    if (notifStatusFilter.value !== 'ALL') {
      params.status = notifStatusFilter.value
    }

    const res = await getNotificationListApi(params)
    if (res.data) {
      notifList.value = res.data.list || []
      notifTotal.value = res.data.total || 0
    }
  } catch (err: any) {
    message.error(err.message || t('profile.fetch_profile_failed'))
  } finally {
    isNotifLoading.value = false
  }
}

function handleNotifSearch() {
  notifPage.value = 1
  fetchProfileNotifications()
}

function handleNotifReset() {
  notifSearchKeyword.value = ''
  notifStatusFilter.value = 'ALL'
  notifPage.value = 1
  fetchProfileNotifications()
}

function handleNotifPageChange(page: number) {
  notifPage.value = page
  fetchProfileNotifications()
}

function handleNotifPageSizeChange(pageSize: number) {
  notifPageSize.value = pageSize
  notifPage.value = 1
  fetchProfileNotifications()
}

async function openNotifDetail(item: NotificationVO) {
  selectedNotif.value = item
  showNotifDetailModal.value = true
  try {
    const res = await getNotificationDetailApi(item.id)
    if (res.data) {
      selectedNotif.value = res.data
    }
    if (item.status === 'UNREAD') {
      item.status = 'READ'
      if (notifUnreadCount.value > 0) {
        notifUnreadCount.value--
      }
    }
  } catch (err) {
    console.error('Failed to get notification detail:', err)
  }
}

async function handleProfileMarkAllRead() {
  try {
    await markAllNotificationsReadApi()
    notifList.value.forEach((item) => (item.status = 'READ'))
    notifUnreadCount.value = 0
    message.success(t('profile.notifications_marked_all_success'))
  } catch (err: any) {
    message.error(err.message || '操作失败')
  }
}

async function handleProfileDeleteNotif(item: NotificationVO) {
  try {
    await deleteNotificationApi(item.id)
    notifList.value = notifList.value.filter((n) => n.id !== item.id)
    notifTotal.value = Math.max(0, notifTotal.value - 1)
    if (item.status === 'UNREAD' && notifUnreadCount.value > 0) {
      notifUnreadCount.value--
    }
    message.success(t('profile.notifications_delete_success'))
  } catch (err: any) {
    message.error(err.message || '删除失败')
  }
}

watch(
  () => route.query.tab,
  (newTab) => {
    if (typeof newTab === 'string' && validTabs.includes(newTab)) {
      activeTab.value = newTab
      if (newTab === 'notifications') {
        fetchProfileNotifications()
        fetchProfileUnreadCount()
      }
    }
  }
)

watch(activeTab, (tab) => {
  if (tab === 'notifications') {
    fetchProfileNotifications()
    fetchProfileUnreadCount()
  }
})

onMounted(() => {
  loadProfile()
  systemStore.fetchPublicSettings()
  if (activeTab.value === 'notifications') {
    fetchProfileNotifications()
    fetchProfileUnreadCount()
  } else {
    fetchProfileUnreadCount()
  }
})

onUnmounted(() => {
  if (pwdTimer) clearInterval(pwdTimer)
  if (delTimer) clearInterval(delTimer)
})
</script>

<style scoped lang="scss">
.profile-view-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 8px 0 40px 0;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.loading-wrapper {
  text-align: center;
  padding: 120px 0;
}

.user-hero-card {
  border-radius: 12px;
  background-color: var(--v-bg-card, #ffffff);
  box-shadow: var(--v-shadow-sm, 0 1px 2px rgba(0, 0, 0, 0.05));
  border: 1px solid var(--v-border-base, #e5e4e7);

  .hero-content {
    display: flex;
    align-items: center;
    gap: 28px;
    padding: 12px 8px;

    .hero-info {
      display: flex;
      flex-direction: column;
      gap: 10px;
      flex: 1;

      .username-row {
        display: flex;
        align-items: center;
        gap: 12px;

        .username-text {
          font-size: 24px;
          font-weight: 700;
          color: var(--v-text-primary, #111111);
          margin: 0;
          line-height: 1.2;
        }
      }

      .hero-meta {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 20px;
        font-size: 13px;
        color: var(--v-text-desc, #444444);

        .meta-item {
          display: flex;
          align-items: center;
          gap: 6px;

          .meta-icon {
            color: var(--v-text-muted, #666666);
          }
        }
      }
    }
  }
}

.profile-tabs-card {
  border-radius: 12px;
  background-color: var(--v-bg-card, #ffffff);
  box-shadow: var(--v-shadow-sm, 0 1px 2px rgba(0, 0, 0, 0.05));
  border: 1px solid var(--v-border-base, #e5e4e7);

  .profile-tabs {
    .tab-label {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      font-weight: 500;
      padding: 4px 8px;
    }
  }

  .tab-pane-content {
    padding: 24px 8px 12px 8px;
  }
}

.info-form {
  max-width: 720px;
}

.code-input-group {
  display: flex;
  gap: 12px;
  width: 100%;

  .send-code-btn {
    min-width: 120px;
    flex-shrink: 0;
    font-weight: 500;
    border-radius: 6px;
  }
}

.form-action-row {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;

  .save-btn {
    background-color: var(--v-btn-primary-bg, #000000);
    color: var(--v-btn-primary-text, #ffffff);
    font-weight: 500;
    min-width: 140px;
    border-radius: 6px;

    &:hover {
      background-color: var(--v-btn-primary-hover, #222222);
    }
  }
}

.privacy-items-list {
  display: flex;
  flex-direction: column;
  max-width: 720px;

  .privacy-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px 0;
    border-bottom: 1px solid var(--v-border-subtle, #f0eff2);

    &:last-child {
      border-bottom: none;
    }

    .privacy-info {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .privacy-title {
        font-size: 15px;
        font-weight: 600;
        color: var(--v-text-primary, #111111);
      }

      .privacy-desc {
        font-size: 13px;
        color: var(--v-text-desc, #444444);
      }
    }
  }
}

.security-card {
  max-width: 720px;
  margin-bottom: 32px;

  .security-card-header {
    margin-bottom: 20px;
    display: flex;
    flex-direction: column;
    gap: 4px;

    .security-header-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
      color: var(--v-text-primary, #111111);

      .sec-icon {
        color: var(--v-text-desc, #444444);
      }
    }

    .security-header-desc {
      font-size: 13px;
      color: var(--v-text-desc, #444444);
    }
  }
}

.danger-zone-card {
  max-width: 720px;
  border: 1px solid rgba(208, 48, 80, 0.25);
  background-color: rgba(208, 48, 80, 0.02);
  border-radius: 8px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;

  .danger-zone-header {
    display: flex;
    flex-direction: column;
    gap: 4px;

    .danger-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 15px;
      font-weight: 600;
      color: #d03050;
    }

    .danger-desc {
      font-size: 13px;
      color: var(--v-text-desc, #444444);
    }
  }

  .danger-zone-action {
    flex-shrink: 0;

    .delete-account-btn {
      font-weight: 500;
      border-radius: 6px;
    }
  }
}

.delete-modal-body {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .delete-alert {
    border-radius: 6px;
  }

  .delete-2fa-form {
    display: flex;
    flex-direction: column;
    gap: 14px;

    .field-item {
      display: flex;
      flex-direction: column;
      gap: 6px;

      .field-label {
        font-size: 13px;
        font-weight: 500;
        color: var(--v-text-desc, #444444);
      }

      .field-value {
        font-size: 14px;
        font-weight: 600;
        color: var(--v-text-primary, #111111);
      }

      .target-username-badge {
        font-size: 15px;
        font-weight: 700;
        color: #d03050;
        user-select: all;
      }
    }
  }

  .final-delete-btn {
    margin-top: 8px;
    font-weight: 500;
    border-radius: 6px;
  }
}

.notif-pane-content {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .notif-header-info {
    margin-bottom: 0;
  }

  .filter-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;

    .filter-left {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
    }

    .filter-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  .theme-primary-btn {
    background-color: #000000;
    color: #ffffff;
    font-weight: 500;
    border-radius: 6px;

    &:hover {
      background-color: #222222;
    }
  }

  .notif-list-wrapper {
    min-height: 240px;
    position: relative;

    .notif-loading-box,
    .notif-empty-box {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 240px;
    }

    .notif-items-list {
      display: flex;
      flex-direction: column;
      gap: 10px;

      .notif-row-card {
        display: flex;
        align-items: flex-start;
        gap: 14px;
        padding: 14px 18px;
        background-color: var(--n-color, #ffffff);
        border: 1px solid var(--v-border-base, #e5e4e7);
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.15s ease;

        &:hover {
          border-color: #000000;
          box-shadow: var(--v-shadow-sm, 0 1px 3px rgba(0, 0, 0, 0.06));

          .notif-del-btn {
            opacity: 1;
          }
        }

        &.is-unread {
          background-color: rgba(99, 102, 241, 0.02);
          border-left: 3px solid #000000;

          .notif-title-text {
            font-weight: 600;
            color: var(--v-text-primary, #000000);
          }
        }

        .notif-type-icon {
          width: 36px;
          height: 36px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
          background-color: rgba(99, 102, 241, 0.1);
          color: #6366f1;

          &.competition {
            background-color: rgba(245, 158, 11, 0.1);
            color: #f59e0b;
          }

          &.team {
            background-color: rgba(16, 185, 129, 0.1);
            color: #10b981;
          }

          &.award {
            background-color: rgba(236, 72, 153, 0.1);
            color: #ec4899;
          }
        }

        .notif-main-info {
          flex: 1;
          min-width: 0;
          display: flex;
          flex-direction: column;
          gap: 6px;

          .notif-row-top {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;

            .notif-title-text {
              font-size: 14px;
              color: var(--v-text-primary, #222222);
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .notif-badges {
              display: flex;
              align-items: center;
              gap: 8px;
              flex-shrink: 0;

              .notif-time-text {
                font-size: 12px;
                color: #888888;
              }
            }
          }

          .notif-content-preview {
            font-size: 13px;
            color: var(--v-text-desc, #444444);
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
            line-height: 1.5;
          }
        }

        .notif-row-actions {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-shrink: 0;
          padding-top: 2px;

          .notif-unread-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: #ef4444;
          }

          .notif-del-btn {
            opacity: 0;
            transition: opacity 0.15s ease;
          }
        }
      }
    }
  }

  .notif-pagination-row {
    display: flex;
    justify-content: flex-end;
    margin-top: 8px;
  }
}

.notif-detail-body {
  display: flex;
  flex-direction: column;
  gap: 14px;

  .notif-detail-header {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .notif-detail-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--v-text-primary, #111111);
    }

    .notif-detail-meta {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 12px;
      color: #888888;
    }
  }

  .notif-detail-content {
    font-size: 14px;
    color: var(--v-text-desc, #444444);
    line-height: 1.6;
    background: rgba(0, 0, 0, 0.02);
    padding: 14px;
    border-radius: 6px;
    white-space: pre-wrap;
  }
}
</style>
