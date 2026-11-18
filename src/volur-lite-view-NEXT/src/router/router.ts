import type { RouteRecordRaw } from 'vue-router'
import { authRoutes } from './routes/auth'
import { competitionRoutes } from './routes/competition'
import { communityRoutes } from './routes/community'
import { trainingRoutes } from './routes/training'
import { adminRoutes } from './routes/admin'
import { commonRoutes } from './routes/common'

export const routes: RouteRecordRaw[] = [
  ...commonRoutes,
  ...competitionRoutes,
  ...trainingRoutes,
  ...communityRoutes,
  ...authRoutes,
  ...adminRoutes
]
