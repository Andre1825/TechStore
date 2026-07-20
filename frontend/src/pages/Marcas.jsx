import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/client'
import { useToast } from '../components/Toast'
import Modal, { ConfirmModal } from '../components/Modal'
import PageHeader from '../components/PageHeader'

export default function Marcas() {
  const toast = useToast()
  const [marcas, setMarcas] = useState([])
  const [cargando, setCargando] = useState(true)
  const [modal, setModal] = useState(null)
  const [confirmar, setConfirmar] = useState(null)

  const cargar = useCallback(() => {
    api.get('/api/marcas')
      .then(setMarcas)
      .catch(() => toast('error', 'No se pudieron cargar las marcas.'))
      .finally(() => setCargando(false))
  }, [toast])

  useEffect(() => { cargar() }, [cargar])

  const guardar = async e => {
    e.preventDefault()
    const { modo, form } = modal
    try {
      if (modo === 'crear') {
        await api.post('/api/marcas', { nombre: form.nombre, descripcion: form.descripcion })
        toast('success', 'Marca registrada correctamente.')
      } else {
        await api.put(`/api/marcas/${form.id}`, { nombre: form.nombre, descripcion: form.descripcion })
        toast('success', 'Marca actualizada correctamente.')
      }
      setModal(null)
      cargar()
    } catch (err) {
      toast('error', err.message)
    }
  }

  const ejecutarConfirmacion = async () => {
    const { tipo, marca } = confirmar
    try {
      if (tipo === 'estado') {
        await api.post(`/api/marcas/${marca.id}/estado`)
        toast('success', `Marca ${marca.activa ? 'desactivada' : 'activada'} correctamente.`)
      } else {
        await api.delete(`/api/marcas/${marca.id}`)
        toast('success', 'Marca eliminada correctamente.')
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
      <PageHeader titulo="Marcas" subtitulo="Fabricantes y marcas de los productos">
        <button type="button" className="btn btn-primary"
                onClick={() => setModal({ modo: 'crear', form: { nombre: '', descripcion: '' } })}>
          <i className="fa-solid fa-plus me-1"></i> Nueva Marca
        </button>
      </PageHeader>

      <div className="card">
        <div className="table-responsive">
          <table className="table table-hover align-middle">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Descripción</th>
                <th className="text-center">Productos</th>
                <th className="text-center">Estado</th>
                <th className="text-center">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr><td colSpan={5}><div className="cargando-pagina py-4"><div className="spinner-border spinner-border-sm text-primary"></div></div></td></tr>
              ) : marcas.length === 0 ? (
                <tr><td colSpan={5}><div className="tabla-vacia"><i className="fa-solid fa-tags"></i>Aún no hay marcas registradas.</div></td></tr>
              ) : marcas.map(m => (
                <tr key={m.id}>
                  <td className="fw-medium">{m.nombre}</td>
                  <td style={{ color: 'var(--text-muted-color)', fontSize: '0.85rem' }}>{m.descripcion || '—'}</td>
                  <td className="text-center"><span className="etiqueta etiqueta-secondary">{m.productos}</span></td>
                  <td className="text-center">
                    <span className={`etiqueta etiqueta-pill etiqueta-${m.activa ? 'success' : 'danger'}`}>
                      {m.activa ? 'Activa' : 'Inactiva'}
                    </span>
                  </td>
                  <td className="text-center">
                    <div className="d-inline-flex gap-1">
                      <button type="button" className="btn-accion" title="Editar"
                              onClick={() => setModal({ modo: 'editar', form: { id: m.id, nombre: m.nombre, descripcion: m.descripcion || '' } })}>
                        <i className="fa-solid fa-pen"></i>
                      </button>
                      <button type="button" className={`btn-accion ${m.activa ? 'peligro' : 'exito'}`}
                              title={m.activa ? 'Desactivar' : 'Activar'}
                              onClick={() => setConfirmar({ tipo: 'estado', marca: m })}>
                        <i className={`fa-solid ${m.activa ? 'fa-toggle-off' : 'fa-toggle-on'}`}></i>
                      </button>
                      <button type="button" className="btn-accion peligro" title="Eliminar"
                              onClick={() => setConfirmar({ tipo: 'eliminar', marca: m })}>
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
        titulo={modal?.modo === 'crear' ? 'Nueva Marca' : 'Editar Marca'}
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
          ? (confirmar?.marca?.activa ? 'Desactivar marca' : 'Activar marca')
          : 'Eliminar marca'}
        mensaje={confirmar?.tipo === 'estado'
          ? `¿Cambiar el estado de "${confirmar?.marca?.nombre}"?`
          : `¿Eliminar definitivamente "${confirmar?.marca?.nombre}"? Solo es posible si no tiene productos asociados.`}
        textoConfirmar={confirmar?.tipo === 'estado' ? 'Cambiar estado' : 'Eliminar'}
        variante={confirmar?.tipo === 'estado' && !confirmar?.marca?.activa ? 'success' : 'danger'}
        onConfirmar={ejecutarConfirmacion}
        onCerrar={() => setConfirmar(null)}
      />
    </>
  )
}
