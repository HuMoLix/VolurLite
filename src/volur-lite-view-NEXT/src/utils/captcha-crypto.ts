import JSEncrypt from 'jsencrypt'

let activePublicKey = ''

export function setCaptchaPublicKey(pem: string): void {
  if (pem && pem.trim()) {
    activePublicKey = pem.trim()
  }
}

function formatPublicKeyPem(pem: string): string {
  const clean = pem.replace(/-----BEGIN PUBLIC KEY-----|-----END PUBLIC KEY-----|[\r\n\s]/g, '')
  return `-----BEGIN PUBLIC KEY-----\n${clean}\n-----END PUBLIC KEY-----`
}

function splitTextByByteLength(text: string, maxBytes: number): string[] {
  const result: string[] = []
  let current = ''
  let currentBytes = 0
  for (const char of text) {
    const code = char.codePointAt(0) || 0
    let charBytes = 1
    if (code > 0x7ff) {
      charBytes = 3
    } else if (code > 0x7f) {
      charBytes = 2
    }
    if (code > 0xffff) {
      charBytes = 4
    }
    if (currentBytes + charBytes > maxBytes) {
      result.push(current)
      current = char
      currentBytes = charBytes
    } else {
      current += char
      currentBytes += charBytes
    }
  }
  if (current) {
    result.push(current)
  }
  return result
}

export function encryptPayload(data: Record<string, unknown>, pem?: string): string {
  const keyToUse = pem?.trim() || activePublicKey
  if (!keyToUse) {
    throw new Error('缺少服务端下发的有效 RSA 公钥')
  }

  setCaptchaPublicKey(keyToUse)

  const payloadWithTs = {
    ...data,
    ts: Date.now()
  }

  const jsonStr = JSON.stringify(payloadWithTs)
  const formattedKey = formatPublicKeyPem(keyToUse)

  const encryptor = new JSEncrypt()
  encryptor.setPublicKey(formattedKey)

  const maxChunkBytes = 117
  const chunks = splitTextByByteLength(jsonStr, maxChunkBytes)

  if (chunks.length === 1) {
    const encrypted = encryptor.encrypt(chunks[0])
    if (!encrypted) {
      throw new Error('RSA 加密失败')
    }
    return encrypted
  }

  const encryptedBlocks: string[] = []
  for (const chunk of chunks) {
    const block = encryptor.encrypt(chunk)
    if (!block) {
      throw new Error('RSA 分段加密失败')
    }
    encryptedBlocks.push(block)
  }

  const byteArrays = encryptedBlocks.map((b64) => {
    const binary = atob(b64)
    const bytes = new Uint8Array(binary.length)
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i)
    }
    return bytes
  })

  const totalLength = byteArrays.reduce((acc, curr) => acc + curr.length, 0)
  const combined = new Uint8Array(totalLength)
  let offset = 0
  for (const bytes of byteArrays) {
    combined.set(bytes, offset)
    offset += bytes.length
  }

  let finalBinary = ''
  for (let i = 0; i < combined.length; i++) {
    finalBinary += String.fromCharCode(combined[i])
  }
  return btoa(finalBinary)
}
