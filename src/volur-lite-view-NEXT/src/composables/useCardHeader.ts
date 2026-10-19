import { inject, provide, ref, watchEffect, isRef, type Ref } from 'vue'

const CARD_HEADER_KEY = Symbol('AUTH_CARD_HEADER')

export interface CardHeaderContext {
  title: Ref<string>
  subtitle: Ref<string>
  setHeader: (title: string, subtitle?: string) => void
}

export type HeaderSource = string | Ref<string> | (() => string)

export function provideCardHeader(defaultTitle: HeaderSource = '', defaultSubtitle: HeaderSource = ''): CardHeaderContext {
  const initialTitle = typeof defaultTitle === 'function' ? defaultTitle() : isRef(defaultTitle) ? defaultTitle.value : defaultTitle
  const initialSubtitle = typeof defaultSubtitle === 'function' ? defaultSubtitle() : isRef(defaultSubtitle) ? defaultSubtitle.value : defaultSubtitle
  const title = ref(initialTitle)
  const subtitle = ref(initialSubtitle)

  function setHeader(newTitle: string, newSubtitle = ''): void {
    title.value = newTitle
    subtitle.value = newSubtitle
  }

  const context: CardHeaderContext = {
    title,
    subtitle,
    setHeader
  }

  provide(CARD_HEADER_KEY, context)
  return context
}

export function useCardHeader(
  titleSource?: HeaderSource,
  subtitleSource?: HeaderSource
): CardHeaderContext | null {
  const context = inject<CardHeaderContext | null>(CARD_HEADER_KEY, null)
  if (!context) return null

  if (titleSource !== undefined || subtitleSource !== undefined) {
    watchEffect(() => {
      const resolvedTitle = typeof titleSource === 'function'
        ? titleSource()
        : isRef(titleSource)
          ? titleSource.value
          : titleSource || ''

      const resolvedSubtitle = typeof subtitleSource === 'function'
        ? subtitleSource()
        : isRef(subtitleSource)
          ? subtitleSource.value
          : subtitleSource || ''

      context.setHeader(resolvedTitle, resolvedSubtitle)
    })
  }

  return context
}
