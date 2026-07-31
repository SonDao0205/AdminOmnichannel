import axios, { AxiosError } from 'axios'
import type {
  AdminProfile,
  ApiProblem,
  CreatedTenant,
  CreateTenantPayload,
  TenantPage,
  TenantStatus,
} from '../types/admin'
import type { SubscriptionPlan, PlanStatus } from '../pages/plan/plan.types'


const adminApi = axios.create({
  baseURL: import.meta.env.VITE_ADMIN_API_URL ?? 'http://localhost:8080',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

type CsrfResponse = {
  headerName: string
  parameterName: string
  token: string
}

async function getCsrfToken() {
  const response = await adminApi.get<CsrfResponse>('/api/admin/auth/csrf')
  return response.data
}

export async function loginAdmin(email: string, password: string) {
  const csrf = await getCsrfToken()
  const response = await adminApi.post<AdminProfile>(
    '/api/admin/auth/login',
    { email, password },
    { headers: { [csrf.headerName]: csrf.token } },
  )
  return response.data
}

export async function getCurrentAdmin() {
  const response = await adminApi.get<AdminProfile>('/api/admin/auth/me')
  return response.data
}

export async function logoutAdmin() {
  const csrf = await getCsrfToken()
  await adminApi.post(
    '/api/admin/auth/logout',
    {},
    { headers: { [csrf.headerName]: csrf.token } },
  )
}

export async function getTenants(params: {
  search?: string
  status?: TenantStatus | 'ALL'
  page?: number
  size?: number
}) {
  const response = await adminApi.get<TenantPage>('/api/admin/tenants', { params })
  return response.data
}

export async function createTenant(payload: CreateTenantPayload) {
  const csrf = await getCsrfToken()
  const response = await adminApi.post<CreatedTenant>(
    '/api/admin/tenants',
    payload,
    { headers: { [csrf.headerName]: csrf.token } },
  )
  return response.data
}

export function getApiErrorMessage(error: unknown) {
  if (!axios.isAxiosError(error)) {
    return 'Đã có lỗi xảy ra. Vui lòng thử lại.'
  }

  const axiosError = error as AxiosError<ApiProblem>
  const validationErrors = axiosError.response?.data?.errors
  if (validationErrors && Object.keys(validationErrors).length > 0) {
    return Object.values(validationErrors)[0]
  }

  if (!axiosError.response) {
    return 'Không thể kết nối đến máy chủ quản trị.'
  }

  if (axiosError.response.status === 401) {
    return 'Email hoặc mật khẩu không chính xác.'
  }

  if (axiosError.response.status === 403) {
    return 'Phiên bảo mật không hợp lệ. Vui lòng thử lại.'
  }

  return axiosError.response.data?.detail
    ?? axiosError.response.data?.title
    ?? 'Không thể xử lý yêu cầu.'
}

export function getApiValidationErrors(error: unknown) {
  if (!axios.isAxiosError<ApiProblem>(error)) return undefined
  return error.response?.data?.errors
}

export interface SubscriptionPlanResponseDTO {
  planId: string
  planCode: string
  planName: string
  billingPeriod: string
  priceAmount: number
  currency: string
  limits: Record<string, any>
  features: Record<string, any>
  status: PlanStatus
  createdAt: string
  updatedAt: string
  activeTenantsCount: number
}

export interface SubscriptionPlanPageResponseDTO {
  items: SubscriptionPlanResponseDTO[]
  totalElements: number
  page: number
  size: number
  totalPages: number
}

export function mapPlanResponseDTOToPlan(dto: SubscriptionPlanResponseDTO): SubscriptionPlan {
  return {
    id: dto.planId,
    plan_code: dto.planCode,
    plan_name: dto.planName,
    billing_period: dto.billingPeriod as any,
    price_amount: dto.priceAmount,
    currency: dto.currency,
    status: dto.status,
    created_at: dto.createdAt,
    updated_at: dto.updatedAt,
    active_tenants_count: dto.activeTenantsCount || 0,
    limits: {
      ai_monthly_tokens: Number(dto.limits?.ai_monthly_tokens ?? 0),
      ai_daily_runs: Number(dto.limits?.ai_daily_runs ?? 0),
      ai_max_kb_docs: Number(dto.limits?.ai_max_kb_docs ?? 0),
      ai_max_rag_chunks: Number(dto.limits?.ai_max_rag_chunks ?? 0),
      ai_allowed_models: Array.isArray(dto.limits?.ai_allowed_models) ? dto.limits.ai_allowed_models : [],
      max_marketplace_accounts: Number(dto.limits?.max_marketplace_accounts ?? 0),
      max_tenant_users: Number(dto.limits?.max_tenant_users ?? 0),
      max_products: Number(dto.limits?.max_products ?? 0),
      max_monthly_orders: Number(dto.limits?.max_monthly_orders ?? 0),
    },
    features: {
      allow_ai_auto_reply: Boolean(dto.features?.allow_ai_auto_reply),
      allow_rag_knowledge: Boolean(dto.features?.allow_rag_knowledge),
      allow_post_purchase_care: Boolean(dto.features?.allow_post_purchase_care),
      allow_multi_channel_sync: Boolean(dto.features?.allow_multi_channel_sync),
      allow_analytics_alerts: Boolean(dto.features?.allow_analytics_alerts),
      allow_priority_support: Boolean(dto.features?.allow_priority_support),
      custom_ai_prompt: Boolean(dto.features?.custom_ai_prompt),
    }
  }
}

export function mapPlanToRequestPayload(plan: Partial<SubscriptionPlan>) {
  return {
    planCode: plan.plan_code,
    planName: plan.plan_name,
    billingPeriod: plan.billing_period,
    priceAmount: plan.price_amount,
    currency: plan.currency || 'VND',
    limits: plan.limits,
    features: plan.features,
    status: plan.status,
  }
}

export async function getPlans(params: {
  search?: string
  status?: PlanStatus | 'ALL'
  page?: number
  size?: number
}) {
  const response = await adminApi.get<SubscriptionPlanPageResponseDTO>('/api/admin/plans', { params })
  return {
    items: response.data.items.map(mapPlanResponseDTOToPlan),
    totalElements: response.data.totalElements,
    page: response.data.page,
    size: response.data.size,
    totalPages: response.data.totalPages,
  }
}

export async function createPlan(plan: Partial<SubscriptionPlan>) {
  const csrf = await getCsrfToken()
  const payload = mapPlanToRequestPayload(plan)
  const response = await adminApi.post<SubscriptionPlanResponseDTO>(
    '/api/admin/plans',
    payload,
    { headers: { [csrf.headerName]: csrf.token } },
  )
  return mapPlanResponseDTOToPlan(response.data)
}

export async function updatePlan(planId: string, plan: Partial<SubscriptionPlan>) {
  const csrf = await getCsrfToken()
  const payload = mapPlanToRequestPayload(plan)
  const response = await adminApi.put<SubscriptionPlanResponseDTO>(
    `/api/admin/plans/${planId}`,
    payload,
    { headers: { [csrf.headerName]: csrf.token } },
  )
  return mapPlanResponseDTOToPlan(response.data)
}

export async function updatePlanStatus(planId: string, status: PlanStatus) {
  const csrf = await getCsrfToken()
  const response = await adminApi.patch<SubscriptionPlanResponseDTO>(
    `/api/admin/plans/${planId}/status`,
    { status },
    { headers: { [csrf.headerName]: csrf.token } },
  )
  return mapPlanResponseDTOToPlan(response.data)
}
