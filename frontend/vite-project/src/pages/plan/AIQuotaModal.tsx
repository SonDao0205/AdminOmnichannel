import React, { useState, useEffect } from 'react'
import {
  CloseOutlined,
  ThunderboltOutlined,
  RobotOutlined,
  CheckCircleOutlined,
  FireOutlined
} from '@ant-design/icons'
import type { SubscriptionPlan, PlanLimits } from './plan.types'

interface AIQuotaModalProps {
  isOpen: boolean
  plan: SubscriptionPlan | null
  onClose: () => void
  onSaveQuota: (planId: string, updatedLimits: PlanLimits) => void
}

const AVAILABLE_MODELS = [
  { id: 'GPT-4o-mini', label: 'GPT-4o mini', tag: 'Fast & Cheap' },
  { id: 'GPT-4o', label: 'GPT-4o', tag: 'Advanced Reasoning' },
  { id: 'Claude-3.5-Sonnet', label: 'Claude 3.5 Sonnet', tag: 'High Quality Chat' },
  { id: 'Gemini-1.5-Pro', label: 'Gemini 1.5 Pro', tag: 'Long Context RAG' }
]

export default function AIQuotaModal({ isOpen, plan, onClose, onSaveQuota }: AIQuotaModalProps) {
  const [limits, setLimits] = useState<PlanLimits>({
    ai_monthly_tokens: 2000000,
    ai_daily_runs: 1000,
    ai_max_kb_docs: 50,
    ai_max_rag_chunks: 10000,
    ai_allowed_models: ['GPT-4o-mini'],
    max_marketplace_accounts: 5,
    max_tenant_users: 10,
    max_products: 5000,
    max_monthly_orders: 10000
  })

  useEffect(() => {
    if (plan) {
      setLimits({ ...plan.limits })
    }
  }, [plan, isOpen])

  if (!isOpen || !plan) return null

  const handleChange = (field: keyof PlanLimits, value: any) => {
    setLimits(prev => ({ ...prev, [field]: value }))
  }

  const applyTokenPreset = (tokens: number) => {
    setLimits(prev => ({ ...prev, ai_monthly_tokens: tokens }))
  }

  const toggleModel = (modelId: string) => {
    const current = limits.ai_allowed_models || []
    const updated = current.includes(modelId)
      ? current.filter(m => m !== modelId)
      : [...current, modelId]
    handleChange('ai_allowed_models', updated)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    onSaveQuota(plan.id, limits)
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" style={{ maxWidth: 620 }} onClick={e => e.stopPropagation()}>
        <div className="modal-header" style={{ background: '#faf5ff', borderColor: '#e9d5ff' }}>
          <div className="modal-title-box">
            <div className="modal-icon ai-purple">
              <ThunderboltOutlined />
            </div>
            <div className="modal-title-text">
              <h3>Điều Chỉnh Quota AI Bổ Sung</h3>
              <p>Cấu hình giới hạn Token, Daily Runs và Vector Storage cho <strong>{plan.plan_name}</strong></p>
            </div>
          </div>
          <button className="btn-close-modal" onClick={onClose} type="button">
            <CloseOutlined />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-body">
          {/* Monthly Tokens */}
          <div className="form-group">
            <label style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span>Tổng Giới Hạn Token AI (Hằng Tháng) *</span>
              <strong style={{ color: '#7c3aed', fontSize: 13 }}>
                {(limits.ai_monthly_tokens / 1000000).toFixed(2)}M Tokens
              </strong>
            </label>
            <input
              type="number"
              required
              min={100000}
              step={100000}
              value={limits.ai_monthly_tokens}
              onChange={e => handleChange('ai_monthly_tokens', Number(e.target.value))}
            />
            <div className="preset-pills">
              <span style={{ fontSize: 11, color: '#64748b', alignSelf: 'center' }}>Gợi ý nhanh:</span>
              <button className="preset-btn" type="button" onClick={() => applyTokenPreset(1000000)}>1.0M</button>
              <button className="preset-btn" type="button" onClick={() => applyTokenPreset(3500000)}>3.5M</button>
              <button className="preset-btn" type="button" onClick={() => applyTokenPreset(10000000)}>10.0M</button>
              <button className="preset-btn" type="button" onClick={() => applyTokenPreset(50000000)}>50.0M</button>
            </div>
          </div>

          <div className="form-grid-2">
            {/* Daily Runs */}
            <div className="form-group">
              <label>Lượt Phản Hồi AI / Ngày</label>
              <input
                type="number"
                min={50}
                step={50}
                value={limits.ai_daily_runs}
                onChange={e => handleChange('ai_daily_runs', Number(e.target.value))}
              />
            </div>
            {/* Max KB Docs */}
            <div className="form-group">
              <label>Max Tài Liệu KB (RAG)</label>
              <input
                type="number"
                min={5}
                value={limits.ai_max_kb_docs}
                onChange={e => handleChange('ai_max_kb_docs', Number(e.target.value))}
              />
            </div>
          </div>

          {/* RAG Vector Chunks */}
          <div className="form-group">
            <label>Max Chunk Dữ Liệu Vector RAG</label>
            <input
              type="number"
              min={500}
              step={500}
              value={limits.ai_max_rag_chunks}
              onChange={e => handleChange('ai_max_rag_chunks', Number(e.target.value))}
            />
          </div>

          {/* AI Models Allowed */}
          <div className="form-group">
            <label>Danh Sách Model AI Được Phép Truy Cập</label>
            <div className="models-chips-container">
              {AVAILABLE_MODELS.map(m => {
                const isSelected = (limits.ai_allowed_models || []).includes(m.id)
                return (
                  <div
                    key={m.id}
                    className={`model-chip ${isSelected ? 'selected' : ''}`}
                    onClick={() => toggleModel(m.id)}
                  >
                    <RobotOutlined />
                    <span>{m.label}</span>
                    <span style={{ fontSize: 9, opacity: 0.7 }}>({m.tag})</span>
                    {isSelected && <CheckCircleOutlined style={{ color: '#7c3aed', marginLeft: 4 }} />}
                  </div>
                )
              })}
            </div>
          </div>
        </form>

        <div className="modal-footer">
          <button className="btn-cancel" onClick={onClose} type="button">
            Hủy
          </button>
          <button className="btn-submit purple" onClick={handleSubmit} type="button">
            Cập Nhật Quota AI
          </button>
        </div>
      </div>
    </div>
  )
}
