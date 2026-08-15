import {
  DownOutlined,
  LogoutOutlined,
  ShopOutlined,
  ShoppingCartOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import { type ReactNode, useEffect, useRef, useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import Swal from 'sweetalert2'
import { getApiErrorMessage } from '../../apis/adminApi'
import { useAuth } from '../../contexts/auth'
import { ROUTES } from '../../routes/paths'

type NavigationItem = {
  label: string
  to: string
  icon: ReactNode
}

const navigationItems: NavigationItem[] = [
  {
    label: 'Tài khoản tenant',
    to: ROUTES.account,
    icon: <TeamOutlined />,
  },
  {
    label: 'Gói dịch vụ',
    to: ROUTES.plan,
    icon: <ShoppingCartOutlined />,
  },
]

export default function Sidebar() {
  const navigate = useNavigate()
  const { admin, logout } = useAuth()
  const [showDropdown, setShowDropdown] = useState(false)
  const [loggingOut, setLoggingOut] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setShowDropdown(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [])

  const handleLogout = async () => {
    setShowDropdown(false)
    const result = await Swal.fire({
      title: 'Xác nhận đăng xuất',
      text: 'Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc2626',
      cancelButtonColor: '#667085',
      confirmButtonText: 'Đăng xuất',
      cancelButtonText: 'Hủy',
      background: '#fff',
      color: '#101828',
      iconColor: '#dc2626',
    })

    if (!result.isConfirmed) return

    setLoggingOut(true)
    try {
      await logout()
      navigate(ROUTES.login, { replace: true })
    } catch (error) {
      await Swal.fire({
        title: 'Chưa thể đăng xuất',
        text: getApiErrorMessage(error),
        icon: 'error',
        confirmButtonColor: '#2563eb',
      })
    } finally {
      setLoggingOut(false)
    }
  }

  const initials = admin?.displayName
    .split(/\s+/)
    .filter(Boolean)
    .slice(-2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('') || 'AD'

  return (
    <aside className="app-sidebar">
      <div className="app-brand">
        <NavLink className="app-logo" to={ROUTES.account}>
          <span className="app-logo-mark" aria-hidden="true">
            <ShopOutlined />
          </span>
          <span className="app-logo-copy">
            <strong>SMARTHUB</strong>
            <small>MANAGEMENT SUITE</small>
          </span>
        </NavLink>
      </div>

      <nav className="app-navigation" aria-label="Điều hướng chính">
        {navigationItems.map((item) => (
          <NavLink
            className={({ isActive }) =>
              isActive ? 'app-nav-link is-active' : 'app-nav-link'
            }
            key={item.to}
            to={item.to}
          >
            <span className="app-nav-icon" aria-hidden="true">
              {item.icon}
            </span>
            <span className="app-nav-label">{item.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="app-profile-container" ref={dropdownRef}>
        {showDropdown && (
          <div className="app-profile-dropdown">
            <button
              className="app-profile-dropdown-item logout"
              disabled={loggingOut}
              onClick={handleLogout}
              type="button"
            >
              <LogoutOutlined />
              <span>{loggingOut ? 'Đang đăng xuất...' : 'Đăng xuất'}</span>
            </button>
          </div>
        )}
        <button
          className={`app-profile ${showDropdown ? 'is-active' : ''}`}
          onClick={() => setShowDropdown(!showDropdown)}
          type="button"
        >
          <span className="app-profile-avatar">{initials}</span>
          <span className="app-profile-copy">
            <strong>{admin?.displayName ?? 'Quản trị viên'}</strong>
            <small>{admin?.email ?? 'SmartHub'}</small>
          </span>
          <DownOutlined className={`app-profile-arrow ${showDropdown ? 'rotate-180' : ''}`} />
        </button>
      </div>
    </aside>
  )
}
