import { useState, useMemo, useEffect } from 'react'
import axios from 'axios'
import {
  PlusOutlined,
  SearchOutlined,
  ExportOutlined,
  EditOutlined,
  DeleteOutlined,
  ThunderboltOutlined,
  CrownOutlined,
  ShopOutlined,
  TeamOutlined,
  BarChartOutlined,
  CheckCircleOutlined,
  StopOutlined,
  DatabaseOutlined,
  ReloadOutlined,
  SwapOutlined
} from '@ant-design/icons'
import Swal from 'sweetalert2'
import type { SubscriptionPlan, BillingPeriod, PlanStatus, PlanLimits } from './plan.types'
import PlanFormModal from './PlanFormModal'
import AIQuotaModal from './AIQuotaModal'
import {
  getPlans,
  createPlan,
  updatePlan,
  updatePlanStatus,
  getApiErrorMessage
} from '../../apis/adminApi'
import './PlansScreen.css'


// INITIAL_PLANS removed to use API data

export default function PlansScreen() {
  const [plans, setPlans] = useState<SubscriptionPlan[]>([])
  const [loading, setLoading] = useState(true)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    getPlans({ page: 0, size: 200 })
      .then(response => {
        if (active) {
          setPlans(response.items)
        }
      })
      .catch(error => {
        console.error(error)
        Swal.fire({
          icon: 'error',
          title: 'Lỗi tải dữ liệu',
          text: getApiErrorMessage(error)
        })
      })
      .finally(() => {
        if (active) {
          setLoading(false)
        }
      })
    return () => {
      active = false
    }
  }, [reloadKey])

  const [searchTerm, setSearchTerm] = useState('')
  const [periodFilter, setPeriodFilter] = useState<string>('ALL')
  const [statusFilter, setStatusFilter] = useState<string>('ALL')
  const [sortBy, setSortBy] = useState<string>('updated')

  // Modals state
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingPlan, setEditingPlan] = useState<SubscriptionPlan | null>(null)
  
  const [isAIQuotaModalOpen, setIsAIQuotaModalOpen] = useState(false)
  const [selectedPlanForQuota, setSelectedPlanForQuota] = useState<SubscriptionPlan | null>(null)

  // KPI Calculations
  const totalPlans = plans.length
  const activePlansCount = plans.filter(p => p.status === 'ACTIVE').length
  const totalMonthlyTokensGranted = plans.reduce((acc, p) => acc + (p.status === 'ACTIVE' ? p.limits.ai_monthly_tokens * p.active_tenants_count : 0), 0)
  
  const estimatedMRR = plans.reduce((acc, p) => {
    if (p.status !== 'ACTIVE') return acc
    if (p.billing_period === 'MONTHLY') return acc + p.price_amount * p.active_tenants_count
    if (p.billing_period === 'YEARLY') return acc + (p.price_amount / 12) * p.active_tenants_count
    return acc + p.price_amount * p.active_tenants_count
  }, 0)

  // Filter & Search Logic
  const filteredPlans = useMemo(() => {
    return plans.filter(p => {
      const matchSearch =
        p.plan_name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        p.plan_code.toLowerCase().includes(searchTerm.toLowerCase())
      
      const matchPeriod = periodFilter === 'ALL' || p.billing_period === periodFilter
      const matchStatus = statusFilter === 'ALL' || p.status === statusFilter

      return matchSearch && matchPeriod && matchStatus
    }).sort((a, b) => {
      if (sortBy === 'updated') return new Date(b.updated_at).getTime() - new Date(a.updated_at).getTime()
      if (sortBy === 'price_desc') return b.price_amount - a.price_amount
      if (sortBy === 'price_asc') return a.price_amount - b.price_amount
      if (sortBy === 'tokens_desc') return b.limits.ai_monthly_tokens - a.limits.ai_monthly_tokens
      return 0
    })
  }, [plans, searchTerm, periodFilter, statusFilter, sortBy])

  // Handlers
  const handleOpenCreateModal = () => {
    setEditingPlan(null)
    setIsFormModalOpen(true)
  }

  const handleOpenEditModal = (plan: SubscriptionPlan) => {
    setEditingPlan(plan)
    setIsFormModalOpen(true)
  }

  const handleOpenAIQuotaModal = (plan: SubscriptionPlan) => {
    setSelectedPlanForQuota(plan)
    setIsAIQuotaModalOpen(true)
  }

  const handleSavePlan = async (planData: Partial<SubscriptionPlan>) => {
    try {
      if (editingPlan) {
        // Update existing
        await updatePlan(editingPlan.id, planData)
        Swal.fire({
          icon: 'success',
          title: 'Cập nhật thành công',
          text: `Đã lưu thông tin gói cước ${planData.plan_name}`,
          timer: 1800,
          showConfirmButton: false
        })
      } else {
        // Create new
        const created = await createPlan(planData)
        Swal.fire({
          icon: 'success',
          title: 'Tạo gói thành công',
          text: `Gói ${created.plan_name} đã sẵn sàng cấp phát cho Tenant!`,
          timer: 2000,
          showConfirmButton: false
        })
      }
      setReloadKey(prev => prev + 1)
      setIsFormModalOpen(false)
    } catch (error) {
      console.error(error)
      if (axios.isAxiosError(error) && error.response?.status === 400) {
        throw error
      }
      Swal.fire({
        icon: 'error',
        title: 'Thao tác thất bại',
        text: getApiErrorMessage(error)
      })
    }
  }

  const handleSaveAIQuota = async (planId: string, updatedLimits: PlanLimits) => {
    try {
      const existingPlan = plans.find(p => p.id === planId)
      if (!existingPlan) return
      await updatePlan(planId, {
        ...existingPlan,
        limits: updatedLimits
      })
      setIsAIQuotaModalOpen(false)
      setReloadKey(prev => prev + 1)
      Swal.fire({
        icon: 'success',
        title: 'Đã cập nhật Quota AI',
        text: 'Mức giới hạn Token & Daily Runs mới đã được áp dụng.',
        timer: 1800,
        showConfirmButton: false
      })
    } catch (error) {
      console.error(error)
      Swal.fire({
        icon: 'error',
        title: 'Cập nhật thất bại',
        text: getApiErrorMessage(error)
      })
    }
  }

  const togglePlanStatus = async (plan: SubscriptionPlan) => {
    const nextStatus: PlanStatus = plan.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try {
      await updatePlanStatus(plan.id, nextStatus)
      setReloadKey(prev => prev + 1)
      Swal.fire({
        toast: true,
        position: 'top-end',
        icon: 'info',
        title: `Trạng thái: ${nextStatus}`,
        showConfirmButton: false,
        timer: 1500
      })
    } catch (error) {
      console.error(error)
      Swal.fire({
        icon: 'error',
        title: 'Thay đổi trạng thái thất bại',
        text: getApiErrorMessage(error)
      })
    }
  }

  const handleDeletePlan = (plan: SubscriptionPlan) => {
    if (plan.active_tenants_count > 0) {
      Swal.fire({
        icon: 'error',
        title: 'Không thể xóa gói',
        text: `Đang có ${plan.active_tenants_count} doanh nghiệp sử dụng gói cước này! Hãy chuyển tenant sang gói khác trước.`,
        confirmButtonColor: '#2563eb'
      })
      return
    }

    Swal.fire({
      title: 'Xác nhận xóa gói cước?',
      text: `Bạn có chắc chắn muốn xóa gói "${plan.plan_name}" (${plan.plan_code}) khỏi hệ thống?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#ef4444',
      cancelButtonColor: '#64748b',
      confirmButtonText: 'Xóa Gói',
      cancelButtonText: 'Hủy'
    }).then(async result => {
      if (result.isConfirmed) {
        try {
          await updatePlanStatus(plan.id, 'ARCHIVED')
          setReloadKey(prev => prev + 1)
          Swal.fire({
            icon: 'success',
            title: 'Đã xóa (Lưu trữ)',
            text: 'Gói cước đã được chuyển sang trạng thái lưu trữ (ARCHIVED) thành công.',
            timer: 1500,
            showConfirmButton: false
          })
        } catch (error) {
          console.error(error)
          Swal.fire({
            icon: 'error',
            title: 'Xóa gói thất bại',
            text: getApiErrorMessage(error)
          })
        }
      }
    })
  }

  const handleExportExcel = () => {
    const csvContent =
      'data:text/csv;charset=utf-8,' +
      ['Mã Gói,Tên Gói,Chu Kỳ,Giá (VND),Quota AI Tokens/Tháng,Max Daily Runs,Số Tenant Active,Trạng Thái']
        .concat(
          plans.map(
            p =>
              `"${p.plan_code}","${p.plan_name}","${p.billing_period}",${p.price_amount},${p.limits.ai_monthly_tokens},${p.limits.ai_daily_runs},${p.active_tenants_count},"${p.status}"`
          )
        )
        .join('\n')

    const encodedUri = encodeURI(csvContent)
    const link = document.createElement('a')
    link.setAttribute('href', encodedUri)
    link.setAttribute('download', `danh_sach_goi_cuoc_omnichannel_${Date.now()}.csv`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  // Format Helper
  const formatPrice = (val: number, period: BillingPeriod) => {
    if (val === 0) return 'MIỄN PHÍ'
    const formatted = new Intl.NumberFormat('vi-VN').format(val) + ' đ'
    if (period === 'MONTHLY') return `${formatted} / tháng`
    if (period === 'YEARLY') return `${formatted} / năm`
    if (period === 'QUARTERLY') return `${formatted} / quý`
    return `${formatted} (custom)`
  }

  const formatTokenDisplay = (tokens: number) => {
    if (tokens >= 1000000) return `${(tokens / 1000000).toFixed(1)}M Tokens`
    return `${(tokens / 1000).toFixed(0)}K Tokens`
  }

  return (
    <div className="tenant-page">
      {/* 1. Header & Page Title */}
      <header className="tenant-topbar">
        <div className="tenant-title">
          <div>
            <p className="tenant-eyebrow">Quản trị hệ thống</p>
            <h1>Gói cước dịch vụ</h1>
          </div>
          <span className="tenant-count-pill">{totalPlans} gói cước</span>
        </div>

        <div className="tenant-top-actions">
          <button
            aria-label="Tải lại danh sách"
            className="tenant-ghost-button"
            onClick={() => setReloadKey(current => current + 1)}
            type="button"
          >
            <ReloadOutlined />
            Làm mới
          </button>
          <button
            aria-label="Xuất file Excel"
            className="tenant-ghost-button"
            onClick={handleExportExcel}
            type="button"
          >
            <ExportOutlined />
            Xuất Excel
          </button>
          <button className="tenant-primary-button" onClick={handleOpenCreateModal} type="button">
            <PlusOutlined />
            Tạo gói cước mới
          </button>
        </div>
      </header>

      <section className="tenant-content">
        <div className="plans-container" style={{ marginTop: '24px' }}>
          {/* 2. KPI Summary Cards Grid */}
          <div className="kpi-grid">
            {/* KPI 1 */}
            <div className="kpi-card red">
              <div className="kpi-card-content">
                <div className="kpi-tag-row">
                  <span className="kpi-title">Gói Bán Chạy Nhất</span>
                  <span className="kpi-badge">42 Tenants</span>
                </div>
                <div className="kpi-value">PRO_OMNI_AI</div>
                <div className="kpi-subtext">
                  <span className="kpi-trend positive">↑ 3.5M Tokens</span> / tenant / tháng
                </div>
              </div>
              <div className="kpi-icon-wrapper">
                <CrownOutlined />
              </div>
            </div>

            {/* KPI 2 */}
            <div className="kpi-card orange">
              <div className="kpi-card-content">
                <div className="kpi-tag-row">
                  <span className="kpi-title">Quota AI Đã Cấp Phát</span>
                  <span className="kpi-badge">82% Capacity</span>
                </div>
                <div className="kpi-value">{(totalMonthlyTokensGranted / 1000000).toFixed(1)}M Tokens</div>
                <div className="kpi-subtext">
                  <span className="kpi-trend neutral">⚡ {plans.reduce((acc, p) => acc + p.limits.ai_daily_runs, 0)}</span> Runs/ngày
                </div>
              </div>
              <div className="kpi-icon-wrapper">
                <ThunderboltOutlined />
              </div>
            </div>

            {/* KPI 3 */}
            <div className="kpi-card purple">
              <div className="kpi-card-content">
                <div className="kpi-tag-row">
                  <span className="kpi-title">Gói Cước Đang Mở</span>
                  <span className="kpi-badge">Active Plans</span>
                </div>
                <div className="kpi-value">0{activePlansCount} / 0{totalPlans} Gói</div>
                <div className="kpi-subtext">
                  <span className="kpi-trend positive">+1 gói mới</span> cập nhật tuần này
                </div>
              </div>
              <div className="kpi-icon-wrapper">
                <DatabaseOutlined />
              </div>
            </div>

            {/* KPI 4 */}
            <div className="kpi-card blue">
              <div className="kpi-card-content">
                <div className="kpi-tag-row">
                  <span className="kpi-title">Doanh Thu Uớc Tính (MRR)</span>
                  <span className="kpi-badge">Monthly Run Rate</span>
                </div>
                <div className="kpi-value">{(estimatedMRR / 1000000).toFixed(1)}M đ</div>
                <div className="kpi-subtext">Tính từ các Tenant đăng ký active</div>
              </div>
              <div className="kpi-icon-wrapper">
                <BarChartOutlined />
              </div>
            </div>
          </div>

          {/* 3. Filter Bar (Search + Filters + Sort) */}
          <div className="plans-controls-bar">
            <div className="plans-filter-group">
              {/* Search Box */}
              <div className="search-box">
                <SearchOutlined className="search-icon" />
                <input
                  className="search-input"
                  type="text"
                  placeholder="Tìm kiếm tên gói, mã plan_code, Quota AI..."
                  value={searchTerm}
                  onChange={e => setSearchTerm(e.target.value)}
                />
              </div>

              {/* Billing Period Filter */}
              <select
                className="filter-select"
                value={periodFilter}
                onChange={e => setPeriodFilter(e.target.value)}
              >
                <option value="ALL">Tất cả chu kỳ thanh toán</option>
                <option value="MONTHLY">Hằng Tháng (Monthly)</option>
                <option value="YEARLY">Hằng Năm (Yearly)</option>
                <option value="CUSTOM">Tùy chỉnh (Custom)</option>
              </select>

              {/* Status Filter */}
              <select
                className="filter-select"
                value={statusFilter}
                onChange={e => setStatusFilter(e.target.value)}
              >
                <option value="ALL">Trạng thái: Tất cả</option>
                <option value="ACTIVE">HOẠT ĐỘNG (Active)</option>
                <option value="INACTIVE">TẠM DỪNG (Inactive)</option>
                <option value="ARCHIVED">LƯU TRỮ (Archived)</option>
              </select>
            </div>

            {/* Sort Group */}
            <div className="plans-sort-group">
              <span>Sắp xếp:</span>
              <select className="filter-select" value={sortBy} onChange={e => setSortBy(e.target.value)}>
                <option value="updated">Mới cập nhật</option>
                <option value="price_desc">Giá giảm dần</option>
                <option value="price_asc">Giá tăng dần</option>
                <option value="tokens_desc">Quota Tokens AI nhiều nhất</option>
              </select>
            </div>
          </div>

          {/* 4. Main Data Table */}
          <div className="table-container">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Thông Tin Gói Cước</th>
                  <th>Chu Kỳ & Giá Thuê</th>
                  <th>Giới Hạn Tài Nguyên AI (Tokens/Quota)</th>
                  <th>Giới Hạn Vận Hành POS</th>
                  <th>Trạng Thái</th>
                  <th>Hành Động</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={6} style={{ textAlign: 'center', padding: '60px 20px' }}>
                      <div className="tenant-loading-spinner" style={{ margin: '0 auto' }} />
                      <strong style={{ display: 'block', marginTop: '12px', color: '#64748b' }}>Đang tải danh sách gói cước...</strong>
                    </td>
                  </tr>
                ) : filteredPlans.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ textAlign: 'center', padding: '40px 20px', color: '#64748b' }}>
                      Không tìm thấy gói cước nào phù hợp với bộ lọc.
                    </td>
                  </tr>
                ) : (
                  filteredPlans.map(plan => (
                    <tr key={plan.id}>
                      {/* Col 1: Plan Info */}
                      <td>
                        <div className="plan-info-cell">
                          <div className={`plan-icon-badge ${plan.plan_code.includes('ENTERPRISE') ? 'enterprise' : plan.plan_code.includes('STARTER') ? 'starter' : ''}`}>
                            {plan.is_popular ? <CrownOutlined style={{ color: '#d97706' }} /> : <ShopOutlined />}
                          </div>
                          <div className="plan-info-text">
                            <div className="plan-name-title">
                              {plan.plan_name}
                              {plan.is_popular && <span className="badge-featured">BÁN CHẠY</span>}
                            </div>
                            <span className="plan-code-subtitle">{plan.plan_code}</span>
                          </div>
                        </div>
                      </td>

                      {/* Col 2: Price & Billing */}
                      <td>
                        <div className="price-display">
                          <span className="price-main">{formatPrice(plan.price_amount, plan.billing_period)}</span>
                          <span className="price-sub">{plan.active_tenants_count} Tenants đang dùng</span>
                        </div>
                      </td>

                      {/* Col 3: AI Resources & Quota */}
                      <td>
                        <div className="ai-quota-cell">
                          <div className="token-highlight">
                            <ThunderboltOutlined />
                            <span>{formatTokenDisplay(plan.limits.ai_monthly_tokens)} / tháng</span>
                          </div>
                          <div className="ai-details-row">
                            <span className="ai-pill">⚡ {plan.limits.ai_daily_runs} runs/ngày</span>
                            <span className="ai-pill">📚 {plan.limits.ai_max_kb_docs} KB Docs</span>
                          </div>
                          <div className="ai-details-row">
                            {(plan.limits.ai_allowed_models || []).map(m => (
                              <span key={m} className="model-tag">
                                {m.replace('-Sonnet', '').replace('-mini', 'm')}
                              </span>
                            ))}
                          </div>
                        </div>
                      </td>

                      {/* Col 4: Operational Limits */}
                      <td>
                        <div className="limits-cell">
                          <div className="limits-item">
                            <ShopOutlined style={{ color: '#2563eb' }} />
                            <span>Max <strong>{plan.limits.max_marketplace_accounts} Gian hàng</strong> sàn</span>
                          </div>
                          <div className="limits-item">
                            <TeamOutlined style={{ color: '#16a34a' }} />
                            <span>Max <strong>{plan.limits.max_tenant_users} Nhân viên</strong></span>
                          </div>
                          <div className="limits-item">
                            <DatabaseOutlined style={{ color: '#d97706' }} />
                            <span>Max <strong>{plan.limits.max_products.toLocaleString()} SKUs</strong></span>
                          </div>
                        </div>
                      </td>

                      {/* Col 5: Status */}
                      <td>
                        <span className={`status-pill ${plan.status.toLowerCase()}`}>
                          {plan.status === 'ACTIVE' && <CheckCircleOutlined />}
                          {plan.status === 'INACTIVE' && <StopOutlined />}
                          {plan.status}
                        </span>
                      </td>

                      {/* Col 6: Action Buttons */}
                      <td>
                        <div className="action-buttons-group">
                          {/* Edit full plan */}
                          <button
                            className="btn-table-action"
                            title="Chỉnh sửa thông tin gói cước"
                            onClick={() => handleOpenEditModal(plan)}
                            type="button"
                          >
                            <EditOutlined />
                          </button>

                          {/* Quick AI Quota adjust */}
                          <button
                            className="btn-table-action ai-quota-btn"
                            title="Chỉnh sửa Quota AI (Tokens / Daily Runs)"
                            onClick={() => handleOpenAIQuotaModal(plan)}
                            type="button"
                          >
                            <ThunderboltOutlined />
                          </button>

                          {/* Toggle status */}
                          <button
                            className="btn-table-action"
                            title="Đổi trạng thái Hoạt động / Tạm dừng"
                            onClick={() => togglePlanStatus(plan)}
                            type="button"
                          >
                            <SwapOutlined />
                          </button>

                          {/* Delete */}
                          <button
                            className="btn-table-action delete-btn"
                            title="Xóa gói cước"
                            onClick={() => handleDeletePlan(plan)}
                            type="button"
                          >
                            <DeleteOutlined />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>

            {/* Table Footer / Pagination */}
            <div className="table-footer">
              <span>Hiển thị 1 - {filteredPlans.length} trong số {filteredPlans.length} gói cước</span>
              <div className="pagination-controls">
                <button className="btn-page" disabled type="button">&lt;</button>
                <button className="btn-page active" type="button">1</button>
                <button className="btn-page" disabled type="button">&gt;</button>
              </div>
            </div>
          </div>

          {/* 5. Modals */}
          <PlanFormModal
            isOpen={isFormModalOpen}
            editingPlan={editingPlan}
            onClose={() => setIsFormModalOpen(false)}
            onSave={handleSavePlan}
          />

          <AIQuotaModal
            isOpen={isAIQuotaModalOpen}
            plan={selectedPlanForQuota}
            onClose={() => setIsAIQuotaModalOpen(false)}
            onSaveQuota={handleSaveAIQuota}
          />
        </div>
      </section>
    </div>
  )
}
