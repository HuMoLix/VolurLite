import type { Directive, DirectiveBinding } from 'vue'

export type TooltipPlacement = 'top' | 'bottom' | 'left' | 'right'

export interface TooltipOptions {
  content?: string
  placement?: TooltipPlacement
  disabled?: boolean
  delay?: number
}

interface TooltipElement extends HTMLElement {
  _tooltipBinding?: TooltipOptions | string
  _tooltipEnterHandler?: () => void
  _tooltipLeaveHandler?: () => void
  _tooltipClickHandler?: () => void
}

let tooltipEl: HTMLElement | null = null
let currentTarget: TooltipElement | null = null
let showTimer: number | null = null

function ensureTooltipElement(): HTMLElement {
  if (!tooltipEl) {
    tooltipEl = document.createElement('div')
    tooltipEl.className = 'v-directive-tooltip'
    document.body.appendChild(tooltipEl)
  }
  return tooltipEl
}

function resolveOptions(binding: DirectiveBinding<string | TooltipOptions>): TooltipOptions {
  const argPlacement = (binding.arg as TooltipPlacement) || undefined
  if (typeof binding.value === 'object' && binding.value !== null) {
    return {
      content: binding.value.content,
      placement: binding.value.placement || argPlacement || 'top',
      disabled: Boolean(binding.value.disabled),
      delay: binding.value.delay ?? 10000
    }
  }
  return {
    content: typeof binding.value === 'string' ? binding.value : '',
    placement: argPlacement || 'top',
    disabled: !binding.value,
    delay: 10000
  }
}

function hideTooltip() {
  if (showTimer !== null) {
    window.clearTimeout(showTimer)
    showTimer = null
  }
  if (tooltipEl) {
    tooltipEl.classList.remove('is-visible')
  }
  currentTarget = null
}

function positionTooltip(el: HTMLElement, placement: TooltipPlacement) {
  if (!tooltipEl) return

  const rect = el.getBoundingClientRect()
  const tipWidth = tooltipEl.offsetWidth
  const tipHeight = tooltipEl.offsetHeight
  const gap = 8

  let top = 0
  let left = 0

  let effectivePlacement = placement
  if (placement === 'top' && rect.top - tipHeight - gap < 4) {
    effectivePlacement = 'bottom'
  } else if (placement === 'bottom' && rect.bottom + tipHeight + gap > window.innerHeight - 4) {
    effectivePlacement = 'top'
  } else if (placement === 'left' && rect.left - tipWidth - gap < 4) {
    effectivePlacement = 'right'
  } else if (placement === 'right' && rect.right + tipWidth + gap > window.innerWidth - 4) {
    effectivePlacement = 'left'
  }

  if (effectivePlacement === 'top') {
    top = rect.top - tipHeight - gap
    left = rect.left + rect.width / 2 - tipWidth / 2
  } else if (effectivePlacement === 'bottom') {
    top = rect.bottom + gap
    left = rect.left + rect.width / 2 - tipWidth / 2
  } else if (effectivePlacement === 'left') {
    top = rect.top + rect.height / 2 - tipHeight / 2
    left = rect.left - tipWidth - gap
  } else {
    top = rect.top + rect.height / 2 - tipHeight / 2
    left = rect.right + gap
  }

  left = Math.max(6, Math.min(left, window.innerWidth - tipWidth - 6))
  top = Math.max(6, Math.min(top, window.innerHeight - tipHeight - 6))

  tooltipEl.setAttribute('data-placement', effectivePlacement)
  tooltipEl.style.top = `${Math.round(top)}px`
  tooltipEl.style.left = `${Math.round(left)}px`
}

function showTooltip(el: TooltipElement, options: TooltipOptions) {
  if (options.disabled || !options.content) {
    hideTooltip()
    return
  }

  if (showTimer !== null) {
    window.clearTimeout(showTimer)
    showTimer = null
  }

  const exec = () => {
    const tip = ensureTooltipElement()
    tip.textContent = options.content || ''
    currentTarget = el

    tip.style.visibility = 'hidden'
    tip.style.display = 'block'
    tip.classList.remove('is-visible')

    positionTooltip(el, options.placement || 'top')

    tip.style.visibility = 'visible'
    requestAnimationFrame(() => {
      if (currentTarget === el) {
        tip.classList.add('is-visible')
      }
    })
  }

  if (options.delay && options.delay > 0) {
    showTimer = window.setTimeout(exec, options.delay)
  } else {
    exec()
  }
}

export const vTooltip: Directive<TooltipElement, string | TooltipOptions> = {
  mounted(el, binding) {
    el._tooltipBinding = binding.value

    const onEnter = () => {
      const opts = resolveOptions(binding)
      showTooltip(el, opts)
    }

    const onLeave = () => {
      if (currentTarget === el) {
        hideTooltip()
      } else if (showTimer !== null) {
        window.clearTimeout(showTimer)
        showTimer = null
      }
    }

    const onClick = () => {
      hideTooltip()
    }

    el._tooltipEnterHandler = onEnter
    el._tooltipLeaveHandler = onLeave
    el._tooltipClickHandler = onClick

    el.addEventListener('mouseenter', onEnter)
    el.addEventListener('mouseleave', onLeave)
    el.addEventListener('click', onClick)
  },

  updated(el, binding) {
    el._tooltipBinding = binding.value
    const options = resolveOptions(binding)
    if (currentTarget === el) {
      if (options.disabled || !options.content) {
        hideTooltip()
      } else {
        if (tooltipEl) {
          tooltipEl.textContent = options.content
          positionTooltip(el, options.placement || 'top')
        }
      }
    }
  },

  unmounted(el) {
    if (currentTarget === el) {
      hideTooltip()
    }
    if (el._tooltipEnterHandler) {
      el.removeEventListener('mouseenter', el._tooltipEnterHandler)
    }
    if (el._tooltipLeaveHandler) {
      el.removeEventListener('mouseleave', el._tooltipLeaveHandler)
    }
    if (el._tooltipClickHandler) {
      el.removeEventListener('click', el._tooltipClickHandler)
    }
    delete el._tooltipBinding
    delete el._tooltipEnterHandler
    delete el._tooltipLeaveHandler
    delete el._tooltipClickHandler
  }
}

export default vTooltip
