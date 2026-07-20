import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/client'
import { useAuth, P } from '../context/AuthContext'
import { useToast } from '../components/Toast'
import Modal, { ConfirmModal } from '../components/Modal'
import PageHeader from '../components/PageHeader'

const soles = n =>
  'S/ ' + Number(n || 0).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const FORM_VACIO = {
  sku: '', nombre: '', descripcion: '', precio: '', stockMinimo: 0, categoriaId: '', marcaId: '',
}

export default function Productos() {
  const { tienePermiso } = useAuth()
  const toast = useToast()
  const puedeEditar = tienePermiso(P.GESTIONAR_PRODUCTOS)

  const [pagina, setPagina] = useState({ contenido: [], paginaActual: 0, totalPaginas: 0, totalElementos: 0 })
  const [categorias, setCategorias] = useState([])
  const [marcas, setMarcas] = useState([])
  const [cargando, setCargando] = useState(true)

  // RF-08: Filtros combinados
  const [filtros, setFiltros] = useState({ sku: '', categoriaId: '', marcaId: '', estado: '' })
  const [page, setPage] = useState(0)

  const [modal, setModal] = useState(null) // { modo: 'crear' | 'editar', form: {...} }
  const [skuExiste, setSkuExiste] = useState(false)
  const [confirmar, setConfirmar] = useState(null) // { tipo: 'estado' | 'eliminar', producto }
  const [historial, setHistorial] = useState(null) // { producto, precios: [] }

  const cargar = useCallback(() => {
    setCargando(true)
    const params = new URLSearchParams({ page })
    if (filtros.sku) params.set('sku', filtros.sku)
    if (filtros.categoriaId) params.set('categoriaId', filtros.categoriaId)
    if (filtros.marcaId) params.set('marcaId', filtros.marcaId)
    if (filtros.estado) params.set('activo', filtros.estado)
    api.get(`/api/productos?${params}`)
      .then(setPagina)
      .catch(() => toast('error', 'No se pudo cargar el catálogo.'))
      .finally(() => setCargando(false))
  }, [page, filtros, toast])

  useEffect(() => { cargar() }, [cargar])

  useEffect(() => {
    api.get('/api/categorias').then(cs => setCategorias(cs.filter(c => c.activa))).catch(() => {})
    api.get('/api/marcas').then(ms => setMarcas(ms.filter(m => m.activa))).catch(() => {})
  }, [])

  // RF-05: Validación de SKU único en vivo
  const validarSku = async sku => {
    if (!sku.trim()) return setSkuExiste(false)
    try {
      const r = await api.get(`/api/productos/validar-sku?sku=${encodeURIComponent(sku)}`)
      setSkuExiste(r.existe)
    } catch { setSkuExiste(false) }
  }

  const abrirCrear = () => { setSkuExiste(false); setModal({ modo: 'crear', form: { ...FORM_VACIO } }) }
  const abrirEditar = p => setModal({
    modo: 'editar',
    form: {
      id: p.id, sku: p.sku, nombre: p.nombre, descripcion: p.descripcion || '',
      precio: p.precio, stockMinimo: p.stockMinimo ?? 0,
      categoriaId: p.categoriaId ?? '', marcaId: p.marcaId ?? '',
    },
  })

  const setForm = cambios => setModal(m => ({ ...m, form: { ...m.form, ...cambios } }))

  const guardar = async e => {
    e.preventDefault()
    const { form, modo } = modal
    try {
      if (modo === 'crear') {
        await api.post('/api/productos', { ...form, marcaId: form.marcaId || null })
        toast('success', 'Producto registrado correctamente.')
      } else {
        await api.put(`/api/productos/${form.id}`, { ...form, marcaId: form.marcaId || null })
        toast('success', 'Producto actualizado correctamente.')
      }
      setModal(null)
      cargar()
    } catch (err) {
      toast('error', err.message)
    }
  }

  const ejecutarConfirmacion = async () => {
    const { tipo, producto } = confirmar
    try {
      if (tipo === 'estado') {
        await api.post(`/api/productos/${producto.id}/estado`)
        toast('success', `Producto ${producto.activo ? 'desactivado' : 'activado'} correctamente.`)
      } else {
        await api.delete(`/api/productos/${producto.id}`)
        toast('success', 'Producto eliminado correctamente.')
      }
      cargar()
    } catch (err) {
      toast('error', err.message)
    } finally {
      setConfirmar(null)
    }
  }

  const verHistorial = async p => {
    try {
      const precios = await api.get(`/api/productos/${p.id}/precios`)
      setHistorial({ producto: p, precios })
    } catch {
      toast('error', 'No se pudo cargar el historial de precios.')
    }
  }

  const limpiarFiltros = () => { setFiltros({ sku: '', categoriaId: '', marcaId: '', estado: '' }); setPage(0) }

  return (
    <>
      <PageHeader titulo="Productos" subtitulo={`Catálogo de productos — ${pagina.totalElementos} registro(s)`}>
        {puedeEditar && (
          <>
            <a href="/export/productos.xlsx" className="btn btn-light">
              <i className="fa-solid fa-file-excel me-1 text-success"></i> Exportar
            </a>
            <button type="button" className="btn btn-primary" onClick={abrirCrear}>
              <i className="fa-solid fa-plus me-1"></i> Nuevo Producto
            </button>
          </>
        )}
      </PageHeader>

      {/* RF-08: Filtros */}
      <div className="card mb-3">
        <div className="card-body py-3">
          <div className="row g-2 align-items-end">
            <div className="col-12 col-md-3">
              <label className="form-label">Buscar SKU / Nombre</label>
              <input
                type="text" className="form-control" placeholder="Ej. TEC-001"
                value={filtros.sku}
                onChange={e => { setFiltros(f => ({ ...f, sku: e.target.value })); setPage(0) }}
              />
            </div>
            <div className="col-6 col-md-3">
              <label className="form-label">Categoría</label>
              <select className="form-select" value={filtros.categoriaId}
                      onChange={e => { setFiltros(f => ({ ...f, categoriaId: e.target.value })); setPage(0) }}>
                <option value="">Todas</option>
                {categorias.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
              </select>
            </div>
            <div className="col-6 col-md-3">
              <label className="form-label">Marca</label>
              <select className="form-select" value={filtros.marcaId}
                      onChange={e => { setFiltros(f => ({ ...f, marcaId: e.target.value })); setPage(0) }}>
                <option value="">Todas</option>
                {marcas.map(m => <option key={m.id} value={m.id}>{m.nombre}</option>)}
              </select>
            </div>
            <div className="col-6 col-md-2">
              <label className="form-label">Estado</label>
              <select className="form-select" value={filtros.estado}
                      onChange={e => { setFiltros(f => ({ ...f, estado: e.target.value })); setPage(0) }}>
                <option value="">Todos</option>
                <option value="true">Activo</option>
                <option value="false">Inactivo</option>
              </select>
            </div>
            <div className="col-6 col-md-1">
              <button type="button" className="btn btn-light w-100" onClick={limpiarFiltros} title="Limpiar filtros">
                <i className="fa-solid fa-eraser"></i>
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Tabla */}
      <div className="card">
        <div className="table-responsive">
          <table className="table table-hover align-middle">
            <thead>
              <tr>
                <th>SKU</th>
                <th>Producto</th>
                <th>Categoría</th>
                <th>Marca</th>
                <th className="text-end">Precio</th>
                <th className="text-center">Stock</th>
                <th className="text-center">Estado</th>
                {puedeEditar && <th className="text-center">Acciones</th>}
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr><td colSpan={8}><div className="cargando-pagina py-4"><div className="spinner-border spinner-border-sm text-primary"></div></div></td></tr>
              ) : pagina.contenido.length === 0 ? (
                <tr><td colSpan={8}><div className="tabla-vacia"><i className="fa-solid fa-box-open"></i>No se encontraron productos con los filtros aplicados.</div></td></tr>
              ) : pagina.contenido.map(p => (
                <tr key={p.id}>
                  <td><span className="etiqueta etiqueta-secondary">{p.sku}</span></td>
                  <td>
                    <div className="fw-medium">{p.nombre}</div>
                    {p.descripcion && (
                      <div style={{ fontSize: '0.76rem', color: 'var(--text-muted-color)' }}>
                        {p.descripcion.length > 60 ? p.descripcion.slice(0, 60) + '…' : p.descripcion}
                      </div>
                    )}
                  </td>
                  <td>{p.categoria || '—'}</td>
                  <td>{p.marca || '—'}</td>
                  <td className="text-end fw-semibold">{soles(p.precio)}</td>
                  <td className="text-center">
                    <span className={`fw-bold ${p.stockBajo ? '' : ''}`} style={{ color: p.stockBajo ? 'var(--danger)' : undefined }}>
                      {p.stock}
                    </span>
                    {p.stockBajo && (
                      <div><span className="etiqueta etiqueta-warning" style={{ fontSize: '0.65rem' }}>
                        <i className="fa-solid fa-triangle-exclamation"></i> Stock bajo
                      </span></div>
                    )}
                  </td>
                  <td className="text-center">
                    <span className={`etiqueta etiqueta-pill etiqueta-${p.activo ? 'success' : 'danger'}`}>
                      {p.activo ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  {puedeEditar && (
                    <td className="text-center">
                      <div className="d-inline-flex gap-1">
                        <button type="button" className="btn-accion" title="Editar" onClick={() => abrirEditar(p)}>
                          <i className="fa-solid fa-pen"></i>
                        </button>
                        <button type="button" className="btn-accion" title="Historial de precios" onClick={() => verHistorial(p)}>
                          <i className="fa-solid fa-clock-rotate-left"></i>
                        </button>
                        <button type="button" className={`btn-accion ${p.activo ? 'peligro' : 'exito'}`}
                                title={p.activo ? 'Desactivar (baja lógica)' : 'Activar'}
                                onClick={() => setConfirmar({ tipo: 'estado', producto: p })}>
                          <i className={`fa-solid ${p.activo ? 'fa-toggle-off' : 'fa-toggle-on'}`}></i>
                        </button>
                        <button type="button" className="btn-accion peligro" title="Eliminar"
                                onClick={() => setConfirmar({ tipo: 'eliminar', producto: p })}>
                          <i className="fa-solid fa-trash"></i>
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {pagina.totalPaginas > 1 && (
          <div className="card-body pt-0 pb-3">
            <div className="paginacion">
              <button type="button" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
                <i className="fa-solid fa-chevron-left"></i>
              </button>
              {Array.from({ length: pagina.totalPaginas }, (_, i) => (
                <button key={i} type="button" className={i === page ? 'actual' : ''} onClick={() => setPage(i)}>
                  {i + 1}
                </button>
              ))}
              <button type="button" disabled={page >= pagina.totalPaginas - 1} onClick={() => setPage(p => p + 1)}>
                <i className="fa-solid fa-chevron-right"></i>
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Modal crear/editar (RF-05 / RF-06) */}
      <Modal
        abierto={!!modal}
        titulo={modal?.modo === 'crear' ? 'Nuevo Producto' : 'Editar Producto'}
        icono={modal?.modo === 'crear' ? 'fa-plus' : 'fa-pen'}
        onCerrar={() => setModal(null)}
        ancho={620}
      >
        {modal && (
          <form onSubmit={guardar}>
            <div className="row g-3">
              <div className="col-12 col-md-5">
                <label className="form-label">SKU *</label>
                <input
                  type="text" className={`form-control ${skuExiste ? 'is-invalid' : ''}`}
                  value={modal.form.sku} required
                  disabled={modal.modo === 'editar'}
                  onChange={e => { setForm({ sku: e.target.value }); validarSku(e.target.value) }}
                />
                {modal.modo === 'editar' ? (
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted-color)', marginTop: 4 }}>
                    El SKU no se puede modificar.
                  </div>
                ) : skuExiste && (
                  <div className="invalid-feedback d-block">Este SKU ya está registrado.</div>
                )}
              </div>
              <div className="col-12 col-md-7">
                <label className="form-label">Nombre *</label>
                <input type="text" className="form-control" value={modal.form.nombre} required
                       onChange={e => setForm({ nombre: e.target.value })} />
              </div>
              <div className="col-12">
                <label className="form-label">Descripción</label>
                <textarea className="form-control" rows={2} value={modal.form.descripcion}
                          onChange={e => setForm({ descripcion: e.target.value })}></textarea>
              </div>
              <div className="col-6 col-md-3">
                <label className="form-label">Precio (S/) *</label>
                <input type="number" step="0.01" min="0" className="form-control" value={modal.form.precio} required
                       onChange={e => setForm({ precio: e.target.value })} />
              </div>
              <div className="col-6 col-md-3">
                <label className="form-label">Stock Mínimo</label>
                <input type="number" min="0" className="form-control" value={modal.form.stockMinimo}
                       onChange={e => setForm({ stockMinimo: e.target.value })} />
              </div>
              <div className="col-6 col-md-3">
                <label className="form-label">Categoría *</label>
                <select className="form-select" value={modal.form.categoriaId} required
                        onChange={e => setForm({ categoriaId: e.target.value })}>
                  <option value="">Seleccione…</option>
                  {categorias.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
                </select>
              </div>
              <div className="col-6 col-md-3">
                <label className="form-label">Marca</label>
                <select className="form-select" value={modal.form.marcaId}
                        onChange={e => setForm({ marcaId: e.target.value })}>
                  <option value="">Sin marca</option>
                  {marcas.map(m => <option key={m.id} value={m.id}>{m.nombre}</option>)}
                </select>
              </div>
            </div>
            {modal.modo === 'crear' && (
              <div className="alert alert-info mt-3 py-2" style={{ fontSize: '0.8rem' }}>
                <i className="fa-solid fa-circle-info me-1"></i>
                El stock inicial es 0; se incrementa registrando <strong>Entradas</strong>.
              </div>
            )}
            <div className="d-flex justify-content-end gap-2 mt-4">
              <button type="button" className="btn btn-light" onClick={() => setModal(null)}>Cancelar</button>
              <button type="submit" className="btn btn-primary" disabled={modal.modo === 'crear' && skuExiste}>
                <i className="fa-solid fa-floppy-disk me-1"></i> Guardar
              </button>
            </div>
          </form>
        )}
      </Modal>

      {/* Modal historial de precios (RF-06) */}
      <Modal
        abierto={!!historial}
        titulo={`Historial de Precios — ${historial?.producto?.nombre || ''}`}
        icono="fa-clock-rotate-left"
        onCerrar={() => setHistorial(null)}
        ancho={560}
      >
        {historial && (historial.precios.length === 0 ? (
          <div className="tabla-vacia"><i className="fa-solid fa-tag"></i>Este producto no tiene cambios de precio registrados.</div>
        ) : (
          <div className="table-responsive">
            <table className="table table-sm">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th className="text-end">Precio Anterior</th>
                  <th className="text-end">Precio Nuevo</th>
                  <th>Usuario</th>
                </tr>
              </thead>
              <tbody>
                {historial.precios.map((h, i) => (
                  <tr key={i}>
                    <td style={{ fontSize: '0.82rem' }}>{h.fecha}</td>
                    <td className="text-end" style={{ color: 'var(--text-muted-color)' }}>{soles(h.anterior)}</td>
                    <td className="text-end fw-semibold" style={{ color: h.nuevo > h.anterior ? 'var(--danger)' : 'var(--success)' }}>
                      {soles(h.nuevo)}
                      <i className={`fa-solid ms-1 ${h.nuevo > h.anterior ? 'fa-arrow-up' : 'fa-arrow-down'}`} style={{ fontSize: '0.7rem' }}></i>
                    </td>
                    <td style={{ fontSize: '0.82rem' }}>{h.usuario}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
      </Modal>

      {/* Confirmaciones (RF-07 baja lógica / eliminar) */}
      <ConfirmModal
        abierto={!!confirmar}
        titulo={confirmar?.tipo === 'estado'
          ? (confirmar?.producto?.activo ? 'Desactivar producto' : 'Activar producto')
          : 'Eliminar producto'}
        mensaje={confirmar?.tipo === 'estado'
          ? `¿Cambiar el estado de "${confirmar?.producto?.nombre}"? La baja lógica conserva el historial de ventas.`
          : `¿Eliminar definitivamente "${confirmar?.producto?.nombre}"? Esta acción no se puede deshacer.`}
        textoConfirmar={confirmar?.tipo === 'estado' ? 'Cambiar estado' : 'Eliminar'}
        variante={confirmar?.tipo === 'estado' && !confirmar?.producto?.activo ? 'success' : 'danger'}
        onConfirmar={ejecutarConfirmacion}
        onCerrar={() => setConfirmar(null)}
      />
    </>
  )
}
