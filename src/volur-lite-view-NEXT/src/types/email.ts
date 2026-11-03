export interface EmailLogVO {
  id: number | string
  messageId?: string
  recipient: string
  subject: string
  content?: string
  type: string
  status: 'SENDING' | 'SENT' | 'DELIVERED' | 'BOUNCED' | 'COMPLAINED' | 'FAILED' | 'SUCCESS' | string
  errorMessage?: string
  sender?: string
  operatorId?: number | string
  operatorIp?: string
  createdAt: string
}

export interface AdminEmailStatisticsVO {
  totalEmails: number
  successEmails: number
  failedEmails: number
  todayEmails: number
}

export interface AdminEmailQueryDTO {
  page?: number
  size?: number
  sortBy?: string
  sortOrder?: string
  keyword?: string
  type?: string
  status?: string
  startTime?: string
  endTime?: string
}

export interface AdminSendEmailDTO {
  recipient: string
  subject: string
  content: string
  html?: boolean
}
