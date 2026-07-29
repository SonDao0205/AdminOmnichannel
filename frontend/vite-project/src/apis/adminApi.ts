import axios, { AxiosError } from 'axios'
import type {
  AdminProfile,
  ApiProblem,
  CreatedTenant,
  CreateTenantPayload,
  TenantPage,
  TenantStatus,
} from '../types/admin'

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
