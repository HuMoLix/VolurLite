export interface AdminContainerTestConnectionDTO {
  containerType?: 'DOCKER_SWARM' | 'K3S' | string
  dockerSwarmApiUrl?: string
  dockerSwarmCertPath?: string
  k3sMasterUrl?: string
  k3sKubeconfigPath?: string
  k3sNamespace?: string
}

export interface AdminContainerTestConnectionVO {
  connected: boolean
  message: string
  clusterVersion?: string
  nodeCount?: number
  latencyMs?: number
}

export interface AdminClusterSettingsVO {
  containerType: 'DOCKER_SWARM' | 'K3S' | string
  portRangeStart: number
  portRangeEnd: number
  publicHost: string
  dockerSwarmApiUrl: string
  dockerSwarmNetwork: string
  dockerSwarmCertPath: string
  dockerSwarmOutboundEnabled?: boolean
  k3sMasterUrl: string
  k3sNamespace: string
  k3sKubeconfigPath: string
  k3sToken?: string
  resourceIsolationEnabled: boolean
  totalNodes: number
  masterNodes: number
  workerNodes: number
  readyNodes: number
}

export interface AdminClusterSettingsDTO {
  containerType: string
  portRangeStart: number
  portRangeEnd: number
  publicHost: string
  dockerSwarmApiUrl?: string
  dockerSwarmNetwork?: string
  dockerSwarmCertPath?: string
  dockerSwarmOutboundEnabled?: boolean
  k3sMasterUrl?: string
  k3sNamespace?: string
  k3sKubeconfigPath?: string
  k3sToken?: string
  resourceIsolationEnabled?: boolean
}

export interface AdminClusterNodeVO {
  id: string | number
  nodeName: string
  ipAddress: string
  sshPort?: number
  sshUser?: string
  hasSshAuth?: boolean
  authType?: 'password' | 'key'
  dockerPort?: number
  tlsEnabled?: boolean
  tlsCertPath?: string
  clusterType: 'DOCKER_SWARM' | 'K3S' | string
  role: 'MASTER' | 'WORKER' | string
  isResourceNode: boolean
  status: 'READY' | 'JOINING' | 'OFFLINE' | 'ERROR' | string
  osInfo?: string
  dockerVersion?: string
  cpuCores?: number
  memoryBytes?: number
  memoryFormatted?: string
  containerCount?: number
  imageCount?: number
  inCluster?: boolean
  clusterNodeId?: string
  joinTime: string
  lastHeartbeat?: string
}

export interface AdminClusterNodeCreateDTO {
  nodeName: string
  ipAddress: string
  sshPort: number
  clusterType: string
  role: string
  isResourceNode?: boolean
  enableSshAuth?: boolean
  sshUser: string
  sshPassword?: string
  sshKey?: string
}

export interface AdminClusterNodeRoleUpdateDTO {
  role: string
  isResourceNode: boolean
}

export interface AdminClusterNodeUpdateDTO {
  nodeName: string
  ipAddress: string
  sshPort: number
  sshUser: string
  sshPassword?: string
  sshKey?: string
  role: string
  isResourceNode?: boolean
}
