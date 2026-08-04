import {
  CheckCircleOutlined,
  CloseOutlined,
  EyeOutlined,
  LockOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  ShopOutlined,
  TeamOutlined,
  UnlockOutlined,
} from '@ant-design/icons'
import {
  useEffect,
  useMemo,
  useState,
  type FormEvent,
} from 'react'
import { toast } from 'react-toastify'
import {
  createTenant,
  getApiErrorMessage,
  getApiValidationErrors,
  getPlans,
  getTenants,
  setTenantLocked,
} from '../../apis/adminApi'
import type { SubscriptionPlan } from '../plan/plan.types'
import type {
  CreatedTenant,
  CreateTenantPayload,
  TenantListItem,
  TenantStatus,
} from '../../types/admin'
import {
  hasValidationErrors,
  normalizeCreateTenant,
  validateCreateTenant,
  type FieldErrors,
} from '../../validation/adminValidation'
import './accounts.css'

const PAGE_SIZE = 8

const initialForm: CreateTenantPayload = {
  tenantName: '',
  legalName: '',
  contactEmail: '',
  timezoneName: 'Asia/Ho_Chi_Minh',
  defaultCurrency: 'VND',
  subscriptionPlanCode: '',
  trialDays: 14,
  ownerEmail: '',
  ownerDisplayName: '',
}

const statusTabs: Array<{ value: TenantStatus | 'ALL'; label: string }> = [
  { value: 'ALL', label: 'Tất cả tài khoản' },
  { value: 'TRIAL', label: 'Đang dùng thử' },
  { value: 'ACTIVE', label: 'Đang hoạt động' },
  { value: 'SUSPENDED', label: 'Tạm ngưng' },
  { value: 'CLOSED', label: 'Đã đóng' },
]

const statusLabels: Record<TenantStatus, string> = {
  TRIAL: 'Dùng thử',
  ACTIVE: 'Hoạt động',
  SUSPENDED: 'Tạm ngưng',
  CLOSED: 'Đã đóng',
}

function formatDate(value: string | null) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(value))
}

export default function AccountsScreen() {
  const [tenants, setTenants] = useState<TenantListItem[]>([])
  const [total, setTotal] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<TenantStatus | 'ALL'>('ALL')
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState<CreateTenantPayload>(initialForm)
  const [formErrors, setFormErrors] =
    useState<FieldErrors<CreateTenantPayload>>({})
  const [availablePlans, setAvailablePlans] = useState<SubscriptionPlan[]>([])
  const [plansLoading, setPlansLoading] = useState(false)
  const [plansError, setPlansError] = useState('')
  const [createdTenant, setCreatedTenant] = useState<CreatedTenant | null>(null)
  const [selectedTenant, setSelectedTenant] = useState<TenantListItem | null>(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [changingAccessTenantId, setChangingAccessTenantId] = useState<string | null>(null)

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const nextSearch = searchInput.trim()
      if (nextSearch === search) return
      setLoading(true)
      setLoadError('')
      setPage(0)
      setSearch(nextSearch)
    }, 320)
    return () => window.clearTimeout(timer)
  }, [search, searchInput])

  useEffect(() => {
    let active = true
    getTenants({ search, status, page, size: PAGE_SIZE })
      .then((response) => {
        if (!active) return
        setLoadError('')
        setTenants(response.items)
        setTotal(response.totalElements)
        setTotalPages(response.totalPages)
      })
      .catch((error) => {
        if (active) setLoadError(getApiErrorMessage(error))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [page, reloadKey, search, status])

  const loadAvailablePlans = async () => {
    setPlansLoading(true)
    setPlansError('')
    try {
      const response = await getPlans({ status: 'ACTIVE', page: 0, size: 100 })
      setAvailablePlans(response.items)
      setForm((current) => {
        const currentPlanExists = response.items.some(
          (plan) => plan.plan_code === current.subscriptionPlanCode,
        )
        return {
          ...current,
          subscriptionPlanCode: currentPlanExists
            ? current.subscriptionPlanCode
            : (response.items[0]?.plan_code ?? ''),
        }
      })
    } catch (error) {
      setAvailablePlans([])
      setPlansError(getApiErrorMessage(error))
    } finally {
      setPlansLoading(false)
    }
  }

  const pageNumbers = useMemo(() => {
    if (totalPages <= 1) return [0]
    const start = Math.max(0, Math.min(page - 1, totalPages - 3))
    return Array.from({ length: Math.min(3, totalPages) }, (_, index) => start + index)
  }, [page, totalPages])

  const updateForm = <Key extends keyof CreateTenantPayload>(
    key: Key,
    value: CreateTenantPayload[Key],
  ) => {
    setForm((current) => ({ ...current, [key]: value }))
    setFormErrors((current) => ({ ...current, [key]: undefined }))
  }

  const openCreateModal = () => {
    setForm(initialForm)
    setFormErrors({})
    setCreatedTenant(null)
    setShowCreate(true)
    void loadAvailablePlans()
  }

  const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const normalizedForm = normalizeCreateTenant(form)
    const validationErrors = validateCreateTenant(normalizedForm)
    setFormErrors(validationErrors)
    if (hasValidationErrors(validationErrors)) return

    setCreating(true)
    try {
      const response = await createTenant(normalizedForm)
      setCreatedTenant(response)
      setLoading(true)
      setReloadKey((current) => current + 1)
      toast.success('Tạo tài khoản thành công, mật khẩu đã được gửi qua email')
    } catch (error) {
      const backendErrors = getApiValidationErrors(error)
      if (backendErrors) {
        setFormErrors(
          backendErrors as FieldErrors<CreateTenantPayload>,
        )
      }
      toast.error(getApiErrorMessage(error))
    } finally {
      setCreating(false)
    }
  }

  const handleTenantAccess = async (tenant: TenantListItem) => {
    if (tenant.tenantStatus === 'CLOSED' || changingAccessTenantId) return
    const locked = tenant.tenantStatus !== 'SUSPENDED'
    const confirmed = window.confirm(
      locked
        ? `Khóa tài khoản ${tenant.tenantName}? Các phiên đăng nhập hiện tại sẽ bị thu hồi.`
        : `Mở lại tài khoản ${tenant.tenantName}?`,
    )
    if (!confirmed) return

    setChangingAccessTenantId(tenant.tenantId)
    try {
      const response = await setTenantLocked(tenant.tenantId, locked)
      setTenants((current) => current.map((item) => (
        item.tenantId === tenant.tenantId
          ? { ...item, tenantStatus: response.tenantStatus }
          : item
      )))
      setSelectedTenant((current) => (
        current?.tenantId === tenant.tenantId
          ? { ...current, tenantStatus: response.tenantStatus }
          : current
      ))
      setLoading(true)
      setReloadKey((current) => current + 1)
      toast.success(locked ? 'Đã khóa tài khoản tenant' : 'Đã mở tài khoản tenant')
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setChangingAccessTenantId(null)
    }
  }

  return (
    <div className="tenant-page">
      <header className="tenant-topbar">
        <div className="tenant-title">
          <div>
            <p className="tenant-eyebrow">Quản trị hệ thống</p>
            <h1>Tài khoản tenant</h1>
          </div>
          <span className="tenant-count-pill">{total} tài khoản</span>
        </div>

        <div className="tenant-top-actions">
          <button
            aria-label="Tải lại danh sách"
            className="tenant-ghost-button"
            onClick={() => {
              setLoading(true)
              setLoadError('')
              setReloadKey((current) => current + 1)
            }}
            type="button"
          >
            <ReloadOutlined />
            Làm mới
          </button>
          <button className="tenant-primary-button" onClick={openCreateModal} type="button">
            <PlusOutlined />
            Tạo tài khoản thuê
          </button>
        </div>
      </header>

      <section className="tenant-content">
        <nav className="tenant-status-tabs" aria-label="Trạng thái tài khoản tenant">
          {statusTabs.map((tab) => (
            <button
              className={`tenant-status-tab ${status === tab.value ? 'is-active' : ''}`}
              key={tab.value}
              onClick={() => {
                setLoading(true)
                setLoadError('')
                setStatus(tab.value)
                setPage(0)
              }}
              type="button"
            >
              {tab.label}
              {status === tab.value && <span>{total}</span>}
            </button>
          ))}
        </nav>

        <div className="tenant-toolbar">
          <label className="tenant-search">
            <SearchOutlined />
            <input
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="Tìm mã, tên tenant hoặc email quản lý..."
              type="search"
              value={searchInput}
            />
          </label>
          <p>Cập nhật dữ liệu trực tiếp từ hệ thống quản trị</p>
        </div>

        <section className="tenant-table-card" aria-label="Danh sách tài khoản tenant">
          {loading ? (
            <div className="tenant-state">
              <span className="tenant-loading-spinner" />
              <strong>Đang tải danh sách tài khoản</strong>
              <p>Vui lòng chờ trong giây lát.</p>
            </div>
          ) : loadError ? (
            <div className="tenant-state is-error">
              <strong>Không thể tải dữ liệu</strong>
              <p>{loadError}</p>
              <button
                onClick={() => {
                  setLoading(true)
                  setLoadError('')
                  setReloadKey((current) => current + 1)
                }}
                type="button"
              >
                Thử lại
              </button>
            </div>
          ) : tenants.length === 0 ? (
            <div className="tenant-state">
              <span className="tenant-empty-icon"><TeamOutlined /></span>
              <strong>Chưa có tài khoản phù hợp</strong>
              <p>Thay đổi bộ lọc hoặc tạo tài khoản thuê đầu tiên.</p>
              <button onClick={openCreateModal} type="button">Tạo tài khoản</button>
            </div>
          ) : (
            <>
              <div className="tenant-table-scroll">
                <table className="tenant-table">
                  <thead>
                    <tr>
                      <th className="tenant-account-col">Tenant</th>
                      <th>Người quản lý</th>
                      <th>Gói dịch vụ</th>
                      <th>Thời hạn</th>
                      <th className="tenant-status-col">Trạng thái</th>
                      <th className="tenant-action-col">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {tenants.map((tenant) => (
                      <tr key={tenant.tenantId}>
                        <td>
                          <div className="tenant-account-cell">
                            <span className="tenant-avatar"><ShopOutlined /></span>
                            <span>
                              <strong>{tenant.tenantName}</strong>
                              <small>{tenant.tenantCode} · {tenant.defaultCurrency}</small>
                            </span>
                          </div>
                        </td>
                        <td>
                          <span className="tenant-main-text">
                            {tenant.ownerDisplayName ?? 'Chưa cập nhật'}
                          </span>
                          <span className="tenant-subtext">{tenant.ownerEmail ?? '—'}</span>
                        </td>
                        <td>
                          <span className="tenant-plan-badge">
                            {tenant.subscriptionPlanCode ?? 'Chưa gán'}
                          </span>
                          <span className="tenant-subtext">
                            {tenant.subscriptionPlanName ?? tenant.subscriptionStatus ?? '—'}
                          </span>
                        </td>
                        <td>
                          <span className="tenant-main-text">
                            {formatDate(tenant.periodEndsAt ?? tenant.trialEndsAt)}
                          </span>
                          <span className="tenant-subtext">
                            Tạo ngày {formatDate(tenant.createdAt)}
                          </span>
                        </td>
                        <td className="tenant-status-col">
                          <span className={`tenant-status-pill is-${tenant.tenantStatus.toLowerCase()}`}>
                            {statusLabels[tenant.tenantStatus]}
                          </span>
                        </td>
                        <td className="tenant-action-col">
                          <div className="tenant-row-actions">
                            <button
                              className="tenant-view-button"
                              onClick={() => setSelectedTenant(tenant)}
                              type="button"
                            >
                              <EyeOutlined />
                              Xem
                            </button>
                            {tenant.tenantStatus !== 'CLOSED' && (
                              <button
                                className={`tenant-access-button ${tenant.tenantStatus === 'SUSPENDED' ? 'is-unlock' : 'is-lock'}`}
                                disabled={changingAccessTenantId === tenant.tenantId}
                                onClick={() => void handleTenantAccess(tenant)}
                                type="button"
                              >
                                {changingAccessTenantId === tenant.tenantId ? (
                                  <span className="tenant-inline-spinner" />
                                ) : tenant.tenantStatus === 'SUSPENDED' ? (
                                  <UnlockOutlined />
                                ) : (
                                  <LockOutlined />
                                )}
                                {tenant.tenantStatus === 'SUSPENDED' ? 'Mở' : 'Khóa'}
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <footer className="tenant-table-footer">
                <span>
                  Hiển thị {page * PAGE_SIZE + 1}–
                  {Math.min((page + 1) * PAGE_SIZE, total)} trong {total} tài khoản
                </span>
                <div className="tenant-pagination">
                  <button
                    disabled={page === 0}
                    onClick={() => {
                      setLoading(true)
                      setPage((current) => Math.max(0, current - 1))
                    }}
                    type="button"
                  >
                    ‹
                  </button>
                  {pageNumbers.map((pageNumber) => (
                    <button
                      className={page === pageNumber ? 'is-active' : ''}
                      key={pageNumber}
                      onClick={() => {
                        setLoading(true)
                        setPage(pageNumber)
                      }}
                      type="button"
                    >
                      {pageNumber + 1}
                    </button>
                  ))}
                  <button
                    disabled={page + 1 >= totalPages}
                    onClick={() => {
                      setLoading(true)
                      setPage((current) => current + 1)
                    }}
                    type="button"
                  >
                    ›
                  </button>
                </div>
              </footer>
            </>
          )}
        </section>
      </section>

      {showCreate && (
        <div className="tenant-modal-backdrop" role="presentation">
          <section
            aria-labelledby="create-tenant-title"
            aria-modal="true"
            className="tenant-modal"
            role="dialog"
          >
            <header className="tenant-modal-header">
              <div>
                <span className="tenant-modal-icon">
                  {createdTenant ? <CheckCircleOutlined /> : <PlusOutlined />}
                </span>
                <div>
                  <p>{createdTenant ? 'Hoàn tất' : 'Tài khoản thuê mới'}</p>
                  <h2 id="create-tenant-title">
                    {createdTenant ? 'Tạo tenant thành công' : 'Tạo tài khoản tenant'}
                  </h2>
                </div>
              </div>
              <button
                aria-label="Đóng"
                className="tenant-modal-close"
                disabled={creating}
                onClick={() => setShowCreate(false)}
                type="button"
              >
                <CloseOutlined />
              </button>
            </header>

            {createdTenant ? (
              <div className="created-tenant-result">
                <div className="created-tenant-banner">
                  <CheckCircleOutlined />
                  <div>
                    <strong>{createdTenant.tenantCode}</strong>
                    <p>Tài khoản quản lý đã được tạo và gán quyền TENANT_MANAGER.</p>
                  </div>
                </div>
                <dl>
                  <div>
                    <dt>Email đăng nhập</dt>
                    <dd>{createdTenant.ownerEmail}</dd>
                  </div>
                  <div>
                    <dt>Gửi mật khẩu</dt>
                    <dd>{createdTenant.credentialsEmailSent ? 'Đã gửi qua email' : 'Chưa gửi được'}</dd>
                  </div>
                  <div>
                    <dt>Vai trò</dt>
                    <dd>{createdTenant.assignedRole}</dd>
                  </div>
                  <div>
                    <dt>Hết hạn dùng thử</dt>
                    <dd>{formatDate(createdTenant.trialEndsAt)}</dd>
                  </div>
                </dl>
                <p className="created-tenant-warning">
                  Mật khẩu tạm thời chỉ được gửi đến email đăng nhập và không hiển thị trên hệ thống quản trị.
                </p>
                <button
                  className="tenant-primary-button tenant-modal-done"
                  onClick={() => setShowCreate(false)}
                  type="button"
                >
                  Hoàn tất
                </button>
              </div>
            ) : (
              <form aria-busy={creating} className="tenant-form" noValidate onSubmit={handleCreate}>
                <div className="tenant-form-section">
                  <div className="tenant-form-heading">
                    <strong>Thông tin doanh nghiệp</strong>
                    <span>Thông tin nhận diện tài khoản tenant.</span>
                  </div>
                  <div className="tenant-form-grid">
                    <label className={formErrors.tenantName ? 'is-invalid' : ''}>
                      <span>Tên tenant *</span>
                      <input
                        aria-invalid={Boolean(formErrors.tenantName)}
                        onChange={(event) => updateForm('tenantName', event.target.value)}
                        placeholder="Cửa hàng thời trang ABC"
                        value={form.tenantName}
                      />
                      {formErrors.tenantName && (
                        <small className="tenant-field-error">{formErrors.tenantName}</small>
                      )}
                    </label>
                    <div className="tenant-auto-code-note">
                      <strong>Mã tenant được tạo tự động</strong>
                      <span>Hệ thống sẽ sinh mã duy nhất sau khi kiểm tra email và tạo tài khoản.</span>
                    </div>
                    <label
                      className={`tenant-form-wide ${formErrors.legalName ? 'is-invalid' : ''}`}
                    >
                      <span>Tên pháp lý</span>
                      <input
                        aria-invalid={Boolean(formErrors.legalName)}
                        onChange={(event) => updateForm('legalName', event.target.value)}
                        placeholder="Công ty TNHH Thời trang ABC"
                        value={form.legalName}
                      />
                      {formErrors.legalName && (
                        <small className="tenant-field-error">{formErrors.legalName}</small>
                      )}
                    </label>
                    <label className={formErrors.contactEmail ? 'is-invalid' : ''}>
                      <span>Email liên hệ</span>
                      <input
                        aria-invalid={Boolean(formErrors.contactEmail)}
                        onChange={(event) => updateForm('contactEmail', event.target.value)}
                        placeholder="contact@abc.vn"
                        type="email"
                        value={form.contactEmail}
                      />
                      {formErrors.contactEmail && (
                        <small className="tenant-field-error">{formErrors.contactEmail}</small>
                      )}
                    </label>
                    <label className={formErrors.timezoneName ? 'is-invalid' : ''}>
                      <span>Múi giờ *</span>
                      <select
                        aria-invalid={Boolean(formErrors.timezoneName)}
                        onChange={(event) => updateForm('timezoneName', event.target.value)}
                        value={form.timezoneName}
                      >
                        <option value="Asia/Ho_Chi_Minh">Asia/Ho_Chi_Minh</option>
                        <option value="Asia/Bangkok">Asia/Bangkok</option>
                        <option value="Asia/Singapore">Asia/Singapore</option>
                      </select>
                      {formErrors.timezoneName && (
                        <small className="tenant-field-error">{formErrors.timezoneName}</small>
                      )}
                    </label>
                  </div>
                </div>

                <div className="tenant-form-section">
                  <div className="tenant-form-heading">
                    <strong>Tài khoản quản lý đầu tiên</strong>
                    <span>Người dùng nhận quyền quản lý tenant.</span>
                  </div>
                  <div className="tenant-form-grid">
                    <label className={formErrors.ownerDisplayName ? 'is-invalid' : ''}>
                      <span>Họ tên quản lý *</span>
                      <input
                        aria-invalid={Boolean(formErrors.ownerDisplayName)}
                        onChange={(event) => updateForm('ownerDisplayName', event.target.value)}
                        placeholder="Nguyễn Văn An"
                        value={form.ownerDisplayName}
                      />
                      {formErrors.ownerDisplayName && (
                        <small className="tenant-field-error">{formErrors.ownerDisplayName}</small>
                      )}
                    </label>
                    <label className={formErrors.ownerEmail ? 'is-invalid' : ''}>
                      <span>Email đăng nhập *</span>
                      <input
                        aria-invalid={Boolean(formErrors.ownerEmail)}
                        onChange={(event) => updateForm('ownerEmail', event.target.value)}
                        placeholder="manager@abc.vn"
                        type="email"
                        value={form.ownerEmail}
                      />
                      {formErrors.ownerEmail && (
                        <small className="tenant-field-error">{formErrors.ownerEmail}</small>
                      )}
                    </label>
                  </div>
                </div>

                <div className="tenant-form-section">
                  <div className="tenant-form-heading">
                    <strong>Gói và thời hạn</strong>
                    <span>Cấu hình thuê ban đầu của tenant.</span>
                  </div>
                  <div className="tenant-form-grid is-three">
                    <label className={formErrors.subscriptionPlanCode ? 'is-invalid' : ''}>
                      <span>Gói dịch vụ *</span>
                      <select
                        aria-invalid={Boolean(formErrors.subscriptionPlanCode)}
                        disabled={plansLoading || availablePlans.length === 0}
                        onChange={(event) => updateForm('subscriptionPlanCode', event.target.value)}
                        value={form.subscriptionPlanCode}
                      >
                        {plansLoading && <option value="">Đang tải danh sách gói...</option>}
                        {!plansLoading && availablePlans.length === 0 && (
                          <option value="">Không có gói đang hoạt động</option>
                        )}
                        {availablePlans.map((plan) => (
                          <option key={plan.id} value={plan.plan_code}>
                            {plan.plan_name} ({plan.plan_code})
                          </option>
                        ))}
                      </select>
                      {formErrors.subscriptionPlanCode && (
                        <small className="tenant-field-error">
                          {formErrors.subscriptionPlanCode}
                        </small>
                      )}
                      {plansError && (
                        <small className="tenant-field-error">{plansError}</small>
                      )}
                    </label>
                    <label className={formErrors.trialDays ? 'is-invalid' : ''}>
                      <span>Số ngày dùng thử *</span>
                      <input
                        aria-invalid={Boolean(formErrors.trialDays)}
                        onChange={(event) => updateForm('trialDays', Number(event.target.value))}
                        type="number"
                        value={form.trialDays}
                      />
                      {formErrors.trialDays && (
                        <small className="tenant-field-error">{formErrors.trialDays}</small>
                      )}
                    </label>
                    <label className={formErrors.defaultCurrency ? 'is-invalid' : ''}>
                      <span>Tiền tệ *</span>
                      <select
                        aria-invalid={Boolean(formErrors.defaultCurrency)}
                        onChange={(event) => updateForm('defaultCurrency', event.target.value)}
                        value={form.defaultCurrency}
                      >
                        <option value="VND">VND</option>
                        <option value="USD">USD</option>
                        <option value="SGD">SGD</option>
                      </select>
                      {formErrors.defaultCurrency && (
                        <small className="tenant-field-error">{formErrors.defaultCurrency}</small>
                      )}
                    </label>
                  </div>
                </div>

                <footer className="tenant-form-actions">
                  <button
                    className="tenant-cancel-button"
                    disabled={creating}
                    onClick={() => setShowCreate(false)}
                    type="button"
                  >
                    Hủy
                  </button>
                  <button
                    className="tenant-primary-button"
                    disabled={
                      creating
                      || plansLoading
                      || availablePlans.length === 0
                    }
                    type="submit"
                  >
                    {creating ? <span className="tenant-button-spinner" /> : <PlusOutlined />}
                    {creating ? 'Đang tạo...' : 'Tạo tài khoản'}
                  </button>
                </footer>
              </form>
            )}
          </section>
        </div>
      )}

      {selectedTenant && (
        <div className="tenant-modal-backdrop" role="presentation">
          <section
            aria-labelledby="tenant-detail-title"
            aria-modal="true"
            className="tenant-modal is-detail"
            role="dialog"
          >
            <header className="tenant-modal-header">
              <div>
                <span className="tenant-modal-icon"><ShopOutlined /></span>
                <div>
                  <p>{selectedTenant.tenantCode}</p>
                  <h2 id="tenant-detail-title">{selectedTenant.tenantName}</h2>
                </div>
              </div>
              <button
                aria-label="Đóng"
                className="tenant-modal-close"
                onClick={() => setSelectedTenant(null)}
                type="button"
              >
                <CloseOutlined />
              </button>
            </header>
            <div className="tenant-detail-body">
              <span className={`tenant-status-pill is-${selectedTenant.tenantStatus.toLowerCase()}`}>
                {statusLabels[selectedTenant.tenantStatus]}
              </span>
              <dl>
                <div><dt>Người quản lý</dt><dd>{selectedTenant.ownerDisplayName ?? '—'}</dd></div>
                <div><dt>Email đăng nhập</dt><dd>{selectedTenant.ownerEmail ?? '—'}</dd></div>
                <div><dt>Email liên hệ</dt><dd>{selectedTenant.contactEmail ?? '—'}</dd></div>
                <div><dt>Gói dịch vụ</dt><dd>{selectedTenant.subscriptionPlanName ?? selectedTenant.subscriptionPlanCode ?? '—'}</dd></div>
                <div><dt>Múi giờ</dt><dd>{selectedTenant.timezoneName}</dd></div>
                <div><dt>Tiền tệ</dt><dd>{selectedTenant.defaultCurrency}</dd></div>
                <div><dt>Ngày tạo</dt><dd>{formatDate(selectedTenant.createdAt)}</dd></div>
                <div><dt>Ngày hết hạn</dt><dd>{formatDate(selectedTenant.periodEndsAt ?? selectedTenant.trialEndsAt)}</dd></div>
              </dl>
              {selectedTenant.tenantStatus !== 'CLOSED' && (
                <button
                  className={`tenant-detail-access ${selectedTenant.tenantStatus === 'SUSPENDED' ? 'is-unlock' : 'is-lock'}`}
                  disabled={changingAccessTenantId === selectedTenant.tenantId}
                  onClick={() => void handleTenantAccess(selectedTenant)}
                  type="button"
                >
                  {changingAccessTenantId === selectedTenant.tenantId ? (
                    <span className="tenant-inline-spinner" />
                  ) : selectedTenant.tenantStatus === 'SUSPENDED' ? (
                    <UnlockOutlined />
                  ) : (
                    <LockOutlined />
                  )}
                  {selectedTenant.tenantStatus === 'SUSPENDED'
                    ? 'Mở lại tài khoản tenant'
                    : 'Khóa tài khoản tenant'}
                </button>
              )}
            </div>
          </section>
        </div>
      )}
    </div>
  )
}
