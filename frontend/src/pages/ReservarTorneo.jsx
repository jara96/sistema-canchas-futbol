import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { api } from '../api/api'

const MODALIDADES = [
  { id: 'DIA_ENTERO', label: 'Día entero', desc: 'Reservar todos los turnos disponibles de un solo día.' },
  { id: 'VARIOS_DIAS', label: 'Varios días completos', desc: 'Reservar todos los turnos en varias fechas elegidas.' },
  { id: 'HORARIOS_VARIOS_DIAS', label: 'Ciertos horarios en varios días', desc: 'Elegís días sueltos y los turnos específicos a reservar en cada uno.' },
  { id: 'RECURRENTE_SEMANAL', label: 'Día de la semana, varias semanas', desc: 'Ej: todos los martes a las 18hs durante 1 mes.' }
]

const DIAS_SEMANA = [
  { id: 'MONDAY', label: 'Lunes' },
  { id: 'TUESDAY', label: 'Martes' },
  { id: 'WEDNESDAY', label: 'Miércoles' },
  { id: 'THURSDAY', label: 'Jueves' },
  { id: 'FRIDAY', label: 'Viernes' },
  { id: 'SATURDAY', label: 'Sábado' },
  { id: 'SUNDAY', label: 'Domingo' }
]

export default function ReservarTorneo() {
  const { canchaId } = useParams()
  const nav = useNavigate()
  const [cancha, setCancha] = useState(null)
  const [turnos, setTurnos] = useState([])
  const [config, setConfig] = useState({ diasMaximoReserva: 30, diasMaximoTorneo: 90 })
  const [modalidad, setModalidad] = useState('DIA_ENTERO')
  const [acepta, setAcepta] = useState(false)
  const [msg, setMsg] = useState(null)
  const [loading, setLoading] = useState(false)

  // Estados por modalidad
  const [fecha, setFecha] = useState('')
  const [fechas, setFechas] = useState([]) // string[]
  const [nuevaFecha, setNuevaFecha] = useState('')
  const [turnoIds, setTurnoIds] = useState([]) // number[]
  const [fechaInicio, setFechaInicio] = useState('')
  const [fechaFin, setFechaFin] = useState('')
  const [diaSemana, setDiaSemana] = useState('TUESDAY')

  useEffect(() => {
    api.get(`/api/canchas/${canchaId}`).then(({ data }) => setCancha(data))
    api.get('/api/turnos/publicos').then(({ data }) => setTurnos(data))
    api.get('/api/config/publico').then(({ data }) => setConfig(data)).catch(() => {})
  }, [canchaId])

  const addDays = (n) => {
    const d = new Date(); d.setDate(d.getDate() + n)
    return d.toISOString().slice(0, 10)
  }
  const minTorneo = addDays(config.diasMaximoReserva + 1)
  const maxTorneo = addDays(config.diasMaximoReserva + config.diasMaximoTorneo)

  const toggleTurno = (id) => {
    setTurnoIds((prev) => prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id])
  }
  const agregarFecha = () => {
    if (nuevaFecha && !fechas.includes(nuevaFecha)) {
      setFechas([...fechas, nuevaFecha].sort())
      setNuevaFecha('')
    }
  }
  const quitarFecha = (f) => setFechas(fechas.filter((x) => x !== f))

  // Estimación previa de turnos antes de crear (cliente)
  const slotsEstimados = (() => {
    if (modalidad === 'DIA_ENTERO') return fecha ? turnos.length : 0
    if (modalidad === 'VARIOS_DIAS') return fechas.length * turnos.length
    if (modalidad === 'HORARIOS_VARIOS_DIAS') return fechas.length * turnoIds.length
    if (modalidad === 'RECURRENTE_SEMANAL') {
      if (!fechaInicio || !fechaFin || turnoIds.length === 0) return 0
      let count = 0
      const ini = new Date(fechaInicio)
      const fin = new Date(fechaFin)
      const dow = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']
      const target = dow.indexOf(diaSemana)
      const cur = new Date(ini)
      while (cur.getDay() !== target && cur <= fin) cur.setDate(cur.getDate() + 1)
      while (cur <= fin) { count++; cur.setDate(cur.getDate() + 7) }
      return count * turnoIds.length
    }
    return 0
  })()

  const totalEstimado = (() => {
    if (!cancha || slotsEstimados === 0) return { total: 0, senia: 0 }
    const turnosUsados = (modalidad === 'DIA_ENTERO' || modalidad === 'VARIOS_DIAS')
      ? turnos
      : turnos.filter((t) => turnoIds.includes(t.id))
    let totalUnTurno = 0
    if (turnosUsados.length > 0) {
      const t = turnosUsados[0]
      const toMin = (h) => { const [hh, mm] = h.split(':').map(Number); return hh * 60 + mm }
      // mejor sumar todos los turnos involucrados (ya que pueden tener distinta duración)
      totalUnTurno = turnosUsados.reduce((acc, tu) => {
        const m = toMin(tu.horaFin) - toMin(tu.horaInicio)
        return acc + Number(cancha.precioHora) * (m / 60)
      }, 0) / turnosUsados.length
    }
    const total = totalUnTurno * slotsEstimados
    const pct = cancha.porcentajeSenia ?? 50
    const senia = total * pct / 100
    return { total, senia }
  })()

  const submit = async () => {
    setMsg(null); setLoading(true)
    try {
      const payload = { canchaId: Number(canchaId), modalidad }
      if (modalidad === 'DIA_ENTERO') payload.fecha = fecha
      if (modalidad === 'VARIOS_DIAS') payload.fechas = fechas
      if (modalidad === 'HORARIOS_VARIOS_DIAS') {
        payload.fechas = fechas
        payload.turnoIds = turnoIds
      }
      if (modalidad === 'RECURRENTE_SEMANAL') {
        payload.fechaInicio = fechaInicio
        payload.fechaFin = fechaFin
        payload.diaSemana = diaSemana
        payload.turnoIds = turnoIds
      }
      const { data: torneo } = await api.post('/api/torneos', payload)
      const { data: pref } = await api.post(`/api/pagos/torneos/${torneo.id}/preferencia`)
      window.location.href = pref.initPoint || pref.sandboxInitPoint
    } catch (e) {
      setMsg(e.response?.data?.message || 'Error al crear el torneo')
    } finally {
      setLoading(false)
    }
  }

  if (!cancha) return <p className="text-center mt-10">Cargando…</p>

  const fmt = (n) => Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

  const puedeEnviar =
    !loading && acepta &&
    (
      (modalidad === 'DIA_ENTERO' && fecha) ||
      (modalidad === 'VARIOS_DIAS' && fechas.length > 0) ||
      (modalidad === 'HORARIOS_VARIOS_DIAS' && fechas.length > 0 && turnoIds.length > 0) ||
      (modalidad === 'RECURRENTE_SEMANAL' && fechaInicio && fechaFin && turnoIds.length > 0)
    )

  return (
    <div className="max-w-3xl mx-auto p-6 mt-6 bg-white rounded-xl shadow">
      <h1 className="text-2xl font-bold mb-1">🏆 Reservar para torneo · {cancha.nombre}</h1>
      <p className="text-slate-600 mb-4">${cancha.precioHora}/h · Tipo {cancha.tipo}</p>

      <div className="bg-amber-50 border border-amber-200 text-amber-800 text-sm p-3 rounded mb-4">
        Las reservas de torneo solo se permiten entre el <b>{minTorneo}</b> y el <b>{maxTorneo}</b>
        ({config.diasMaximoTorneo} días después del límite normal de reserva).
      </div>

      <label className="block text-sm font-medium mb-1">Elegí la modalidad</label>
      <div className="grid md:grid-cols-2 gap-2 mb-4">
        {MODALIDADES.map((m) => (
          <button key={m.id} onClick={() => setModalidad(m.id)}
            className={`text-left p-3 rounded border ${modalidad === m.id ? 'border-emerald-700 bg-emerald-50' : 'bg-white hover:bg-slate-50'}`}>
            <p className="font-bold text-sm">{m.label}</p>
            <p className="text-xs text-slate-600">{m.desc}</p>
          </button>
        ))}
      </div>

      {/* ============ MODALIDAD A ============ */}
      {modalidad === 'DIA_ENTERO' && (
        <div className="mb-4">
          <label className="block text-sm font-medium mb-1">Fecha</label>
          <input type="date" value={fecha} onChange={(e) => setFecha(e.target.value)}
            min={minTorneo} max={maxTorneo}
            className="w-full border rounded px-3 py-2" />
          <p className="text-xs text-slate-500 mt-1">
            Se reservarán todos los {turnos.length} turnos disponibles de ese día.
          </p>
        </div>
      )}

      {/* ============ MODALIDAD B / C - lista de fechas ============ */}
      {(modalidad === 'VARIOS_DIAS' || modalidad === 'HORARIOS_VARIOS_DIAS') && (
        <div className="mb-4">
          <label className="block text-sm font-medium mb-1">Fechas a reservar</label>
          <div className="flex gap-2">
            <input type="date" value={nuevaFecha}
              onChange={(e) => setNuevaFecha(e.target.value)}
              min={minTorneo} max={maxTorneo}
              className="border rounded px-3 py-2 flex-1" />
            <button onClick={agregarFecha} type="button"
              className="bg-emerald-700 text-white px-3 rounded">Agregar</button>
          </div>
          {fechas.length > 0 && (
            <div className="flex flex-wrap gap-2 mt-3">
              {fechas.map((f) => (
                <span key={f} className="bg-slate-100 border rounded px-2 py-1 text-sm">
                  {f} <button onClick={() => quitarFecha(f)} className="text-red-600 ml-1">×</button>
                </span>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ============ MODALIDAD C / D - selector de turnos ============ */}
      {(modalidad === 'HORARIOS_VARIOS_DIAS' || modalidad === 'RECURRENTE_SEMANAL') && (
        <div className="mb-4">
          <label className="block text-sm font-medium mb-1">Turnos a reservar</label>
          <div className="grid grid-cols-3 gap-2">
            {turnos.map((t) => {
              const sel = turnoIds.includes(t.id)
              return (
                <button key={t.id} type="button" onClick={() => toggleTurno(t.id)}
                  className={`py-2 rounded border text-sm
                    ${sel ? 'bg-emerald-700 text-white' : 'bg-white hover:bg-slate-50'}`}>
                  {t.horaInicio?.slice(0, 5)} - {t.horaFin?.slice(0, 5)}
                </button>
              )
            })}
          </div>
        </div>
      )}

      {/* ============ MODALIDAD D - rango fechas + dia semana ============ */}
      {modalidad === 'RECURRENTE_SEMANAL' && (
        <div className="mb-4 grid md:grid-cols-3 gap-2">
          <label>
            <span className="text-sm font-medium">Fecha desde</span>
            <input type="date" value={fechaInicio}
              onChange={(e) => setFechaInicio(e.target.value)}
              min={minTorneo} max={maxTorneo}
              className="w-full border rounded px-3 py-2" />
          </label>
          <label>
            <span className="text-sm font-medium">Fecha hasta</span>
            <input type="date" value={fechaFin}
              onChange={(e) => setFechaFin(e.target.value)}
              min={minTorneo} max={maxTorneo}
              className="w-full border rounded px-3 py-2" />
          </label>
          <label>
            <span className="text-sm font-medium">Día de la semana</span>
            <select value={diaSemana} onChange={(e) => setDiaSemana(e.target.value)}
              className="w-full border rounded px-3 py-2">
              {DIAS_SEMANA.map((d) => <option key={d.id} value={d.id}>{d.label}</option>)}
            </select>
          </label>
        </div>
      )}

      {/* Resumen */}
      <div className="bg-slate-50 border rounded p-3 text-sm mb-3">
        <p>Turnos a reservar: <b>{slotsEstimados}</b></p>
        <p>Total estimado: <b>${fmt(totalEstimado.total)}</b></p>
        <p>Seña a pagar ({cancha.porcentajeSenia ?? 50}%): <b>${fmt(totalEstimado.senia)}</b></p>
        <p className="text-xs text-slate-500">Resto a pagar en cancha: ${fmt(totalEstimado.total - totalEstimado.senia)}</p>
      </div>

      <p className="text-xs text-amber-700 bg-amber-50 border border-amber-200 p-2 rounded mb-3">
        ⏱️ Tenés <b>10 minutos</b> para completar el pago. Si no pagás, el torneo expira y todos los turnos quedan libres.
      </p>

      <div className="text-xs text-red-700 bg-red-50 border border-red-200 p-3 rounded mb-3">
        <p className="font-bold mb-1">⚠️ Política de cancelación</p>
        <p>
          La seña <b>no se devuelve</b>. Si no te presentás a algún turno del torneo,
          perdés la seña de ese turno (los demás siguen vigentes). Vas a recibir un
          email con la lista de códigos individuales para cada turno.
        </p>
      </div>

      <label className="flex items-start gap-2 text-sm mb-3 cursor-pointer select-none">
        <input type="checkbox" checked={acepta}
          onChange={(e) => setAcepta(e.target.checked)} className="mt-1" />
        <span>Entiendo y acepto: la seña no es reembolsable.</span>
      </label>

      {msg && <p className="text-red-600 text-sm mb-2">{msg}</p>}

      <button onClick={submit} disabled={!puedeEnviar}
        className="w-full bg-emerald-700 text-white py-2 rounded hover:bg-emerald-800 disabled:opacity-50 disabled:cursor-not-allowed">
        {loading ? 'Procesando…' : 'Crear torneo y pagar seña'}
      </button>
      <button onClick={() => nav(-1)} className="w-full mt-2 text-slate-500 hover:underline">
        Volver
      </button>
    </div>
  )
}
