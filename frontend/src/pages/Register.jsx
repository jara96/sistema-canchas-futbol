import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Register() {
  const { register } = useAuth()
  const nav = useNavigate()
  const [form, setForm] = useState({ nombre: '', email: '', password: '' })
  const [err, setErr] = useState(null)

  const submit = async (e) => {
    e.preventDefault()
    setErr(null)
    try { await register(form.nombre, form.email, form.password); nav('/') }
    catch (e) { setErr(e.response?.data?.message || 'Error') }
  }

  return (
    <div className="max-w-md mx-auto mt-10 bg-white p-8 rounded-xl shadow">
      <h2 className="text-2xl font-bold mb-6">Crear cuenta</h2>
      <form onSubmit={submit} className="space-y-4">
        <input placeholder="Nombre" required value={form.nombre}
          onChange={(e) => setForm({ ...form, nombre: e.target.value })}
          className="w-full border rounded px-3 py-2" />
        <input type="email" placeholder="Email" required value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
          className="w-full border rounded px-3 py-2" />
        <input type="password" placeholder="Contraseña (mín 6)" minLength={6} required value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
          className="w-full border rounded px-3 py-2" />
        {err && <p className="text-red-600 text-sm">{err}</p>}
        <button className="w-full bg-emerald-700 text-white py-2 rounded hover:bg-emerald-800">
          Registrarme
        </button>
      </form>
      <p className="mt-4 text-center text-sm">
        ¿Ya tenés cuenta? <Link to="/login" className="text-emerald-700 font-medium">Iniciar sesión</Link>
      </p>
    </div>
  )
}
