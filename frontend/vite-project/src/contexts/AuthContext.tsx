import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import {
  getCurrentAdmin,
  loginAdmin,
  logoutAdmin,
} from '../apis/adminApi'
import type { AdminProfile } from '../types/admin'
import { AuthContext, type AuthContextValue } from './auth'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [admin, setAdmin] = useState<AdminProfile | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    try {
      setAdmin(await getCurrentAdmin())
    } catch {
      setAdmin(null)
    }
  }, [])

  useEffect(() => {
    let active = true
    getCurrentAdmin()
      .then((profile) => {
        if (active) setAdmin(profile)
      })
      .catch(() => {
        if (active) setAdmin(null)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    admin,
    loading,
    login: async (email, password) => {
      setAdmin(await loginAdmin(email, password))
    },
    logout: async () => {
      try {
        await logoutAdmin()
      } finally {
        setAdmin(null)
      }
    },
    refresh,
  }), [admin, loading, refresh])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
