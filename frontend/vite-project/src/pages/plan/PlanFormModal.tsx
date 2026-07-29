import React, { useState, useEffect } from 'react'
import {
  CloseOutlined,
  ShoppingCartOutlined,
  ThunderboltOutlined,
  RobotOutlined,
  CheckCircleOutlined,
  SafetyCertificateOutlined
} from '@ant-design/icons'
import type { SubscriptionPlan, BillingPeriod, PlanStatus } from './plan.types'

interface PlanFormModalProps {
  isOpen: boolean
  onClose: () => void
  onSave: (planData: Partial<SubscriptionPlan>) => void
  editingPlan?: SubscriptionPlan | null
}

const AVAILABLE_AI_MODELS = [
  { id: 'GPT-4o-mini', label: 'GPT-4o mini', provider: 'OpenAI' },
  { id: 'GPT-4o', label: 'GPT-4o', provider: 'OpenAI' },
  { id: 'Claude-3.5-Sonnet', label: 'Claude 3.5 Sonnet', provider: 'Anthropic' },
  { id: 'Gemini-1.5-Pro', label: 'Gemini 1.5 Pro', provider: 'Google' }
]

export default function PlanFormModal({ isOpen, onClose, onSave, editingPlan }: PlanFormModalProps) {
  const [formData, setFormData] = useState<Partial<SubscriptionPlan>>({
    plan_code: '',
    plan_name: '',
    billing_period: 'MONTHLY',
    price_amount: 990000,
    currency: 'VND',
    status: 'ACTIVE',
    is_popular: false,
    limits: {
      ai_monthly_tokens: 2000000,
      ai_daily_runs: 1000,
      ai_max_kb_docs: 50,
      ai_max_rag_chunks: 10000,
      ai_allowed_models: ['GPT-4o-mini'],
      max_marketplace_accounts: 5,
      max_tenant_users: 10,
      max_products: 5000,
      max_monthly_orders: 10000
    },
    features: {
      allow_ai_auto_reply: true,
      allow_rag_knowledge: true,
      allow_post_purchase_care: true,
      allow_multi_channel_sync: true,
      allow_analytics_alerts: true,
      allow_priority_support: false,
      custom_ai_prompt: true
    }
  })

  useEffect(() => {
    if (editingPlan) {
      setFormData({
        ...editingPlan,
        limits: { ...editingPlan.limits },
        features: { ...editingPlan.features }
      })
    } else {
      setFormData({
        plan_code: `PLAN_${Date.now().toString().slice(-4)}`,
        plan_name: '',
        billing_period: 'MONTHLY',
        price_amount: 990000,
        currency: 'VND',
        status: 'ACTIVE',
        is_popular: false,
        limits: {
          ai_monthly_tokens: 2000000,
          ai_daily_runs: 1000,
          ai_max_kb_docs: 50,
          ai_max_rag_chunks: 10000,
          ai_allowed_models: ['GPT-4o-mini'],
          max_marketplace_accounts: 5,
          max_tenant_users: 10,
          max_products: 5000,
          max_monthly_orders: 10000
        },
        features: {
          allow_ai_auto_reply: true,
          allow_rag_knowledge: true,
          allow_post_purchase_care: true,
          allow_multi_channel_sync: true,
          allow_analytics_alerts: true,
          allow_priority_support: false,
          custom_ai_prompt: true
        }
      })
    }
  }, [editingPlan, isOpen])

  if (!isOpen) return null

  const handleInputChange = (field: string, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }))
  }

  const handleLimitChange = (field: string, value: any) => {
    setFormData(prev => ({
      ...prev,
      limits: { ...prev.limits!, [field]: value }
    }))
  }

  const handleFeatureToggle = (field: string) => {
    setFormData(prev => ({
      ...prev,
      features: {
        ...prev.features!,
        [field]: !prev.features![field as keyof typeof prev.features]
      }
    }))
  }

  const toggleModel = (modelId: string) => {
    const current = formData.limits?.ai_allowed_models || []
    const updated = current.includes(modelId)
      ? current.filter(m => m !== modelId)
      : [...current, modelId]
    handleLimitChange('ai_allowed_models', updated)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    onSave(formData)
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title-box">
            <div className="modal-icon">
              <ShoppingCartOutlined />
            </div>
            <div className="modal-title-text">
              <h3>{editingPlan ? 'Cấu Hình Gói Cước Service' : 'Thêm Gói Cước Dịch Vụ Mới'}</h3>
              <p>Quản lý giá, giới hạn gian hàng & phân bổ Quota AI cho Tenant</p>
            </div>
          </div>
          <button className="btn-close-modal" onClick={onClose} type="button">
            <CloseOutlined />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-body">
          {/* SECTION 1: Thông tin cơ bản */}
          <div className="form-section">
            <div className="section-label">
              <ShoppingCartOutlined /> 1. Thông Tin Cơ Bản & Giá Thuê
            </div>
            <div className="form-grid-2">
              <div className="form-group">
                <label>Mã Gói Cước (Code) *</label>
                <input
                  type="text"
                  required
                  placeholder="VD: PLAN_PRO_V2"
                  value={formData.plan_code || ''}
                  onChange={e => handleInputChange('plan_code', e.target.value.toUpperCase())}
                />
              </div>
              <div className="form-group">
                <label>Tên Gói Dịch Vụ *</label>
                <input
                  type="text"
                  required
                  placeholder="VD: Gói Chuyên Nghiệp AI"
                  value={formData.plan_name || ''}
                  onChange={e => handleInputChange('plan_name', e.target.value)}
                />
              </div>
            </div>

            <div className="form-grid-3">
              <div className="form-group">
                <label>Chu Kỳ Thanh Toán</label>
                <select
                  value={formData.billing_period || 'MONTHLY'}
                  onChange={e => handleInputChange('billing_period', e.target.value as BillingPeriod)}
                >
                  <option value="MONTHLY">Hằng Tháng (Monthly)</option>
                  <option value="QUARTERLY">Hằng Quý (Quarterly)</option>
                  <option value="YEARLY">Hằng Năm (Yearly)</option>
                  <option value="CUSTOM">Tùy Chỉnh (Custom)</option>
                </select>
              </div>
              <div className="form-group">
                <label>Giá Thuê (VND) *</label>
                <input
                  type="number"
                  required
                  min={0}
                  step={10000}
                  value={formData.price_amount ?? 0}
                  onChange={e => handleInputChange('price_amount', Number(e.target.value))}
                />
              </div>
              <div className="form-group">
                <label>Trạng Thái</label>
                <select
                  value={formData.status || 'ACTIVE'}
                  onChange={e => handleInputChange('status', e.target.value as PlanStatus)}
                >
                  <option value="ACTIVE">HOẠT ĐỘNG (Active)</option>
                  <option value="INACTIVE">TẠM DỪNG (Inactive)</option>
                  <option value="ARCHIVED">LƯU TRỮ (Archived)</option>
                </select>
              </div>
            </div>
          </div>

          {/* SECTION 2: Quota & Giới hạn AI */}
          <div className="form-section">
            <div className="section-label" style={{ color: '#7c3aed' }}>
              <ThunderboltOutlined /> 2. Giới Hạn Tài Nguyên AI (Tokens/Quota & Models)
            </div>
            <div className="form-grid-2">
              <div className="form-group">
                <label>Max AI Tokens / Tháng *</label>
                <input
                  type="number"
                  required
                  min={100000}
                  step={100000}
                  value={formData.limits?.ai_monthly_tokens ?? 2000000}
                  onChange={e => handleLimitChange('ai_monthly_tokens', Number(e.target.value))}
                />
                <small style={{ color: '#64748b', fontSize: 11 }}>
                  Tương đương ~{((formData.limits?.ai_monthly_tokens || 0) / 1000000).toFixed(1)}M Tokens/tháng
                </small>
              </div>
              <div className="form-group">
                <label>Max Runs AI Auto-Reply / Ngày *</label>
                <input
                  type="number"
                  required
                  min={50}
                  step={50}
                  value={formData.limits?.ai_daily_runs ?? 1000}
                  onChange={e => handleLimitChange('ai_daily_runs', Number(e.target.value))}
                />
              </div>
            </div>

            <div className="form-grid-2">
              <div className="form-group">
                <label>Max Tài Liệu KB (RAG Documents)</label>
                <input
                  type="number"
                  value={formData.limits?.ai_max_kb_docs ?? 50}
                  onChange={e => handleLimitChange('ai_max_kb_docs', Number(e.target.value))}
                />
              </div>
              <div className="form-group">
                <label>Max Vector Chunks (RAG Memory)</label>
                <input
                  type="number"
                  value={formData.limits?.ai_max_rag_chunks ?? 10000}
                  onChange={e => handleLimitChange('ai_max_rag_chunks', Number(e.target.value))}
                />
              </div>
            </div>

            <div className="form-group">
              <label>Model AI Cho Phép Sử Dụng</label>
              <div className="models-chips-container">
                {AVAILABLE_AI_MODELS.map(m => {
                  const isSelected = (formData.limits?.ai_allowed_models || []).includes(m.id)
                  return (
                    <div
                      key={m.id}
                      className={`model-chip ${isSelected ? 'selected' : ''}`}
                      onClick={() => toggleModel(m.id)}
                    >
                      <RobotOutlined />
                      <span>{m.label} ({m.provider})</span>
                      {isSelected && <CheckCircleOutlined style={{ color: '#7c3aed' }} />}
                    </div>
                  )
                })}
              </div>
            </div>
          </div>

          {/* SECTION 3: Giới hạn Vận hành POS */}
          <div className="form-section">
            <div className="section-label">
              <SafetyCertificateOutlined /> 3. Giới Hạn Vận Hành POS & Sàn
            </div>
            <div className="form-grid-3">
              <div className="form-group">
                <label>Max Gian Hàng (Shops)</label>
                <input
                  type="number"
                  min={1}
                  value={formData.limits?.max_marketplace_accounts ?? 5}
                  onChange={e => handleLimitChange('max_marketplace_accounts', Number(e.target.value))}
                />
              </div>
              <div className="form-group">
                <label>Max Nhân Viên (Users)</label>
                <input
                  type="number"
                  min={1}
                  value={formData.limits?.max_tenant_users ?? 10}
                  onChange={e => handleLimitChange('max_tenant_users', Number(e.target.value))}
                />
              </div>
              <div className="form-group">
                <label>Max Sản Phẩm (Catalog)</label>
                <input
                  type="number"
                  min={100}
                  value={formData.limits?.max_products ?? 5000}
                  onChange={e => handleLimitChange('max_products', Number(e.target.value))}
                />
              </div>
            </div>
          </div>

          {/* SECTION 4: Features Enabled */}
          <div className="form-section">
            <div className="section-label">
              <CheckCircleOutlined /> 4. Quyền Hạn Tính Năng Mở Rộng
            </div>
            <div className="checkbox-group-grid">
              <label className="checkbox-card">
                <input
                  type="checkbox"
                  checked={formData.features?.allow_ai_auto_reply ?? true}
                  onChange={() => handleFeatureToggle('allow_ai_auto_reply')}
                />
                <span>Tự động phản hồi Chat AI</span>
              </label>
              <label className="checkbox-card">
                <input
                  type="checkbox"
                  checked={formData.features?.allow_rag_knowledge ?? true}
                  onChange={() => handleFeatureToggle('allow_rag_knowledge')}
                />
                <span>RAG Knowledge Base & FAQ</span>
              </label>
              <label className="checkbox-card">
                <input
                  type="checkbox"
                  checked={formData.features?.allow_post_purchase_care ?? true}
                  onChange={() => handleFeatureToggle('allow_post_purchase_care')}
                />
                <span>Chăm sóc sau bán tự động</span>
              </label>
              <label className="checkbox-card">
                <input
                  type="checkbox"
                  checked={formData.features?.allow_multi_channel_sync ?? true}
                  onChange={() => handleFeatureToggle('allow_multi_channel_sync')}
                />
                <span>Đồng bộ tồn kho đa sàn (Sync)</span>
              </label>
            </div>
          </div>
        </form>

        <div className="modal-footer">
          <button className="btn-cancel" onClick={onClose} type="button">
            Hủy Bỏ
          </button>
          <button className="btn-submit" onClick={handleSubmit} type="button">
            {editingPlan ? 'Lưu Thay Đổi' : 'Tạo Gói Cước Mới'}
          </button>
        </div>
      </div>
    </div>
  )
}
