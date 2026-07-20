import { useAuth, rolLegible } from '../context/AuthContext'
import PageHeader from '../components/PageHeader'

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
  const { user } = useAuth()

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
              <div className="row g-3">
                <div className="col-12 col-md-6">
                  <label className="form-label">Usuario</label>
                  <div className="form-control" style={{ background: 'var(--bg-input)' }}>{user?.username}</div>
                </div>
                <div className="col-12 col-md-6">
                  <label className="form-label">Nombre Completo</label>
                  <div className="form-control" style={{ background: 'var(--bg-input)' }}>{user?.nombreCompleto || '—'}</div>
                </div>
                <div className="col-12 col-md-6">
                  <label className="form-label">Correo</label>
                  <div className="form-control" style={{ background: 'var(--bg-input)' }}>{user?.correo || '—'}</div>
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
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
