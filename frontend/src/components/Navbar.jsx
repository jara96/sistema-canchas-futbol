import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Navbar() {
  const { user, isAdmin, logout } = useAuth()
  const nav = useNavigate()

  const onLogout = () => { logout(); nav('/login') }

  return (
    <nav className="bg-emerald-700 text-white px-6 py-3 flex items-center justify-between shadow">
      <Link to="/" className="text-xl font-bold">⚽ TuCancha</Link>
      <div className="flex gap-4 items-center">
        <Link to="/" className="hover:underline">Canchas</Link>
        {user && <Link to="/mis-reservas" className="hover:underline">Mis reservas</Link>}
        {isAdmin && <Link to="/admin" className="hover:underline">Admin</Link>}
        {user ? (
          <button onClick={onLogout} className="bg-emerald-900 px-3 py-1 rounded hover:bg-emerald-800">
            Salir
          </button>
        ) : (
          <>
            <Link to="/login" className="hover:underline">Login</Link>
            <Link to="/register" className="bg-white text-emerald-700 px-3 py-1 rounded font-medium">
              Registrarse
            </Link>
          </>
        )}
      </div>
    </nav>
  )
}
