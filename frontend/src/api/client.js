// Cliente HTTP central: JSON + sesión (cookies) + manejo uniforme de errores.

class ApiError extends Error {
  constructor(status, data) {
    super((data && data.mensaje) || `Error HTTP ${status}`)
    this.status = status
    this.data = data
  }
}

let csrfPromise = null

function obtenerCsrf() {
  if (!csrfPromise) {
    csrfPromise = fetch('/api/auth/csrf', { credentials: 'same-origin', cache: 'no-store' })
      .then(async res => {
        if (res.status === 401) window.dispatchEvent(new CustomEvent('auth:expired'))
        if (!res.ok) throw new ApiError(res.status, await res.json().catch(() => null))
        return res.json()
      })
      .catch(err => { csrfPromise = null; throw err })
  }
  return csrfPromise
}

async function request(url, options = {}, reintentarCsrf = true) {
  const config = {
    credentials: 'same-origin',
    ...options,
    headers: { ...options.headers },
  }

  const modifica = !['GET', 'HEAD', 'OPTIONS'].includes((config.method || 'GET').toUpperCase())
  if (modifica) {
    const csrf = await obtenerCsrf()
    config.headers[csrf.headerName] = csrf.token
  }
  if (options.body !== undefined) {
    config.headers['Content-Type'] = 'application/json'
    config.body = JSON.stringify(options.body)
  }

  const res = await fetch(url, config)

  // Sesión expirada o sin permisos: la SPA reacciona globalmente
  if (res.status === 401 && url !== '/api/auth/login') {
    csrfPromise = null
    window.dispatchEvent(new CustomEvent('auth:expired'))
  }

  let data = null
  const text = await res.text()
  if (text) {
    try { data = JSON.parse(text) } catch { data = null }
  }

  // El servidor rechaza CSRF antes de ejecutar la operación: es seguro renovar y reintentar una vez.
  if (res.status === 403 && data?.error === 'csrf' && modifica && reintentarCsrf) {
    csrfPromise = null
    return request(url, options, false)
  }
  if (!res.ok) throw new ApiError(res.status, data)
  if (url === '/api/auth/login' || url === '/api/auth/logout' || url === '/api/auth/password') {
    csrfPromise = null
  }
  return data
}

export const api = {
  get: (url) => request(url),
  post: (url, body) => request(url, { method: 'POST', body }),
  put: (url, body) => request(url, { method: 'PUT', body }),
  delete: (url) => request(url, { method: 'DELETE' }),
}

export { ApiError }
