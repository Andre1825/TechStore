import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Chart as ChartJS,
  CategoryScale, LinearScale, BarElement, ArcElement,
  PointElement, LineElement, Tooltip, Legend, Filler,
} from 'chart.js'
import { Bar, Doughnut, Line } from 'react-chartjs-2'
import { api } from '../api/client'
import { useTheme } from '../context/ThemeContext'
import PageHeader from '../components/PageHeader'

ChartJS.register(
  CategoryScale, LinearScale, BarElement, ArcElement,
  PointElement, LineElement, Tooltip, Legend, Filler,
)

const soles = n =>
  'S/ ' + Number(n || 0).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

function Kpi({ label, valor, detalle, icono, color, tamano }) {
  return (
    <div className="kpi-card">
      <div className="d-flex justify-content-between align-items-start">
        <div>
          <p className="kpi-label">{label}</p>
          <p className="kpi-value" style={tamano ? { fontSize: tamano } : undefined}>{valor}</p>
          <span className={`etiqueta etiqueta-${color}`}>{detalle}</span>
        </div>
        <div className={`kpi-icon etiqueta-${color}`}>
          <i className={`fa-solid ${icono}`}></i>
        </div>
      </div>
    </div>
  )
}

export default function Dashboard() {
  const { theme } = useTheme()
  const [data, setData] = useState(null)
  const [alertaVisible, setAlertaVisible] = useState(true)

  useEffect(() => {
    api.get('/api/dashboard').then(setData).catch(() => setData(null))
  }, [])

  if (!data) {
    return (
      <div className="cargando-pagina">
        <div className="spinner-border text-primary"></div>
        <span>Cargando dashboard…</span>
      </div>
    )
  }

  const esOscuro = theme === 'dark'
  const textColor = esOscuro ? '#a0aec0' : '#64748b'
  const gridColor = esOscuro ? 'rgba(255,255,255,0.07)' : 'rgba(0,0,0,0.06)'

  const opcionesEjes = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { ticks: { color: textColor }, grid: { color: gridColor } },
      y: { ticks: { color: textColor, precision: 0 }, grid: { color: gridColor }, beginAtZero: true },
    },
  }

  return (
    <>
      <PageHeader titulo="Dashboard" subtitulo="Resumen general del inventario y analíticas" />

      {/* RF-15: Alerta de stock bajo */}
      {data.stockBajoCount > 0 && alertaVisible && (
        <div className="alert alert-warning d-flex align-items-center gap-3 mb-4">
          <i className="fa-solid fa-triangle-exclamation fa-lg"></i>
          <div>
            <strong>{data.stockBajoCount}</strong> producto(s) con stock por debajo del mínimo configurado.
            <Link to="/productos" className="alert-link ms-2">Ver catálogo</Link>
          </div>
          <button type="button" className="btn-close ms-auto" onClick={() => setAlertaVisible(false)}></button>
        </div>
      )}

      {/* KPIs (RF-14) */}
      <div className="row g-3 mb-4">
        <div className="col-12 col-sm-6 col-xl-3">
          <Kpi label="Total Productos" valor={data.totalProductos} icono="fa-box"
               color="primary" detalle={`${data.totalActivos} activos`} />
        </div>
        <div className="col-12 col-sm-6 col-xl-3">
          <Kpi label="Categorías" valor={data.totalCategorias} icono="fa-folder-open"
               color="info" detalle="familias de productos" />
        </div>
        <div className="col-12 col-sm-6 col-xl-3">
          <Kpi label="Valor del Inventario" valor={soles(data.valorInventario)} tamano="1.4rem"
               icono="fa-sack-dollar" color="success" detalle="productos activos" />
        </div>
        <div className="col-12 col-sm-6 col-xl-3">
          <Kpi label="Movimientos" valor={Number(data.totalEntradas) + Number(data.totalSalidas)}
               icono="fa-right-left" color="secondary"
               detalle={`${data.totalEntradas} entradas / ${data.totalSalidas} salidas`} />
        </div>
      </div>

      {/* Gráficos fila 1 */}
      <div className="row g-3 mb-3">
        <div className="col-12 col-lg-7">
          <div className="card h-100">
            <div className="card-body">
              <h6 className="card-titulo">
                <i className="fa-solid fa-chart-bar me-2 text-primary"></i>Productos Activos por Categoría
              </h6>
              <div style={{ position: 'relative', height: 210 }}>
                <Bar
                  data={{
                    labels: data.categoriaLabels,
                    datasets: [{
                      label: 'Productos activos',
                      data: data.categoriaCounts,
                      backgroundColor: 'rgba(13, 110, 253, 0.75)',
                      borderRadius: 6,
                      borderSkipped: false,
                    }],
                  }}
                  options={opcionesEjes}
                />
              </div>
            </div>
          </div>
        </div>

        <div className="col-12 col-lg-5">
          <div className="card h-100">
            <div className="card-body d-flex flex-column">
              <h6 className="card-titulo">
                <i className="fa-solid fa-chart-pie me-2 text-info"></i>Estado del Inventario
              </h6>
              <div className="d-flex justify-content-center align-items-center flex-grow-1">
                <div style={{ position: 'relative', width: 190, height: 190 }}>
                  <Doughnut
                    data={{
                      labels: ['Activos', 'Inactivos'],
                      datasets: [{
                        data: [data.totalActivos, data.totalInactivos],
                        backgroundColor: ['rgba(25, 135, 84, 0.8)', 'rgba(220, 53, 69, 0.7)'],
                        borderWidth: 0,
                        hoverOffset: 6,
                      }],
                    }}
                    options={{
                      responsive: true,
                      maintainAspectRatio: false,
                      cutout: '72%',
                      plugins: { legend: { display: false } },
                    }}
                  />
                </div>
              </div>
              <div className="d-flex justify-content-center gap-4 mt-3">
                <div className="text-center">
                  <div className="fw-bold" style={{ color: 'var(--success)' }}>{data.totalActivos}</div>
                  <div style={{ fontSize: '0.78rem', color: 'var(--text-muted-color)' }}>Activos</div>
                </div>
                <div className="text-center">
                  <div className="fw-bold" style={{ color: 'var(--danger)' }}>{data.totalInactivos}</div>
                  <div style={{ fontSize: '0.78rem', color: 'var(--text-muted-color)' }}>Inactivos</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Gráfico fila 2 */}
      <div className="card mb-3">
        <div className="card-body">
          <h6 className="card-titulo">
            <i className="fa-solid fa-chart-line me-2 text-success"></i>Entradas y Salidas — Últimos 6 Meses
          </h6>
          <div style={{ position: 'relative', height: 200 }}>
            <Line
              data={{
                labels: data.mesesLabels,
                datasets: [
                  {
                    label: 'Entradas',
                    data: data.entradasMes,
                    borderColor: 'rgba(25, 135, 84, 0.9)',
                    backgroundColor: 'rgba(25, 135, 84, 0.1)',
                    borderWidth: 2, pointRadius: 4, fill: true, tension: 0.35,
                  },
                  {
                    label: 'Salidas',
                    data: data.salidasMes,
                    borderColor: 'rgba(220, 53, 69, 0.9)',
                    backgroundColor: 'rgba(220, 53, 69, 0.1)',
                    borderWidth: 2, pointRadius: 4, fill: true, tension: 0.35,
                  },
                ],
              }}
              options={{
                ...opcionesEjes,
                plugins: {
                  legend: { position: 'top', labels: { color: textColor, boxWidth: 12, padding: 16 } },
                  tooltip: { mode: 'index', intersect: false },
                },
              }}
            />
          </div>
        </div>
      </div>

      {/* Stock bajo + Últimos movimientos */}
      <div className="row g-3 mb-4">
        {data.stockBajoCount > 0 && (
          <div className="col-12 col-lg-6">
            <div className="card h-100">
              <div className="card-body">
                <h6 className="card-titulo">
                  <i className="fa-solid fa-triangle-exclamation me-2 text-warning"></i>Productos con Stock Bajo
                </h6>
                <div className="table-responsive">
                  <table className="table table-sm">
                    <thead>
                      <tr>
                        <th>Producto</th>
                        <th className="text-center">Stock Actual</th>
                        <th className="text-center">Mínimo</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.productosStockBajo.map(p => (
                        <tr key={p.id}>
                          <td>
                            <div className="fw-medium" style={{ fontSize: '0.85rem' }}>{p.nombre}</div>
                            <div style={{ fontSize: '0.76rem', color: 'var(--text-muted-color)' }}>{p.sku}</div>
                          </td>
                          <td className="text-center fw-bold" style={{ color: 'var(--danger)' }}>{p.stock}</td>
                          <td className="text-center" style={{ color: 'var(--text-muted-color)' }}>{p.stockMinimo}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        )}

        <div className={data.stockBajoCount > 0 ? 'col-12 col-lg-6' : 'col-12'}>
          <div className="card h-100">
            <div className="card-body">
              <h6 className="card-titulo">
                <i className="fa-solid fa-clock-rotate-left me-2 text-secondary"></i>Últimos Movimientos
              </h6>
              {data.ultimosMovimientos.length === 0 ? (
                <div className="tabla-vacia">
                  <i className="fa-solid fa-inbox"></i>
                  Sin movimientos registrados.
                </div>
              ) : (
                <>
                  <div className="table-responsive">
                    <table className="table table-sm">
                      <thead>
                        <tr>
                          <th>Producto</th>
                          <th className="text-center">Tipo</th>
                          <th className="text-center">Cant.</th>
                          <th className="text-end">Fecha</th>
                        </tr>
                      </thead>
                      <tbody>
                        {data.ultimosMovimientos.map(mov => (
                          <tr key={mov.id}>
                            <td style={{ fontSize: '0.84rem' }}>{mov.producto}</td>
                            <td className="text-center">
                              <span className={`etiqueta etiqueta-pill etiqueta-${mov.tipo === 'ENTRADA' ? 'success' : 'danger'}`}>
                                {mov.tipo}
                              </span>
                            </td>
                            <td className="text-center fw-semibold">{mov.cantidad}</td>
                            <td className="text-end" style={{ fontSize: '0.78rem', color: 'var(--text-muted-color)' }}>
                              {new Date(mov.fecha).toLocaleString('es-PE', {
                                day: '2-digit', month: '2-digit', year: '2-digit',
                                hour: '2-digit', minute: '2-digit',
                              })}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                  <div className="mt-2 text-end">
                    <Link to="/movimientos" className="enlace-simple">Ver Kardex completo →</Link>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Accesos rápidos + Estado del sistema */}
      <div className="row g-3">
        <div className="col-12 col-md-6">
          <div className="card h-100">
            <div className="card-body">
              <h6 className="card-titulo">
                <i className="fa-solid fa-bolt me-2 text-warning"></i>Accesos Rápidos
              </h6>
              <div className="d-flex flex-wrap gap-2">
                <Link to="/productos" className="btn btn-primary btn-sm rounded-pill px-3">
                  <i className="fa-solid fa-box me-1"></i> Productos
                </Link>
                <Link to="/entradas" className="btn btn-sm rounded-pill px-3 etiqueta-success"
                      style={{ border: '1px solid var(--border-color)' }}>
                  <i className="fa-solid fa-truck-ramp-box me-1"></i> Entradas
                </Link>
                <Link to="/salidas" className="btn btn-sm rounded-pill px-3 etiqueta-danger"
                      style={{ border: '1px solid var(--border-color)' }}>
                  <i className="fa-solid fa-dolly me-1"></i> Salidas
                </Link>
                <Link to="/movimientos" className="btn btn-sm rounded-pill px-3 etiqueta-info"
                      style={{ border: '1px solid var(--border-color)' }}>
                  <i className="fa-solid fa-list-check me-1"></i> Kardex
                </Link>
              </div>
            </div>
          </div>
        </div>
        <div className="col-12 col-md-6">
          <div className="card h-100">
            <div className="card-body">
              <h6 className="card-titulo">
                <i className="fa-solid fa-circle-info me-2 text-primary"></i>Estado del Sistema
              </h6>
              <div className="d-flex flex-column gap-2">
                <div className="d-flex justify-content-between align-items-center">
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                    <i className="fa-solid fa-database me-2" style={{ color: 'var(--text-muted-color)' }}></i>Base de datos
                  </span>
                  <span className="etiqueta etiqueta-success">Conectada</span>
                </div>
                <div className="d-flex justify-content-between align-items-center">
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                    <i className="fa-solid fa-shield-halved me-2" style={{ color: 'var(--text-muted-color)' }}></i>Seguridad
                  </span>
                  <span className="etiqueta etiqueta-success">Activa</span>
                </div>
                <div className="d-flex justify-content-between align-items-center">
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                    <i className="fa-solid fa-users me-2" style={{ color: 'var(--text-muted-color)' }}></i>Usuarios activos
                  </span>
                  <span className="etiqueta etiqueta-primary">{data.totalUsuarios}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
