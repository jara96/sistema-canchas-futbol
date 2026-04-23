import { createContext, useContext, useEffect, useState } from 'react'
import { api } from '../api/api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('user')
    return raw ? JSON.parse(raw) : null
  })

  const persist = (data) => {
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data))
    setUser(data)
  }

  const login = async (email, password) => {
    const { data } = await api.post('/api/auth/login', { email, password })
    persist(data)
  }

  const register = async (nombre, email, password) => {
    const { data } = await api.post('/api/auth/register', { nombre, email, password })
    persist(data)
  }

  const loginWithGoogle = () => {
    window.location.href = `${api.defaults.baseURL}/oauth2/authorization/google`
  }

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
  }

  const setTokenFromOAuth = async (token) => {
    localStorage.setItem('token', token)
    setUser({ token })
  }

  const isAdmin = !!user?.roles?.includes('ROLE_ADMIN')

  return (
    <AuthContext.Provider value={{ user, isAdmin, login, register, logout, loginWithGoogle, setTokenFromOAuth }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
