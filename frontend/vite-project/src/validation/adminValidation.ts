import type { CreateTenantPayload } from '../types/admin'

export type LoginValues = {
  email: string
  password: string
}

export type FieldErrors<T> = Partial<Record<keyof T, string>>

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+$/
const TENANT_CODE_PATTERN = /^[A-Za-z][A-Za-z0-9_]{2,49}$/
const PLAN_CODE_PATTERN = /^[A-Za-z0-9_]{2,50}$/
const CURRENCY_PATTERN = /^[A-Za-z]{3}$/

function isValidTimeZone(value: string) {
  try {
    new Intl.DateTimeFormat('vi-VN', { timeZone: value }).format()
    return true
  } catch {
    return false
  }
}

function emailError(value: string, fieldLabel: string) {
  if (!value) return `${fieldLabel} không được để trống.`
  if (value.length > 255) return `${fieldLabel} không được vượt quá 255 ký tự.`
  if (!EMAIL_PATTERN.test(value)) return `${fieldLabel} không đúng định dạng.`
  return undefined
}

export function normalizeLoginValues(values: LoginValues): LoginValues {
  return {
    email: values.email.trim().toLowerCase(),
    password: values.password,
  }
}

export function validateLogin(
  rawValues: LoginValues,
): FieldErrors<LoginValues> {
  const values = normalizeLoginValues(rawValues)
  const errors: FieldErrors<LoginValues> = {}
  const invalidEmail = emailError(values.email, 'Email quản trị')

  if (invalidEmail) errors.email = invalidEmail
  if (!values.password.trim()) {
    errors.password = 'Mật khẩu không được để trống.'
  } else if (values.password.length > 128) {
    errors.password = 'Mật khẩu không được vượt quá 128 ký tự.'
  }
  return errors
}

export function normalizeCreateTenant(
  values: CreateTenantPayload,
): CreateTenantPayload {
  return {
    tenantCode: values.tenantCode.trim().toUpperCase(),
    tenantName: values.tenantName.trim(),
    legalName: values.legalName.trim(),
    contactEmail: values.contactEmail.trim().toLowerCase(),
    timezoneName: values.timezoneName.trim(),
    defaultCurrency: values.defaultCurrency.trim().toUpperCase(),
    subscriptionPlanCode: values.subscriptionPlanCode.trim().toUpperCase(),
    trialDays: values.trialDays,
    ownerEmail: values.ownerEmail.trim().toLowerCase(),
    ownerDisplayName: values.ownerDisplayName.trim(),
  }
}

export function validateCreateTenant(
  rawValues: CreateTenantPayload,
): FieldErrors<CreateTenantPayload> {
  const values = normalizeCreateTenant(rawValues)
  const errors: FieldErrors<CreateTenantPayload> = {}

  if (!values.tenantCode) {
    errors.tenantCode = 'Mã tenant không được để trống.'
  } else if (!TENANT_CODE_PATTERN.test(values.tenantCode)) {
    errors.tenantCode =
      'Mã tenant phải dài 3-50 ký tự, bắt đầu bằng chữ và chỉ chứa chữ, số hoặc dấu gạch dưới.'
  }

  if (!values.tenantName) {
    errors.tenantName = 'Tên tenant không được để trống.'
  } else if (values.tenantName.length < 2 || values.tenantName.length > 255) {
    errors.tenantName = 'Tên tenant phải có từ 2 đến 255 ký tự.'
  }

  if (values.legalName.length > 255) {
    errors.legalName = 'Tên pháp lý không được vượt quá 255 ký tự.'
  }

  if (values.contactEmail) {
    const invalidContactEmail = emailError(values.contactEmail, 'Email liên hệ')
    if (invalidContactEmail) errors.contactEmail = invalidContactEmail
  }

  if (!values.timezoneName) {
    errors.timezoneName = 'Múi giờ không được để trống.'
  } else if (
    values.timezoneName.length > 64
    || !isValidTimeZone(values.timezoneName)
  ) {
    errors.timezoneName = 'Múi giờ không hợp lệ.'
  }

  if (!values.defaultCurrency) {
    errors.defaultCurrency = 'Mã tiền tệ không được để trống.'
  } else if (!CURRENCY_PATTERN.test(values.defaultCurrency)) {
    errors.defaultCurrency = 'Mã tiền tệ phải gồm đúng 3 chữ cái.'
  }

  if (!values.subscriptionPlanCode) {
    errors.subscriptionPlanCode = 'Mã gói không được để trống.'
  } else if (!PLAN_CODE_PATTERN.test(values.subscriptionPlanCode)) {
    errors.subscriptionPlanCode =
      'Mã gói phải có từ 2 đến 50 ký tự và chỉ chứa chữ, số hoặc dấu gạch dưới.'
  }

  if (
    !Number.isInteger(values.trialDays)
    || values.trialDays < 1
    || values.trialDays > 365
  ) {
    errors.trialDays = 'Số ngày dùng thử phải là số nguyên từ 1 đến 365.'
  }

  const invalidOwnerEmail = emailError(
    values.ownerEmail,
    'Email đăng nhập',
  )
  if (invalidOwnerEmail) errors.ownerEmail = invalidOwnerEmail

  if (!values.ownerDisplayName) {
    errors.ownerDisplayName = 'Họ tên quản lý không được để trống.'
  } else if (
    values.ownerDisplayName.length < 2
    || values.ownerDisplayName.length > 255
  ) {
    errors.ownerDisplayName = 'Họ tên quản lý phải có từ 2 đến 255 ký tự.'
  }

  return errors
}

export function hasValidationErrors<T extends object>(
  errors: FieldErrors<T>,
) {
  return Object.keys(errors).length > 0
}
