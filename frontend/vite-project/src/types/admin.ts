export type AdminProfile = {
  id: string
  email: string
  displayName: string
  actorType: 'PLATFORM_ADMIN'
  expiresAt?: string
}

export type TenantStatus = 'TRIAL' | 'ACTIVE' | 'SUSPENDED' | 'CLOSED'

export type TenantListItem = {
  tenantId: string
  tenantCode: string
  tenantName: string
  tenantStatus: TenantStatus
  contactEmail: string | null
  timezoneName: string
  defaultCurrency: string
  ownerUserId: string | null
  ownerEmail: string | null
  ownerDisplayName: string | null
  subscriptionPlanCode: string | null
  subscriptionPlanName: string | null
  subscriptionStatus: string | null
  trialEndsAt: string | null
  periodEndsAt: string | null
  createdAt: string
}

export type TenantPage = {
  items: TenantListItem[]
  totalElements: number
  page: number
  size: number
  totalPages: number
}

export type CreateTenantPayload = {
  tenantName: string
  legalName: string
  contactEmail: string
  timezoneName: string
  defaultCurrency: string
  subscriptionPlanCode: string
  trialDays: number
  ownerEmail: string
  ownerDisplayName: string
}

export type CreatedTenant = {
  tenantId: string
  tenantCode: string
  tenantStatus: TenantStatus
  subscriptionId: string
  subscriptionStatus: string
  trialEndsAt: string
  ownerUserId: string
  ownerEmail: string
  assignedRole: string
  credentialsEmailSent: boolean
  mustChangePassword: boolean
}

export type TenantAccessResult = {
  tenantId: string
  tenantStatus: TenantStatus
  locked: boolean
  revokedSessions: number
}

export type ApiProblem = {
  detail?: string
  title?: string
  errors?: Record<string, string>
}
