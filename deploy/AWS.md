# Despliegue de TechStore en AWS

TechStore distribuye React y Spring Boot en un único JAR. La arquitectura AWS utiliza una instancia EC2 para servir la aplicación, RDS PostgreSQL para los datos, S3 para el artefacto de despliegue y SNS para las alertas de stock bajo. El repositorio contiene el código de la aplicación, la plantilla de CloudFormation y los archivos de configuración del servidor.

## Topología y decisiones

| Componente | Función |
|---|---|
| Amazon EC2 | Amazon Linux 2023, Java 21 y Nginx. Nginx termina HTTPS y reenvía al proceso Java en `127.0.0.1:8080`. `systemd` administra el proceso. |
| Amazon RDS for PostgreSQL | Persiste el catálogo, los usuarios y los movimientos del Kardex. La instancia Single-AZ usa un grupo de subredes privadas y acepta tráfico en `5432` desde el grupo de seguridad de EC2. JDBC usa TLS con `sslmode=verify-full`. |
| Amazon S3 | Bucket privado para el JAR y los archivos de configuración sin secretos en `artefactos/`. El prefijo `backups/` admite cargas manuales de respaldos. Las exportaciones de la aplicación se descargan desde su API. |
| Amazon SNS | Tema Standard para los avisos que se publican después de confirmar una salida que cruza el umbral de stock. Cada destinatario confirma su suscripción por correo. |
| IAM | El perfil de instancia de EC2 entrega credenciales temporales para acceder a los prefijos de S3 y al tema SNS. La plantilla incluye permisos KMS acotados para el tema cifrado. |

La VPC ubica EC2 en una subred pública y RDS en subredes privadas. El acceso web entra por `443`. Si se habilita SSH, limita `22` a la IP de administración. La topología utiliza un servidor de aplicación y una instancia de base de datos Single-AZ.

## Aprovisionar la infraestructura con CloudFormation

1. En la región de AWS que usarás para TechStore, prepara un par de claves de EC2 y selecciona dos zonas de disponibilidad distintas.
2. En CloudFormation, crea un stack y carga [cloudformation.yaml](aws/cloudformation.yaml). Indica las dos zonas, el nombre del par de claves, tu IPv4 pública en formato `/32` para SSH y la contraseña inicial de PostgreSQL. Con `WebIngressCidr` puedes restringir quién accede por HTTPS.
3. Acepta la creación de recursos IAM. Si usas la CLI para crear el stack, declara la capacidad `CAPABILITY_IAM`.
4. Cuando el stack llegue a `CREATE_COMPLETE`, usa `AppPublicIp` para conectarte a EC2 y configurar el DNS, `ArtifactBucketName` para publicar el JAR y `DbEndpoint`, `DbName`, `DeploymentRegion` y `StockAlertsTopicArn` para configurar el entorno.

El stack aprovisiona la red, EC2, RDS privado, el bucket de artefactos, el tema SNS cifrado y el rol de la instancia. La instalación del JAR, el certificado HTTPS y la confirmación de suscripciones se completan después, como se indica abajo. Al eliminar un stack creado correctamente, CloudFormation conserva el bucket de artefactos y crea un snapshot final de RDS.

## Configuración versionada

| Archivo | Uso |
|---|---|
| [techstore.service](aws/techstore.service) | Ejecuta el JAR con un usuario de servicio y carga las variables desde `/etc/techstore/techstore.env`. |
| [nginx-techstore.conf](aws/nginx-techstore.conf) | Proxy HTTPS a Spring Boot y cabeceras de reenvío. |
| [techstore.env.example](aws/techstore.env.example) | Configuración del perfil `prod`, conexión RDS y SNS. |
| [ec2-trust-policy.json](aws/ec2-trust-policy.json) y [ec2-permissions.json](aws/ec2-permissions.json) | Confianza del rol de EC2 y permisos sobre los recursos de la aplicación. |
| [s3-require-tls.json](aws/s3-require-tls.json) | Política de bucket que rechaza solicitudes sin TLS. |

## Desplegar una versión

1. **Preparar los recursos.** Usa el stack anterior. Para una instalación manual, configura la VPC, los grupos de seguridad, EC2, la base `techstore_db` en RDS, un bucket S3 privado y un tema SNS Standard. Ajusta el rol de la instancia al bucket, al tema y a la clave KMS de ese entorno.
2. **Empaquetar y publicar.** Ejecuta `./mvnw clean package -Pprod -DskipTests` desde la raíz. Publica `target/tech-store-project-0.0.1-SNAPSHOT.jar`, `techstore.service`, `nginx-techstore.conf` y `techstore.env.example` en el prefijo `artefactos/`. Conserva las credenciales reales en EC2.
3. **Instalar en EC2.** Instala Java 21, Nginx y AWS CLI. Descarga los artefactos con el rol de la instancia, por ejemplo `aws s3 cp "s3://$BUCKET/artefactos/tech-store-project-0.0.1-SNAPSHOT.jar" /tmp/techstore.jar`, con `BUCKET` definido. Crea el usuario y grupo de sistema `techstore`. Instala el JAR legible por ese usuario en `/opt/techstore/techstore.jar` y la unidad en `/etc/systemd/system/techstore.service`.
4. **Configurar el entorno y TLS.** Crea `/etc/techstore/techstore.env` desde la plantilla con propietario `root` y permisos `600`. Ajusta `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `AWS_REGION` y `SNS_TOPIC_ARN`. Define `ADMIN_PASSWORD` al iniciar con una base sin usuarios. Instala el paquete de CA de RDS en la ruta de `sslrootcert`. Copia la configuración de Nginx a `/etc/nginx/conf.d/techstore.conf` y configura el dominio y las rutas del certificado HTTPS y su clave.
5. **Iniciar los servicios.**

   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable --now techstore
   sudo nginx -t
   sudo systemctl enable --now nginx
   sudo systemctl reload nginx
   ```

En el primer arranque, el perfil `prod` inicializa el esquema y el administrador cuando corresponde. Los datos de desarrollo se mantienen en su propia base. Para actualizar el JAR, reemplázalo en `/opt/techstore/` y reinicia `techstore` con `systemctl`.

## Validar el despliegue

| Verificación | Resultado esperado |
|---|---|
| `systemctl status techstore` y `journalctl -u techstore -n 80 --no-pager` | Servicio activo y aplicación iniciada sin errores de conexión o bootstrap. |
| `curl -I http://127.0.0.1:8080/` desde EC2 y acceso web por HTTPS | El proceso Java responde por loopback y el navegador llega a la aplicación a través de Nginx. |
| Registrar un movimiento y consultar Kardex | El stock y el historial se actualizan. Los datos siguen accesibles después de reiniciar el servicio. |
| Confirmar el correo SNS y registrar una salida que cruce el mínimo | Se recibe el aviso. Otra salida que mantiene el stock por debajo del umbral evita un nuevo aviso por ese cruce. |

Para configurar y probar las notificaciones, consulta [avisos de stock por correo](../README.md#avisos-de-stock-por-correo). Las comprobaciones locales del proyecto emplearon SNS simulado. Comprueba el acceso HTTPS, la actualización del esquema y la entrega real de correo en el entorno de destino.
