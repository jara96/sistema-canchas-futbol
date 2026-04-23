import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/api'

export default function Canchas() {
  const [canchas, setCanchas] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/canchas/publicas')
      .then(({ data }) => setCanchas(data))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p className="text-center mt-10">Cargando...</p>

  return (
    <div className="max-w-5xl mx-auto p-6">
      <h1 className="text-3xl font-bold mb-6">Nuestras canchas</h1>
      <div className="grid md:grid-cols-3 gap-4">
        {canchas.map((c) => (
          <div key={c.id} className="bg-white rounded-xl p-5 shadow hover:shadow-lg transition">
            <h3 className="text-xl font-bold">{c.nombre}</h3>
            <p className="text-slate-600">Tipo: {c.tipo}</p>
            <p className="text-emerald-700 font-bold text-lg my-2">${c.precioHora}/h</p>
            <Link to={`/reservar/${c.id}`}
              className="block text-center bg-emerald-700 text-white py-2 rounded hover:bg-emerald-800">
              Reservar
            </Link>
          </div>
        ))}
        {canchas.length === 0 && <p className="text-slate-500">No hay canchas disponibles</p>}
      </div>
    </div>
  )
}
