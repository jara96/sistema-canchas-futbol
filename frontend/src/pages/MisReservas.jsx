import { useEffect, useState } from 'react'
import { api } from '../api/api'

const colorEstado = {
  PENDIENTE: 'bg-yellow-100 text-yellow-800',
  CONFIRMADA: 'bg-emerald-100 text-emerald-800',
  FINALIZADA: 'bg-slate-200 text-slate-700',
  CANCELADA: 'bg-red-100 text-red-700'
}

const colorEstadoTorneo = {
  PENDIENTE: 'bg-yellow-100 text-yellow-800',
  CONFIRMADO: 'bg-emerald-100 text-emerald-800',
  EXPIRADO: 'bg-red-100 text-red-700',
  CANCELADO: 'bg-red-100 text-red-700'
}

const fmt = (n) => Number(n ?? 0).toLocaleString('es-AR', { minimumFractionDigits: 2 })

export default function MisReservas() {
  const [reservas, setReservas] = useState([])
  const [torneos, setTorneos] = useState([])

  const cargar = () => {
    api.get('/api/reservas/mis').then(({ data }) => setReservas(data))
    api.get('/api/torneos/mios').then(({ data }) => setTorneos(data)).catch(() => setTorneos([]))
  }
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

  const pagarTorneo = async (id) => {
    const { data } = await api.post(`/api/pagos/torneos/${id}/preferencia`)
    window.location.href = data.initPoint || data.sandboxInitPoint
  }

  // Reservas individuales: las que no pertenecen a un torneo
  const reservasIndividuales = reservas.filter((r) => !r.torneoId)

  return (
    <div className="max-w-4xl mx-auto p-6">
      <h1 className="text-2xl font-bold mb-6">Mis reservas</h1>

      {/* ===== TORNEOS ===== */}
      {torneos.length > 0 && (
        <div className="mb-8">
          <h2 className="text-lg font-bold mb-3">🏆 Mis torneos</h2>
          <div className="space-y-4">
            {torneos.map((t) => {
              const saldoTotal = Number(t.total ?? 0) - Number(t.totalSenia ?? 0)
              return (
                <div key={t.id} className="bg-white rounded shadow p-4 border-l-4 border-amber-500">
                  <div className="flex flex-wrap items-center justify-between gap-3 mb-2">
                    <div>
                      <p className="font-bold">🏆 Torneo #{t.id} · {t.canchaNombre}</p>
                      <p className="text-xs text-slate-500">{t.modalidad?.replace(/_/g, ' ')}</p>
                      <p className="text-sm">
                        {t.cantidadTurnos} turnos · Total ${fmt(t.total)} · Seña ${fmt(t.totalSenia)}
                      </p>
                    </div>
                    <span className={`px-3 py-1 rounded-full text-xs font-bold ${colorEstadoTorneo[t.estado] || 'bg-slate-100'}`}>
                      {t.estado}
                    </span>
                    {t.estado === 'PENDIENTE' && (
                      <button onClick={() => pagarTorneo(t.id)}
                        className="bg-emerald-700 text-white px-3 py-1 rounded hover:bg-emerald-800">
                        Pagar seña
                      </button>
                    )}
                  </div>

                  {t.estado === 'CONFIRMADO' && (
                    <div className="bg-emerald-50 border rounded p-3 mt-2">
                      <p className="text-xs font-bold text-emerald-800 mb-2">
                        TURNOS CONFIRMADOS · Saldo total a pagar en cancha: ${fmt(saldoTotal)}
                      </p>
                      <ul className="text-sm divide-y">
                        {(t.reservas || []).map((r) => {
                          const saldo = Number(r.total ?? 0) - Number(r.senia ?? 0)
                          return (
                            <li key={r.id} className="py-2 flex flex-wrap justify-between items-center gap-2">
                              <span>
                                📅 {r.fecha} · {r.horaInicio?.slice(0, 5)}-{r.horaFin?.slice(0, 5)}
                              </span>
                              <span className="font-mono font-bold tracking-wider text-emerald-800">
                                {r.codigoRetiro || '—'}
                              </span>
                              <span>
                                {r.saldoPagado
                                  ? <span className="text-emerald-700 font-bold">pagado ✓</span>
                                  : <span>${fmt(saldo)}</span>}
                              </span>
                            </li>
                          )
                        })}
                      </ul>
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* ===== RESERVAS INDIVIDUALES ===== */}
      <h2 className="text-lg font-bold mb-3">Reservas individuales</h2>
      <div className="space-y-3">
        {reservasIndividuales.map((r) => {
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
                  {r.estado !== 'CANCELADA' && r.estado !== 'FINALIZADA' && (
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
        {reservasIndividuales.length === 0 && torneos.length === 0 && (
          <p className="text-slate-500">No tenés reservas todavía.</p>
        )}
        {reservasIndividuales.length === 0 && torneos.length > 0 && (
          <p className="text-slate-500 text-sm">Sin reservas individuales.</p>
        )}
      </div>
    </div>
  )
}
