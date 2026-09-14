/**
 * 将字节数值格式化为人类可读的自适应存储单位 (B, KB, MB, GB, TB)
 * @param bytes 字节数数值
 * @param decimals 保留小数位数，默认为 1
 * @returns 格式化后的字符串，如 "128.5 MB", "1.2 GB", "0 B"
 */
export function formatBytes(bytes?: number | null, decimals = 1): string {
  if (bytes == null || isNaN(bytes) || bytes <= 0) {
    return '0 B'
  }
  const k = 1024
  const dm = decimals < 0 ? 0 : decimals
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(k)), units.length - 1)
  if (i === 0) {
    return `${bytes} B`
  }
  return `${(bytes / Math.pow(k, i)).toFixed(dm)} ${units[i]}`
}

