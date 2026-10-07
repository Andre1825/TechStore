import { useAuth, rolLegible } from '../context/AuthContext'
import PageHeader from '../components/PageHeader'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { useToast } from '../components/Toast'

function iniciales(nombre) {
  return (nombre || '?')
    .split(' ')
    .map(p => p[0])
    .filter(Boolean)
    .slice(0, 2)
    .join('')
    .toUpperCase()
}

export default function Perfil() {
  const { user, logout, actualizarPerfil, refrescarUsuario, tienePermiso } = useAuth()
  const navigate = useNavigate()
  const toast = useToast()
  const [passwords, setPasswords] = useState({ actual: '', nueva: '', confirmacion: '' })
  const [guardando, setGuardando] = useState(false)
  const [perfil, setPerfil] = useState({ nombreCompleto: '', correo: '' })
  const [editandoPerfil, setEditandoPerfil] = useState(false)
  const [guardandoPerfil, setGuardandoPerfil] = useState(false)
  const [solicitandoAvisos, setSolicitandoAvisos] = useState(false)

  useEffect(() => {
    setPerfil({ nombreCompleto: user?.nombreCompleto || '', correo: user?.correo || '' })
  }, [user?.nombreCompleto, user?.correo])

  const cancelarEdicion = () => {
    setPerfil({ nombreCompleto: user?.nombreCompleto || '', correo: user?.correo || '' })
    setEditandoPerfil(false)
  }

  const guardarPerfil = async e => {
    e.preventDefault()
    if (!editandoPerfil || guardandoPerfil) return
    setGuardandoPerfil(true)
    try {
      const correoAnterior = user?.correo || ''
      const actualizado = await actualizarPerfil(perfil)
      setEditandoPerfil(false)
      toast('success', correoAnterior !== (actualizado.correo || '')
        ? 'Perfil actualizado. Si quieres avisos, actívalos para el correo guardado.'
        : 'Perfil actualizado correctamente.')
    } catch (err) {
      toast('error', err.message)
    } finally {
      setGuardandoPerfil(false)
    }
  }

  const configurarAvisos = async activar => {
    setSolicitandoAvisos(true)
    try {
      const resultado = activar
        ? await api.post('/api/auth/stock-alerts/subscribe')
        : await api.delete('/api/auth/stock-alerts')
      await refrescarUsuario()
      toast('success', resultado.mensaje)
    } catch (err) {
      toast('error', err.message)
    } finally {
      setSolicitandoAvisos(false)
    }
  }

  const cambiarPassword = async e => {
    e.preventDefault()
    if (passwords.nueva !== passwords.confirmacion) {
      toast('error', 'La confirmación no coincide con la nueva contraseña.')
      return
    }
    if (new TextEncoder().encode(passwords.nueva).length > 72) {
      toast('error', 'La contraseña supera el máximo de 72 bytes. Reduce su longitud.')
      return
    }
    setGuardando(true)
    try {
      await api.post('/api/auth/password', {
        passwordActual: passwords.actual, passwordNueva: passwords.nueva,
      })
      toast('success', 'Contraseña actualizada. Vuelve a iniciar sesión.')
      await logout()
      navigate('/login', { replace: true })
    } catch (err) {
      toast('error', err.message)
    } finally {
      setGuardando(false)
    }
  }

  const fmtFecha = f => f
    ? new Date(f).toLocaleString('es-PE', { day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' })
    : '—'

  return (
    <>
      <PageHeader titulo="Mi Perfil" subtitulo="Información de tu cuenta" />

      <div className="row g-3">
        <div className="col-12 col-md-4 col-lg-3">
          <div className="card h-100">
            <div className="card-body text-center py-4">
              <div
                className="navbar-user-avatar mx-auto mb-3"
                style={{ width: 84, height: 84, fontSize: '1.6rem' }}
              >
                {iniciales(user?.nombreCompleto || user?.username)}
              </div>
              <h5 className="mb-1" style={{ color: 'var(--text-primary)' }}>
                {user?.nombreCompleto || user?.username}
              </h5>
              <span className="etiqueta etiqueta-primary">
                <i className="fa-solid fa-shield-halved"></i> {rolLegible(user?.rol)}
              </span>
            </div>
          </div>
        </div>

        <div className="col-12 col-md-8 col-lg-9">
          <div className="card h-100">
            <div className="card-body">
              <h6 className="card-titulo">
                <i className="fa-solid fa-id-card me-2 text-primary"></i>Datos de la Cuenta
              </h6>
              <form onSubmit={guardarPerfil}>
              <div className="row g-3">
                <div className="col-12 col-md-6">
                  <label className="form-label">Usuario</label>
                  <div className="form-control" style={{ background: 'var(--bg-input)' }}>{user?.username}</div>
                </div>
                <div className="col-12 col-md-6">
                  <label htmlFor="perfil-nombre" className="form-label">Nombre completo</label>
                  <input id="perfil-nombre" className="form-control" maxLength={255} autoComplete="name"
                    disabled={!editandoPerfil || guardandoPerfil} value={perfil.nombreCompleto}
                    onChange={e => setPerfil(p => ({ ...p, nombreCompleto: e.target.value }))} />
                </div>
                <div className="col-12 col-md-6">
                  <label htmlFor="perfil-correo" className="form-label">Correo</label>
                  <input id="perfil-correo" type="email" className="form-control" maxLength={255} autoComplete="email"
                    disabled={!editandoPerfil || guardandoPerfil} value={perfil.correo}
                    onChange={e => setPerfil(p => ({ ...p, correo: e.target.value }))} />
                </div>
                <div className="col-12 col-md-6">
                  <label className="form-label">Rol de Acceso</label>
                  <div className="form-control" style={{ background: 'var(--bg-input)' }}>{rolLegible(user?.rol)}</div>
                </div>
                <div className="col-12 col-md-6">
                  <label className="form-label">Último Acceso</label>
                  <div className="form-control" style={{ background: 'var(--bg-input)' }}>{fmtFecha(user?.ultimoAcceso)}</div>
                </div>
              </div>
              <div className="d-flex gap-2 mt-3">
                {editandoPerfil ? <>
                  <button key="guardar-perfil" type="submit" className="btn btn-primary" disabled={guardandoPerfil}>
                    {guardandoPerfil ? 'Guardando…' : 'Guardar cambios'}
                  </button>
                  <button key="cancelar-perfil" type="button" className="btn btn-outline-secondary" disabled={guardandoPerfil} onClick={cancelarEdicion}>Cancelar</button>
                </> : <button key="editar-perfil" type="button" className="btn btn-primary" onClick={e => {
                  // Cancelar la acción nativa antes de cambiar los botones del formulario.
                  e.preventDefault()
                  setEditandoPerfil(true)
                }}>Editar datos</button>}
              </div>
              </form>
            </div>
          </div>
        </div>
      </div>
      {tienePermiso('GESTIONAR_USUARIOS') && <div className="card mt-3">
        <div className="card-body">
          <h6 className="card-titulo">Avisos de stock por correo</h6>
          <p>Recibe un aviso cuando una salida haga que un producto alcance o quede por debajo de su stock mínimo.</p>
          <p>Correo de destino: <strong>{user?.correo || 'Guarda un correo en tus datos de la cuenta.'}</strong></p>
          {!user?.avisosStockDisponibles && <p className="text-muted">Los avisos por correo aún no están configurados en el servidor.</p>}
          {user?.stockAlertasActivas && <p className="text-muted">Avisos solicitados. Para recibirlos debes confirmar la suscripción desde el correo de Amazon SNS.</p>}
          {editandoPerfil && <p className="text-muted">Guarda o cancela los cambios de tu perfil antes de configurar los avisos.</p>}
          <div className="d-flex gap-2 flex-wrap">
            <button type="button" className="btn btn-primary" onClick={() => configurarAvisos(true)}
              disabled={solicitandoAvisos || editandoPerfil || !user?.correo || !user?.avisosStockDisponibles}>
              {solicitandoAvisos ? 'Procesando…' : user?.stockAlertasActivas ? 'Solicitar suscripción nuevamente' : 'Activar avisos'}
            </button>
            {user?.stockAlertasActivas && <button type="button" className="btn btn-outline-secondary"
              disabled={solicitandoAvisos || editandoPerfil} onClick={() => configurarAvisos(false)}>Desactivar avisos</button>}
          </div>
        </div>
      </div>}
      <div className="card mt-3">
        <div className="card-body">
          <h6 className="card-titulo">Cambiar contraseña</h6>
          <p className="text-muted">Usa al menos 12 caracteres. Al cambiarla, se cerrarán las sesiones de tu cuenta.</p>
          <form onSubmit={cambiarPassword}>
            <div className="row g-3">
              <div className="col-12 col-md-4">
                <label htmlFor="password-actual" className="form-label">Contraseña actual</label>
                <input id="password-actual" type="password" className="form-control" required maxLength={72}
                  autoComplete="current-password" value={passwords.actual}
                  onChange={e => setPasswords(p => ({ ...p, actual: e.target.value }))} />
              </div>
              <div className="col-12 col-md-4">
                <label htmlFor="password-nueva" className="form-label">Nueva contraseña</label>
                <input id="password-nueva" type="password" className="form-control" required minLength={12} maxLength={72}
                  autoComplete="new-password" value={passwords.nueva}
                  onChange={e => setPasswords(p => ({ ...p, nueva: e.target.value }))} />
              </div>
              <div className="col-12 col-md-4">
                <label htmlFor="password-confirmacion" className="form-label">Confirmar nueva contraseña</label>
                <input id="password-confirmacion" type="password" className="form-control" required minLength={12} maxLength={72}
                  autoComplete="new-password" value={passwords.confirmacion}
                  onChange={e => setPasswords(p => ({ ...p, confirmacion: e.target.value }))} />
              </div>
            </div>
            <button type="submit" className="btn btn-primary mt-3" disabled={guardando}>
              {guardando ? 'Guardando…' : 'Actualizar contraseña'}
            </button>
          </form>
        </div>
      </div>
    </>
  )
}
