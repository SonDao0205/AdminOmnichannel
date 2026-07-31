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
import {
  hasValidationErrors,
  normalizeLoginValues,
  validateLogin,
  type FieldErrors,
  type LoginValues,
} from '../../validation/adminValidation'
import './login.css'

export default function LoginScreen() {
  const navigate = useNavigate()
  const { admin, loading: sessionLoading, login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] =
    useState<FieldErrors<LoginValues>>({})

  useEffect(() => {
    if (!sessionLoading && admin) {
      navigate(ROUTES.account, { replace: true })
    }
  }, [admin, navigate, sessionLoading])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')
    const values = normalizeLoginValues({ email, password })
    const validationErrors = validateLogin(values)
    setFieldErrors(validationErrors)
    if (hasValidationErrors(validationErrors)) return

    setSubmitting(true)
    try {
      await login(values.email, values.password)
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

        <form className="login-form" noValidate onSubmit={handleSubmit}>
          <label
            className={`login-field ${fieldErrors.email ? 'is-invalid' : ''}`}
          >
            <span>Email quản trị</span>
            <span className="login-input">
              <MailOutlined aria-hidden="true" />
              <input
                aria-describedby={fieldErrors.email ? 'admin-email-error' : undefined}
                aria-invalid={Boolean(fieldErrors.email)}
                autoComplete="username"
                autoFocus
                onChange={(event) => {
                  setEmail(event.target.value)
                  setFieldErrors((current) => ({
                    ...current,
                    email: undefined,
                  }))
                }}
                placeholder="admin@omnichannel.vn"
                type="email"
                value={email}
              />
            </span>
            {fieldErrors.email && (
              <small className="login-field-error" id="admin-email-error">
                {fieldErrors.email}
              </small>
            )}
          </label>

          <label
            className={`login-field ${fieldErrors.password ? 'is-invalid' : ''}`}
          >
            <span>Mật khẩu</span>
            <span className="login-input">
              <LockOutlined aria-hidden="true" />
              <input
                aria-describedby={
                  fieldErrors.password ? 'admin-password-error' : undefined
                }
                aria-invalid={Boolean(fieldErrors.password)}
                autoComplete="current-password"
                onChange={(event) => {
                  setPassword(event.target.value)
                  setFieldErrors((current) => ({
                    ...current,
                    password: undefined,
                  }))
                }}
                placeholder="Nhập mật khẩu của bạn"
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
            {fieldErrors.password && (
              <small className="login-field-error" id="admin-password-error">
                {fieldErrors.password}
              </small>
            )}
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
