import { createPinia } from 'pinia'

const pinia = createPinia()

export default pinia
export * from './modules/user'
export * from './modules/app'
export * from './modules/notification'
export * from './modules/competition'
export * from './modules/system'
