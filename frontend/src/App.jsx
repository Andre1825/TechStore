import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth, P } from './context/AuthContext'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Productos from './pages/Productos'
import Categorias from './pages/Categorias'
import Marcas from './pages/Marcas'
import Entradas from './pages/Entradas'
import Salidas from './pages/Salidas'
import Movimientos from './pages/Movimientos'
import Usuarios from './pages/Usuarios'
import Roles from './pages/Roles'
import Perfil from './pages/Perfil'
import AccesoDenegado from './pages/AccesoDenegado'

function PantallaCarga() {
  return (
    <div className="cargando-pagina" style={{ minHeight: '100vh' }}>
      <div className="spinner-border text-primary" role="status"></div>
      <span>Cargando…</span>
    </div>
  )
}

// Protege una ruta: exige sesión y, opcionalmente, un permiso específico (RF-03)
function Protegida({ permiso, children }) {
  const { user, cargando, tienePermiso } = useAuth()
  if (cargando) return <PantallaCarga />
  if (!user) return <Navigate to="/login" replace />
  if (permiso && !tienePermiso(permiso)) return <Navigate to="/acceso-denegado" replace />
  return children
}

export default function App() {
  const { user, cargando } = useAuth()

  return (
    <Routes>
      <Route
        path="/login"
        element={cargando ? <PantallaCarga /> : user ? <Navigate to="/" replace /> : <Login />}
      />

      <Route element={<Protegida><Layout /></Protegida>}>
        <Route path="/" element={<Protegida permiso={P.VER_DASHBOARD}><Dashboard /></Protegida>} />
        <Route path="/productos" element={<Productos />} />
        <Route
          path="/categorias"
          element={<Protegida permiso={P.GESTIONAR_CATEGORIAS}><Categorias /></Protegida>}
        />
        <Route
          path="/marcas"
          element={<Protegida permiso={P.GESTIONAR_MARCAS}><Marcas /></Protegida>}
        />
        <Route
          path="/entradas"
          element={<Protegida permiso={P.REGISTRAR_ENTRADAS}><Entradas /></Protegida>}
        />
        <Route
          path="/salidas"
          element={<Protegida permiso={P.REGISTRAR_SALIDAS}><Salidas /></Protegida>}
        />
        <Route
          path="/movimientos"
          element={<Protegida permiso={P.VER_MOVIMIENTOS}><Movimientos /></Protegida>}
        />
        <Route
          path="/usuarios"
          element={<Protegida permiso={P.GESTIONAR_USUARIOS}><Usuarios /></Protegida>}
        />
        <Route
          path="/roles"
          element={<Protegida permiso={P.GESTIONAR_ROLES}><Roles /></Protegida>}
        />
        <Route path="/perfil" element={<Perfil />} />
        <Route path="/acceso-denegado" element={<AccesoDenegado />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
