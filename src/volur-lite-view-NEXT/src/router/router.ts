import type { RouteRecordRaw } from 'vue-router'
import { authRoutes } from './routes/auth'
import { competitionRoutes } from './routes/competition'
import { adminRoutes } from './routes/admin'
import { commonRoutes } from './routes/common'

export const routes: RouteRecordRaw[] = [
  ...commonRoutes,
  ...competitionRoutes,
  ...authRoutes,
  ...adminRoutes
]
