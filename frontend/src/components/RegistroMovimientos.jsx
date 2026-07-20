import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/client'
import { useToast } from './Toast'
import Modal from './Modal'
import PageHeader from './PageHeader'

const FORM_VACIO = { productoId: '', cantidad: '', documentoRef: '', observacion: '' }

const fmtFecha = f =>
  new Date(f).toLocaleString('es-PE', {
    day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit',
  })

/**
 * Página compartida para Entradas (RF-09) y Salidas (RF-10/RF-11).
 */
export default function RegistroMovimientos({ tipo }) {
  const esEntrada = tipo === 'ENTRADA'
  const toast = useToast()

  const [movimientos, setMovimientos] = useState([])
  const [productos, setProductos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [modalAbierto, setModalAbierto] = useState(false)
  const [form, setFormulario] = useState(FORM_VACIO)
  const [enviando, setEnviando] = useState(false)

  const url = esEntrada ? '/api/entradas' : '/api/salidas'

  const cargar = useCallback(() => {
    setCargando(true)
    Promise.all([api.get(url), api.get('/api/productos/activos')])
      .then(([movs, prods]) => { setMovimientos(movs); setProductos(prods) })
      .catch(() => toast('error', 'No se pudieron cargar los datos.'))
      .finally(() => setCargando(false))
  }, [url, toast])

  useEffect(() => { cargar() }, [cargar])

  const productoSel = productos.find(p => String(p.id) === String(form.productoId))
  // RF-11: En salidas, avisar en vivo si la cantidad supera el stock
  const excedeStock = !esEntrada && productoSel && Number(form.cantidad) > productoSel.stock

  const setForm = c => setFormulario(f => ({ ...f, ...c }))

  const guardar = async e => {
    e.preventDefault()
    setEnviando(true)
    try {
      await api.post(url, form)
      toast('success', `${esEntrada ? 'Entrada' : 'Salida'} registrada correctamente.`)
      setModalAbierto(false)
      setFormulario(FORM_VACIO)
      cargar()
    } catch (err) {
      toast('error', err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <>
      <PageHeader
        titulo={esEntrada ? 'Entradas de Mercadería' : 'Salidas de Mercadería'}
        subtitulo={esEntrada
          ? 'Ingresos al almacén — suman stock'
          : 'Egresos del almacén — restan stock con validación de disponibilidad'}
      >
        <button type="button" className="btn btn-primary" onClick={() => { setFormulario(FORM_VACIO); setModalAbierto(true) }}>
          <i className={`fa-solid ${esEntrada ? 'fa-truck-ramp-box' : 'fa-dolly'} me-1`}></i>
          Registrar {esEntrada ? 'Entrada' : 'Salida'}
        </button>
      </PageHeader>

      <div className="card">
        <div className="table-responsive">
          <table className="table table-hover align-middle">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Producto</th>
                <th className="text-center">Cantidad</th>
                <th className="text-center">Stock Ant. → Result.</th>
                <th>Documento</th>
                <th>Usuario</th>
                <th>Observación</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr><td colSpan={7}><div className="cargando-pagina py-4"><div className="spinner-border spinner-border-sm text-primary"></div></div></td></tr>
              ) : movimientos.length === 0 ? (
                <tr><td colSpan={7}><div className="tabla-vacia">
                  <i className={`fa-solid ${esEntrada ? 'fa-truck-ramp-box' : 'fa-dolly'}`}></i>
                  Aún no hay {esEntrada ? 'entradas' : 'salidas'} registradas.
                </div></td></tr>
              ) : movimientos.map(m => (
                <tr key={m.id}>
                  <td style={{ fontSize: '0.82rem', whiteSpace: 'nowrap' }}>{fmtFecha(m.fecha)}</td>
                  <td>
                    <div className="fw-medium" style={{ fontSize: '0.87rem' }}>{m.producto}</div>
                    <div style={{ fontSize: '0.74rem', color: 'var(--text-muted-color)' }}>{m.sku}</div>
                  </td>
                  <td className="text-center">
                    <span className={`etiqueta etiqueta-pill etiqueta-${esEntrada ? 'success' : 'danger'}`}>
                      {esEntrada ? '+' : '−'}{m.cantidad}
                    </span>
                  </td>
                  <td className="text-center" style={{ fontSize: '0.85rem' }}>
                    <span style={{ color: 'var(--text-muted-color)' }}>{m.stockAnterior}</span>
                    <i className="fa-solid fa-arrow-right mx-2" style={{ fontSize: '0.65rem', opacity: 0.5 }}></i>
                    <span className="fw-semibold">{m.stockResultante}</span>
                  </td>
                  <td style={{ fontSize: '0.83rem' }}>{m.documentoRef || '—'}</td>
                  <td style={{ fontSize: '0.83rem' }}>{m.usuario}</td>
                  <td style={{ fontSize: '0.8rem', color: 'var(--text-muted-color)', maxWidth: 200 }}>
                    {m.observacion || '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <Modal
        abierto={modalAbierto}
        titulo={`Registrar ${esEntrada ? 'Entrada' : 'Salida'}`}
        icono={esEntrada ? 'fa-truck-ramp-box' : 'fa-dolly'}
        onCerrar={() => setModalAbierto(false)}
      >
        <form onSubmit={guardar}>
          <div className="mb-3">
            <label className="form-label">Producto *</label>
            <select className="form-select" value={form.productoId} required autoFocus
                    onChange={e => setForm({ productoId: e.target.value })}>
              <option value="">Seleccione un producto…</option>
              {productos.map(p => (
                <option key={p.id} value={p.id}>{p.sku} — {p.nombre} (stock: {p.stock})</option>
              ))}
            </select>
          </div>
          <div className="mb-3">
            <label className="form-label">Cantidad *</label>
            <input type="number" min="1" className={`form-control ${excedeStock ? 'is-invalid' : ''}`}
                   value={form.cantidad} required
                   onChange={e => setForm({ cantidad: e.target.value })} />
            {excedeStock && (
              <div className="invalid-feedback d-block">
                Stock insuficiente: solo hay {productoSel.stock} unidades disponibles.
              </div>
            )}
          </div>
          <div className="mb-3">
            <label className="form-label">Documento de referencia</label>
            <input type="text" className="form-control" placeholder={esEntrada ? 'Ej. Factura F001-123' : 'Ej. Boleta B001-456'}
                   value={form.documentoRef}
                   onChange={e => setForm({ documentoRef: e.target.value })} />
          </div>
          <div className="mb-3">
            <label className="form-label">Observación</label>
            <textarea className="form-control" rows={2} value={form.observacion}
                      onChange={e => setForm({ observacion: e.target.value })}></textarea>
          </div>
          <div className="d-flex justify-content-end gap-2">
            <button type="button" className="btn btn-light" onClick={() => setModalAbierto(false)}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={enviando || excedeStock}>
              {enviando
                ? <><span className="spinner-border spinner-border-sm me-2"></span>Guardando…</>
                : <><i className="fa-solid fa-floppy-disk me-1"></i> Registrar</>}
            </button>
          </div>
        </form>
      </Modal>
    </>
  )
}
