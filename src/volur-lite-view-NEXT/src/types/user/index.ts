export type Gender = 'UNKNOWN' | 'MALE' | 'FEMALE'
export type PrivacyType = 'PUBLIC' | 'PRIVATE'

export interface UserSummaryVO {
  id: number | string
  username: string
  email: string
  avatarUrl?: string | null
  role: string
  org?: string | null
  bio?: string | null
  createdAt?: string
}

export interface UserProfileResponseVO {
  id: number | string
  username: string
  email: string
  phone?: string
  gender?: Gender
  birthday?: string
  role: string
  org?: string
  major?: string
  bio?: string
  avatarUrl?: string
  bannerUrl?: string
  notice?: string
  emailPrivacy?: PrivacyType
  phonePrivacy?: PrivacyType
  orgPrivacy?: PrivacyType
  createdAt: string
}

export interface UpdateUserProfileRequestDTO {
  gender?: Gender
  birthday?: string
  org?: string
  major?: string
  bio?: string
  emailPrivacy?: PrivacyType
  phonePrivacy?: PrivacyType
  orgPrivacy?: PrivacyType
}

export interface UpdateAvatarRequestDTO {
  avatarUrl: string
}
