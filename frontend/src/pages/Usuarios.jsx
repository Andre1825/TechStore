import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/client'
import { useToast } from '../components/Toast'
import Modal, { ConfirmModal } from '../components/Modal'
import PageHeader from '../components/PageHeader'

const FORM_VACIO = { username: '', password: '', nombreCompleto: '', correo: '', rol: '' }

const fmtFecha = f => f
  ? new Date(f).toLocaleString('es-PE', { day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit' })
  : 'Nunca'

/**
 * RF-03: Gestión de usuarios y asignación de roles (solo ADMIN).
 */
export default function Usuarios() {
  const toast = useToast()
  const [usuarios, setUsuarios] = useState([])
  const [roles, setRoles] = useState([])
  const [cargando, setCargando] = useState(true)
  const [modal, setModal] = useState(null)
  const [confirmar, setConfirmar] = useState(null)

  const cargar = useCallback(() => {
    Promise.all([api.get('/api/usuarios'), api.get('/api/roles/activos')])
      .then(([us, rs]) => { setUsuarios(us); setRoles(rs) })
      .catch(() => toast('error', 'No se pudieron cargar los usuarios.'))
      .finally(() => setCargando(false))
  }, [toast])

  useEffect(() => { cargar() }, [cargar])

  const setForm = c => setModal(m => ({ ...m, form: { ...m.form, ...c } }))

  const guardar = async e => {
    e.preventDefault()
    const { modo, form } = modal
    try {
      if (modo === 'crear') {
        await api.post('/api/usuarios', form)
        toast('success', 'Usuario registrado correctamente.')
      } else {
        await api.put(`/api/usuarios/${form.id}`, {
          nombreCompleto: form.nombreCompleto, correo: form.correo, rol: form.rol,
        })
        toast('success', 'Usuario actualizado correctamente.')
      }
      setModal(null)
      cargar()
    } catch (err) {
      toast('error', err.message)
    }
  }

  const toggleBloqueo = async () => {
    try {
      await api.post(`/api/usuarios/${confirmar.usuario.id}/bloqueo`)
      toast('success', confirmar.usuario.cuentaBloqueada
        ? 'Cuenta desbloqueada; intentos fallidos reiniciados.'
        : 'Cuenta bloqueada correctamente.')
      cargar()
    } catch (err) {
      toast('error', err.message)
    } finally {
      setConfirmar(null)
    }
  }

  return (
    <>
      <PageHeader titulo="Usuarios" subtitulo="Cuentas de acceso al sistema y sus roles">
        <button type="button" className="btn btn-primary"
                onClick={() => setModal({ modo: 'crear', form: { ...FORM_VACIO, rol: roles[0]?.id ?? '' } })}>
          <i className="fa-solid fa-user-plus me-1"></i> Nuevo Usuario
        </button>
      </PageHeader>

      <div className="card">
        <div className="table-responsive">
          <table className="table table-hover align-middle">
            <thead>
              <tr>
                <th>Usuario</th>
                <th>Nombre Completo</th>
                <th>Correo</th>
                <th className="text-center">Rol</th>
                <th className="text-center">Estado</th>
                <th>Último Acceso</th>
                <th className="text-center">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr><td colSpan={7}><div className="cargando-pagina py-4"><div className="spinner-border spinner-border-sm text-primary"></div></div></td></tr>
              ) : usuarios.map(u => (
                <tr key={u.id}>
                  <td className="fw-medium">{u.username}</td>
                  <td style={{ fontSize: '0.87rem' }}>{u.nombreCompleto || '—'}</td>
                  <td style={{ fontSize: '0.85rem', color: 'var(--text-muted-color)' }}>{u.correo || '—'}</td>
                  <td className="text-center">
                    <span className="etiqueta etiqueta-primary">
                      <i className="fa-solid fa-shield-halved"></i> {u.rol || '—'}
                    </span>
                  </td>
                  <td className="text-center">
                    {u.cuentaBloqueada ? (
                      <span className="etiqueta etiqueta-pill etiqueta-danger">
                        <i className="fa-solid fa-lock"></i> Bloqueada
                      </span>
                    ) : (
                      <span className={`etiqueta etiqueta-pill etiqueta-${u.activo ? 'success' : 'secondary'}`}>
                        {u.activo ? 'Activo' : 'Inactivo'}
                      </span>
                    )}
                  </td>
                  <td style={{ fontSize: '0.8rem', color: 'var(--text-muted-color)' }}>{fmtFecha(u.ultimoAcceso)}</td>
                  <td className="text-center">
                    <div className="d-inline-flex gap-1">
                      <button type="button" className="btn-accion" title="Editar"
                              onClick={() => setModal({
                                modo: 'editar',
                                form: {
                                  id: u.id, username: u.username, nombreCompleto: u.nombreCompleto || '',
                                  correo: u.correo || '', rol: u.rolId ?? '',
                                },
                              })}>
                        <i className="fa-solid fa-pen"></i>
                      </button>
                      <button type="button" className={`btn-accion ${u.cuentaBloqueada ? 'exito' : 'peligro'}`}
                              title={u.cuentaBloqueada ? 'Desbloquear cuenta' : 'Bloquear cuenta'}
                              onClick={() => setConfirmar({ usuario: u })}>
                        <i className={`fa-solid ${u.cuentaBloqueada ? 'fa-lock-open' : 'fa-lock'}`}></i>
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
        titulo={modal?.modo === 'crear' ? 'Nuevo Usuario' : `Editar Usuario — ${modal?.form?.username || ''}`}
        icono={modal?.modo === 'crear' ? 'fa-user-plus' : 'fa-user-pen'}
        onCerrar={() => setModal(null)}
      >
        {modal && (
          <form onSubmit={guardar}>
            {modal.modo === 'crear' && (
              <div className="row g-3 mb-3">
                <div className="col-12 col-md-6">
                  <label className="form-label">Usuario *</label>
                  <input type="text" className="form-control" value={modal.form.username} required autoFocus maxLength={100}
                         onChange={e => setForm({ username: e.target.value })} />
                </div>
                <div className="col-12 col-md-6">
                  <label className="form-label">Contraseña *</label>
                  <input type="password" className="form-control" value={modal.form.password} required minLength={12} maxLength={72}
                         autoComplete="new-password"
                         onChange={e => setForm({ password: e.target.value })} />
                </div>
              </div>
            )}
            <div className="mb-3">
              <label className="form-label">Nombre Completo</label>
              <input type="text" className="form-control" value={modal.form.nombreCompleto} maxLength={255}
                     onChange={e => setForm({ nombreCompleto: e.target.value })} />
            </div>
            <div className="mb-3">
              <label className="form-label">Correo</label>
              <input type="email" className="form-control" value={modal.form.correo} maxLength={255}
                     onChange={e => setForm({ correo: e.target.value })} />
            </div>
            <div className="mb-3">
              <label className="form-label">Rol *</label>
              <select className="form-select" value={modal.form.rol} required
                      onChange={e => setForm({ rol: e.target.value })}>
                <option value="" disabled>Selecciona un rol…</option>
                {roles.map(r => <option key={r.id} value={r.id}>{r.nombre}</option>)}
              </select>
            </div>
            <div className="d-flex justify-content-end gap-2 mt-4">
              <button type="button" className="btn btn-light" onClick={() => setModal(null)}>Cancelar</button>
              <button type="submit" className="btn btn-primary"><i className="fa-solid fa-floppy-disk me-1"></i> Guardar</button>
            </div>
          </form>
        )}
      </Modal>

      <ConfirmModal
        abierto={!!confirmar}
        titulo={confirmar?.usuario?.cuentaBloqueada ? 'Desbloquear cuenta' : 'Bloquear cuenta'}
        mensaje={confirmar?.usuario?.cuentaBloqueada
          ? `¿Desbloquear la cuenta de "${confirmar?.usuario?.username}"? Se reiniciarán sus intentos fallidos.`
          : `¿Bloquear la cuenta de "${confirmar?.usuario?.username}"? No podrá iniciar sesión hasta que se desbloquee.`}
        textoConfirmar={confirmar?.usuario?.cuentaBloqueada ? 'Desbloquear' : 'Bloquear'}
        variante={confirmar?.usuario?.cuentaBloqueada ? 'success' : 'danger'}
        onConfirmar={toggleBloqueo}
        onCerrar={() => setConfirmar(null)}
      />
    </>
  )
}
