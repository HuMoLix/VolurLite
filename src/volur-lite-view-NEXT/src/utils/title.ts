export function formatPageTitle(title?: string | null): string {
  if (!title) {
    return 'Volur'
  }
  const cleanTitle = title.trim()
  if (!cleanTitle || cleanTitle === 'Volur') {
    return 'Volur'
  }
  if (cleanTitle.endsWith(' - Volur')) {
    return cleanTitle
  }
  const stripped = cleanTitle.replace(/\s*-\s*Volur\s*(?:账号|帳號|Account)$/i, '').trim()
  if (!stripped || stripped === 'Volur') {
    return 'Volur'
  }
  if (stripped.endsWith(' - Volur')) {
    return stripped
  }
  return `${stripped} - Volur`
}
