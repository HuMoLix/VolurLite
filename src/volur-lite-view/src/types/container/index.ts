/**
 * 靶机与容器模块类型定义 (对应 com.humolix.volur.container)
 */

/** 靶机实例详情 VO */
export interface ChallengeInstanceResponseVO {
  instanceId: string
  challengeId: number | string
  targetHost: string
  targetPort: number
  connectionUrl: string
  expireTime: string
  remainSeconds: number
}

/** 实例启动/销毁请求 DTO */
export interface ContainerInstanceDTO {
  challengeId: number | string
  competitionId?: number | string
}
