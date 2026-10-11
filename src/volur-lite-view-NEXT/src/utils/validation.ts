export const CHINA_MOBILE_REGEX = /^1(34[0-8]|(3[5-9]|4[78]|5[0-27-9]|7[28]|8[2-478]|9[578])\d)\d{7}$/
export const CHINA_UNICOM_REGEX = /^1(3[0-2]|4[56]|5[56]|66|7[56]|8[56]|96)\d{8}$/
export const CHINA_TELECOM_REGEX = /^1(349|(33|49|53|7[347]|8[019]|9[0139])\d)\d{7}$/
export const CHINA_BROADNET_REGEX = /^192\d{8}$/

export type ChinaCarrier = 'CHINA_MOBILE' | 'CHINA_UNICOM' | 'CHINA_TELECOM' | 'CHINA_BROADNET'

export function detectChinaCarrier(phone?: string | null): ChinaCarrier | null {
  if (!phone) return null
  const trimmed = phone.trim()
  if (trimmed.length !== 11) return null

  if (CHINA_MOBILE_REGEX.test(trimmed)) return 'CHINA_MOBILE'
  if (CHINA_UNICOM_REGEX.test(trimmed)) return 'CHINA_UNICOM'
  if (CHINA_TELECOM_REGEX.test(trimmed)) return 'CHINA_TELECOM'
  if (CHINA_BROADNET_REGEX.test(trimmed)) return 'CHINA_BROADNET'

  return null
}

export function isValidChinaPhone(phone?: string | null): boolean {
  return detectChinaCarrier(phone) !== null
}

const PROVINCE_CODES = new Set([
  '11', '12', '13', '14', '15',
  '21', '22', '23',
  '31', '32', '33', '34', '35', '36', '37',
  '41', '42', '43', '44', '45', '46',
  '50', '51', '52', '53', '54',
  '61', '62', '63', '64', '65',
  '71', '81', '82'
])

const ID_CARD_PATTERN = /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/
const WEIGHTS = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
const CHECK_CODES = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']

export function isValidIdCard(idCard?: string | null): boolean {
  if (!idCard) return false
  const trimmed = idCard.trim()
  if (trimmed.length !== 18) return false

  if (!ID_CARD_PATTERN.test(trimmed)) return false

  const province = trimmed.substring(0, 2)
  if (!PROVINCE_CODES.has(province)) return false

  const year = parseInt(trimmed.substring(6, 10), 10)
  const month = parseInt(trimmed.substring(10, 12), 10)
  const day = parseInt(trimmed.substring(12, 14), 10)

  const currentYear = new Date().getFullYear()
  if (year < 1900 || year > currentYear) return false

  const dateObj = new Date(year, month - 1, day)
  if (
    dateObj.getFullYear() !== year ||
    dateObj.getMonth() !== month - 1 ||
    dateObj.getDate() !== day
  ) {
    return false
  }
  if (dateObj.getTime() > Date.now()) return false

  let sum = 0
  for (let i = 0; i < 17; i++) {
    sum += parseInt(trimmed.charAt(i), 10) * WEIGHTS[i]
  }
  const remainder = sum % 11
  const expectedCheckCode = CHECK_CODES[remainder]
  const actualCheckCode = trimmed.charAt(17).toUpperCase()

  return expectedCheckCode === actualCheckCode
}

export function extractIdCardBirthDate(idCard?: string | null): string | null {
  if (!isValidIdCard(idCard)) return null
  const trimmed = idCard!.trim()
  const year = trimmed.substring(6, 10)
  const month = trimmed.substring(10, 12)
  const day = trimmed.substring(12, 14)
  return `${year}-${month}-${day}`
}

export function extractIdCardGender(idCard?: string | null): 'MALE' | 'FEMALE' | null {
  if (!isValidIdCard(idCard)) return null
  const trimmed = idCard!.trim()
  const genderDigit = parseInt(trimmed.charAt(16), 10)
  return genderDigit % 2 !== 0 ? 'MALE' : 'FEMALE'
}
