import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './store'

// 导入 vfonts 高品质字体
import 'vfonts/Lato.css'
import 'vfonts/FiraCode.css'

// 导入包含 Tailwind、SCSS 变量与全局自定义属性的样式入口
import './styles/index.scss'

const app = createApp(App)
app.use(pinia)
app.use(router)
app.mount('#app')
