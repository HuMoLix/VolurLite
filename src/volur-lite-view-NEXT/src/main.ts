import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './store'
import i18n from './i18n'

import 'vfonts/Lato.css'
import 'vfonts/FiraCode.css'
import './styles/index.scss'

const app = createApp(App)
app.use(pinia)
app.use(i18n)
app.use(router)
app.mount('#app')
