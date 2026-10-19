import type { Config } from 'tailwindcss'

export default {
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}',
  ],
  darkMode: 'class', // 配合 App.vue 与 html.dark 类名无缝联动
  corePlugins: {
    preflight: false, // 禁用默认 preflight，杜绝破坏 Naive UI 按钮/表单/输入框默认样式
  },
  theme: {
    extend: {
      colors: {
        google: {
          blue: {
            DEFAULT: '#1a73e8',
            hover: '#1765cc',
            active: '#1557b0',
            dark: '#a8c7fa',
            'dark-hover': '#b6d0f8',
          },
          red: {
            DEFAULT: '#ea4335',
            dark: '#f28b82',
          },
          green: {
            DEFAULT: '#34a853',
            dark: '#81c995',
          },
          yellow: {
            DEFAULT: '#fbbc04',
            dark: '#fdd663',
          },
          cyan: {
            DEFAULT: '#24c1e0',
            dark: '#78d9ec',
          },
          surface: {
            light: '#f8f9fa',
            card: '#ffffff',
            elevated: '#f1f3f4',
            dark: '#1e1f20',
            darkCard: '#0e0e0e',
            darkElevated: '#303134',
          },
          text: {
            primary: '#202124',
            desc: '#3c4043',
            muted: '#5f6368',
            sub: '#80868b',
            darkPrimary: '#e3e3e3',
            darkDesc: '#bdc1c6',
            darkMuted: '#9aa0a6',
          },
          border: {
            light: '#dadce0',
            dark: '#3c4043',
          }
        },
        'sys-color-surface-container': '#f0f4f9',
        'sys-color-surface-container-dark': '#1e1f20',
      },
      fontFamily: {
        sans: ['Lato', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'Helvetica Neue', 'Arial', 'sans-serif'],
        mono: ['Fira Code', 'monospace'],
      },
    },
  },
  plugins: [],
} satisfies Config
