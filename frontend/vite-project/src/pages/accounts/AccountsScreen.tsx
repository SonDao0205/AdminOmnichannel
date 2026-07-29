import {
  CheckCircleOutlined,
  CloseOutlined,
  CopyOutlined,
  EyeOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  ShopOutlined,
  TeamOutlined,
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
  getTenants,
} from '../../apis/adminApi'
import type {
  CreatedTenant,
  CreateTenantPayload,
  TenantListItem,
  TenantStatus,
} from '../../types/admin'
import './accounts.css'

const PAGE_SIZE = 8

const initialForm: CreateTenantPayload = {
  tenantCode: '',
  tenantName: '',
  legalName: '',
  contactEmail: '',
  timezoneName: 'Asia/Ho_Chi_Minh',
  defaultCurrency: 'VND',
  subscriptionPlanCode: 'DEMO',
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
  const [createdTenant, setCreatedTenant] = useState<CreatedTenant | null>(null)
  const [selectedTenant, setSelectedTenant] = useState<TenantListItem | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

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
  }

  const openCreateModal = () => {
    setForm(initialForm)
    setCreatedTenant(null)
    setShowCreate(true)
  }

  const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setCreating(true)
    try {
      const response = await createTenant({
        ...form,
        tenantCode: form.tenantCode.trim().toUpperCase(),
        defaultCurrency: form.defaultCurrency.trim().toUpperCase(),
        subscriptionPlanCode: form.subscriptionPlanCode.trim().toUpperCase(),
      })
      setCreatedTenant(response)
      setLoading(true)
      setReloadKey((current) => current + 1)
      toast.success('Tạo tài khoản tenant thành công')
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setCreating(false)
    }
  }

  const copyTemporaryPassword = async () => {
    if (!createdTenant) return
    await navigator.clipboard.writeText(createdTenant.temporaryPassword)
    toast.success('Đã sao chép mật khẩu tạm thời')
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
                          <button
                            className="tenant-view-button"
                            onClick={() => setSelectedTenant(tenant)}
                            type="button"
                          >
                            <EyeOutlined />
                            Xem
                          </button>
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
                    <dt>Mật khẩu tạm thời</dt>
                    <dd className="temporary-password">
                      <code>{createdTenant.temporaryPassword}</code>
                      <button onClick={copyTemporaryPassword} type="button">
                        <CopyOutlined /> Sao chép
                      </button>
                    </dd>
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
                  Mật khẩu này chỉ hiển thị một lần. Hãy gửi cho người thuê qua kênh an toàn.
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
              <form className="tenant-form" onSubmit={handleCreate}>
                <div className="tenant-form-section">
                  <div className="tenant-form-heading">
                    <strong>Thông tin doanh nghiệp</strong>
                    <span>Thông tin nhận diện tài khoản tenant.</span>
                  </div>
                  <div className="tenant-form-grid">
                    <label>
                      <span>Mã tenant *</span>
                      <input
                        maxLength={50}
                        onChange={(event) => updateForm('tenantCode', event.target.value)}
                        pattern="[A-Za-z][A-Za-z0-9_]{2,49}"
                        placeholder="SHOP_001"
                        required
                        value={form.tenantCode}
                      />
                    </label>
                    <label>
                      <span>Tên tenant *</span>
                      <input
                        maxLength={255}
                        minLength={2}
                        onChange={(event) => updateForm('tenantName', event.target.value)}
                        placeholder="Cửa hàng thời trang ABC"
                        required
                        value={form.tenantName}
                      />
                    </label>
                    <label className="tenant-form-wide">
                      <span>Tên pháp lý</span>
                      <input
                        maxLength={255}
                        onChange={(event) => updateForm('legalName', event.target.value)}
                        placeholder="Công ty TNHH Thời trang ABC"
                        value={form.legalName}
                      />
                    </label>
                    <label>
                      <span>Email liên hệ</span>
                      <input
                        onChange={(event) => updateForm('contactEmail', event.target.value)}
                        placeholder="contact@abc.vn"
                        type="email"
                        value={form.contactEmail}
                      />
                    </label>
                    <label>
                      <span>Múi giờ *</span>
                      <select
                        onChange={(event) => updateForm('timezoneName', event.target.value)}
                        value={form.timezoneName}
                      >
                        <option value="Asia/Ho_Chi_Minh">Asia/Ho_Chi_Minh</option>
                        <option value="Asia/Bangkok">Asia/Bangkok</option>
                        <option value="Asia/Singapore">Asia/Singapore</option>
                      </select>
                    </label>
                  </div>
                </div>

                <div className="tenant-form-section">
                  <div className="tenant-form-heading">
                    <strong>Tài khoản quản lý đầu tiên</strong>
                    <span>Người dùng nhận quyền quản lý tenant.</span>
                  </div>
                  <div className="tenant-form-grid">
                    <label>
                      <span>Họ tên quản lý *</span>
                      <input
                        maxLength={255}
                        minLength={2}
                        onChange={(event) => updateForm('ownerDisplayName', event.target.value)}
                        placeholder="Nguyễn Văn An"
                        required
                        value={form.ownerDisplayName}
                      />
                    </label>
                    <label>
                      <span>Email đăng nhập *</span>
                      <input
                        onChange={(event) => updateForm('ownerEmail', event.target.value)}
                        placeholder="manager@abc.vn"
                        required
                        type="email"
                        value={form.ownerEmail}
                      />
                    </label>
                  </div>
                </div>

                <div className="tenant-form-section">
                  <div className="tenant-form-heading">
                    <strong>Gói và thời hạn</strong>
                    <span>Cấu hình thuê ban đầu của tenant.</span>
                  </div>
                  <div className="tenant-form-grid is-three">
                    <label>
                      <span>Mã gói *</span>
                      <input
                        onChange={(event) => updateForm('subscriptionPlanCode', event.target.value)}
                        required
                        value={form.subscriptionPlanCode}
                      />
                    </label>
                    <label>
                      <span>Số ngày dùng thử *</span>
                      <input
                        max={365}
                        min={1}
                        onChange={(event) => updateForm('trialDays', Number(event.target.value))}
                        required
                        type="number"
                        value={form.trialDays}
                      />
                    </label>
                    <label>
                      <span>Tiền tệ *</span>
                      <select
                        onChange={(event) => updateForm('defaultCurrency', event.target.value)}
                        value={form.defaultCurrency}
                      >
                        <option value="VND">VND</option>
                        <option value="USD">USD</option>
                        <option value="SGD">SGD</option>
                      </select>
                    </label>
                  </div>
                </div>

                <footer className="tenant-form-actions">
                  <button
                    className="tenant-cancel-button"
                    onClick={() => setShowCreate(false)}
                    type="button"
                  >
                    Hủy
                  </button>
                  <button className="tenant-primary-button" disabled={creating} type="submit">
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
            </div>
          </section>
        </div>
      )}
    </div>
  )
}
