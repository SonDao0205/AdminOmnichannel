/* plan.types.ts - Subscription Plan & AI Quota Types */

export type BillingPeriod = 'MONTHLY' | 'QUARTERLY' | 'YEARLY' | 'CUSTOM'
export type PlanStatus = 'ACTIVE' | 'INACTIVE' | 'ARCHIVED'

export interface PlanLimits {
  ai_monthly_tokens: number
  ai_daily_runs: number
  ai_max_kb_docs: number
  ai_max_rag_chunks: number
  ai_allowed_models: string[]
  max_marketplace_accounts: number
  max_tenant_users: number
  max_products: number
  max_monthly_orders: number
}

export interface PlanFeatures {
  allow_ai_auto_reply: boolean
  allow_rag_knowledge: boolean
  allow_post_purchase_care: boolean
  allow_multi_channel_sync: boolean
  allow_analytics_alerts: boolean
  allow_priority_support: boolean
  custom_ai_prompt: boolean
}

export interface SubscriptionPlan {
  id: string
  plan_code: string
  plan_name: string
  billing_period: BillingPeriod
  price_amount: number
  currency: string
  status: PlanStatus
  created_at: string
  updated_at: string
  active_tenants_count: number
  is_popular?: boolean

  limits: PlanLimits
  features: PlanFeatures
}
