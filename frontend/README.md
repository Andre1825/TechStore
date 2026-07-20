# TechStore — Frontend React

SPA en React (Vite) que consume el API REST del backend Spring Boot. Reemplaza las vistas Thymeleaf manteniendo todos los requisitos funcionales (RF-01 a RF-15).

## Stack

- **React 19** + **Vite** — SPA con recarga instantánea
- **React Router 7** — enrutamiento con rutas protegidas por rol
- **Chart.js + react-chartjs-2** — gráficos del dashboard
- **Bootstrap 5 (CSS)** + sistema de diseño propio con modo claro/oscuro
- **Font Awesome 6** — iconografía

## Cómo ejecutar (desarrollo)

Se necesitan 3 procesos:

```bash
# 1. Base de datos (PostgreSQL en Docker)
docker compose up -d          # desde la raíz del proyecto

# 2. Backend Spring Boot (puerto 8080)
./mvnw spring-boot:run        # desde la raíz del proyecto

# 3. Frontend React (puerto 5173)
cd frontend
npm install                   # solo la primera vez
npm run dev
```

Abrir **http://localhost:5173** — usuario inicial: `admin` / `123456`.

El dev server de Vite hace proxy de `/api` y `/export` hacia `localhost:8080`, por lo que la SPA comparte la sesión HTTP de Spring Security sin configurar CORS.

## Build de producción

```bash
cd frontend
npm run build   # genera frontend/dist/
```

## Arquitectura

```
src/
├── api/client.js          # fetch wrapper: JSON + sesión + errores
├── context/
│   ├── AuthContext.jsx    # sesión, login/logout, roles (RF-01/02/03)
│   └── ThemeContext.jsx   # modo claro/oscuro persistente
├── components/
│   ├── Layout.jsx         # sidebar + navbar + <Outlet/>
│   ├── Sidebar.jsx        # menú con secciones según rol
│   ├── Navbar.jsx         # tema, alertas stock bajo (RF-15), usuario
│   ├── Modal.jsx          # modal propio + ConfirmModal
│   ├── Toast.jsx          # notificaciones
│   └── RegistroMovimientos.jsx  # página compartida Entradas/Salidas
└── pages/                 # Dashboard, Productos, Categorías, Marcas,
                           # Entradas, Salidas, Kardex, Usuarios, Roles, Perfil
```

## API REST del backend

Los controladores están en `src/main/java/.../controller/api/`:

| Recurso | Endpoints |
|---|---|
| Autenticación | `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me` |
| Dashboard | `GET /api/dashboard` (RF-14/15) |
| Productos | `GET/POST /api/productos`, `PUT /api/productos/{id}`, `POST .../estado`, `DELETE`, `GET .../validar-sku`, `GET .../{id}/precios` (RF-05..08) |
| Categorías | `GET/POST /api/categorias`, `PUT/{id}`, `POST /{id}/estado`, `DELETE /{id}` (RF-04) |
| Marcas | igual que categorías |
| Kardex | `GET/POST /api/entradas`, `GET/POST /api/salidas`, `GET /api/movimientos` (RF-09..13) |
| Usuarios | `GET/POST /api/usuarios`, `PUT /{id}`, `POST /{id}/bloqueo` (RF-02/03, solo ADMIN) |
| Roles | `GET/POST /api/roles`, `PUT /{id}` (solo ADMIN) |

La seguridad usa la misma sesión HTTP de Spring Security: el API responde `401/403` en JSON y las reglas por rol replican las del sistema original.
