# STG Inventory Control System

> Sistema web de **gestión y control de inventarios** para la empresa **STG Technology & Logistics S.A.C.**

Arquitectura desacoplada: **SPA en React** (frontend) + **API REST en Spring Boot** (backend) sobre **PostgreSQL**.

<p>
  <img alt="Java" src="https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-4-6DB33F?logo=springboot&logoColor=white">
  <img alt="React" src="https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black">
  <img alt="Vite" src="https://img.shields.io/badge/Vite-6-646CFF?logo=vite&logoColor=white">
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql&logoColor=white">
  <img alt="License" src="https://img.shields.io/badge/Licencia-Académica-lightgrey">
</p>

---

## 📑 Tabla de contenidos

- [Descripción](#-descripción)
- [Arquitectura](#-arquitectura)
- [Stack tecnológico](#-stack-tecnológico)
- [Prerequisitos](#-prerequisitos)
- [Puesta en marcha (desarrollo)](#-puesta-en-marcha-desarrollo)
- [Build de producción](#-build-de-producción)
- [Usuario por defecto](#-usuario-por-defecto)
- [API REST](#-api-rest-resumen)
- [Estructura del proyecto](#-estructura-del-proyecto)
- [Características destacadas](#-características-destacadas)
- [Tests](#-tests)
- [Despliegue](#-despliegue)
- [Autores](#-autores)
- [Licencia](#-licencia)

---

## 📋 Descripción

Aplicación que optimiza el control de inventarios de una tienda de tecnología, garantizando:

- ✅ **Trazabilidad completa** de entradas y salidas de mercadería.
- ✅ **Registro inalterable** en Kardex para auditoría.
- ✅ **Precisión del stock** en tiempo real.
- ✅ **Alertas automáticas** de reabastecimiento (stock bajo).
- ✅ **Gestión de usuarios** con roles (Administrador, Almacenero, Vendedor).

---

## 🏗️ Arquitectura

```
┌──────────────────────┐        /api/*  (JSON)        ┌──────────────────────────┐
│   Frontend (React)   │ ───────────────────────────► │   Backend (Spring Boot)  │
│   Vite · puerto 5173 │ ◄─────────────────────────── │   API REST · puerto 8080 │
└──────────────────────┘        sesión (cookie)       └────────────┬─────────────┘
                                                                    │ JPA / Hibernate
                                                                    ▼
                                                          ┌──────────────────┐
                                                          │   PostgreSQL 15  │
                                                          │   (Docker)       │
                                                          └──────────────────┘
```

- **React** renderiza toda la interfaz en el navegador.
- **Spring Boot** expone únicamente endpoints REST (`/api/*`) que devuelven JSON.
- La autenticación usa **sesión HTTP + cookies** (Spring Security), compartida entre login y llamadas del SPA.
- En desarrollo, Vite hace **proxy** de `/api` y `/export` hacia el backend (sin CORS).
- En producción, el frontend se compila y se empaqueta **dentro del mismo `.jar`** (ver [Build de producción](#-build-de-producción)).

---

## 🛠️ Stack tecnológico

### Backend
- **Java 21** · **Spring Boot 4**
- **Spring Web** (API REST) · **Spring Security** (auth, roles, BCrypt)
- **Spring Data JPA** / Hibernate
- **PostgreSQL 15**
- **Apache POI** (exportación a Excel `.xlsx`)
- **Maven** (con wrapper `./mvnw`)

### Frontend
- **React 19** · **Vite 6**
- **React Router 7** (navegación SPA con rutas protegidas por rol)
- **Bootstrap 5** · **Font Awesome 6** (estilos e iconos)
- **Chart.js 4** + **react-chartjs-2** (gráficas del dashboard)

---

## 📦 Prerequisitos

| Herramienta | Versión mínima | Notas |
|-------------|----------------|-------|
| Java (JDK)  | 21             | Para el backend |
| Node.js + npm | 18            | Para el frontend |
| Docker      | —              | Para PostgreSQL (o una instancia local de PostgreSQL 15) |
| Maven       | 3.9            | Opcional: el wrapper `./mvnw` ya viene incluido |

---

## 🚀 Puesta en marcha (desarrollo)

### 1. Clonar el repositorio
```bash
git clone https://github.com/swiftdeskk/stg-inventory-control.git
cd stg-inventory-control
```

### 2. Levantar la base de datos (Docker)
```bash
docker compose up -d
```
Crea PostgreSQL con la base `techstore_db` (usuario `admin_techstore` / `password123`).
La configuración vive en [`src/main/resources/application.properties`](src/main/resources/application.properties).

### 3. Levantar el backend (API REST — puerto 8080)
```bash
./mvnw spring-boot:run
```
En el primer arranque se crean los roles y el usuario administrador por defecto.

### 4. Levantar el frontend (React — puerto 5173)
```bash
cd frontend
npm install
npm run dev
```

### 5. Abrir la aplicación
Navega a **http://localhost:5173**

---

## 🏭 Build de producción

Un único comando compila el frontend React y lo empaqueta **dentro del `.jar`** del backend (frontend + API autocontenidos):

```bash
./mvnw clean package -Pprod -DskipTests
```

Genera `target/tech-store-project-0.0.1-SNAPSHOT.jar`. Para ejecutarlo:

```bash
java -jar target/tech-store-project-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

> El perfil `prod` ([`application-prod.properties`](src/main/resources/application-prod.properties)) lee las credenciales de la BD desde **variables de entorno** (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) y endurece la cookie de sesión (HTTPS-only). **Nunca** escribas contraseñas reales en el repositorio.

---

## 👤 Usuario por defecto

Al iniciar el backend por primera vez se crea automáticamente:

| Usuario | Contraseña | Rol   |
|---------|------------|-------|
| `admin` | `123456`   | ADMIN |

> ⚠️ **Cambia esta contraseña inmediatamente** tras el primer login en cualquier entorno accesible.

Los usuarios **Almacenero** y **Vendedor** se crean desde el módulo *Usuarios* con la cuenta de administrador.

---

## 🔌 API REST (resumen)

| Recurso        | Endpoints principales                                                        | Acceso                          |
|----------------|------------------------------------------------------------------------------|---------------------------------|
| Autenticación  | `POST /api/auth/login` · `POST /api/auth/logout` · `GET /api/auth/me`        | Público / autenticado           |
| Dashboard      | `GET /api/dashboard` · `GET /api/dashboard/stock-bajo[/count]`               | Autenticado                     |
| Categorías     | `GET/POST/PUT/DELETE /api/categorias` · `POST /api/categorias/{id}/estado`   | ADMIN, ALMACENERO               |
| Marcas         | `GET/POST/PUT/DELETE /api/marcas` · `POST /api/marcas/{id}/estado`           | ADMIN, ALMACENERO               |
| Productos      | `GET /api/productos[/activos]` · `POST/PUT/DELETE` · `GET /validar-sku`      | Lectura: autenticado · Escritura: ADMIN, ALMACENERO |
| Entradas       | `GET/POST /api/entradas`                                                     | ADMIN, ALMACENERO               |
| Salidas        | `GET/POST /api/salidas`                                                      | ADMIN, ALMACENERO, VENDEDOR     |
| Movimientos    | `GET /api/movimientos`                                                       | Autenticado                     |
| Usuarios       | `GET/POST/PUT /api/usuarios` · `POST /api/usuarios/{id}/bloqueo`             | ADMIN                           |
| Roles          | `GET /api/roles[/activos]` · `POST/PUT`                                      | ADMIN                           |
| Exportación    | `GET /export/productos.xlsx` · `GET /export/movimientos.xlsx`               | Autenticado                     |

Respuestas de error uniformes en JSON: `{ "mensaje": "..." }` con el código HTTP correspondiente (400/401/403/500).

---

## 📂 Estructura del proyecto

```
stg-inventory-control/
├── docker-compose.yml              # PostgreSQL para desarrollo
├── pom.xml                         # Backend (Maven) + perfil 'prod'
├── mvnw / mvnw.cmd                 # Maven Wrapper
├── deploy/                         # Guía y plantilla de despliegue en Windows Server
├── src/
│   ├── main/java/com/techstore/tech_store_project/
│   │   ├── controller/
│   │   │   ├── api/                # @RestController — endpoints /api/* (JSON)
│   │   │   └── ExportController.java   # Exportación a Excel (.xlsx)
│   │   ├── model/                  # Entidades JPA
│   │   ├── repository/             # Repositorios Spring Data
│   │   ├── service/                # Lógica de negocio y listeners de login
│   │   └── config/                 # SecurityConfig, DataInitializer, SPA, errores
│   ├── main/resources/
│   │   ├── application.properties       # Config de desarrollo
│   │   └── application-prod.properties   # Config de producción (variables de entorno)
│   └── test/                       # Tests (JUnit + Spring Boot Test)
└── frontend/                       # SPA React (Vite)
    ├── package.json
    ├── vite.config.js              # Proxy /api y /export → :8080
    └── src/
        ├── api/                    # Cliente HTTP central
        ├── context/                # Auth y tema (claro/oscuro)
        ├── components/             # Layout, Navbar, Sidebar, Modal, Toast…
        └── pages/                  # Login, Dashboard, Productos, Categorías…
```

---

## 🔑 Características destacadas

### Seguridad
- Contraseñas cifradas con **BCrypt**.
- **Bloqueo de cuenta** tras 3 intentos fallidos.
- Control de acceso por **roles** (ADMIN / ALMACENERO / VENDEDOR).
- Timeout automático de sesión tras 15 minutos de inactividad.
- API sin estado de vistas: respuestas **401/403 en JSON**.

### Kardex (movimientos)
- Registro **inalterable** de entradas y salidas.
- Trazabilidad: fecha, hora, usuario, producto y cantidad.
- Validación de stock disponible en las salidas.
- Consulta e historial con filtros.

### Dashboard
- Totales de productos, categorías, marcas, entradas y salidas.
- Valor total del inventario.
- Gráfica de productos por categoría y movimientos por mes.
- **Alertas de stock bajo** en tiempo real.

---

## 🧪 Tests

```bash
./mvnw test
```

---

## 🌐 Despliegue

Para un despliegue real en **Windows Server** (Nginx como reverse proxy + HTTPS con Let's Encrypt, la app como servicio de Windows con WinSW), consulta la guía paso a paso en [`deploy/README-DESPLIEGUE.md`](deploy/README-DESPLIEGUE.md).

---

## 👨‍💻 Autores

- Quispe Sánchez Juan André
- Mauricio Lopez Sebastian Alessandro
- Salazar Bustamante Angelo Gianpiero
- Cerna Martinez Arian
- Sullcapuma Bustamante Jaren John

---

## 📄 Licencia

Proyecto académico — UTP 2026.
