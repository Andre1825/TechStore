export default function PageHeader({ titulo, subtitulo, children }) {
  return (
    <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-4">
      <div>
        <h1 className="page-header-title">{titulo}</h1>
        {subtitulo && <p className="page-header-subtitle">{subtitulo}</p>}
      </div>
      {children && <div className="d-flex gap-2">{children}</div>}
    </div>
  )
}
