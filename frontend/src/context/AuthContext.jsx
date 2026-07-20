import { createContext, useContext, useEffect, useState, useCallback } from 'react'
import { api } from '../api/client'

const AuthContext = createContext(null)

// RF-03: Catálogo fijo de permisos por módulo (debe coincidir con el enum Permiso del backend)
export const PERMISOS = [
  { clave: 'VER_DASHBOARD', etiqueta: 'Ver dashboard' },
  { clave: 'GESTIONAR_PRODUCTOS', etiqueta: 'Gestionar productos' },
  { clave: 'GESTIONAR_CATEGORIAS', etiqueta: 'Gestionar categorías' },
  { clave: 'GESTIONAR_MARCAS', etiqueta: 'Gestionar marcas' },
  { clave: 'REGISTRAR_ENTRADAS', etiqueta: 'Registrar entradas' },
  { clave: 'REGISTRAR_SALIDAS', etiqueta: 'Registrar salidas' },
  { clave: 'VER_MOVIMIENTOS', etiqueta: 'Ver movimientos (Kardex)' },
  { clave: 'GESTIONAR_USUARIOS', etiqueta: 'Gestionar usuarios' },
  { clave: 'GESTIONAR_ROLES', etiqueta: 'Gestionar roles' },
]

// Acceso rápido por clave: P.GESTIONAR_PRODUCTOS === 'GESTIONAR_PRODUCTOS'
export const P = Object.fromEntries(PERMISOS.map(p => [p.clave, p.clave]))

// Etiqueta legible de una clave de permiso
export function permisoLegible(clave) {
  return PERMISOS.find(p => p.clave === clave)?.etiqueta || clave
}

// El rol del usuario ahora es su nombre legible; passthrough con guardia de nulos
export function rolLegible(rol) {
  return rol || '—'
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [cargando, setCargando] = useState(true)

  // Restaurar sesión al cargar/recargar la SPA
  useEffect(() => {
    api.get('/api/auth/me')
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setCargando(false))
  }, [])

  // Si el backend responde 401 en cualquier llamada, cerrar sesión local
  useEffect(() => {
    const onExpired = () => setUser(null)
    window.addEventListener('auth:expired', onExpired)
    return () => window.removeEventListener('auth:expired', onExpired)
  }, [])

  const login = useCallback(async (username, password) => {
    const me = await api.post('/api/auth/login', { username, password })
    setUser(me)
    return me
  }, [])

  const logout = useCallback(async () => {
    try { await api.post('/api/auth/logout') } catch { /* la sesión local se limpia igual */ }
    setUser(null)
  }, [])

  // RF-03: true si el usuario tiene al menos uno de los permisos indicados
  const tienePermiso = useCallback(
    (...claves) => !!user && claves.some(c => (user.permisos || []).includes(c)),
    [user],
  )

  return (
    <AuthContext.Provider value={{ user, cargando, login, logout, tienePermiso }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
