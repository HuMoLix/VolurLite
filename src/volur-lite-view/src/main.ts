import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './store'
import i18n from './i18n'

// Import vfonts
import 'vfonts/Lato.css'
import 'vfonts/FiraCode.css'

// Import global SCSS styles
import './styles/index.scss'

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(i18n)

app.mount('#app')
