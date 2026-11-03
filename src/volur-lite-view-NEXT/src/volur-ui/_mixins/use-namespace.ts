export const DEFAULT_PREFIX = 'volur'

export function useNamespace(block: string, customPrefix = DEFAULT_PREFIX) {
  const b = (blockSuffix = '') => {
    return blockSuffix ? `${customPrefix}-${block}-${blockSuffix}` : `${customPrefix}-${block}`
  }

  const e = (element = '') => {
    return element ? `${b()}__${element}` : ''
  }

  const m = (modifier = '') => {
    return modifier ? `${b()}--${modifier}` : ''
  }

  const em = (element = '', modifier = '') => {
    return element && modifier ? `${b()}__${element}--${modifier}` : ''
  }

  const is = (name: string, state = true) => {
    return state ? `is-${name}` : ''
  }

  const cssVar = (name: string) => {
    return `--${customPrefix}-${block}-${name}`
  }

  const cssVarBlock = (name: string) => {
    return `--${customPrefix}-${name}`
  }

  return {
    b,
    e,
    m,
    em,
    is,
    cssVar,
    cssVarBlock
  }
}
