# Configuración de las mejoras

## Primer arranque

1. Define `ADMIN_PASSWORD` en el entorno del proceso Java. Debe ser única, tener al menos 12 caracteres y ocupar como máximo 72 bytes UTF-8 (límite de BCrypt).
2. En producción, define además `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`, y activa el perfil `prod`.
3. Inicia sesión como `admin` con la contraseña elegida. La variable de bootstrap no reemplaza la contraseña de cuentas existentes.
4. Para cambiarla posteriormente, usa **Mi Perfil → Cambiar contraseña**. Todas las sesiones de esa cuenta se cierran.

No hay una contraseña inicial fija. Sin `ADMIN_PASSWORD` y sin usuarios previos, el arranque falla con un mensaje de configuración. Producción también rechaza al administrador antiguo si conserva `123456`; primero cambia esa contraseña en desarrollo.

## Datos de demostración y dinero

`prod` no carga categorías, marcas ni productos de demostración. En desarrollo puedes desactivarlos con `--techstore.seed-demo-data=false`.

Los precios de productos y su historial utilizan `BigDecimal` y `NUMERIC(12,2)`: hasta 10 dígitos enteros y 2 decimales. Se rechazan valores negativos o con más decimales. Dashboard y exportaciones usan estos valores; Excel recibe un número al escribir la celda.

Antes de arrancar esta versión contra una base existente, conserva una copia de seguridad: cambia el tipo de las columnas de precio. Se mantiene el mecanismo actual `ddl-auto=update`; no se incorporaron Flyway, Liquibase ni una migración de datos independiente.

## Sesiones, permisos y CSRF

- React obtiene un token mediante `GET /api/auth/csrf` y lo envía en la cabecera indicada para POST, PUT y DELETE. También se exige para login y logout. El token se renueva tras cambios de sesión; un rechazo CSRF puede reintentarse una vez porque el servidor no llegó a ejecutar la operación.
- El login rota el identificador de sesión. El bloqueo de cuentas (incluido por intentos fallidos), los cambios de rol/permisos y de contraseña revocan las sesiones afectadas después de confirmar los cambios en la base de datos. La siguiente petición recibe 401.
- El registro de sesiones pertenece al proceso Java. Esta versión está preparada para una sola instancia; ejecutar varias requiere coordinar sesiones y revocaciones entre instancias.
- `/export/productos.xlsx` requiere `GESTIONAR_PRODUCTOS`; `/export/movimientos.xlsx` requiere `VER_MOVIMIENTOS`.
- En `prod`, la cookie de sesión requiere HTTPS. Mantén el acceso a través del proxy HTTPS que se describe en la guía de despliegue.

## Alcance del sandbox informado

El PDF facilitado permite `us-east-1` y `us-west-2`. EC2 permite familias t2/t3/t3a/t4g en tamaños micro, small y medium, hasta 9 instancias y volúmenes de hasta 100 GB. RDS permite t3/t4g micro, small y medium, hasta 50 GB y sin IOPS aprovisionado. Budgets y facturación no están disponibles.

API Gateway, Secrets Manager, Systems Manager y EventBridge Scheduler no aparecen en la tabla facilitada: su disponibilidad debe confirmarse antes de planificar una práctica que dependa de ellos. Estas mejoras no requieren esos servicios ni crean recursos de AWS.

## Verificación realizada

La práctica con EC2, RDS, S3 y SNS se explica en [GUIA-AWS-4-HORAS.md](GUIA-AWS-4-HORAS.md). La edición de Mi Perfil guarda únicamente el nombre y correo propios. Los avisos administrativos requieren `GESTIONAR_USUARIOS`, consentimiento mediante el botón Activar y confirmación del correo en SNS. Cambiar el correo desactiva la preferencia; el filtro por destinatario evita nuevas publicaciones para direcciones antiguas.

Los avisos se procesan después de confirmar la transacción y en segundo plano. Un fallo de SNS no revierte el inventario. La cola es local y los fallos se registran: todavía no hay entrega duradera, outbox ni reintentos persistentes.

Se generó el JAR de producción con React y el backend. La versión con perfil/SNS pasó 88 comprobaciones locales: 75 de API e inventario con una base H2 temporal y SNS simulado, 7 de las solicitudes y configuración del transporte SNS sin llamadas AWS y 6 de arranque/configuración inicial en producción. Incluyen permisos, cambio de correo, umbrales y fallos de notificación. No se modificó la suite de pruebas del proyecto ni se incorporaron migraciones.

Falta verificar la actualización del esquema y el comportamiento con PostgreSQL/RDS. Durante la instalación de dependencias de frontend se reportaron 6 alertas de seguridad existentes (1 moderada y 5 altas); su actualización quedó pendiente y fuera de estas seis mejoras.
