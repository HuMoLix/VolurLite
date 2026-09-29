import type { RouteRecordRaw } from 'vue-router'
import { authRoutes } from './routes/auth'
import { competitionRoutes } from './routes/competition'
import { adminRoutes } from './routes/admin'
import { commonRoutes } from './routes/common'

/**
 * Volur Lite NEXT 统一聚合路由表
 * 将各个子业务模块 (auth, competition, admin, common) 汇聚导出
 */
export const routes: RouteRecordRaw[] = [
  ...commonRoutes,
  ...competitionRoutes,
  ...authRoutes,
  ...adminRoutes
]
