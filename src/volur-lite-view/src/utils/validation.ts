/**
 * 全局通用表单与身份数据校验工具
 * 严格 1:1 对齐后端 volur-framework 校验体系：
 * - 中国大陆第二代公民身份证 (GB 11643-1999)
 * - 中国大陆四大基础电信业务经营商手机号 (中国移动、中国联通、中国电信、中国广电)
 */

/**
 * 中国移动 (China Mobile) 号段正则
 * 134[0-8], 135-139, 147, 148, 150-152, 157-159, 172, 178, 182-184, 187-188, 195, 197, 198
 */
export const CHINA_MOBILE_REGEX = /^1(34[0-8]|(3[5-9]|4[78]|5[0-27-9]|7[28]|8[2-478]|9[578])\d)\d{7}$/

/**
 * 中国联通 (China Unicom) 号段正则
 * 130-132, 145, 146, 155, 156, 166, 175, 176, 185, 186, 196
 */
export const CHINA_UNICOM_REGEX = /^1(3[0-2]|4[56]|5[56]|66|7[56]|8[56]|96)\d{8}$/

/**
 * 中国电信 (China Telecom) 号段正则
 * 133, 1349, 149, 153, 173, 174, 177, 180, 181, 189, 190, 191, 193, 199
 */
export const CHINA_TELECOM_REGEX = /^1(349|(33|49|53|7[347]|8[019]|9[0139])\d)\d{7}$/

/**
 * 中国广电 (China Broadnet) 号段正则
 * 192
 */
export const CHINA_BROADNET_REGEX = /^192\d{8}$/

export type ChinaCarrier = 'CHINA_MOBILE' | 'CHINA_UNICOM' | 'CHINA_TELECOM' | 'CHINA_BROADNET'

/**
 * 探测手机号所属的四大基础电信运营商
 * 排除 170/171/162/165/167 等虚拟运营商 (MVNO) 号段
 */
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

/**
 * 校验手机号是否为中国四大运营商合法号段
 */
export function isValidChinaPhone(phone?: string | null): boolean {
  return detectChinaCarrier(phone) !== null
}

/**
 * GB/T 2260 省级行政区划代码 (前 2 位)
 */
const PROVINCE_CODES = new Set([
  // 华北
  '11', '12', '13', '14', '15',
  // 东北
  '21', '22', '23',
  // 华东
  '31', '32', '33', '34', '35', '36', '37',
  // 中南
  '41', '42', '43', '44', '45', '46',
  // 西南
  '50', '51', '52', '53', '54',
  // 西北
  '61', '62', '63', '64', '65',
  // 港澳台
  '71', '81', '82'
])

/**
 * 18 位二代身份证基础格式正则
 */
const ID_CARD_PATTERN = /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/

/**
 * ISO 7064:1983.MOD 11-2 加权因子
 */
const WEIGHTS = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]

/**
 * 模 11 余数映射校验码
 */
const CHECK_CODES = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']

/**
 * 校验中国大陆第二代居民身份证有效性 (GB 11643-1999)
 * 包含：18 位定长、行政区划、公历平闰年有效出生日期、ISO 7064:1983.MOD 11-2 加权校验码计算核验
 */
export function isValidIdCard(idCard?: string | null): boolean {
  if (!idCard) return false
  const trimmed = idCard.trim()
  if (trimmed.length !== 18) return false

  // 1. 正则预检
  if (!ID_CARD_PATTERN.test(trimmed)) return false

  // 2. 省份代码校验
  const province = trimmed.substring(0, 2)
  if (!PROVINCE_CODES.has(province)) return false

  // 3. 出生日期校验 (含平闰年、历史范围与未来日期校验)
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

  // 4. ISO 7064:1983.MOD 11-2 校验码加权核验
  let sum = 0
  for (let i = 0; i < 17; i++) {
    sum += parseInt(trimmed.charAt(i), 10) * WEIGHTS[i]
  }
  const remainder = sum % 11
  const expectedCheckCode = CHECK_CODES[remainder]
  const actualCheckCode = trimmed.charAt(17).toUpperCase()

  return expectedCheckCode === actualCheckCode
}

/**
 * 从二代身份证提取出生日期 (YYYY-MM-DD)
 */
export function extractIdCardBirthDate(idCard?: string | null): string | null {
  if (!isValidIdCard(idCard)) return null
  const trimmed = idCard!.trim()
  const year = trimmed.substring(6, 10)
  const month = trimmed.substring(10, 12)
  const day = trimmed.substring(12, 14)
  return `${year}-${month}-${day}`
}

/**
 * 从二代身份证判定性别 (第 17 位奇数男，偶数女)
 */
export function extractIdCardGender(idCard?: string | null): 'MALE' | 'FEMALE' | null {
  if (!isValidIdCard(idCard)) return null
  const trimmed = idCard!.trim()
  const genderDigit = parseInt(trimmed.charAt(16), 10)
  return genderDigit % 2 !== 0 ? 'MALE' : 'FEMALE'
}
