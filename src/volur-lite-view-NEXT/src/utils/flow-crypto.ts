const _0x1a8f: readonly number[] = [
  0x37, 0x31, 0x57, 0x65, 0xbf, 0xf9, 0xe2, 0x1f, 0x2b, 0x53,
  0x67, 0xfa, 0x95, 0xb0, 0x49, 0x14, 0x00, 0xdd, 0xf2, 0x92,
  0xa5, 0x4e, 0x74, 0x28, 0xcd
]

function _0x4e12(): string {
  const _0x5b3 = 0x6c
  return _0x1a8f
    .map((_0x2c, _0x3d) => String.fromCharCode(_0x2c ^ _0x5b3 ^ (((_0x3d * 37) + 13) & 0xff)))
    .join('')
}

function _0x3c7a(x: number, y: number): number {
  const lsw = (x & 0xffff) + (y & 0xffff)
  const msw = (x >> 16) + (y >> 16) + (lsw >> 16)
  return (msw << 16) | (lsw & 0xffff)
}

function _0x5f11(num: number, cnt: number): number {
  return (num << cnt) | (num >>> (32 - cnt))
}

function _0x29ba(q: number, a: number, b: number, x: number, s: number, t: number): number {
  return _0x3c7a(_0x5f11(_0x3c7a(_0x3c7a(a, q), _0x3c7a(x, t)), s), b)
}

function _0x11ab(a: number, b: number, c: number, d: number, x: number, s: number, t: number): number {
  return _0x29ba((b & c) | (~b & d), a, b, x, s, t)
}

function _0x22cd(a: number, b: number, c: number, d: number, x: number, s: number, t: number): number {
  return _0x29ba((b & d) | (c & ~d), a, b, x, s, t)
}

function _0x33ef(a: number, b: number, c: number, d: number, x: number, s: number, t: number): number {
  return _0x29ba(b ^ c ^ d, a, b, x, s, t)
}

function _0x44fa(a: number, b: number, c: number, d: number, x: number, s: number, t: number): number {
  return _0x29ba(c ^ (b | ~d), a, b, x, s, t)
}

function _0x2f99(str: string): string {
  const utf8 = new TextEncoder().encode(str)
  const words: number[] = []
  for (let i = 0; i < utf8.length; i++) {
    words[i >> 2] = (words[i >> 2] || 0) | (utf8[i] << ((i % 4) * 8))
  }
  words[utf8.length >> 2] = (words[utf8.length >> 2] || 0) | (0x80 << ((utf8.length % 4) * 8))
  const totalWords = (((utf8.length + 8) >> 6) + 1) * 16
  for (let i = words.length; i < totalWords; i++) words[i] = 0
  words[totalWords - 2] = (utf8.length * 8) & 0xffffffff
  words[totalWords - 1] = Math.floor((utf8.length * 8) / 0x100000000)

  let a = 1732584193
  let b = -271733879
  let c = -1732584194
  let d = 271733878

  for (let i = 0; i < totalWords; i += 16) {
    const olda = a
    const oldb = b
    const oldc = c
    const oldd = d

    a = _0x11ab(a, b, c, d, words[i + 0], 7, -680876936)
    d = _0x11ab(d, a, b, c, words[i + 1], 12, -389564586)
    c = _0x11ab(c, d, a, b, words[i + 2], 17, 606105819)
    b = _0x11ab(b, c, d, a, words[i + 3], 22, -1044525330)
    a = _0x11ab(a, b, c, d, words[i + 4], 7, -176418897)
    d = _0x11ab(d, a, b, c, words[i + 5], 12, 1200080426)
    c = _0x11ab(c, d, a, b, words[i + 6], 17, -1473231341)
    b = _0x11ab(b, c, d, a, words[i + 7], 22, -45705983)
    a = _0x11ab(a, b, c, d, words[i + 8], 7, 1770035416)
    d = _0x11ab(d, a, b, c, words[i + 9], 12, -1958414417)
    c = _0x11ab(c, d, a, b, words[i + 10], 17, -42063)
    b = _0x11ab(b, c, d, a, words[i + 11], 22, -1990404162)
    a = _0x11ab(a, b, c, d, words[i + 12], 7, 1804603682)
    d = _0x11ab(d, a, b, c, words[i + 13], 12, -40341101)
    c = _0x11ab(c, d, a, b, words[i + 14], 17, -1502002290)
    b = _0x11ab(b, c, d, a, words[i + 15], 22, 1236535329)

    a = _0x22cd(a, b, c, d, words[i + 1], 5, -165796510)
    d = _0x22cd(d, a, b, c, words[i + 6], 9, -1069501632)
    c = _0x22cd(c, d, a, b, words[i + 11], 14, 643717713)
    b = _0x22cd(b, c, d, a, words[i + 0], 20, -373897302)
    a = _0x22cd(a, b, c, d, words[i + 5], 5, -701558691)
    d = _0x22cd(d, a, b, c, words[i + 10], 9, 38016083)
    c = _0x22cd(c, d, a, b, words[i + 15], 14, -660478335)
    b = _0x22cd(b, c, d, a, words[i + 4], 20, -405537848)
    a = _0x22cd(a, b, c, d, words[i + 9], 5, 568446438)
    d = _0x22cd(d, a, b, c, words[i + 14], 9, -1019803690)
    c = _0x22cd(c, d, a, b, words[i + 3], 14, -187363961)
    b = _0x22cd(b, c, d, a, words[i + 8], 20, 1163531501)
    a = _0x22cd(a, b, c, d, words[i + 13], 5, -1444681467)
    d = _0x22cd(d, a, b, c, words[i + 2], 9, -51403784)
    c = _0x22cd(c, d, a, b, words[i + 7], 14, 1735328473)
    b = _0x22cd(b, c, d, a, words[i + 12], 20, -1926607734)

    a = _0x33ef(a, b, c, d, words[i + 5], 4, -378558)
    d = _0x33ef(d, a, b, c, words[i + 8], 11, -2022574463)
    c = _0x33ef(c, d, a, b, words[i + 11], 16, 1839030562)
    b = _0x33ef(b, c, d, a, words[i + 14], 23, -35309556)
    a = _0x33ef(a, b, c, d, words[i + 1], 4, -1530992060)
    d = _0x33ef(d, a, b, c, words[i + 4], 11, 1272893353)
    c = _0x33ef(c, d, a, b, words[i + 7], 16, -155497632)
    b = _0x33ef(b, c, d, a, words[i + 10], 23, -1094730640)
    a = _0x33ef(a, b, c, d, words[i + 13], 4, 681279174)
    d = _0x33ef(d, a, b, c, words[i + 0], 11, -358537222)
    c = _0x33ef(c, d, a, b, words[i + 3], 16, -722521979)
    b = _0x33ef(b, c, d, a, words[i + 6], 23, 76029189)
    a = _0x33ef(a, b, c, d, words[i + 9], 4, -640364487)
    d = _0x33ef(d, a, b, c, words[i + 12], 11, -421815835)
    c = _0x33ef(c, d, a, b, words[i + 15], 16, 530742520)
    b = _0x33ef(b, c, d, a, words[i + 2], 23, -995338651)

    a = _0x44fa(a, b, c, d, words[i + 0], 6, -198630844)
    d = _0x44fa(d, a, b, c, words[i + 7], 10, 1126891415)
    c = _0x44fa(c, d, a, b, words[i + 14], 15, -1416354905)
    b = _0x44fa(b, c, d, a, words[i + 5], 21, -57434055)
    a = _0x44fa(a, b, c, d, words[i + 12], 6, 1700485571)
    d = _0x44fa(d, a, b, c, words[i + 3], 10, -1894986606)
    c = _0x44fa(c, d, a, b, words[i + 10], 15, -1051523)
    b = _0x44fa(b, c, d, a, words[i + 1], 21, -2054922799)
    a = _0x44fa(a, b, c, d, words[i + 8], 6, 1873313359)
    d = _0x44fa(d, a, b, c, words[i + 15], 10, -30611744)
    c = _0x44fa(c, d, a, b, words[i + 6], 15, -1560198380)
    b = _0x44fa(b, c, d, a, words[i + 13], 21, 1309151649)
    a = _0x44fa(a, b, c, d, words[i + 4], 6, -145523070)
    d = _0x44fa(d, a, b, c, words[i + 11], 10, -1120210379)
    c = _0x44fa(c, d, a, b, words[i + 2], 15, 718787259)
    b = _0x44fa(b, c, d, a, words[i + 9], 21, -343485551)

    a = _0x3c7a(a, olda)
    b = _0x3c7a(b, oldb)
    c = _0x3c7a(c, oldc)
    d = _0x3c7a(d, oldd)
  }

  return [a, b, c, d].map((num) => {
    let s = ''
    for (let j = 0; j < 4; j++) {
      s += ((num >> (j * 8)) & 0xff).toString(16).padStart(2, '0')
    }
    return s
  }).join('')
}

export async function encryptFlowCredential(
  plainText: string,
  flowName: string,
  flowEntry: string,
  dsh?: string | null
): Promise<string> {
  if (!plainText) {
    return plainText
  }

  const targetDsh = dsh || ''
  const timestamp = Date.now()
  const dynamicStr = `${flowName || ''}:${flowEntry || ''}:${targetDsh}:${timestamp}`
  const dynamicHex = _0x2f99(dynamicStr)
  const rawKey = `${_0x4e12()}#${dynamicHex}`

  const keyBytes = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(rawKey))
  const cryptoKey = await crypto.subtle.importKey(
    'raw',
    keyBytes,
    { name: 'AES-GCM' },
    false,
    ['encrypt']
  )

  const iv = crypto.getRandomValues(new Uint8Array(12))
  const plainBytes = new TextEncoder().encode(plainText)
  const cipherBuffer = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv, tagLength: 128 },
    cryptoKey,
    plainBytes
  )
  const cipherBytes = new Uint8Array(cipherBuffer)

  const payload = new Uint8Array(12 + cipherBytes.byteLength)
  payload.set(iv, 0)
  payload.set(cipherBytes, 12)

  let binary = ''
  const len = payload.byteLength
  for (let i = 0; i < len; i++) {
    binary += String.fromCharCode(payload[i])
  }
  const base64 = btoa(binary)
  const prefix = String.fromCharCode(36, 86, 49, 36, 71, 67, 77, 36)
  return `${prefix}${timestamp}$${base64}`
}
