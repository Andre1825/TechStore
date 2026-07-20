import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'

export default function Login() {
  const { login } = useAuth()
  const { theme, toggleTheme } = useTheme()
  const navigate = useNavigate()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  const onSubmit = async e => {
    e.preventDefault()
    setError(null)
    setEnviando(true)
    try {
      await login(username, password)
      navigate('/', { replace: true })
    } catch (err) {
      // RF-02: Mensaje específico si la cuenta está bloqueada por intentos fallidos
      setError(err.data?.mensaje || 'Usuario o contraseña incorrectos.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="login-body">
      <button type="button" className="btn-icono login-theme-btn" onClick={toggleTheme} title="Cambiar tema">
        <i className={`fa-solid ${theme === 'dark' ? 'fa-sun' : 'fa-moon'}`}></i>
      </button>

      <div className="login-card">
        <div className="login-logo-box">
          <i className="fa-solid fa-boxes-stacked"></i>
        </div>
        <h4 className="login-title">TechStore</h4>
        <p className="login-subtitle">Sistema de Control de Inventarios</p>

        {error && (
          <div className="alert alert-danger text-center small py-2 mb-3">
            <i className="fa-solid fa-circle-exclamation me-1"></i> {error}
          </div>
        )}

        <form onSubmit={onSubmit}>
          <div className="mb-3">
            <label htmlFor="username" className="form-label">Usuario</label>
            <div className="input-group">
              <span className="input-group-text"><i className="fa-solid fa-user"></i></span>
              <input
                type="text"
                id="username"
                className="form-control"
                placeholder="Tu nombre de usuario"
                value={username}
                onChange={e => setUsername(e.target.value)}
                required
                autoFocus
              />
            </div>
          </div>
          <div className="mb-4">
            <label htmlFor="password" className="form-label">Contraseña</label>
            <div className="input-group">
              <span className="input-group-text"><i className="fa-solid fa-lock"></i></span>
              <input
                type="password"
                id="password"
                className="form-control"
                placeholder="••••••••"
                value={password}
                onChange={e => setPassword(e.target.value)}
                required
              />
            </div>
          </div>
          <button type="submit" className="btn btn-login" disabled={enviando}>
            {enviando
              ? <><span className="spinner-border spinner-border-sm me-2"></span>Verificando…</>
              : <>Ingresar al Sistema <i className="fa-solid fa-arrow-right-to-bracket ms-2"></i></>}
          </button>
        </form>
      </div>
    </div>
  )
}
