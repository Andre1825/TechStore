// Cliente HTTP central: JSON + sesión (cookies) + manejo uniforme de errores.

class ApiError extends Error {
  constructor(status, data) {
    super((data && data.mensaje) || `Error HTTP ${status}`)
    this.status = status
    this.data = data
  }
}

async function request(url, options = {}) {
  const config = {
    credentials: 'same-origin',
    headers: {},
    ...options,
  }
  if (options.body !== undefined) {
    config.headers['Content-Type'] = 'application/json'
    config.body = JSON.stringify(options.body)
  }

  const res = await fetch(url, config)

  // Sesión expirada o sin permisos: la SPA reacciona globalmente
  if (res.status === 401 && !url.startsWith('/api/auth/')) {
    window.dispatchEvent(new CustomEvent('auth:expired'))
  }

  let data = null
  const text = await res.text()
  if (text) {
    try { data = JSON.parse(text) } catch { data = null }
  }

  if (!res.ok) throw new ApiError(res.status, data)
  return data
}

export const api = {
  get: (url) => request(url),
  post: (url, body) => request(url, { method: 'POST', body }),
  put: (url, body) => request(url, { method: 'PUT', body }),
  delete: (url) => request(url, { method: 'DELETE' }),
}

export { ApiError }
