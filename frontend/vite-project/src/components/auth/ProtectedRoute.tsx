import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../../contexts/auth'
import { ROUTES } from '../../routes/paths'

export default function ProtectedRoute({ children }: { children: ReactNode }) {
  const { admin, loading } = useAuth()

  if (loading) {
    return (
      <div className="auth-checking">
        <span className="auth-checking-spinner" />
        <p>Đang kiểm tra phiên đăng nhập...</p>
      </div>
    )
  }

  if (!admin) {
    return <Navigate replace to={ROUTES.login} />
  }

  return children
}
