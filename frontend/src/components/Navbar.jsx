import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth, rolLegible } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import { api } from '../api/client'

function iniciales(nombre) {
  return (nombre || '?')
    .split(' ')
    .map(p => p[0])
    .filter(Boolean)
    .slice(0, 2)
    .join('')
    .toUpperCase()
}

export default function Navbar({ onToggleSidebar }) {
  const { user, logout } = useAuth()
  const { theme, toggleTheme } = useTheme()
  const navigate = useNavigate()

  const [stockBajo, setStockBajo] = useState(0)
  const [alertas, setAlertas] = useState(null) // lista de productos con stock bajo (se carga al abrir)
  const [panelAlertas, setPanelAlertas] = useState(false)
  const [menuAbierto, setMenuAbierto] = useState(false)
  const menuRef = useRef(null)
  const alertasRef = useRef(null)

  // RF-15: Contador de alertas de stock bajo (se refresca cada 30 s)
  useEffect(() => {
    let activo = true
    const consultar = () => {
      api.get('/api/dashboard/stock-bajo/count')
        .then(d => { if (activo) setStockBajo(d.count || 0) })
        .catch(() => {})
    }
    consultar()
    const intervalo = setInterval(consultar, 30000)
    return () => { activo = false; clearInterval(intervalo) }
  }, [])

  useEffect(() => {
    const cerrar = e => {
      if (menuRef.current && !menuRef.current.contains(e.target)) setMenuAbierto(false)
      if (alertasRef.current && !alertasRef.current.contains(e.target)) setPanelAlertas(false)
    }
    document.addEventListener('mousedown', cerrar)
    return () => document.removeEventListener('mousedown', cerrar)
  }, [])

  const toggleAlertas = () => {
    const abrir = !panelAlertas
    setPanelAlertas(abrir)
    if (abrir) {
      setAlertas(null)
      api.get('/api/dashboard/stock-bajo').then(setAlertas).catch(() => setAlertas([]))
    }
  }

  const cerrarSesion = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <header className="top-navbar">
      <button type="button" className="btn-icono" onClick={onToggleSidebar} title="Ocultar/Mostrar menú">
        <i className="fa-solid fa-bars"></i>
      </button>

      <div className="ms-auto d-flex align-items-center gap-2">
        <button type="button" className="btn-icono" onClick={toggleTheme} title="Cambiar tema">
          <i className={`fa-solid ${theme === 'dark' ? 'fa-sun' : 'fa-moon'}`}></i>
        </button>

        {/* RF-15: Panel de alertas de stock bajo */}
        <div className="position-relative" ref={alertasRef}>
          <button type="button" className="btn-icono" title="Alertas de stock bajo" onClick={toggleAlertas}>
            <i className="fa-solid fa-bell"></i>
            {stockBajo > 0 && (
              <span className="badge-notificacion">{stockBajo > 99 ? '99+' : stockBajo}</span>
            )}
          </button>

          {panelAlertas && (
            <div className="dropdown-panel" style={{ minWidth: 300, maxWidth: 340 }}>
              <div className="px-3 py-2 d-flex align-items-center gap-2">
                <i className="fa-solid fa-triangle-exclamation" style={{ color: 'var(--warning)' }}></i>
                <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                  Alertas de Stock Bajo
                </span>
              </div>
              <hr />
              {alertas === null ? (
                <div className="text-center py-3">
                  <div className="spinner-border spinner-border-sm text-primary"></div>
                </div>
              ) : alertas.length === 0 ? (
                <div className="px-3 py-3 text-center" style={{ fontSize: '0.82rem', color: 'var(--text-muted-color)' }}>
                  <i className="fa-solid fa-circle-check d-block mb-2" style={{ color: 'var(--success)', fontSize: '1.2rem' }}></i>
                  Todo el inventario está por encima del mínimo.
                </div>
              ) : (
                <div style={{ maxHeight: 280, overflowY: 'auto' }}>
                  {alertas.map(p => (
                    <div key={p.id} className="px-3 py-2 d-flex justify-content-between align-items-center gap-2"
                         style={{ borderBottom: '1px solid var(--border-color)' }}>
                      <div style={{ minWidth: 0 }}>
                        <div style={{ fontSize: '0.82rem', fontWeight: 500, color: 'var(--text-primary)',
                                      whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {p.nombre}
                        </div>
                        <div style={{ fontSize: '0.72rem', color: 'var(--text-muted-color)' }}>{p.sku}</div>
                      </div>
                      <span className="etiqueta etiqueta-danger" style={{ whiteSpace: 'nowrap' }}>
                        {p.stock} / mín. {p.stockMinimo}
                      </span>
                    </div>
                  ))}
                </div>
              )}
              <hr />
              <Link to="/productos" className="item justify-content-center" onClick={() => setPanelAlertas(false)}
                    style={{ color: 'var(--color-primary)', fontWeight: 500 }}>
                Ver catálogo completo →
              </Link>
            </div>
          )}
        </div>

        <div className="position-relative" ref={menuRef}>
          <button type="button" className="navbar-user-btn" onClick={() => setMenuAbierto(a => !a)}>
            <span className="navbar-user-avatar">{iniciales(user?.nombreCompleto || user?.username)}</span>
            <span className="navbar-user-name d-none d-sm-inline">{user?.username}</span>
            <i className="fa-solid fa-chevron-down" style={{ fontSize: '0.6rem', opacity: 0.5 }}></i>
          </button>

          {menuAbierto && (
            <div className="dropdown-panel">
              <div className="px-3 py-2">
                <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                  {user?.nombreCompleto || user?.username}
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted-color)' }}>
                  {rolLegible(user?.rol)}
                </div>
              </div>
              <hr />
              <Link to="/perfil" className="item" onClick={() => setMenuAbierto(false)}>
                <i className="fa-solid fa-user-gear" style={{ opacity: 0.6 }}></i> Mi Perfil
              </Link>
              <hr />
              <button type="button" className="item peligro" onClick={cerrarSesion}>
                <i className="fa-solid fa-power-off"></i> Cerrar Sesión
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
