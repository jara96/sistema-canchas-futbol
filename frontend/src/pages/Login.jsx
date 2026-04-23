import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Login() {
  const { login, loginWithGoogle } = useAuth()
  const nav = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [err, setErr] = useState(null)
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setErr(null); setLoading(true)
    try { await login(email, password); nav('/') }
    catch (e) { setErr(e.response?.data?.message || 'Error de login') }
    finally { setLoading(false) }
  }

  return (
    <div className="max-w-md mx-auto mt-10 bg-white p-8 rounded-xl shadow">
      <h2 className="text-2xl font-bold mb-6">Iniciar sesión</h2>
      <form onSubmit={submit} className="space-y-4">
        <input type="email" placeholder="Email" required value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="w-full border rounded px-3 py-2" />
        <input type="password" placeholder="Contraseña" required value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="w-full border rounded px-3 py-2" />
        {err && <p className="text-red-600 text-sm">{err}</p>}
        <button disabled={loading}
          className="w-full bg-emerald-700 text-white py-2 rounded hover:bg-emerald-800 disabled:opacity-50">
          {loading ? 'Entrando...' : 'Entrar'}
        </button>
      </form>
      <div className="my-4 text-center text-slate-400">o</div>
      <button onClick={loginWithGoogle}
        className="w-full border border-slate-300 py-2 rounded hover:bg-slate-50 flex items-center justify-center gap-3 font-medium text-slate-700">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 48 48" aria-hidden="true">
          <path fill="#FFC107" d="M43.611 20.083H42V20H24v8h11.303c-1.649 4.657-6.08 8-11.303 8-6.627 0-12-5.373-12-12s5.373-12 12-12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 12.955 4 4 12.955 4 24s8.955 20 20 20 20-8.955 20-20c0-1.341-.138-2.65-.389-3.917z"/>
          <path fill="#FF3D00" d="m6.306 14.691 6.571 4.819C14.655 15.108 18.961 12 24 12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 16.318 4 9.656 8.337 6.306 14.691z"/>
          <path fill="#4CAF50" d="M24 44c5.166 0 9.86-1.977 13.409-5.192l-6.19-5.238A11.91 11.91 0 0 1 24 36c-5.202 0-9.619-3.317-11.283-7.946l-6.522 5.025C9.505 39.556 16.227 44 24 44z"/>
          <path fill="#1976D2" d="M43.611 20.083H42V20H24v8h11.303a12.04 12.04 0 0 1-4.087 5.571l.003-.002 6.19 5.238C36.971 39.205 44 34 44 24c0-1.341-.138-2.65-.389-3.917z"/>
        </svg>
        Continuar con Google
      </button>
      <p className="mt-4 text-center text-sm">
        ¿No tenés cuenta? <Link to="/register" className="text-emerald-700 font-medium">Registrate</Link>
      </p>
    </div>
  )
}
