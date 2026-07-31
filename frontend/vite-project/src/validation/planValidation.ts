import type { SubscriptionPlan } from '../pages/plan/plan.types'

export type PlanFieldErrors = Record<string, string>

const PLAN_CODE_PATTERN = /^[A-Za-z][A-Za-z0-9_]{1,49}$/
const BILLING_PERIODS = new Set(['MONTHLY', 'QUARTERLY', 'YEARLY', 'CUSTOM'])
const STATUSES = new Set(['ACTIVE', 'INACTIVE', 'ARCHIVED'])

export function validateSubscriptionPlan(
  values: Partial<SubscriptionPlan>,
): PlanFieldErrors {
  const errors: PlanFieldErrors = {}
  const planCode = values.plan_code?.trim() ?? ''
  const planName = values.plan_name?.trim() ?? ''
  const billingPeriod = values.billing_period ?? ''
  const currency = values.currency?.trim() ?? ''
  const status = values.status ?? ''
  const price = values.price_amount

  if (!planCode) {
    errors.plan_code = 'Mã gói không được để trống.'
  } else if (!PLAN_CODE_PATTERN.test(planCode)) {
    errors.plan_code =
      'Mã gói phải dài 2-50 ký tự, bắt đầu bằng chữ và chỉ chứa chữ, số hoặc dấu gạch dưới.'
  }

  if (!planName) {
    errors.plan_name = 'Tên gói không được để trống.'
  } else if (planName.length < 2 || planName.length > 150) {
    errors.plan_name = 'Tên gói phải có từ 2 đến 150 ký tự.'
  }

  if (!BILLING_PERIODS.has(billingPeriod)) {
    errors.billing_period = 'Chu kỳ thanh toán không hợp lệ.'
  }

  if (
    price === undefined
    || price === null
    || !Number.isFinite(price)
    || price < 0
    || !/^\d{1,16}(?:\.\d{1,2})?$/.test(String(price))
  ) {
    errors.price_amount =
      'Giá phải là số không âm, tối đa 16 chữ số nguyên và 2 chữ số thập phân.'
  }

  if (!/^[A-Za-z]{3}$/.test(currency)) {
    errors.currency = 'Mã tiền tệ phải gồm đúng 3 chữ cái.'
  }

  if (
    !values.limits
    || typeof values.limits !== 'object'
    || Object.keys(values.limits).length > 100
  ) {
    errors.limits = 'Cấu hình giới hạn không hợp lệ.'
  }

  if (
    !values.features
    || typeof values.features !== 'object'
    || Object.keys(values.features).length > 100
  ) {
    errors.features = 'Cấu hình tính năng không hợp lệ.'
  }

  if (!STATUSES.has(status)) {
    errors.status = 'Trạng thái gói không hợp lệ.'
  }

  return errors
}
