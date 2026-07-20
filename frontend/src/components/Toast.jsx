import { createContext, useCallback, useContext, useRef, useState } from 'react'

const ToastContext = createContext(null)

const ICONOS = {
  success: 'fa-circle-check',
  error: 'fa-circle-xmark',
  warning: 'fa-triangle-exclamation',
  info: 'fa-circle-info',
}

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const idRef = useRef(0)

  const toast = useCallback((tipo, mensaje) => {
    const id = ++idRef.current
    setToasts(t => [...t, { id, tipo, mensaje }])
    setTimeout(() => setToasts(t => t.filter(x => x.id !== id)), 4200)
  }, [])

  const cerrar = id => setToasts(t => t.filter(x => x.id !== id))

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <div className="toast-stack">
        {toasts.map(t => (
          <div key={t.id} className={`toast-item toast-${t.tipo}`}>
            <i className={`fa-solid ${ICONOS[t.tipo] || ICONOS.info}`}></i>
            <span>{t.mensaje}</span>
            <button type="button" onClick={() => cerrar(t.id)} aria-label="Cerrar">
              <i className="fa-solid fa-xmark"></i>
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => useContext(ToastContext)
