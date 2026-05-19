import { useEffect, useMemo, useState } from 'react'
import { Chart } from 'react-google-charts'
import { api } from '../api/api'

const tabs = [
  { id: 'canchas', label: 'Canchas' },
  { id: 'turnos', label: 'Turnos' },
  { id: 'cerrados', label: 'Días cerrados' },
  { id: 'reservas', label: 'Reservas' },
  { id: 'agenda', label: 'Reservas del día' },
  { id: 'stats', label: 'Estadísticas' },
  { id: 'config', label: 'Configuración' }
]

export default function Admin() {
  const [tab, setTab] = useState('canchas')
  return (
    <div className="max-w-5xl mx-auto p-6">
      <h1 className="text-2xl font-bold mb-4">Panel administrador</h1>
      <div className="flex gap-2 mb-4 flex-wrap">
        {tabs.map((t) => (
          <button key={t.id} onClick={() => setTab(t.id)}
            className={`px-4 py-2 rounded ${tab === t.id ? 'bg-emerald-700 text-white' : 'bg-white border'}`}>
            {t.label}
          </button>
        ))}
      </div>
      {tab === 'canchas' && <CanchasAdmin />}
      {tab === 'turnos' && <TurnosAdmin />}
      {tab === 'cerrados' && <DiasCerradosAdmin />}
      {tab === 'reservas' && <ReservasAdmin />}
      {tab === 'agenda' && <AgendaDelDia />}
      {tab === 'stats' && <StatsAdmin />}
      {tab === 'config' && <ConfiguracionAdmin />}
    </div>
  )
}

/* ======================== CONFIGURACIÓN ======================== */
function ConfiguracionAdmin() {
  const [form, setForm] = useState({ diasMaximoReserva: 30, diasMaximoTorneo: 90 })
  const [loaded, setLoaded] = useState(false)
  const [msg, setMsg] = useState(null)
  const [error, setError] = useState(null)

  const cargar = () => {
    api.get('/api/config').then(({ data }) => {
      setForm({
        diasMaximoReserva: data.diasMaximoReserva ?? 30,
        diasMaximoTorneo: data.diasMaximoTorneo ?? 90
      })
      setLoaded(true)
    })
  }
  useEffect(() => { cargar() }, [])

  const guardar = async (e) => {
    e.preventDefault()
    setMsg(null); setError(null)
    try {
      await api.put('/api/config', {
        diasMaximoReserva: Number(form.diasMaximoReserva),
        diasMaximoTorneo: Number(form.diasMaximoTorneo)
      })
      setMsg('Configuración guardada ✓')
    } catch (err) {
      setError(err.response?.data?.message || 'Error guardando')
    }
  }

  if (!loaded) return <p className="text-slate-500">Cargando configuración…</p>

  const r = Number(form.diasMaximoReserva || 0)
  const t = Number(form.diasMaximoTorneo || 0)

  return (
    <div className="max-w-xl">
      <form onSubmit={guardar} className="bg-white p-6 rounded shadow space-y-4">
        <div>
          <h3 className="font-bold text-lg mb-1">Límites de reserva</h3>
          <p className="text-sm text-slate-500">
            Estos valores controlan hasta cuándo los usuarios pueden reservar a futuro.
          </p>
        </div>

        <label className="block">
          <span className="text-sm font-medium">Días máximos para reservas comunes</span>
          <input type="number" min="1" max="365" required
            value={form.diasMaximoReserva}
            onChange={(e) => setForm({ ...form, diasMaximoReserva: e.target.value })}
            className="mt-1 border rounded px-3 py-2 w-full" />
          <span className="text-xs text-slate-500">
            Default: 30. Los usuarios podrán reservar entre hoy y los próximos {r} días.
          </span>
        </label>

        <label className="block">
          <span className="text-sm font-medium">Días máximos adicionales para torneos</span>
          <input type="number" min="1" max="365" required
            value={form.diasMaximoTorneo}
            onChange={(e) => setForm({ ...form, diasMaximoTorneo: e.target.value })}
            className="mt-1 border rounded px-3 py-2 w-full" />
          <span className="text-xs text-slate-500">
            Default: 90. Las reservas de torneo se permiten desde el día {r + 1} al día {r + t}.
          </span>
        </label>

        <div className="bg-slate-50 border rounded p-3 text-sm">
          <p>📅 <b>Reservas comunes:</b> hoy → {r} días</p>
          <p>🏆 <b>Torneos:</b> día {r + 1} → día {r + t}</p>
        </div>

        {msg && <p className="text-emerald-700 text-sm">{msg}</p>}
        {error && <p className="text-red-600 text-sm">{error}</p>}

        <button className="bg-emerald-700 text-white px-4 py-2 rounded hover:bg-emerald-800">
          Guardar configuración
        </button>
      </form>
    </div>
  )
}

/* ======================== CANCHAS ======================== */
function CanchasAdmin() {
  const [items, setItems] = useState([])
  const [verArchivadas, setVerArchivadas] = useState(false)
  const emptyForm = { nombre: '', tipo: 'F5', precioHora: '', porcentajeSenia: 50, activa: true }
  const [form, setForm] = useState(emptyForm)
  const [editId, setEditId] = useState(null)

  const cargar = () => api.get('/api/canchas').then(({ data }) => setItems(data))
  useEffect(() => { cargar() }, [])

  const visibles = useMemo(
    () => items.filter((c) => verArchivadas ? !c.activa : c.activa),
    [items, verArchivadas]
  )
  const archivadasCount = items.filter((c) => !c.activa).length

  const submit = async (e) => {
    e.preventDefault()
    const payload = {
      ...form,
      precioHora: Number(form.precioHora),
      porcentajeSenia: Number(form.porcentajeSenia)
    }
    if (editId) await api.put(`/api/canchas/${editId}`, payload)
    else await api.post('/api/canchas', payload)
    setForm(emptyForm); setEditId(null); cargar()
  }

  const editar = (c) => {
    setEditId(c.id)
    setForm({
      nombre: c.nombre, tipo: c.tipo, precioHora: c.precioHora,
      porcentajeSenia: c.porcentajeSenia ?? 50, activa: c.activa
    })
  }

  const eliminar = async (id) => {
    if (!confirm('¿Eliminar esta cancha?\n(Si tiene reservas, se archivará en vez de borrarse)')) return
    const { data } = await api.delete(`/api/canchas/${id}`)
    if (data?.archived) alert(data.message)
    cargar()
  }

  const toggleActiva = async (c) => {
    await api.put(`/api/canchas/${c.id}`, {
      nombre: c.nombre, tipo: c.tipo, precioHora: c.precioHora,
      porcentajeSenia: c.porcentajeSenia ?? 50, activa: !c.activa
    })
    cargar()
  }

  return (
    <div>
      <form onSubmit={submit} className="bg-white p-4 rounded shadow mb-4 grid md:grid-cols-6 gap-2 items-end">
        <label className="col-span-2">
          <span className="text-xs text-slate-500">Nombre</span>
          <input placeholder="Nombre" required value={form.nombre}
            onChange={(e) => setForm({ ...form, nombre: e.target.value })}
            className="border rounded px-2 py-1 w-full" />
        </label>
        <label>
          <span className="text-xs text-slate-500">Tipo</span>
          <select value={form.tipo} onChange={(e) => setForm({ ...form, tipo: e.target.value })}
            className="border rounded px-2 py-1 w-full">
            <option>F5</option><option>F7</option><option>F11</option>
          </select>
        </label>
        <label>
          <span className="text-xs text-slate-500">Precio/h</span>
          <input type="number" step="0.01" placeholder="Precio/h" required value={form.precioHora}
            onChange={(e) => setForm({ ...form, precioHora: e.target.value })}
            className="border rounded px-2 py-1 w-full" />
        </label>
        <label>
          <span className="text-xs text-slate-500">% seña</span>
          <input type="number" min="1" max="100" required value={form.porcentajeSenia}
            onChange={(e) => setForm({ ...form, porcentajeSenia: e.target.value })}
            className="border rounded px-2 py-1 w-full" />
        </label>
        <button className="bg-emerald-700 text-white rounded py-1">
          {editId ? 'Guardar' : 'Agregar'}
        </button>
        {editId && (
          <button type="button" onClick={() => { setEditId(null); setForm(emptyForm) }}
            className="col-span-6 text-xs text-slate-500 hover:underline text-left">
            Cancelar edición
          </button>
        )}
      </form>
      <div className="flex justify-between items-center mb-2">
        <button onClick={() => setVerArchivadas(!verArchivadas)}
          className="text-sm text-slate-600 hover:underline">
          {verArchivadas ? '← Ver activas' : `Ver archivadas (${archivadasCount})`}
        </button>
        <span className="text-xs text-slate-500">
          {verArchivadas ? 'Canchas archivadas' : 'Canchas activas'}
        </span>
      </div>
      <table className="w-full bg-white rounded shadow">
        <thead className="bg-slate-100"><tr>
          <th className="p-2 text-left">Nombre</th><th>Tipo</th><th>Precio</th><th>% seña</th><th>Activa</th><th></th>
        </tr></thead>
        <tbody>
          {visibles.map((c) => (
            <tr key={c.id} className="border-t">
              <td className="p-2">{c.nombre}</td>
              <td className="text-center">{c.tipo}</td>
              <td className="text-center">${c.precioHora}</td>
              <td className="text-center">{c.porcentajeSenia ?? 50}%</td>
              <td className="text-center">
                <button onClick={() => toggleActiva(c)} title="Cambiar estado"
                  className="text-lg">{c.activa ? '✅' : '❌'}</button>
              </td>
              <td className="text-center space-x-2">
                <button onClick={() => editar(c)} className="text-emerald-700 hover:underline">Editar</button>
                <button onClick={() => eliminar(c.id)} className="text-red-600 hover:underline">Eliminar</button>
              </td>
            </tr>
          ))}
          {visibles.length === 0 && (
            <tr><td colSpan="6" className="p-4 text-center text-slate-500">
              {verArchivadas ? 'No hay canchas archivadas' : 'No hay canchas activas'}
            </td></tr>
          )}
        </tbody>
      </table>
    </div>
  )
}

/* ======================== TURNOS ======================== */
function TurnosAdmin() {
  const [items, setItems] = useState([])
  const [verArchivados, setVerArchivados] = useState(false)
  const [mode, setMode] = useState('bulk')
  const [single, setSingle] = useState({ horaInicio: '18:00', horaFin: '19:00' })
  const [bulk, setBulk] = useState({ horaDesde: '08:00', horaHasta: '23:00', duracionMinutos: 60 })

  const cargar = () => api.get('/api/turnos').then(({ data }) => setItems(data))
  useEffect(() => { cargar() }, [])

  const visibles = useMemo(
    () => items.filter((t) => verArchivados ? !t.activo : t.activo),
    [items, verArchivados]
  )
  const archivadosCount = items.filter((t) => !t.activo).length

  const crearUno = async (e) => {
    e.preventDefault()
    await api.post('/api/turnos', { ...single, activo: true })
    cargar()
  }

  const crearBulk = async (e) => {
    e.preventDefault()
    const { data } = await api.post('/api/turnos/bulk', {
      ...bulk, duracionMinutos: Number(bulk.duracionMinutos)
    })
    alert(`Se crearon ${data.length} turnos nuevos (los existentes se saltaron)`)
    cargar()
  }

  const eliminar = async (id) => {
    if (!confirm('¿Eliminar este turno?\n(Si tiene reservas, se archivará en vez de borrarse)')) return
    const { data } = await api.delete(`/api/turnos/${id}`)
    if (data?.archived) alert(data.message)
    cargar()
  }

  const toggleActivo = async (t) => {
    await api.put(`/api/turnos/${t.id}`, {
      horaInicio: t.horaInicio, horaFin: t.horaFin, activo: !t.activo
    })
    cargar()
  }

  return (
    <div>
      <div className="flex gap-2 mb-3">
        <button onClick={() => setMode('bulk')}
          className={`px-3 py-1 rounded ${mode === 'bulk' ? 'bg-emerald-700 text-white' : 'bg-white border'}`}>
          Generar día completo
        </button>
        <button onClick={() => setMode('single')}
          className={`px-3 py-1 rounded ${mode === 'single' ? 'bg-emerald-700 text-white' : 'bg-white border'}`}>
          Crear uno solo
        </button>
      </div>

      {mode === 'bulk' ? (
        <form onSubmit={crearBulk} className="bg-white p-4 rounded shadow mb-4 grid md:grid-cols-4 gap-2 items-end">
          <label>
            <span className="text-xs text-slate-500">Desde</span>
            <input type="time" value={bulk.horaDesde}
              onChange={(e) => setBulk({ ...bulk, horaDesde: e.target.value })}
              className="border rounded px-2 py-1 w-full" />
          </label>
          <label>
            <span className="text-xs text-slate-500">Hasta</span>
            <input type="time" value={bulk.horaHasta}
              onChange={(e) => setBulk({ ...bulk, horaHasta: e.target.value })}
              className="border rounded px-2 py-1 w-full" />
          </label>
          <label>
            <span className="text-xs text-slate-500">Duración</span>
            <select value={bulk.duracionMinutos}
              onChange={(e) => setBulk({ ...bulk, duracionMinutos: e.target.value })}
              className="border rounded px-2 py-1 w-full">
              <option value="30">30 min</option>
              <option value="45">45 min</option>
              <option value="60">60 min</option>
              <option value="90">90 min</option>
              <option value="120">120 min</option>
            </select>
          </label>
          <button className="bg-emerald-700 text-white rounded py-1">Generar turnos</button>
        </form>
      ) : (
        <form onSubmit={crearUno} className="bg-white p-4 rounded shadow mb-4 grid md:grid-cols-3 gap-2 items-end">
          <label>
            <span className="text-xs text-slate-500">Inicio</span>
            <input type="time" value={single.horaInicio}
              onChange={(e) => setSingle({ ...single, horaInicio: e.target.value })}
              className="border rounded px-2 py-1 w-full" />
          </label>
          <label>
            <span className="text-xs text-slate-500">Fin</span>
            <input type="time" value={single.horaFin}
              onChange={(e) => setSingle({ ...single, horaFin: e.target.value })}
              className="border rounded px-2 py-1 w-full" />
          </label>
          <button className="bg-emerald-700 text-white rounded py-1">Agregar turno</button>
        </form>
      )}

      <div className="flex justify-between items-center mb-2">
        <button onClick={() => setVerArchivados(!verArchivados)}
          className="text-sm text-slate-600 hover:underline">
          {verArchivados ? '← Ver activos' : `Ver archivados (${archivadosCount})`}
        </button>
        <span className="text-xs text-slate-500">
          {verArchivados ? 'Turnos archivados' : 'Turnos activos'}
        </span>
      </div>
      <ul className="bg-white rounded shadow divide-y">
        {visibles.map((t) => (
          <li key={t.id} className="p-3 flex justify-between items-center">
            <span className={t.activo ? '' : 'line-through text-slate-400'}>
              {t.horaInicio?.slice(0, 5)} - {t.horaFin?.slice(0, 5)}
              {!t.activo && <span className="ml-2 text-xs bg-slate-200 px-2 rounded">archivado</span>}
            </span>
            <div className="space-x-2">
              <button onClick={() => toggleActivo(t)}
                className="text-emerald-700 hover:underline">{t.activo ? 'Desactivar' : 'Activar'}</button>
              <button onClick={() => eliminar(t.id)} className="text-red-600 hover:underline">Eliminar</button>
            </div>
          </li>
        ))}
        {visibles.length === 0 && (
          <li className="p-3 text-slate-500 text-center">
            {verArchivados ? 'No hay turnos archivados' : 'No hay turnos activos'}
          </li>
        )}
      </ul>
    </div>
  )
}

/* ======================== DÍAS CERRADOS ======================== */
function DiasCerradosAdmin() {
  const [items, setItems] = useState([])
  const [form, setForm] = useState({ fecha: '', motivo: '' })

  const cargar = () => api.get('/api/dias-cerrados').then(({ data }) => setItems(data))
  useEffect(() => { cargar() }, [])

  const crear = async (e) => {
    e.preventDefault()
    try {
      await api.post('/api/dias-cerrados', form)
      setForm({ fecha: '', motivo: '' })
      cargar()
    } catch (err) {
      alert(err.response?.data?.message || 'Error')
    }
  }

  const eliminar = async (id) => {
    if (!confirm('¿Reabrir este día?')) return
    await api.delete(`/api/dias-cerrados/${id}`); cargar()
  }

  return (
    <div>
      <form onSubmit={crear} className="bg-white p-4 rounded shadow mb-4 grid md:grid-cols-3 gap-2 items-end">
        <label>
          <span className="text-xs text-slate-500">Fecha</span>
          <input type="date" required value={form.fecha}
            onChange={(e) => setForm({ ...form, fecha: e.target.value })}
            className="border rounded px-2 py-1 w-full" />
        </label>
        <label>
          <span className="text-xs text-slate-500">Motivo (opcional)</span>
          <input placeholder="Feriado, mantenimiento..." value={form.motivo}
            onChange={(e) => setForm({ ...form, motivo: e.target.value })}
            className="border rounded px-2 py-1 w-full" />
        </label>
        <button className="bg-emerald-700 text-white rounded py-1">Marcar día cerrado</button>
      </form>
      <ul className="bg-white rounded shadow divide-y">
        {items.sort((a, b) => a.fecha.localeCompare(b.fecha)).map((d) => (
          <li key={d.id} className="p-3 flex justify-between items-center">
            <div>
              <span className="font-medium">{d.fecha}</span>
              {d.motivo && <span className="text-slate-500 text-sm ml-2">· {d.motivo}</span>}
            </div>
            <button onClick={() => eliminar(d.id)} className="text-red-600 hover:underline">Reabrir</button>
          </li>
        ))}
        {items.length === 0 && <li className="p-3 text-slate-500">No hay días cerrados</li>}
      </ul>
    </div>
  )
}

/* ======================== RESERVAS ======================== */
function ReservasAdmin() {
  const [items, setItems] = useState([])
  const [canchas, setCanchas] = useState([])
  const [filtroCancha, setFiltroCancha] = useState('')
  const [filtroEstado, setFiltroEstado] = useState('')
  const [verHistorial, setVerHistorial] = useState(false)
  const [busquedaCodigo, setBusquedaCodigo] = useState('')
  const [qrModal, setQrModal] = useState(null) // { reserva, initPoint, saldo }

  const cargar = () => api.get('/api/reservas').then(({ data }) => setItems(data))
  useEffect(() => {
    cargar()
    api.get('/api/canchas').then(({ data }) => setCanchas(data))
  }, [])

  const confirmar = async (id) => { await api.post(`/api/reservas/${id}/confirmar`); cargar() }
  const cancelar = async (id) => { await api.post(`/api/reservas/${id}/cancelar`); cargar() }

  const cobrarSaldo = async (r) => {
    try {
      const { data } = await api.post(`/api/pagos/reservas/${r.id}/saldo`)
      const saldo = Number(r.total ?? 0) - Number(r.senia ?? 0)
      setQrModal({
        reserva: r,
        initPoint: data.initPoint || data.sandboxInitPoint,
        saldo
      })
    } catch (err) {
      alert(err.response?.data?.message || 'Error generando QR')
    }
  }

  const filtradas = useMemo(() => items.filter((r) => {
    const esHistorial = r.estado === 'CANCELADA' || r.estado === 'FINALIZADA'
    if (!verHistorial && esHistorial) return false
    if (verHistorial && !esHistorial) return false
    if (filtroCancha && String(r.canchaId) !== filtroCancha) return false
    if (filtroEstado && r.estado !== filtroEstado) return false
    if (busquedaCodigo.trim()) {
      const q = busquedaCodigo.trim().toLowerCase()
      const codigo = (r.codigoRetiro || '').toLowerCase()
      const email = (r.usuarioEmail || '').toLowerCase()
      const nombre = (r.usuarioNombre || '').toLowerCase()
      if (!codigo.includes(q) && !email.includes(q) && !nombre.includes(q)) return false
    }
    return true
  }), [items, filtroCancha, filtroEstado, verHistorial, busquedaCodigo])

  const canceladasCount = items.filter((r) => r.estado === 'CANCELADA' || r.estado === 'FINALIZADA').length

  const fmt = (n) => Number(n ?? 0).toLocaleString('es-AR', { minimumFractionDigits: 2 })

  return (
    <div>
      <div className="flex gap-3 mb-4 flex-wrap">
        <label>
          <span className="text-xs text-slate-500 block">Buscar (código / email / nombre)</span>
          <input placeholder="Ej. 123456" value={busquedaCodigo}
            onChange={(e) => setBusquedaCodigo(e.target.value)}
            className="border rounded px-3 py-1" />
        </label>
        <label>
          <span className="text-xs text-slate-500 block">Cancha</span>
          <select value={filtroCancha} onChange={(e) => setFiltroCancha(e.target.value)}
            className="border rounded px-3 py-1">
            <option value="">Todas</option>
            {canchas.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
          </select>
        </label>
        {!verHistorial && (
          <label>
            <span className="text-xs text-slate-500 block">Estado</span>
            <select value={filtroEstado} onChange={(e) => setFiltroEstado(e.target.value)}
              className="border rounded px-3 py-1">
              <option value="">Todos (activos)</option>
              <option>PENDIENTE</option>
              <option>CONFIRMADA</option>
            </select>
          </label>
        )}
        <button onClick={() => { setVerHistorial(!verHistorial); setFiltroEstado('') }}
          className="self-end text-sm text-slate-600 hover:underline">
          {verHistorial ? '← Volver a activas' : `Ver historial (canceladas + finalizadas) (${canceladasCount})`}
        </button>
        <div className="ml-auto self-end text-sm text-slate-500">
          Mostrando {filtradas.length}
        </div>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full bg-white rounded shadow text-sm">
          <thead className="bg-slate-100"><tr>
            <th className="p-2">Cliente</th>
            <th>Cancha</th><th>Fecha</th><th>Turno</th>
            <th>Total</th><th>Seña</th><th>Saldo</th>
            <th>Código</th><th>Estado</th><th></th>
          </tr></thead>
          <tbody>
            {filtradas.map((r) => {
              const saldo = Number(r.total ?? 0) - Number(r.senia ?? 0)
              return (
                <tr key={r.id} className="border-t text-center">
                  <td className="p-2 text-left">
                    <div className="font-medium">{r.usuarioNombre || '—'}</div>
                    <div className="text-xs text-slate-500">{r.usuarioEmail}</div>
                  </td>
                  <td>{r.canchaNombre}</td>
                  <td>{r.fecha}</td>
                  <td>{r.horaInicio?.slice(0, 5)}-{r.horaFin?.slice(0, 5)}</td>
                  <td>${fmt(r.total)}</td>
                  <td>${fmt(r.senia)}</td>
                  <td>
                    {r.saldoPagado
                      ? <span className="text-emerald-700 font-bold">pagado ✓</span>
                      : <span>${fmt(saldo)}</span>}
                  </td>
                  <td>
                    {r.codigoRetiro
                      ? <span className="font-mono font-bold tracking-wider">{r.codigoRetiro}</span>
                      : <span className="text-slate-400">—</span>}
                  </td>
                  <td>{r.estado}</td>
                  <td className="space-x-2 whitespace-nowrap">
                    {r.estado === 'PENDIENTE' && (
                      <button onClick={() => confirmar(r.id)} className="text-emerald-700 hover:underline">Confirmar</button>
                    )}
                    {r.estado === 'CONFIRMADA' && !r.saldoPagado && r.fecha >= new Date().toISOString().slice(0,10) && (
                      <button onClick={() => cobrarSaldo(r)}
                        className="bg-emerald-700 text-white px-2 py-1 rounded hover:bg-emerald-800">
                        Cobrar saldo
                      </button>
                    )}
                    {r.estado !== 'CANCELADA' && r.estado !== 'FINALIZADA' && (
                      <button onClick={() => cancelar(r.id)} className="text-red-600 hover:underline">Cancelar</button>
                    )}
                  </td>
                </tr>
              )
            })}
            {filtradas.length === 0 && (
              <tr><td colSpan="10" className="p-4 text-center text-slate-500">Sin reservas</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {qrModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
             onClick={() => setQrModal(null)}>
          <div className="bg-white rounded-lg p-6 max-w-md w-full"
               onClick={(e) => e.stopPropagation()}>
            <div className="flex justify-between items-start mb-4">
              <div>
                <h3 className="text-lg font-bold">Cobrar saldo</h3>
                <p className="text-sm text-slate-600">
                  {qrModal.reserva.canchaNombre} · {qrModal.reserva.fecha}
                </p>
                <p className="text-sm">
                  Cliente: <b>{qrModal.reserva.usuarioNombre}</b>
                </p>
                <p className="text-2xl font-bold text-emerald-700 mt-1">
                  ${fmt(qrModal.saldo)}
                </p>
              </div>
              <button onClick={() => setQrModal(null)}
                className="text-slate-400 hover:text-slate-700 text-2xl leading-none">×</button>
            </div>
            <div className="flex justify-center bg-white border rounded p-4">
              <img alt="QR de pago"
                src={`https://api.qrserver.com/v1/create-qr-code/?size=280x280&data=${encodeURIComponent(qrModal.initPoint)}`}
                className="w-64 h-64" />
            </div>
            <p className="text-xs text-slate-500 text-center mt-3">
              Escaneá el QR con la app de MercadoPago del cliente para cobrar el saldo.
            </p>
            <a href={qrModal.initPoint} target="_blank" rel="noreferrer"
              className="block text-center text-xs text-emerald-700 hover:underline mt-2 break-all">
              Abrir link de pago
            </a>
            <div className="mt-4 flex gap-2">
              <button onClick={() => { setQrModal(null); cargar() }}
                className="flex-1 bg-emerald-700 text-white rounded py-2 hover:bg-emerald-800">
                Listo, actualizar
              </button>
              <button onClick={() => setQrModal(null)}
                className="flex-1 border rounded py-2 hover:bg-slate-50">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

/* ======================== ESTADÍSTICAS ======================== */
function StatsAdmin() {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const cargar = () => {
    setLoading(true)
    api.get('/api/stats/dashboard')
      .then(({ data }) => { setStats(data); setError(null) })
      .catch((e) => setError(e.response?.data?.message || 'Error cargando estadísticas'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { cargar() }, [])

  if (loading) return <p className="text-slate-500">Cargando estadísticas…</p>
  if (error) return <p className="text-red-600">{error}</p>
  if (!stats) return null

  const fmtPlata = (n) => '$' + Number(n ?? 0).toLocaleString('es-AR', { minimumFractionDigits: 2 })

  // Datos para google-charts
  const pieEstado = [
    ['Estado', 'Cantidad'],
    ['Confirmadas', stats.reservasConfirmadas],
    ['Pendientes', stats.reservasPendientes],
    ['Canceladas', stats.reservasCanceladas]
  ]

  const barCancha = [
    ['Cancha', 'Reservas'],
    ...(stats.reservasPorCancha || []).map((x) => [x.label, x.value])
  ]

  const barIngresosCancha = [
    ['Cancha', 'Ingresos'],
    ...(stats.ingresosPorCancha || []).map((x) => [x.cancha, Number(x.monto)])
  ]

  const lineDia = [
    ['Día', 'Reservas'],
    ...(stats.reservasPorDia || []).map((x) => [x.label.slice(5), x.value])
  ]

  const barTurnos = [
    ['Turno', 'Reservas'],
    ...(stats.turnosPopulares || []).map((x) => [x.label, x.value])
  ]

  return (
    <div className="space-y-6">
      {/* Cards de resumen */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <StatCard label="Reservas totales" value={stats.totalReservas} />
        <StatCard label="Confirmadas" value={stats.reservasConfirmadas} color="emerald" />
        <StatCard label="Canceladas" value={stats.reservasCanceladas} color="red" />
        <StatCard label="Pendientes" value={stats.reservasPendientes} color="yellow" />
        <StatCard label="Ingresos totales" value={fmtPlata(stats.ingresosTotales)} color="emerald" />
        <StatCard label="Por señas" value={fmtPlata(stats.ingresosSenia)} />
        <StatCard label="Por saldos" value={fmtPlata(stats.ingresosSaldo)} />
        <StatCard label="Saldos pendientes" value={stats.saldosPendientes} color="yellow" />
      </div>

      <div className="grid md:grid-cols-2 gap-4">
        <div className="bg-white rounded shadow p-4">
          <h3 className="font-bold mb-2">Reservas por estado</h3>
          {stats.totalReservas > 0 ? (
            <Chart chartType="PieChart" data={pieEstado} width="100%" height="280px"
              options={{
                colors: ['#10b981', '#f59e0b', '#ef4444'],
                pieHole: 0.4, legend: { position: 'bottom' }, chartArea: { width: '90%' }
              }} />
          ) : <Empty />}
        </div>

        <div className="bg-white rounded shadow p-4">
          <h3 className="font-bold mb-2">Reservas por cancha</h3>
          {(stats.reservasPorCancha || []).length > 0 ? (
            <Chart chartType="BarChart" data={barCancha} width="100%" height="280px"
              options={{
                legend: { position: 'none' },
                colors: ['#059669'], chartArea: { width: '70%', height: '80%' },
                hAxis: { minValue: 0 }
              }} />
          ) : <Empty />}
        </div>

        <div className="bg-white rounded shadow p-4">
          <h3 className="font-bold mb-2">Ingresos por cancha (pagos aprobados)</h3>
          {(stats.ingresosPorCancha || []).length > 0 ? (
            <Chart chartType="BarChart" data={barIngresosCancha} width="100%" height="280px"
              options={{
                legend: { position: 'none' },
                colors: ['#0ea5e9'], chartArea: { width: '70%', height: '80%' },
                hAxis: { minValue: 0, format: 'short' }
              }} />
          ) : <Empty />}
        </div>

        <div className="bg-white rounded shadow p-4">
          <h3 className="font-bold mb-2">Horarios más populares</h3>
          {(stats.turnosPopulares || []).length > 0 ? (
            <Chart chartType="ColumnChart" data={barTurnos} width="100%" height="280px"
              options={{
                legend: { position: 'none' },
                colors: ['#8b5cf6'], chartArea: { width: '85%', height: '75%' },
                vAxis: { minValue: 0 }
              }} />
          ) : <Empty />}
        </div>

        <div className="bg-white rounded shadow p-4 md:col-span-2">
          <h3 className="font-bold mb-2">Reservas por día (últimos 30 días)</h3>
          <Chart chartType="LineChart" data={lineDia} width="100%" height="300px"
            options={{
              legend: { position: 'none' },
              colors: ['#059669'], chartArea: { width: '88%', height: '75%' },
              pointSize: 4, vAxis: { minValue: 0, format: '0' }
            }} />
        </div>
      </div>

      <div className="text-right">
        <button onClick={cargar} className="text-sm text-emerald-700 hover:underline">
          ↻ Actualizar
        </button>
      </div>
    </div>
  )
}

function StatCard({ label, value, color }) {
  const colors = {
    emerald: 'text-emerald-700',
    red: 'text-red-600',
    yellow: 'text-yellow-700',
    default: 'text-slate-800'
  }
  const cls = colors[color] || colors.default
  return (
    <div className="bg-white rounded shadow p-3">
      <p className="text-xs text-slate-500 uppercase">{label}</p>
      <p className={`text-2xl font-bold ${cls}`}>{value}</p>
    </div>
  )
}

function Empty() {
  return <p className="text-sm text-slate-400 text-center py-12">Sin datos aún</p>
}

/* ======================== AGENDA / RESERVAS DEL DÍA ======================== */
const ESTADO_CHIP = {
  PENDIENTE:  'bg-yellow-100 text-yellow-800 border-yellow-300',
  CONFIRMADA: 'bg-emerald-100 text-emerald-800 border-emerald-300',
  FINALIZADA: 'bg-slate-200 text-slate-700 border-slate-300',
  CANCELADA:  'bg-red-100 text-red-700 border-red-300'
}

function AgendaDelDia() {
  const hoyStr = new Date().toISOString().slice(0, 10)
  const [fecha, setFecha] = useState(hoyStr)
  const [canchas, setCanchas] = useState([])
  const [turnos, setTurnos] = useState([])
  const [reservas, setReservas] = useState([])
  const [loading, setLoading] = useState(true)
  const [qrModal, setQrModal] = useState(null) // { reserva, initPoint, saldo }

  const cargar = () => {
    setLoading(true)
    Promise.all([
      api.get('/api/canchas'),
      api.get('/api/turnos'),
      api.get('/api/reservas')
    ]).then(([cs, ts, rs]) => {
      setCanchas(cs.data.filter((c) => c.activa))
      setTurnos(ts.data.filter((t) => t.activo).sort((a, b) =>
        a.horaInicio.localeCompare(b.horaInicio)))
      setReservas(rs.data)
    }).finally(() => setLoading(false))
  }
  useEffect(() => { cargar() }, [])

  const cobrarSaldo = async (r) => {
    try {
      const { data } = await api.post(`/api/pagos/reservas/${r.id}/saldo`)
      const saldo = Number(r.total ?? 0) - Number(r.senia ?? 0)
      setQrModal({
        reserva: r,
        initPoint: data.initPoint || data.sandboxInitPoint,
        saldo
      })
    } catch (err) {
      alert(err.response?.data?.message || 'Error generando QR')
    }
  }

  // Buscar la reserva ACTIVA (no CANCELADA) para ese cruce (cancha, turno, fecha)
  const buscarReserva = (canchaId, turnoId) => {
    return reservas.find((r) =>
      r.canchaId === canchaId &&
      r.turnoId === turnoId &&
      r.fecha === fecha &&
      r.estado !== 'CANCELADA'
    )
  }

  const esPasado = fecha < hoyStr
  const fmt = (n) => Number(n ?? 0).toLocaleString('es-AR', { minimumFractionDigits: 2 })

  if (loading) return <p className="text-slate-500">Cargando agenda…</p>

  return (
    <div>
      <div className="flex flex-wrap gap-3 mb-4 items-end">
        <label>
          <span className="text-xs text-slate-500 block">Fecha</span>
          <input type="date" value={fecha}
            onChange={(e) => setFecha(e.target.value)}
            className="border rounded px-3 py-1" />
        </label>
        <button onClick={() => setFecha(hoyStr)}
          className="text-sm text-emerald-700 hover:underline">Hoy</button>
        <button onClick={cargar}
          className="ml-auto text-sm text-slate-600 hover:underline">↻ Actualizar</button>
      </div>

      {esPasado && (
        <p className="bg-slate-100 border border-slate-300 text-slate-700 text-sm p-2 rounded mb-3">
          📅 Fecha pasada · solo lectura. Las reservas confirmadas aparecen como FINALIZADA.
        </p>
      )}

      <div className="overflow-x-auto bg-white rounded shadow">
        <table className="min-w-full text-xs border-collapse">
          <thead>
            <tr className="bg-slate-100">
              <th className="p-2 text-left sticky left-0 bg-slate-100 z-10 border-r"
                  style={{ minWidth: '140px' }}>Cancha</th>
              {turnos.map((t) => (
                <th key={t.id} className="p-2 text-center border-l whitespace-nowrap"
                    style={{ minWidth: '160px' }}>
                  {t.horaInicio?.slice(0, 5)} - {t.horaFin?.slice(0, 5)}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {canchas.map((c) => (
              <tr key={c.id} className="border-t align-top">
                <td className="p-2 font-bold sticky left-0 bg-white z-10 border-r"
                    style={{ minWidth: '140px' }}>
                  <div>{c.nombre}</div>
                  <div className="text-[10px] font-normal text-slate-500">{c.tipo}</div>
                </td>
                {turnos.map((t) => {
                  const r = buscarReserva(c.id, t.id)
                  if (!r) {
                    return (
                      <td key={t.id} className="p-2 text-center border-l text-slate-300">
                        —
                      </td>
                    )
                  }
                  const saldo = Number(r.total ?? 0) - Number(r.senia ?? 0)
                  const puedeCobrarSaldo =
                    !esPasado &&
                    r.estado === 'CONFIRMADA' &&
                    !r.saldoPagado
                  return (
                    <td key={t.id} className="p-2 border-l align-top">
                      <div className="text-[11px] font-bold truncate" title={r.usuarioEmail}>
                        {r.usuarioNombre || r.usuarioEmail}
                      </div>
                      {r.torneoId && (
                        <span className="inline-block text-[10px] bg-amber-100 text-amber-800 rounded px-1 mr-1">
                          🏆 #{r.torneoId}
                        </span>
                      )}
                      <span className={`inline-block text-[10px] border px-1 rounded ${ESTADO_CHIP[r.estado] || ''}`}>
                        {r.estado}
                      </span>
                      {r.codigoRetiro && (
                        <div className="font-mono font-bold tracking-wider text-emerald-800 mt-1">
                          {r.codigoRetiro}
                        </div>
                      )}
                      <div className="text-[10px] text-slate-600 mt-1">
                        {r.saldoPagado
                          ? <span className="text-emerald-700 font-bold">saldo ✓</span>
                          : <span>saldo ${fmt(saldo)}</span>}
                      </div>
                      {puedeCobrarSaldo && (
                        <button onClick={() => cobrarSaldo(r)}
                          className="mt-1 text-[10px] bg-emerald-700 text-white px-2 py-0.5 rounded hover:bg-emerald-800">
                          Cobrar saldo
                        </button>
                      )}
                    </td>
                  )
                })}
              </tr>
            ))}
            {canchas.length === 0 && (
              <tr><td colSpan={turnos.length + 1} className="p-4 text-center text-slate-500">
                No hay canchas activas
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="flex flex-wrap gap-2 mt-3 text-[11px] text-slate-500">
        <span>Estados:</span>
        <span className="border rounded px-1 bg-yellow-100 text-yellow-800">PENDIENTE</span>
        <span className="border rounded px-1 bg-emerald-100 text-emerald-800">CONFIRMADA</span>
        <span className="border rounded px-1 bg-slate-200 text-slate-700">FINALIZADA</span>
        <span className="ml-2">🏆 = parte de un torneo</span>
      </div>

      {qrModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
             onClick={() => setQrModal(null)}>
          <div className="bg-white rounded-lg p-6 max-w-md w-full"
               onClick={(e) => e.stopPropagation()}>
            <div className="flex justify-between items-start mb-4">
              <div>
                <h3 className="text-lg font-bold">Cobrar saldo</h3>
                <p className="text-sm text-slate-600">
                  {qrModal.reserva.canchaNombre} · {qrModal.reserva.fecha}
                </p>
                <p className="text-sm">
                  Cliente: <b>{qrModal.reserva.usuarioNombre}</b>
                </p>
                <p className="text-2xl font-bold text-emerald-700 mt-1">
                  ${fmt(qrModal.saldo)}
                </p>
              </div>
              <button onClick={() => setQrModal(null)}
                className="text-slate-400 hover:text-slate-700 text-2xl leading-none">×</button>
            </div>
            <div className="flex justify-center bg-white border rounded p-4">
              <img alt="QR de pago"
                src={`https://api.qrserver.com/v1/create-qr-code/?size=280x280&data=${encodeURIComponent(qrModal.initPoint)}`}
                className="w-64 h-64" />
            </div>
            <p className="text-xs text-slate-500 text-center mt-3">
              Escaneá el QR con la app de MercadoPago del cliente para cobrar el saldo.
            </p>
            <a href={qrModal.initPoint} target="_blank" rel="noreferrer"
              className="block text-center text-xs text-emerald-700 hover:underline mt-2 break-all">
              Abrir link de pago
            </a>
            <div className="mt-4 flex gap-2">
              <button onClick={() => { setQrModal(null); cargar() }}
                className="flex-1 bg-emerald-700 text-white rounded py-2 hover:bg-emerald-800">
                Listo, actualizar
              </button>
              <button onClick={() => setQrModal(null)}
                className="flex-1 border rounded py-2 hover:bg-slate-50">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
