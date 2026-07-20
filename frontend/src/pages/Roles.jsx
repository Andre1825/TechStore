import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/client'
import { PERMISOS, permisoLegible } from '../context/AuthContext'
import { useToast } from '../components/Toast'
import Modal from '../components/Modal'
import PageHeader from '../components/PageHeader'

const FORM_VACIO = { nombre: '', descripcion: '', permisos: [] }

/**
 * RF-03: Roles y permisos del sistema (RBAC). Cada rol define un conjunto de permisos.
 */
export default function Roles() {
  const toast = useToast()
  const [roles, setRoles] = useState([])
  const [cargando, setCargando] = useState(true)
  const [modal, setModal] = useState(null)

  const cargar = useCallback(() => {
    api.get('/api/roles')
      .then(setRoles)
      .catch(() => toast('error', 'No se pudieron cargar los roles.'))
      .finally(() => setCargando(false))
  }, [toast])

  useEffect(() => { cargar() }, [cargar])

  const setForm = c => setModal(m => ({ ...m, form: { ...m.form, ...c } }))

  const togglePermiso = clave => setModal(m => {
    const tiene = m.form.permisos.includes(clave)
    const permisos = tiene
      ? m.form.permisos.filter(p => p !== clave)
      : [...m.form.permisos, clave]
    return { ...m, form: { ...m.form, permisos } }
  })

  const guardar = async e => {
    e.preventDefault()
    const { modo, form } = modal
    if (form.permisos.length === 0) {
      toast('error', 'Selecciona al menos un permiso para el rol.')
      return
    }
    try {
      if (modo === 'crear') {
        await api.post('/api/roles', form)
        toast('success', 'Rol registrado correctamente.')
      } else {
        await api.put(`/api/roles/${form.id}`, form)
        toast('success', 'Rol actualizado correctamente.')
      }
      setModal(null)
      cargar()
    } catch (err) {
      toast('error', err.message)
    }
  }

  return (
    <>
      <PageHeader titulo="Roles y Permisos" subtitulo="Roles del sistema y sus permisos por módulo">
        <button type="button" className="btn btn-primary"
                onClick={() => setModal({ modo: 'crear', form: { ...FORM_VACIO, permisos: [] } })}>
          <i className="fa-solid fa-plus me-1"></i> Nuevo Rol
        </button>
      </PageHeader>

      <div className="card">
        <div className="table-responsive">
          <table className="table table-hover align-middle">
            <thead>
              <tr>
                <th>Rol</th>
                <th>Descripción</th>
                <th>Permisos</th>
                <th className="text-center">Usuarios</th>
                <th className="text-center">Estado</th>
                <th className="text-center">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr><td colSpan={6}><div className="cargando-pagina py-4"><div className="spinner-border spinner-border-sm text-primary"></div></div></td></tr>
              ) : roles.length === 0 ? (
                <tr><td colSpan={6}><div className="tabla-vacia"><i className="fa-solid fa-shield-halved"></i>Aún no hay roles registrados.</div></td></tr>
              ) : roles.map(r => (
                <tr key={r.id}>
                  <td className="fw-medium">{r.nombre}</td>
                  <td style={{ fontSize: '0.85rem', color: 'var(--text-muted-color)' }}>{r.descripcion || '—'}</td>
                  <td>
                    <div className="d-flex flex-wrap gap-1" style={{ maxWidth: '360px' }}>
                      {(r.permisos || []).length === 0
                        ? <span style={{ fontSize: '0.8rem', color: 'var(--text-muted-color)' }}>Sin permisos</span>
                        : r.permisos.map(p => (
                          <span key={p} className="etiqueta etiqueta-info" style={{ fontSize: '0.72rem' }}>
                            {permisoLegible(p)}
                          </span>
                        ))}
                    </div>
                  </td>
                  <td className="text-center"><span className="etiqueta etiqueta-secondary">{r.usuarios ?? 0}</span></td>
                  <td className="text-center">
                    <span className={`etiqueta etiqueta-pill etiqueta-${r.activo ? 'success' : 'danger'}`}>
                      {r.activo ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  <td className="text-center">
                    <button type="button" className="btn-accion" title="Editar"
                            onClick={() => setModal({
                              modo: 'editar',
                              form: { id: r.id, nombre: r.nombre, descripcion: r.descripcion || '', permisos: [...(r.permisos || [])] },
                            })}>
                      <i className="fa-solid fa-pen"></i>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <Modal
        abierto={!!modal}
        titulo={modal?.modo === 'crear' ? 'Nuevo Rol' : 'Editar Rol'}
        icono="fa-shield-halved"
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
              <textarea className="form-control" rows={2} value={modal.form.descripcion}
                        onChange={e => setForm({ descripcion: e.target.value })}></textarea>
            </div>
            <div className="mb-2">
              <label className="form-label">Permisos *</label>
              <div className="row g-2">
                {PERMISOS.map(p => (
                  <div className="col-12 col-md-6" key={p.clave}>
                    <div className="form-check">
                      <input className="form-check-input" type="checkbox" id={`perm-${p.clave}`}
                             checked={modal.form.permisos.includes(p.clave)}
                             onChange={() => togglePermiso(p.clave)} />
                      <label className="form-check-label" htmlFor={`perm-${p.clave}`}>{p.etiqueta}</label>
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <div className="d-flex justify-content-end gap-2 mt-4">
              <button type="button" className="btn btn-light" onClick={() => setModal(null)}>Cancelar</button>
              <button type="submit" className="btn btn-primary"><i className="fa-solid fa-floppy-disk me-1"></i> Guardar</button>
            </div>
          </form>
        )}
      </Modal>
    </>
  )
}
