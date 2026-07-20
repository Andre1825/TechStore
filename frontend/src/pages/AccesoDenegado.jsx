import { Link } from 'react-router-dom'

export default function AccesoDenegado() {
  return (
    <div className="cargando-pagina" style={{ paddingTop: 100 }}>
      <div
        className="kpi-icon etiqueta-danger"
        style={{ width: 72, height: 72, fontSize: '1.8rem', borderRadius: 20 }}
      >
        <i className="fa-solid fa-ban"></i>
      </div>
      <h4 style={{ color: 'var(--text-primary)', marginTop: 8 }}>Acceso Denegado</h4>
      <p style={{ maxWidth: 380, textAlign: 'center' }}>
        No tienes permisos para acceder a esta sección. Si crees que es un error, contacta al administrador.
      </p>
      <Link to="/" className="btn btn-primary">
        <i className="fa-solid fa-house me-1"></i> Volver al Escritorio
      </Link>
    </div>
  )
}
