# Despliegue en Windows Server (24/7)

Guía real del despliegue de TechStore en un Windows Server accedido por RDP, con dominio y HTTPS.

**Arquitectura montada:**

```
Internet → techstore.moonlygg.com (Cloudflare DNS "solo DNS" → IP pública del server)
   → Nginx :443  (HTTPS con certificado Let's Encrypt)
   → Spring Boot .jar :8080  (servicio de Windows, sirve React + API)
   → PostgreSQL :5432  (local)
```

Todo en un mismo server, **sin Docker**, conviviendo con otros sitios del mismo Nginx.

---

## Fase 0 — Compilar en TU PC (no en el server)

```bash
./mvnw clean package -Pprod -DskipTests
```

Genera `target/tech-store-project-0.0.1-SNAPSHOT.jar` (backend + React en un solo archivo).
Cópialo al server (por RDP, copiar/pegar) a `C:\techstore\` y renómbralo a `tech-store-project.jar`.

> El perfil `-Pprod` compila el React (Node aislado) y lo empaqueta dentro del `.jar`.

---

## Fase 1 — Instalar en el server

| # | Programa | Notas |
|---|----------|-------|
| 1 | **JDK 21** | Ejecuta el `.jar`. Verificar con `java -version` |
| 2 | **PostgreSQL 16** | Instalador nativo (sin Docker). Se instala como servicio con arranque **Automatic** |
| 3 | **WinSW** (`WinSW-x64.exe`) | Para correr el `.jar` como servicio de Windows |

> Node.js NO se instala en el server: el React ya viene compilado dentro del `.jar`.
> Nginx y win-acme (wacs) ya estaban instalados en este server para otros sitios.

---

## Fase 2 — Base de datos

Conéctate como superusuario `postgres` (con la contraseña puesta al instalar):

```powershell
& 'C:\Program Files\PostgreSQL\16\bin\psql.exe' -U postgres
```

Crea el usuario y la base (el usuario es **dueño** para poder crear las tablas):

```sql
CREATE USER admin_techstore WITH PASSWORD 'UNA_CONTRASENA_FUERTE';
CREATE DATABASE techstore_db OWNER admin_techstore;
\q
```

Las tablas, el rol admin y los datos de ejemplo se crean solos en el primer arranque.
Usuario inicial de la app: **admin / 123456** (cambiar de inmediato tras el primer login).

---

## Fase 3 — Correr la app como Servicio de Windows (24/7)

1. Copia `WinSW-x64.exe` a `C:\techstore\` y renómbralo a `techstore-service.exe`.
2. Crea `C:\techstore\techstore-service.xml` (ver `deploy/techstore-service.xml` como plantilla). Ajusta:
   - Ruta real de `java.exe` (ej. `C:\Program Files\Common Files\Oracle\Java\javapath\java.exe`).
   - `DB_PASSWORD` = la contraseña de `admin_techstore`.
3. PowerShell **como Administrador** en `C:\techstore`:

```powershell
cd C:\techstore
.\techstore-service.exe install
.\techstore-service.exe start
.\techstore-service.exe status   # debe decir "Started"
```

Prueba local en el server: `http://localhost:8080` debe cargar el login.
Comandos: `stop` | `restart` | `uninstall`. Logs en `C:\techstore\*.out.log`.

> El servicio arranca con `--spring.profiles.active=prod` (usa `application-prod.properties`:
> credenciales por variable de entorno, `ddl-auto=update`, cookie de sesión segura, etc.).

---

## Fase 4 — Dominio (Cloudflare) y firewall

1. **DNS:** en Cloudflare, registro **A** `techstore` → IP pública del server, en modo **"DNS only"** (nube gris).
   Verificar: `nslookup techstore.moonlygg.com` debe resolver a tu IP.
2. **Firewall:** abrir 80 y 443:

```powershell
New-NetFirewallRule -DisplayName "HTTP 80"  -Direction Inbound -Protocol TCP -LocalPort 80  -Action Allow
New-NetFirewallRule -DisplayName "HTTPS 443" -Direction Inbound -Protocol TCP -LocalPort 443 -Action Allow
```

> El modo "DNS only" permite que Let's Encrypt valide por el puerto 80 sin que Cloudflare interfiera.
> (Opcional a futuro: activar el proxy de Cloudflare con SSL "Full (strict)".)

---

## Fase 5 — Nginx como reverse proxy + HTTPS

Nginx (en `C:\nginx`) enruta por nombre de dominio, así que se **añade** el sitio de techstore sin tocar los demás.
Ejecutar siempre `nginx.exe` desde `C:\nginx` (`cd C:\nginx`).

### 5.1 Bloque HTTP (para validar el certificado)

En `C:\nginx\conf\nginx.conf`, dentro de `http { }`, añadir:

```nginx
    server {
        listen 80;
        server_name techstore.moonlygg.com;
        location /.well-known/acme-challenge/ { root C:/wacs/webroot; }
        location / { return 301 https://$host$request_uri; }
    }
```

Aplicar (este Nginx no permite `-s reload` por permisos, así que se reinicia):

```powershell
cd C:\nginx
.\nginx.exe -t
Stop-Process -Name nginx -Force
Start-Sleep -Seconds 1
Start-Process -FilePath "C:\nginx\nginx.exe" -WorkingDirectory "C:\nginx"
```

### 5.2 Generar el certificado (win-acme)

```powershell
cd C:\wacs
.\wacs.exe --source manual --host techstore.moonlygg.com --validation filesystem --webroot C:\wacs\webroot --store pemfiles --pemfilespath C:\nginx\ssl --installation none --accepttos --emailaddress TU_CORREO
```

Genera `C:\nginx\ssl\techstore.moonlygg.com-chain.pem` y `-key.pem`, y programa la **renovación automática**.

### 5.3 Bloque HTTPS (proxy a la app)

Añadir en `nginx.conf`:

```nginx
    server {
        listen 443 ssl;
        server_name techstore.moonlygg.com;

        ssl_certificate     C:/nginx/ssl/techstore.moonlygg.com-chain.pem;
        ssl_certificate_key C:/nginx/ssl/techstore.moonlygg.com-key.pem;
        ssl_protocols       TLSv1.2 TLSv1.3;

        client_max_body_size 20m;

        location / {
            proxy_pass http://localhost:8080;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;   # clave: HTTPS para la cookie segura
            proxy_read_timeout 300;
        }
    }
```

Volver a aplicar (`nginx -t` + reinicio como en 5.1) y probar `https://techstore.moonlygg.com`.

> `X-Forwarded-Proto $scheme` es imprescindible: sin él, Spring cree que la conexión es HTTP
> y la cookie de sesión segura no se envía → no se puede iniciar sesión.

---

## Fase 6 — Que todo sobreviva a un reinicio

| Componente | Mecanismo de arranque |
|---|---|
| Spring app | Servicio WinSW (`startmode Automatic`) |
| PostgreSQL | Servicio de Windows (Automatic, por defecto del instalador) |
| Nginx | Tarea programada al inicio del sistema, como SYSTEM |
| Certificado | Tarea de renovación de win-acme |

Nginx como tarea al boot (si no existiera ya):

```powershell
$action    = New-ScheduledTaskAction -Execute "C:\nginx\nginx.exe" -WorkingDirectory "C:\nginx"
$trigger   = New-ScheduledTaskTrigger -AtStartup
$principal = New-ScheduledTaskPrincipal -UserId "SYSTEM" -LogonType ServiceAccount -RunLevel Highest
Register-ScheduledTask -TaskName "nginx" -Action $action -Trigger $trigger -Principal $principal
```

---

## Checklist final
- [ ] `https://techstore.moonlygg.com` carga con candado y permite login
- [ ] Contraseña del usuario `admin` **cambiada**
- [ ] `Get-Service *postgresql*` → Running / Automatic
- [ ] Servicio `techstore` en estado Started
- [ ] Tarea `nginx` con `<BootTrigger/>` y usuario SYSTEM

## Actualizar la app a futuro
1. `./mvnw clean package -Pprod -DskipTests` en tu PC
2. Copia el nuevo `.jar` al server (reemplaza `C:\techstore\tech-store-project.jar`)
3. `cd C:\techstore; .\techstore-service.exe restart`

## Pendiente recomendado
- Configurar en win-acme un script post-renovación que ejecute `nginx -s reload` (o reinicie Nginx)
  para que tome el certificado nuevo automáticamente cada ~90 días.
