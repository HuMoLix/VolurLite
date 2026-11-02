import type { GlobalThemeOverrides } from 'naive-ui'

export function createVolurTheme(isDark: boolean): GlobalThemeOverrides {
  return {
    common: {
      primaryColor: isDark ? '#a8c7fa' : '#1a73e8',
      primaryColorHover: isDark ? '#b6d0f8' : '#1765cc',
      primaryColorPressed: isDark ? '#a8c7fa' : '#1557b0',
      primaryColorSuppl: isDark ? '#a8c7fa' : '#4285f4',
      successColor: isDark ? '#81c995' : '#34a853',
      warningColor: isDark ? '#fdd663' : '#fbbc04',
      errorColor: isDark ? '#f28b82' : '#ea4335',
      infoColor: isDark ? '#78d9ec' : '#24c1e0',
      baseColor: isDark ? '#1e1f20' : '#ffffff',
      bodyColor: isDark ? '#1e1f20' : '#f8f9fa',
      cardColor: isDark ? '#0e0e0e' : '#ffffff',
      modalColor: isDark ? '#303134' : '#ffffff',
      popoverColor: isDark ? '#303134' : '#ffffff',
      tableColor: isDark ? '#292a2d' : '#ffffff',
      inputColor: isDark ? '#303134' : '#ffffff',
      borderColor: isDark ? '#3c4043' : '#dadce0',
      dividerColor: isDark ? '#3c4043' : '#e8eaed',
      textColorBase: isDark ? '#e3e3e3' : '#202124',
      textColor1: isDark ? '#e3e3e3' : '#202124',
      textColor2: isDark ? '#bdc1c6' : '#3c4043',
      textColor3: isDark ? '#9aa0a6' : '#5f6368',
      textColorDisabled: isDark ? '#80868b' : '#9aa0a6',
      placeholderColor: isDark ? '#80868b' : '#9aa0a6',
      borderRadius: '8px'
    },
    Button: {
      colorPrimary: isDark ? '#a8c7fa' : '#1a73e8',
      colorHoverPrimary: isDark ? '#b6d0f8' : '#1765cc',
      colorPressedPrimary: isDark ? '#a8c7fa' : '#1557b0',
      colorFocusPrimary: isDark ? '#b6d0f8' : '#1765cc',
      textColorPrimary: isDark ? '#062e6f' : '#ffffff',
      textColorHoverPrimary: isDark ? '#062e6f' : '#ffffff',
      textColorPressedPrimary: isDark ? '#062e6f' : '#ffffff',
      textColorFocusPrimary: isDark ? '#062e6f' : '#ffffff',
      borderRadiusMedium: '8px',
      borderRadiusLarge: '8px'
    },
    Card: {
      color: isDark ? '#0e0e0e' : '#ffffff',
      borderColor: isDark ? '#3c4043' : '#dadce0',
      textColor: isDark ? '#bdc1c6' : '#3c4043',
      titleTextColor: isDark ? '#e3e3e3' : '#202124'
    },
    Layout: {
      color: isDark ? '#1e1f20' : '#f8f9fa',
      siderColor: isDark ? '#292a2d' : '#ffffff',
      headerColor: isDark ? '#292a2d' : '#ffffff',
      footerColor: isDark ? '#292a2d' : '#ffffff'
    },
    Form: {
      labelTextColor: isDark ? '#bdc1c6' : '#3c4043'
    },
    Modal: {
      maskColor: isDark ? 'rgba(0, 0, 0, 0.6)' : 'rgba(0, 0, 0, 0.4)'
    },
    Tooltip: {
      color: isDark ? '#292a2d' : '#ffffff',
      textColor: isDark ? '#bdc1c6' : '#2b2d31',
      boxShadow: isDark
        ? '0 1px 3px 0 rgba(0, 0, 0, 0.4), 0 1px 2px -1px rgba(0, 0, 0, 0.4)'
        : '0 1px 3px 0 rgba(10, 13, 18, 0.1), 0 1px 2px -1px rgba(10, 13, 18, 0.1)',
      padding: '8px 12px',
      borderRadius: '8px'
    }
  }
}
