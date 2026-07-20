import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import Navbar from './Navbar'

export default function Layout() {
  const [sidebarOculto, setSidebarOculto] = useState(false)
  const [abiertoMovil, setAbiertoMovil] = useState(false)

  const toggle = () => {
    if (window.innerWidth <= 768) setAbiertoMovil(a => !a)
    else setSidebarOculto(o => !o)
  }

  return (
    <div className="app-wrapper">
      <Sidebar
        oculto={sidebarOculto}
        abiertoMovil={abiertoMovil}
        onCerrarMovil={() => setAbiertoMovil(false)}
      />
      {abiertoMovil && <div className="sidebar-overlay" onClick={() => setAbiertoMovil(false)}></div>}

      <div className="app-content">
        <Navbar onToggleSidebar={toggle} />
        <main className="container-fluid px-4 py-4">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
