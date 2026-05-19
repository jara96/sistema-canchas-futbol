import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { api } from '../api/api'

export default function Reservar() {
  const { canchaId } = useParams()
  const nav = useNavigate()
  const [cancha, setCancha] = useState(null)
  const [turnos, setTurnos] = useState([])
  const [ocupados, setOcupados] = useState([])
  const [diasCerrados, setDiasCerrados] = useState([])
  const [fecha, setFecha] = useState(new Date().toISOString().slice(0, 10))
  const [turnoId, setTurnoId] = useState('')
  const [acepta, setAcepta] = useState(false)
  const [msg, setMsg] = useState(null)
  const [config, setConfig] = useState({ diasMaximoReserva: 30, diasMaximoTorneo: 90 })

  useEffect(() => {
    api.get(`/api/canchas/${canchaId}`).then(({ data }) => setCancha(data))
    api.get('/api/turnos/publicos').then(({ data }) => setTurnos(data))
    api.get('/api/dias-cerrados/publicos').then(({ data }) => setDiasCerrados(data.map(d => d.fecha)))
    api.get('/api/config/publico').then(({ data }) => setConfig(data)).catch(() => {})
  }, [canchaId])

  const addDays = (n) => {
    const d = new Date()
    d.setDate(d.getDate() + n)
    return d.toISOString().slice(0, 10)
  }
  const limiteReservaStr = addDays(config.diasMaximoReserva)
  const fueraDeLimite = fecha > limiteReservaStr
  const cerrado = diasCerrados.includes(fecha)

  useEffect(() => {
    if (!fecha) return
    api.get(`/api/reservas/disponibilidad?canchaId=${canchaId}&fecha=${fecha}`)
      .then(({ data }) => setOcupados(data))
  }, [fecha, canchaId])

  const reservar = async () => {
    setMsg(null)
    try {
      const { data: reserva } = await api.post('/api/reservas', {
        canchaId: Number(canchaId),
        turnoId: Number(turnoId),
        fecha
      })
      const { data: pref } = await api.post(`/api/pagos/reservas/${reserva.id}/preferencia`)
      window.location.href = pref.initPoint || pref.sandboxInitPoint
    } catch (e) {
      setMsg(e.response?.data?.message || 'Error al reservar')
    }
  }

  if (!cancha) return <p className="text-center mt-10">Cargando...</p>

  return (
    <div className="max-w-xl mx-auto p-6 mt-6 bg-white rounded-xl shadow">
      <h1 className="text-2xl font-bold mb-2">Reservar {cancha.nombre}</h1>
      <p className="text-slate-600 mb-4">${cancha.precioHora}/h · Tipo {cancha.tipo}</p>

      <button onClick={() => nav(`/torneo/${canchaId}`)}
        className="w-full mb-4 bg-amber-500 hover:bg-amber-600 text-white py-2 rounded font-medium">
        🏆 ¿Es para un torneo? Reservar varios turnos
      </button>

      <label className="block text-sm font-medium mb-1">Fecha</label>
      <input type="date" value={fecha} onChange={(e) => setFecha(e.target.value)}
        min={new Date().toISOString().slice(0, 10)}
        max={limiteReservaStr}
        className="w-full border rounded px-3 py-2 mb-4" />

      {fueraDeLimite && (
        <div className="bg-amber-50 border border-amber-200 text-amber-800 text-sm p-3 rounded mb-3">
          <p className="font-bold mb-1">📅 Fecha fuera del rango de reservas comunes</p>
          <p className="text-xs">
            Solo se pueden reservar turnos individuales hasta el <b>{limiteReservaStr}</b>.
            Para fechas posteriores podés crear una <b>reserva de torneo</b>.
          </p>
          <button onClick={() => nav(`/torneo/${canchaId}`)}
            className="mt-2 bg-amber-600 text-white px-3 py-1 rounded text-xs hover:bg-amber-700">
            🏆 Reservar para torneo
          </button>
        </div>
      )}

      {cerrado && !fueraDeLimite && (
        <p className="bg-red-50 border border-red-200 text-red-700 text-sm p-2 rounded mb-3">
          🚫 Ese día las canchas están cerradas
        </p>
      )}

      <label className="block text-sm font-medium mb-1">Turno</label>
      <div className="grid grid-cols-3 gap-2 mb-4">
        {turnos.map((t) => {
          const ocup = ocupados.includes(t.id) || cerrado
          const sel = String(turnoId) === String(t.id)
          return (
            <button key={t.id} disabled={ocup}
              onClick={() => setTurnoId(t.id)}
              className={`py-2 rounded border text-sm
                ${ocup ? 'bg-slate-200 text-slate-400 cursor-not-allowed line-through' : ''}
                ${sel ? 'bg-emerald-700 text-white' : 'bg-white hover:bg-slate-50'}`}>
              {t.horaInicio?.slice(0, 5)} - {t.horaFin?.slice(0, 5)}
            </button>
          )
        })}
      </div>

      {msg && <p className="text-red-600 text-sm mb-2">{msg}</p>}

      {(() => {
        const turnoSel = turnos.find((t) => String(t.id) === String(turnoId))
        if (!turnoSel) {
          return (
            <p className="text-sm text-slate-600 mb-2">
              Seña a pagar: <b>{cancha.porcentajeSenia ?? 50}%</b> del total
              <span className="text-slate-400"> · elegí un turno para ver el monto</span>
            </p>
          )
        }
        const toMin = (h) => { const [hh, mm] = h.split(':').map(Number); return hh * 60 + mm }
        const minutos = toMin(turnoSel.horaFin) - toMin(turnoSel.horaInicio)
        const total = Number(cancha.precioHora) * (minutos / 60)
        const pct = cancha.porcentajeSenia ?? 50
        const senia = total * pct / 100
        const fmt = (n) => n.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
        return (
          <div className="text-sm mb-2 bg-slate-50 border rounded p-2">
            <div>Total del turno: <b>${fmt(total)}</b></div>
            <div>Seña a pagar: <b>{pct}%</b> · <b>${fmt(senia)}</b></div>
            <div className="text-xs text-slate-500">Resto a pagar en la cancha: ${fmt(total - senia)}</div>
          </div>
        )
      })()}
      <p className="text-xs text-amber-700 bg-amber-50 border border-amber-200 p-2 rounded mb-3">
        ⏱️ Tenés <b>10 minutos</b> para completar el pago. Si no pagás en ese tiempo, la
        reserva expira automáticamente y el turno queda libre para otros.
      </p>

      <div className="text-xs text-red-700 bg-red-50 border border-red-200 p-3 rounded mb-3">
        <p className="font-bold mb-1">⚠️ Política de cancelación</p>
        <p>
          La seña <b>no se devuelve</b> en caso de cancelación o inasistencia. Al reservar,
          aceptás que si no te presentás a tu turno o lo cancelás, perdés el dinero de la
          seña pagada.
        </p>
      </div>

      <label className="flex items-start gap-2 text-sm mb-3 cursor-pointer select-none">
        <input type="checkbox" checked={acepta}
          onChange={(e) => setAcepta(e.target.checked)}
          className="mt-1" />
        <span>Entiendo y acepto los términos: la seña no es reembolsable.</span>
      </label>

      <button onClick={reservar} disabled={!turnoId || cerrado || !acepta || fueraDeLimite}
        className="w-full bg-emerald-700 text-white py-2 rounded hover:bg-emerald-800 disabled:opacity-50 disabled:cursor-not-allowed">
        Reservar y pagar seña
      </button>
      <button onClick={() => nav(-1)} className="w-full mt-2 text-slate-500 hover:underline">
        Volver
      </button>
    </div>
  )
}
