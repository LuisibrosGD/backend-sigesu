# SIGESU Backend

API Spring Boot 3.5.16, Java 21, Maven, Hibernate/JPA y PostgreSQL. Repositorio independiente del frontend SIGESU–UNMSM. Un proceso desplegable, organizado por módulos.

## Estructura y dependencias

En `src/main/java/pe/edu/unmsm/sigesu` cada módulo funcional contiene las capas que necesita:

- `routes`: controladores HTTP y validación de entrada.
- `schemas`: DTOs de entrada y salida; nunca contienen hashes de contraseñas.
- `services`: casos de uso, permisos, transacciones y coordinación.
- `repository`: entidades JPA y repositorios Spring Data, con tablas y claves foráneas gestionadas por Flyway.

Módulos: identidad, catalogos, solicitudes, reservas, notificaciones, archivos, indicadores. `application` compone los DTOs compatibles con el frontend y la carga inicial; `common` contiene errores, permisos, códigos y paginación. Los controladores no acceden directamente a repositorios. Los datos institucionales se leen a través de `CatalogosService`; las notificaciones e historial se generan en la misma transacción de cada operación. El backend valida cada rol y relación de propiedad/área/asignación, aunque el cliente oculte botones.

## Desarrollo local

1. Instalar JDK 21 o superior y definir `JAVA_HOME`. Se incluye Maven Wrapper (`mvnw.cmd` en Windows, `./mvnw` en Unix).
2. Crear una rama/base **de desarrollo** en Neon y copiar su conexión como URL JDBC: `jdbc:postgresql://HOST/BASE?sslmode=require`. El usuario y contraseña se configuran por separado. Usar el host pooler en `DATABASE_URL` y el directo en `MIGRATION_DATABASE_URL`. Si el usuario de migraciones es distinto, definir `MIGRATION_DATABASE_USERNAME` y `MIGRATION_DATABASE_PASSWORD`.
3. Copiar `.env.example` a `.env` y establecer sus valores como variables del proceso desde el IDE o la consola. Spring Boot no carga `.env` automáticamente. Nunca confirmar `.env` en Git.
4. Definir `SPRING_PROFILES_ACTIVE=dev` y `COOKIE_SECURE=false` al usar HTTP local. Ejecutar `mvnw.cmd spring-boot:run`. Puerto: 8080, modificable con `PORT`.
5. Arrancar el frontend con Vite en el puerto 3000. Su proxy `/api` apunta a este backend.

El perfil `dev` carga la semilla original del frontend en una base sin usuarios: 14 usuarios, 32 solicitudes y 7 reservas. Las cuatro cuentas principales son `estudiante@demo.edu.pe`, `tecnico@demo.edu.pe`, `supervisor@demo.edu.pe` y `administrador@demo.edu.pe`, con contraseña `Demo2026!`, persistida como BCrypt. Los antiguos adjuntos SVG de muestra se omiten porque la política admite únicamente PDF/JPG/PNG; se pueden cargar archivos reales desde el detalle. La semilla es idempotente. Las contraseñas demo solo aparecen en el recurso de desarrollo y nunca en respuestas API.

También se puede iniciar PostgreSQL local aislado con `docker compose up -d` y usar `jdbc:postgresql://localhost:55432/sigesu_test`, usuario `sigesu` y contraseña `local-test-only`. No conectar el perfil dev a la base de producción.

## Supabase Storage

Crear proyectos separados para desarrollo y producción. En cada uno, crear el bucket privado `sigesu-evidencias`. Desactivar acceso público; no crear políticas de acceso para clientes anónimos. Configurar los MIME `application/pdf`, `image/jpeg`, `image/png` y un límite duro de 5 MB en el bucket. El backend aplica el límite institucional actual (2 MB iniciales, administrable entre 1 y 5 MB) y comprueba firma, MIME y extensión antes de subir.

Configurar `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` y `SUPABASE_STORAGE_BUCKET` exclusivamente en el proceso backend. La clave service-role permite acceso privilegiado: todos los permisos de la aplicación se verifican en Spring antes de usarla. El frontend no necesita SDK ni claves de Supabase ni Supabase Auth.

La carga multipart pasa por Spring y guarda los objetos en `solicitudes/{id}/{uuid}.{ext}`. Neon almacena el nombre original, MIME, tamaño, usuario, fecha y ruta; no almacena los bytes. Si falla la transacción de metadatos, se intenta eliminar el objeto remoto y se registra su ID si falla la compensación. Las descargas vuelven a comprobar acceso a la solicitud y generan una URL firmada válida por 300 segundos. Cualquier persona con esa URL puede usarla hasta que caduque; las respuestas que la entregan tienen `Cache-Control: no-store`. El endpoint `contenido` redirige a esa URL, manteniendo las vistas previas del frontend.

## Sesión y contrato HTTP

El cliente obtiene `GET /api/auth/csrf` y envía el `token` devuelto en `X-CSRF-TOKEN` en cada POST/PUT/PATCH/DELETE, incluido login, registro y logout. Las solicitudes usan `credentials: include`. Tras login/logout se solicita un token nuevo. La sesión opaca se almacena en Neon con Spring Session JDBC, vence tras ocho horas de inactividad y usa cookie HttpOnly, SameSite=Lax y Secure en producción. La sesión rota al autenticar. Los usuarios desactivados y los cambios de rol se revalidan leyendo al actor en cada operación.

| API | Operación |
| --- | --- |
| `GET /api/public/configuracion` | Facultades y configuración pública para el login/registro |
| `POST /api/auth/login`, `/register`, `/logout` | Autenticación y registro siempre con rol Estudiante |
| `GET /api/auth/me` | Usuario actual sin contraseña/hash |
| `PUT /api/auth/profile`, `/password` | Perfil y cambio validando contraseña actual |
| `POST /api/auth/verify-password` | Verificación para el formulario de perfil |
| `GET /api/bootstrap` | Snapshot autorizado compatible con el `AppContext` actual |
| `GET /api/catalogos` | Catálogos institucionales |
| `GET/POST /api/solicitudes`, `GET/PUT /api/solicitudes/{id}` | Consulta, alta, detalle y edición |
| `POST /api/solicitudes/{id}/acciones/{accion}` | `evaluar`, `asignar`, `iniciar`, `resolver`, `cerrar`, `devolver` |
| `POST /api/solicitudes/{id}/comentarios` | Texto y marca `esInterno`, filtrada en las respuestas |
| `POST /api/solicitudes/{id}/evidencias` | Multipart con un campo `file` |
| `GET /api/evidencias/{id}/url` o `/contenido` | URL temporal o redirección autorizada |
| `GET /api/reservas/ocupaciones` | Intervalos activos sin solicitante, código, motivo ni historial |
| `GET/POST /api/reservas`, `GET /api/reservas/{id}` | Consulta y reserva de ambiente |
| `POST /api/reservas/{id}/acciones/{accion}` | `aprobar`, `rechazar`, `cancelar` |
| `GET /api/notificaciones`, `PATCH /api/notificaciones/lectura` | `{ids, leida}`, con ids nulo para todas |
| `POST /api/admin/{recurso}` | usuarios, areas, categorias, facultades, ambientes (id opcional para alta) |
| `PUT /api/admin/prioridades`, `/configuracion` | Parámetros institucionales |
| `GET /api/dashboard/resumen`, `/indicadores` | Totales por estado, prioridad y área según permisos y filtros |
| `POST /api/admin/reset-demo` | Solo perfil dev y administrador; borra cambios compartidos en la base de desarrollo |

Listados de solicitudes/reservas aceptan `page` (base cero), `size` (máximo 100), `sort`, `order`, `search`, `estado`, `prioridad`, `areaId`, `fechaDesde`, `fechaHasta`. Responden `{content,page,size,totalElements}`. Bootstrap devuelve el conjunto autorizado completo para conservar los filtros y cálculos actuales del frontend; para volúmenes grandes debe consumirse la paginación en vez de bootstrap. Bootstrap incluye `ocupaciones`, con identificador y horarios activos para mostrar disponibilidad sin revelar datos de otros solicitantes. Los comentarios internos se excluyen para estudiantes en todos los DTOs. Los códigos correlativos anuales se generan atómicamente en PostgreSQL y las reservas activas bloquean su intervalo semiabierto `[inicio,fin)` mediante bloqueo del ambiente en la transacción. Las reglas de fechas usan America/Lima; los instantes se persisten en UTC. Un proceso horario genera alertas de vencimiento sin duplicarlas.

Errores: 400 validación, 401 sesión/credenciales, 403 permisos o CSRF, 404 inexistente, 409 conflicto, 413 tamaño global y 502/503 almacenamiento. Formato `{message, errors?}`. El frontend no reintenta automáticamente mutaciones. La creación de solicitud y las cargas son operaciones separadas: una carga fallida mantiene la solicitud creada y se informa para reintentar desde su detalle.

Swagger/OpenAPI está habilitado en dev en `/swagger-ui/index.html` y `/v3/api-docs`. En producción está desactivado salvo `API_DOCS_ENABLED=true`.

## Pruebas

`mvnw.cmd test` ejecuta pruebas unitarias de permisos y del cliente Storage con un servidor HTTP simulado, sin credenciales reales. Las pruebas de integración requieren una **base dedicada de pruebas** y se omiten si `TEST_DATABASE_URL` no está definido:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
$env:TEST_DATABASE_URL='jdbc:postgresql://localhost:55432/sigesu_test'
$env:TEST_DATABASE_USERNAME='sigesu'
$env:TEST_DATABASE_PASSWORD='local-test-only'
.\mvnw.cmd test
```

La suite comprueba migraciones y semilla, CSRF, sesión persistida, filtros de datos y comentarios internos, el ciclo de solicitudes, protección del último administrador, reemplazo de técnicos y dos reservas concurrentes. No apuntar esas variables a producción: se crean datos temporales y la semilla dev cuando no hay usuarios.

## Producción en Render y Vercel

1. Crear repositorio remoto para este directorio y subir el código. Crear el servicio Docker de Render con `render.yaml`; revisar el plan de cómputo antes de crear el servicio. La imagen usa Java 21 y el puerto `PORT`. Configurar `/actuator/health` como health check.
2. Crear una base/rama de producción en Neon. Configurar las variables de conexión y migración, `SPRING_PROFILES_ACTIVE=prod`, `COOKIE_SECURE=true`, las tres variables privadas de Storage y `ALLOWED_ORIGINS` con los orígenes exactos del frontend.
3. Configurar `BOOTSTRAP_ADMIN_EMAIL` y una `BOOTSTRAP_ADMIN_PASSWORD` única de al menos 12 caracteres para el primer arranque. Se crea el administrador solo si no existe ninguno activo; retirar esas variables después. Producción inicia con configuración y prioridades, sin cuentas ni operaciones demo. Cargar áreas, facultades, edificios, categorías y ambientes desde administración; habilitar registro público cuando estén listos.
4. Una vez que Render asigne la URL HTTPS real, configurar en el repositorio frontend un rewrite de Vercel `/api/:path*` hacia `https://HOST-REAL.onrender.com/api/:path*`. El frontend debe usar `/api` bajo su propio origen para que las cookies sean de primera parte. Configurar también el origen HTTPS del frontend en `ALLOWED_ORIGINS`. No usar llamadas directas entre dominios distintos con SameSite=Lax.
5. Desplegar el frontend, probar login/logout, cargas, permisos y reservas. Verificar health check y logs en Render. Flyway aplica versiones pendientes y Hibernate valida el esquema; no usar `ddl-auto=update`. Antes de cambios incompatibles de esquema, preparar respaldo/branch en Neon y una migración compatible con la versión anterior.

No se incluyen credenciales ni recursos cloud creados. El repositorio local no tiene remoto configurado: los servicios y secretos deben pertenecer a las cuentas del equipo.


## Rendimiento

La lectura por lotes y sus mediciones se documentan en [PERFORMANCE.md](PERFORMANCE.md). Reiniciar el backend después de actualizar el código.


## Carga progresiva por pantalla

El frontend actual no utiliza /api/bootstrap. Los listados paginan en PostgreSQL, los indicadores se agregan en SQL y los detalles se cargan al abrirlos. Contratos, caché, pruebas y mediciones: [CARGA_PROGRESIVA.md](CARGA_PROGRESIVA.md). Reiniciar SigesuApplication para cargar las clases nuevas.


## Refactorización SOLID focalizada

Las lecturas se organizan por responsabilidad. `ProgressiveReadService` y `ProgressiveRoutes` fueron retirados; sus endpoints mantienen las mismas URLs, parámetros y respuestas.

| Responsabilidad | Servicio | Persistencia o colaboración |
| --- | --- | --- |
| Listado y exportación de solicitudes | `SolicitudesReadService` | `SolicitudSpecifications`, `SolicitudRepository` y lectura por lotes |
| Reservas y ocupaciones públicas | `ReservasReadService` | `ReservaSpecifications`, `ReservaRepository` y lectura por lotes |
| Catálogos seleccionados | `CatalogosReadService` | Métodos de `CatalogosService`; personal visible con el filtro existente |
| Página y resumen de notificaciones | `NotificacionesReadService` | `NotificacionRepository` |
| Usuarios administrativos y pendientes | `AdminUsuariosReadService` | `UsuariosReadRepository` y lectura autorizada de solicitudes |
| Historial administrativo global | `HistorialReadService` | `HistorialReadRepository` |
| Agregaciones de indicadores | `DashboardReadService` | `IndicadoresReadRepository` y pendientes del servicio de solicitudes |

Los controladores llaman al servicio de su responsabilidad. Las Specifications permanecen en `repository`; las consultas SQL nativas de estas lecturas están en repositorios específicos. `ReadPages` comparte solamente la interpretación de paginación y su envoltura JSON. Las transacciones de lectura siguen en los servicios.

### Catálogos

`CatalogosService` tiene repositorios privados y expone consultas y validaciones concretas, sin devolver repositorios. `AdminCatalogosService` mantiene las escrituras administrativas con sus propias dependencias privadas. `CatalogosInitializationService` encapsula valores predeterminados e inserción de catálogos demo; `InitialData` conserva el orden de inicialización y la transacción de arranque.

`lockAmbiente` participa en la transacción existente del caso de uso; no abre una transacción independiente que libere anticipadamente el bloqueo. `Projections`, `BatchProjections` y bootstrap conservan su composición y lectura por lotes.

### Almacenamiento sustituible

`EvidenceStorage` define `upload`, `delete`, `signedUrl` y `ttl`. `SupabaseStorage` es el adaptador actual. `ArchivosService` y `DevResetService` reciben el contrato por constructor y no dependen del proveedor concreto.

La autorización, la validación del contenido, la transacción de metadatos y la compensación continúan en el caso de uso. Los errores del adaptador y la expiración de las URLs no cambian. Las pruebas sustituyen Supabase por almacenamiento en memoria, sin credenciales ni red externa.

### Principios aplicados y límites

- **SRP:** servicios de lectura, consultas SQL e inicialización de catálogos tienen responsabilidades separadas.
- **ISP:** cada controlador consume el servicio de lectura correspondiente; no depende de un servicio general de todos los módulos.
- **DIP:** los consumidores del almacenamiento dependen de `EvidenceStorage`.
- **OCP:** otro proveedor puede implementar ese contrato sin modificar los consumidores.
- **LSP:** el adaptador real y la implementación de prueba respetan las operaciones del contrato; las pruebas comprueban sustitución, enlaces, commit, rollback y compensación.

No se añadieron interfaces por cada servicio. Los flujos de acciones de solicitudes y reservas conservan sus reglas; sus cambios se limitan al acceso encapsulado a catálogos. No se modificaron frontend, esquema, IDs ni configuración cloud.

### Validación de la refactorización

La suite incluye las 22 pruebas anteriores y 11 nuevas: contratos HTTP por rol, equivalencia de catálogos, rollback real de metadatos, almacenamiento sustituible, compensación y límites de dependencias. Las comprobaciones de rendimiento exigen siete consultas y 40 entidades para diez filas entre 1.032 solicitudes; bootstrap conserva 15 consultas y renderizar un snapshot cargado ejecuta cero consultas adicionales.

Ejecutar `mvnw.cmd clean package` con las variables `TEST_DATABASE_*` apuntando a PostgreSQL aislado. El arranque del contexto verifica también que no existan dependencias circulares ni mappings HTTP duplicados. Estas mejoras son de organización y capacidad de prueba; no representan una nueva reducción de latencia medida con Neon.

Resultado del 23 de octubre de 2026: 33 pruebas aprobadas (22 de integración y 11 unitarias), sin fallos ni omisiones, y paquete ejecutable generado. La ejecución desde una base PostgreSQL vacía validó las tres migraciones existentes y la semilla refactorizada; no se añadió ninguna migración.
