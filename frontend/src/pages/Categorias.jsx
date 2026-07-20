import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/client'
import { useToast } from '../components/Toast'
import Modal, { ConfirmModal } from '../components/Modal'
import PageHeader from '../components/PageHeader'

export default function Categorias() {
  const toast = useToast()
  const [categorias, setCategorias] = useState([])
  const [cargando, setCargando] = useState(true)
  const [modal, setModal] = useState(null) // { modo, form }
  const [confirmar, setConfirmar] = useState(null)

  const cargar = useCallback(() => {
    api.get('/api/categorias')
      .then(setCategorias)
      .catch(() => toast('error', 'No se pudieron cargar las categorías.'))
      .finally(() => setCargando(false))
  }, [toast])

  useEffect(() => { cargar() }, [cargar])

  const guardar = async e => {
    e.preventDefault()
    const { modo, form } = modal
    try {
      if (modo === 'crear') {
        // RF-04: nombre único + código automático generado en el backend
        await api.post('/api/categorias', { nombre: form.nombre, descripcion: form.descripcion })
        toast('success', 'Categoría registrada correctamente.')
      } else {
        await api.put(`/api/categorias/${form.id}`, { nombre: form.nombre, descripcion: form.descripcion })
        toast('success', 'Categoría actualizada correctamente.')
      }
      setModal(null)
      cargar()
    } catch (err) {
      toast('error', err.message)
    }
  }

  const ejecutarConfirmacion = async () => {
    const { tipo, categoria } = confirmar
    try {
      if (tipo === 'estado') {
        await api.post(`/api/categorias/${categoria.id}/estado`)
        toast('success', `Categoría ${categoria.activa ? 'desactivada' : 'activada'} correctamente.`)
      } else {
        await api.delete(`/api/categorias/${categoria.id}`)
        toast('success', 'Categoría eliminada correctamente.')
      }
      cargar()
    } catch (err) {
      toast('error', err.message)
    } finally {
      setConfirmar(null)
    }
  }

  const setForm = c => setModal(m => ({ ...m, form: { ...m.form, ...c } }))

  return (
    <>
      <PageHeader titulo="Categorías" subtitulo="Familias de productos del catálogo">
        <button type="button" className="btn btn-primary"
                onClick={() => setModal({ modo: 'crear', form: { nombre: '', descripcion: '' } })}>
          <i className="fa-solid fa-plus me-1"></i> Nueva Categoría
        </button>
      </PageHeader>

      <div className="card">
        <div className="table-responsive">
          <table className="table table-hover align-middle">
            <thead>
              <tr>
                <th>Código</th>
                <th>Nombre</th>
                <th>Descripción</th>
                <th className="text-center">Productos</th>
                <th className="text-center">Estado</th>
                <th className="text-center">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr><td colSpan={6}><div className="cargando-pagina py-4"><div className="spinner-border spinner-border-sm text-primary"></div></div></td></tr>
              ) : categorias.length === 0 ? (
                <tr><td colSpan={6}><div className="tabla-vacia"><i className="fa-solid fa-folder-open"></i>Aún no hay categorías registradas.</div></td></tr>
              ) : categorias.map(c => (
                <tr key={c.id}>
                  <td><span className="etiqueta etiqueta-primary">{c.codigo}</span></td>
                  <td className="fw-medium">{c.nombre}</td>
                  <td style={{ color: 'var(--text-muted-color)', fontSize: '0.85rem' }}>{c.descripcion || '—'}</td>
                  <td className="text-center"><span className="etiqueta etiqueta-secondary">{c.productos}</span></td>
                  <td className="text-center">
                    <span className={`etiqueta etiqueta-pill etiqueta-${c.activa ? 'success' : 'danger'}`}>
                      {c.activa ? 'Activa' : 'Inactiva'}
                    </span>
                  </td>
                  <td className="text-center">
                    <div className="d-inline-flex gap-1">
                      <button type="button" className="btn-accion" title="Editar"
                              onClick={() => setModal({ modo: 'editar', form: { id: c.id, nombre: c.nombre, descripcion: c.descripcion || '' } })}>
                        <i className="fa-solid fa-pen"></i>
                      </button>
                      <button type="button" className={`btn-accion ${c.activa ? 'peligro' : 'exito'}`}
                              title={c.activa ? 'Desactivar' : 'Activar'}
                              onClick={() => setConfirmar({ tipo: 'estado', categoria: c })}>
                        <i className={`fa-solid ${c.activa ? 'fa-toggle-off' : 'fa-toggle-on'}`}></i>
                      </button>
                      <button type="button" className="btn-accion peligro" title="Eliminar"
                              onClick={() => setConfirmar({ tipo: 'eliminar', categoria: c })}>
                        <i className="fa-solid fa-trash"></i>
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <Modal
        abierto={!!modal}
        titulo={modal?.modo === 'crear' ? 'Nueva Categoría' : 'Editar Categoría'}
        icono={modal?.modo === 'crear' ? 'fa-plus' : 'fa-pen'}
        onCerrar={() => setModal(null)}
      >
        {modal && (
          <form onSubmit={guardar}>
            <div className="mb-3">
              <label className="form-label">Nombre *</label>
              <input type="text" className="form-control" value={modal.form.nombre} required autoFocus
                     onChange={e => setForm({ nombre: e.target.value })} />
            </div>
            <div className="mb-3">
              <label className="form-label">Descripción</label>
              <textarea className="form-control" rows={3} value={modal.form.descripcion}
                        onChange={e => setForm({ descripcion: e.target.value })}></textarea>
            </div>
            {modal.modo === 'crear' && (
              <div className="alert alert-info py-2" style={{ fontSize: '0.8rem' }}>
                <i className="fa-solid fa-circle-info me-1"></i>
                El código (CAT-XX) se genera automáticamente.
              </div>
            )}
            <div className="d-flex justify-content-end gap-2 mt-3">
              <button type="button" className="btn btn-light" onClick={() => setModal(null)}>Cancelar</button>
              <button type="submit" className="btn btn-primary"><i className="fa-solid fa-floppy-disk me-1"></i> Guardar</button>
            </div>
          </form>
        )}
      </Modal>

      <ConfirmModal
        abierto={!!confirmar}
        titulo={confirmar?.tipo === 'estado'
          ? (confirmar?.categoria?.activa ? 'Desactivar categoría' : 'Activar categoría')
          : 'Eliminar categoría'}
        mensaje={confirmar?.tipo === 'estado'
          ? `¿Cambiar el estado de "${confirmar?.categoria?.nombre}"?`
          : `¿Eliminar definitivamente "${confirmar?.categoria?.nombre}"? Solo es posible si no tiene productos asociados.`}
        textoConfirmar={confirmar?.tipo === 'estado' ? 'Cambiar estado' : 'Eliminar'}
        variante={confirmar?.tipo === 'estado' && !confirmar?.categoria?.activa ? 'success' : 'danger'}
        onConfirmar={ejecutarConfirmacion}
        onCerrar={() => setConfirmar(null)}
      />
    </>
  )
}
