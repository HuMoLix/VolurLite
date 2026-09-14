import { marked } from 'marked'
import DOMPurify from 'dompurify'

// 配置 marked 选项以支持 GFM（GitHub Flavored Markdown）及友好断行
marked.setOptions({
  gfm: true,
  breaks: true
})

/**
 * 将 Markdown 格式的文本安全渲染为 HTML 字符串
 * 使用 DOMPurify 针对 XSS 跨站脚本进行全面防御与清洗
 *
 * @param content 原始 Markdown 字符串
 * @returns 经过清洗的安全 HTML 字符串
 */
export function renderMarkdown(content?: string | null): string {
  if (!content || !content.trim()) {
    return ''
  }

  try {
    const rawHtml = marked.parse(content) as string
    return DOMPurify.sanitize(rawHtml, {
      ADD_ATTR: ['target', 'rel']
    })
  } catch (err) {
    console.error('Failed to parse markdown:', err)
    return DOMPurify.sanitize(content)
  }
}
