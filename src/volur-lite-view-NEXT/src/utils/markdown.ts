import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.setOptions({
  gfm: true,
  breaks: true
})

export function renderMarkdown(content?: string | null): string {
  if (!content || !content.trim()) {
    return ''
  }

  try {
    const rawHtml = marked.parse(content) as string
    return DOMPurify.sanitize(rawHtml, {
      ADD_ATTR: ['target', 'rel']
    })
  } catch {
    return DOMPurify.sanitize(content)
  }
}
