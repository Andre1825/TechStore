import { useState } from 'react'
import { NavLink, Link, useLocation } from 'react-router-dom'
import { useAuth, P } from '../context/AuthContext'

function Submenu({ icono, titulo, rutas, children }) {
  const location = useLocation()
  const activo = rutas.some(r => location.pathname.startsWith(r))
  const [abierto, setAbierto] = useState(activo)

  return (
    <li>
      <button type="button" className="sidebar-link" onClick={() => setAbierto(a => !a)}>
        <i className={`fa-solid ${icono} fa-fw`}></i>
        <span>{titulo}</span>
        <i className={`fa-solid fa-chevron-down sidebar-chevron ${abierto ? 'abierto' : ''}`}></i>
      </button>
      <ul className="list-unstyled sidebar-submenu" style={{ maxHeight: abierto ? '300px' : 0 }}>
        {children}
      </ul>
    </li>
  )
}

function Item({ a, icono, children, sub = false, onNavegar }) {
  return (
    <li>
      <NavLink
        to={a}
        end={a === '/'}
        onClick={onNavegar}
        className={({ isActive }) =>
          `sidebar-link ${sub ? 'sidebar-sublink' : ''} ${isActive ? 'activo' : ''}`
        }
      >
        {icono && <i className={`fa-solid ${icono} fa-fw`}></i>}
        <span>{children}</span>
      </NavLink>
    </li>
  )
}

export default function Sidebar({ oculto, abiertoMovil, onCerrarMovil }) {
  const { tienePermiso } = useAuth()

  // Gating por permiso de módulo (RF-03)
  const puedeCategorias = tienePermiso(P.GESTIONAR_CATEGORIAS)
  const puedeMarcas = tienePermiso(P.GESTIONAR_MARCAS)
  const puedeAlmacen = puedeCategorias || puedeMarcas
  const puedeEntradas = tienePermiso(P.REGISTRAR_ENTRADAS)
  const puedeSalidas = tienePermiso(P.REGISTRAR_SALIDAS)
  const puedeMovimientos = tienePermiso(P.VER_MOVIMIENTOS)
  const puedeOperaciones = puedeEntradas || puedeSalidas || puedeMovimientos
  const puedeUsuarios = tienePermiso(P.GESTIONAR_USUARIOS)
  const puedeRoles = tienePermiso(P.GESTIONAR_ROLES)
  const puedeAdmin = puedeUsuarios || puedeRoles

  return (
    <nav className={`sidebar shadow-lg ${oculto ? 'oculto' : ''} ${abiertoMovil ? 'abierto-movil' : ''}`}>
      <div className="sidebar-header">
        <Link to="/" className="sidebar-brand" onClick={onCerrarMovil}>
          <div className="sidebar-brand-icon">
            <i className="fa-solid fa-boxes-stacked"></i>
          </div>
          <span className="sidebar-brand-name">TechStore</span>
        </Link>
        <button type="button" className="sidebar-close-btn d-md-none" onClick={onCerrarMovil}>
          <i className="fa-solid fa-xmark"></i>
        </button>
      </div>

      <ul className="list-unstyled py-2 m-0">
        <Item a="/" icono="fa-chart-pie" onNavegar={onCerrarMovil}>Escritorio</Item>

        {puedeAlmacen ? (
          <Submenu icono="fa-box" titulo="Almacén" rutas={['/categorias', '/marcas', '/productos']}>
            {puedeCategorias && <Item a="/categorias" sub onNavegar={onCerrarMovil}>Categorías</Item>}
            {puedeMarcas && <Item a="/marcas" sub onNavegar={onCerrarMovil}>Marcas</Item>}
            <Item a="/productos" sub onNavegar={onCerrarMovil}>Productos</Item>
          </Submenu>
        ) : (
          <Item a="/productos" icono="fa-box" onNavegar={onCerrarMovil}>Productos</Item>
        )}

        {puedeOperaciones && (
          <Submenu icono="fa-right-left" titulo="Operaciones" rutas={['/entradas', '/salidas', '/movimientos']}>
            {puedeEntradas && <Item a="/entradas" sub onNavegar={onCerrarMovil}>Entradas</Item>}
            {puedeSalidas && <Item a="/salidas" sub onNavegar={onCerrarMovil}>Salidas</Item>}
            {puedeMovimientos && <Item a="/movimientos" sub onNavegar={onCerrarMovil}>Kardex</Item>}
          </Submenu>
        )}

        {puedeAdmin && (
          <>
            <li><p className="sidebar-section-label">Administración</p></li>
            <Submenu icono="fa-shield-halved" titulo="Seguridad" rutas={['/usuarios', '/roles']}>
              {puedeUsuarios && <Item a="/usuarios" sub onNavegar={onCerrarMovil}>Usuarios</Item>}
              {puedeRoles && <Item a="/roles" sub onNavegar={onCerrarMovil}>Roles y Permisos</Item>}
            </Submenu>
          </>
        )}
      </ul>
    </nav>
  )
}
