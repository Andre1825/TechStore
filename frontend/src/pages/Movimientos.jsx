import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/client'
import { useToast } from '../components/Toast'
import PageHeader from '../components/PageHeader'

const fmtFecha = f =>
  new Date(f).toLocaleString('es-PE', {
    day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit',
  })

/**
 * RF-13: Kardex — historial completo de movimientos con filtros por tipo y fechas.
 */
export default function Movimientos() {
  const toast = useToast()
  const [movimientos, setMovimientos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [filtros, setFiltros] = useState({ tipo: '', fechaDesde: '', fechaHasta: '' })

  const cargar = useCallback(() => {
    setCargando(true)
    const params = new URLSearchParams()
    if (filtros.tipo) params.set('tipo', filtros.tipo)
    if (filtros.fechaDesde) params.set('fechaDesde', filtros.fechaDesde)
    if (filtros.fechaHasta) params.set('fechaHasta', filtros.fechaHasta)
    api.get(`/api/movimientos?${params}`)
      .then(setMovimientos)
      .catch(() => toast('error', 'No se pudo cargar el Kardex.'))
      .finally(() => setCargando(false))
  }, [filtros, toast])

  useEffect(() => { cargar() }, [cargar])

  return (
    <>
      <PageHeader titulo="Kardex de Movimientos" subtitulo={`Registro inalterable de entradas y salidas — ${movimientos.length} movimiento(s)`}>
        <a href="/export/movimientos.xlsx" className="btn btn-light">
          <i className="fa-solid fa-file-excel me-1 text-success"></i> Exportar
        </a>
      </PageHeader>

      {/* RF-13: Filtros por tipo y rango de fechas */}
      <div className="card mb-3">
        <div className="card-body py-3">
          <div className="row g-2 align-items-end">
            <div className="col-6 col-md-3">
              <label className="form-label">Tipo</label>
              <select className="form-select" value={filtros.tipo}
                      onChange={e => setFiltros(f => ({ ...f, tipo: e.target.value }))}>
                <option value="">Todos</option>
                <option value="ENTRADA">Entradas</option>
                <option value="SALIDA">Salidas</option>
              </select>
            </div>
            <div className="col-6 col-md-3">
              <label className="form-label">Desde</label>
              <input type="date" className="form-control" value={filtros.fechaDesde}
                     onChange={e => setFiltros(f => ({ ...f, fechaDesde: e.target.value }))} />
            </div>
            <div className="col-6 col-md-3">
              <label className="form-label">Hasta</label>
              <input type="date" className="form-control" value={filtros.fechaHasta}
                     onChange={e => setFiltros(f => ({ ...f, fechaHasta: e.target.value }))} />
            </div>
            <div className="col-6 col-md-1">
              <button type="button" className="btn btn-light w-100" title="Limpiar filtros"
                      onClick={() => setFiltros({ tipo: '', fechaDesde: '', fechaHasta: '' })}>
                <i className="fa-solid fa-eraser"></i>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="table-responsive">
          <table className="table table-hover align-middle">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Producto</th>
                <th className="text-center">Tipo</th>
                <th className="text-center">Cantidad</th>
                <th className="text-center">Stock Ant. → Result.</th>
                <th>Documento</th>
                <th>Usuario</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr><td colSpan={7}><div className="cargando-pagina py-4"><div className="spinner-border spinner-border-sm text-primary"></div></div></td></tr>
              ) : movimientos.length === 0 ? (
                <tr><td colSpan={7}><div className="tabla-vacia"><i className="fa-solid fa-list-check"></i>No hay movimientos con los filtros aplicados.</div></td></tr>
              ) : movimientos.map(m => (
                <tr key={m.id}>
                  <td style={{ fontSize: '0.82rem', whiteSpace: 'nowrap' }}>{fmtFecha(m.fecha)}</td>
                  <td>
                    <div className="fw-medium" style={{ fontSize: '0.87rem' }}>{m.producto}</div>
                    <div style={{ fontSize: '0.74rem', color: 'var(--text-muted-color)' }}>{m.sku}</div>
                  </td>
                  <td className="text-center">
                    <span className={`etiqueta etiqueta-pill etiqueta-${m.tipo === 'ENTRADA' ? 'success' : 'danger'}`}>
                      <i className={`fa-solid ${m.tipo === 'ENTRADA' ? 'fa-arrow-down' : 'fa-arrow-up'}`}></i>
                      {m.tipo}
                    </span>
                  </td>
                  <td className="text-center fw-semibold">{m.cantidad}</td>
                  <td className="text-center" style={{ fontSize: '0.85rem' }}>
                    <span style={{ color: 'var(--text-muted-color)' }}>{m.stockAnterior}</span>
                    <i className="fa-solid fa-arrow-right mx-2" style={{ fontSize: '0.65rem', opacity: 0.5 }}></i>
                    <span className="fw-semibold">{m.stockResultante}</span>
                  </td>
                  <td style={{ fontSize: '0.83rem' }}>{m.documentoRef || '—'}</td>
                  <td style={{ fontSize: '0.83rem' }}>{m.usuario}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}
