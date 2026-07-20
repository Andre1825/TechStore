import { useEffect } from 'react'

/**
 * Modal propio (sin Bootstrap JS): overlay + tarjeta centrada, cierra con Escape.
 */
export default function Modal({ abierto, titulo, icono, onCerrar, children, ancho = 520 }) {
  useEffect(() => {
    if (!abierto) return
    const onKey = e => { if (e.key === 'Escape') onCerrar() }
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    }
  }, [abierto, onCerrar])

  if (!abierto) return null

  return (
    <div className="app-modal-overlay" onMouseDown={e => { if (e.target === e.currentTarget) onCerrar() }}>
      <div className="app-modal" style={{ maxWidth: ancho }} role="dialog" aria-modal="true">
        <div className="app-modal-header">
          <h6 className="m-0 d-flex align-items-center gap-2">
            {icono && <i className={`fa-solid ${icono} text-primary`}></i>}
            {titulo}
          </h6>
          <button type="button" className="app-modal-close" onClick={onCerrar} aria-label="Cerrar">
            <i className="fa-solid fa-xmark"></i>
          </button>
        </div>
        <div className="app-modal-body">{children}</div>
      </div>
    </div>
  )
}

export function ConfirmModal({ abierto, titulo, mensaje, textoConfirmar = 'Confirmar', variante = 'danger', onConfirmar, onCerrar }) {
  return (
    <Modal abierto={abierto} titulo={titulo} icono="fa-triangle-exclamation" onCerrar={onCerrar} ancho={430}>
      <p className="mb-4" style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>{mensaje}</p>
      <div className="d-flex justify-content-end gap-2">
        <button type="button" className="btn btn-light" onClick={onCerrar}>Cancelar</button>
        <button type="button" className={`btn btn-${variante}`} onClick={onConfirmar}>{textoConfirmar}</button>
      </div>
    </Modal>
  )
}
