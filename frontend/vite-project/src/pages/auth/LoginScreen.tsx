import {
  EyeInvisibleOutlined,
  EyeOutlined,
  LockOutlined,
  MailOutlined,
  ShopOutlined,
} from '@ant-design/icons'
import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { getApiErrorMessage } from '../../apis/adminApi'
import { useAuth } from '../../contexts/auth'
import { ROUTES } from '../../routes/paths'
import './login.css'

export default function LoginScreen() {
  const navigate = useNavigate()
  const { admin, loading: sessionLoading, login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!sessionLoading && admin) {
      navigate(ROUTES.account, { replace: true })
    }
  }, [admin, navigate, sessionLoading])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login(email.trim(), password)
      navigate(ROUTES.account, { replace: true })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-card" aria-labelledby="login-title">
        <div className="login-brand" aria-hidden="true">
          <span><ShopOutlined /></span>
        </div>
        <p className="login-eyebrow">OmnichannelPOS</p>
        <h1 id="login-title">Đăng nhập quản trị</h1>
        <p className="login-subtitle">
          Truy cập khu vực quản lý tài khoản thuê của hệ thống.
        </p>

        <form className="login-form" onSubmit={handleSubmit}>
          <label className="login-field">
            <span>Email quản trị</span>
            <span className="login-input">
              <MailOutlined aria-hidden="true" />
              <input
                autoComplete="username"
                autoFocus
                onChange={(event) => setEmail(event.target.value)}
                placeholder="admin@omnichannel.vn"
                required
                type="email"
                value={email}
              />
            </span>
          </label>

          <label className="login-field">
            <span>Mật khẩu</span>
            <span className="login-input">
              <LockOutlined aria-hidden="true" />
              <input
                autoComplete="current-password"
                minLength={12}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Nhập mật khẩu của bạn"
                required
                type={showPassword ? 'text' : 'password'}
                value={password}
              />
              <button
                aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                className="password-toggle"
                onClick={() => setShowPassword((current) => !current)}
                type="button"
              >
                {showPassword ? <EyeInvisibleOutlined /> : <EyeOutlined />}
              </button>
            </span>
          </label>

          {error && <p className="login-error" role="alert">{error}</p>}

          <button className="login-submit" disabled={submitting} type="submit">
            {submitting ? <span className="button-spinner" /> : null}
            {submitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
          </button>
        </form>

        <p className="login-security-note">
          Khu vực dành riêng cho quản trị viên nền tảng
        </p>
      </section>
    </main>
  )
}
