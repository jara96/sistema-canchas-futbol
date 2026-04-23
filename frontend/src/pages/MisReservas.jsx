import { useEffect, useState } from 'react'
import { api } from '../api/api'

const colorEstado = {
  PENDIENTE: 'bg-yellow-100 text-yellow-800',
  CONFIRMADA: 'bg-emerald-100 text-emerald-800',
  CANCELADA: 'bg-red-100 text-red-700'
}

const fmt = (n) => Number(n ?? 0).toLocaleString('es-AR', { minimumFractionDigits: 2 })

export default function MisReservas() {
  const [reservas, setReservas] = useState([])

  const cargar = () => api.get('/api/reservas/mis').then(({ data }) => setReservas(data))
  useEffect(() => { cargar() }, [])

  const cancelar = async (id) => {
    if (!confirm('¿Cancelar esta reserva?')) return
    await api.post(`/api/reservas/${id}/cancelar`)
    cargar()
  }

  const pagar = async (id) => {
    const { data } = await api.post(`/api/pagos/reservas/${id}/preferencia`)
    window.location.href = data.initPoint || data.sandboxInitPoint
  }

  return (
    <div className="max-w-4xl mx-auto p-6">
      <h1 className="text-2xl font-bold mb-6">Mis reservas</h1>
      <div className="space-y-3">
        {reservas.map((r) => {
          const saldo = Number(r.total ?? 0) - Number(r.senia ?? 0)
          const saldoPago = Boolean(r.saldoPagado)
          return (
            <div key={r.id} className="bg-white rounded p-4 shadow">
              <div className="flex flex-wrap items-center gap-4 justify-between">
                <div>
                  <p className="font-bold">{r.canchaNombre}</p>
                  <p className="text-sm text-slate-600">
                    {r.fecha} · {r.horaInicio?.slice(0, 5)} - {r.horaFin?.slice(0, 5)}
                  </p>
                  <p className="text-sm">
                    Total: <b>${fmt(r.total)}</b> · Seña: <b>${fmt(r.senia)}</b>
                  </p>
                </div>
                <span className={`px-3 py-1 rounded-full text-xs font-bold ${colorEstado[r.estado]}`}>
                  {r.estado}
                </span>
                <div className="flex gap-2">
                  {r.estado === 'PENDIENTE' && (
                    <button onClick={() => pagar(r.id)}
                      className="bg-emerald-700 text-white px-3 py-1 rounded hover:bg-emerald-800">
                      Pagar
                    </button>
                  )}
                  {r.estado !== 'CANCELADA' && (
                    <button onClick={() => cancelar(r.id)}
                      className="border border-red-400 text-red-600 px-3 py-1 rounded hover:bg-red-50">
                      Cancelar
                    </button>
                  )}
                </div>
              </div>

              {r.estado === 'CONFIRMADA' && r.codigoRetiro && (
                <div className="mt-3 border-t pt-3 bg-emerald-50 -mx-4 -mb-4 px-4 pb-4 rounded-b">
                  <div className="flex flex-wrap items-center gap-3 justify-between">
                    <div>
                      <p className="text-xs text-slate-600 uppercase font-bold">
                        Tu código de retiro
                      </p>
                      <p className="text-3xl font-mono font-bold tracking-widest text-emerald-800">
                        {r.codigoRetiro}
                      </p>
                      <p className="text-xs text-slate-600 mt-1">
                        Mostralo al encargado de la cancha al llegar.
                      </p>
                    </div>
                    <div className="text-right">
                      {saldoPago ? (
                        <span className="inline-block px-3 py-2 bg-emerald-700 text-white rounded font-bold">
                          Saldo pagado ✓
                        </span>
                      ) : (
                        <>
                          <p className="text-xs text-slate-600">Saldo a pagar en la cancha</p>
                          <p className="text-xl font-bold text-emerald-800">${fmt(saldo)}</p>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              )}
            </div>
          )
        })}
        {reservas.length === 0 && <p className="text-slate-500">No tenés reservas todavía.</p>}
      </div>
    </div>
  )
}
