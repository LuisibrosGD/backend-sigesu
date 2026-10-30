# SIGESU: colección de Postman para el backend

Generada el 29 de octubre de 2026 a partir del código local de `SIGESU-backend`: controladores, DTO, seguridad, filtros y reglas de negocio. Las peticiones apuntan directamente al backend; no requieren que el frontend esté funcionando.

## Importar y empezar

1. En Postman, selecciona **Import** e importa `SIGESU.postman_collection.json` y `SIGESU.local.postman_environment.json`.
2. Selecciona el entorno **SIGESU — Local**.
3. Configura `baseUrl`, `email` y `password` en los valores locales del entorno. El origen predeterminado es `http://localhost:8080`, sin `/api` ni barra final. Usa una cuenta existente; el archivo no contiene credenciales.
4. Ejecuta la carpeta **00 Inicio y lecturas básicas**, en orden, o envía primero **04 Iniciar sesión** y luego **06 Catálogos por include**.
5. Copia los IDs de los catálogos a las variables del entorno para las operaciones que quieras probar. Los GET de las listas permiten copiar IDs de solicitudes, reservas, usuarios y notificaciones existentes.

La carpeta 00 puede ejecutarse con Collection Runner: solo abre la sesión y consulta datos. Las otras carpetas contienen operaciones independientes y alternativas de estado. Ejecutarlas todas consecutivamente puede modificar datos y provocar errores por estados incompatibles. Selecciona las peticiones del escenario que quieras probar.

Para `localhost`, usa Postman Desktop o el agente de escritorio. Mantén habilitado su cookie jar. En HTTP local, inicia el backend con el perfil `dev` o `COOKIE_SECURE=false`: la configuración general usa una cookie Secure y el perfil `dev` la desactiva para desarrollo. Para un backend desplegado, cambia `baseUrl` al origen HTTPS real.

## Autenticación y CSRF

El backend usa la cookie HttpOnly `SIGESU_SESSION`, no autenticación Bearer. Postman la administra en su cookie jar; no debes copiarla a una variable ni a un encabezado manual.

El script de colección consulta `GET /api/auth/csrf` antes de cada POST, PUT o PATCH y añade el encabezado devuelto por el backend. Esto incluye login, registro y logout. El token se usa en esa petición y no se guarda en el entorno. Al iniciar sesión se invalida la sesión anterior; por eso se obtiene nuevamente el CSRF para la siguiente escritura.

Si falla esa consulta, el script omite la escritura y muestra el error. Los scripts usan `await pm.sendRequest` y `pm.execution.skipRequest`; utiliza una versión actual de Postman. No hay reintentos automáticos de operaciones que cambian datos.

Para cambiar de rol, configura las credenciales locales de otra cuenta y vuelve a enviar **04 Iniciar sesión**. La sesión activa es compartida por las peticiones al mismo host; prueba los roles secuencialmente.

## Variables y escenarios

| Variables | Cómo obtenerlas o configurarlas |
| --- | --- |
| `baseUrl` | Origen del backend; por defecto `http://localhost:8080`. |
| `email`, `password` | Cuenta existente; valores locales. Las contraseñas están vacías y marcadas como secretas en el archivo del entorno. |
| `page`, `size` | Página desde 0; tamaño entre 1 y 100. |
| `categoriaId`, `areaId`, `tecnicoId` | Catálogos. El técnico debe estar activo y pertenecer al área responsable de la categoría. `include=tecnicos` devuelve la clave `usuarios`; filtra por rol Técnico. |
| `facultadId`, `edificioId` | Catálogos: el edificio debe pertenecer a la facultad seleccionada. Para registro sin sesión, consulta la configuración pública. |
| `ambienteId` | Ambiente activo de los catálogos. |
| `edificioNombre` | Nombre real de un edificio de la facultad, para el DTO de administración de ambientes. |
| `solicitudId`, `reservaId`, `evidenciaId` | Se guardan automáticamente en el entorno al crear o subir el recurso. También puedes indicar uno existente. |
| `notificacionId`, `usuarioId` | Copia el ID desde su lista. No se elige automáticamente ningún registro existente. |
| `fechaReserva` | `YYYY-MM-DD`; vacía usa mañana en Lima para esa petición. Debe estar dentro de la antelación configurada. |
| `horaInicio`, `horaFin` | Por defecto `09:00` y `10:00`; ajusta al horario y disponibilidad del ambiente. |
| `emailRegistro`, `passwordRegistro`, `codigoRegistro` | Datos nuevos para registro público; requiere que esté habilitado. |
| `emailUsuarioNuevo`, `passwordUsuarioNuevo`, `codigoUsuarioNuevo` | Datos nuevos para crear un usuario como administrador. |
| `codigoUsuarioEditar`, `nombreUsuarioEditar`, `emailUsuarioEditar` | Datos reales del usuario objetivo. Revisa también rol, áreas, facultad y estado en el cuerpo antes de actualizar. |
| `nombrePerfil`, `telefonoPerfil`, `cargoPerfil` | Valores deseados para tu perfil. |
| `newPassword` | Contraseña nueva para el cambio manual; después actualiza `password` localmente. |
| `codigoAreaNueva`, `siglasFacultadNueva`, `codigoAmbienteNuevo` | Valores de prueba propios para crear catálogos. |
| `periodoAcademico` | Ejemplo `2026-II`; revisa todo el cuerpo de configuración antes de enviarlo. |
| `usuarioCreadoId`, `areaCreadaId`, `categoriaCreadaId`, `facultadCreadaId`, `ambienteCreadoId` | IDs guardados al crear recursos administrativos. No sobrescriben las variables operativas anteriores. |

Los filtros opcionales están desactivados en la pestaña **Params**. Activa únicamente los necesarios y completa sus valores. La exportación de solicitudes devuelve JSON, no CSV.

En los cuerpos JSON verás referencias como `{{json_password}}`. El pre-request las prepara a partir de la variable original (`password`) y escapa comillas, barras y saltos de línea. Configura las variables originales del entorno; no necesitas crear las que empiezan por `json_`.

Para solicitudes, el recorrido habitual es **crear → evaluar → asignar → iniciar → resolver → cerrar**. Evaluar es opcional. Devolver es una alternativa a cerrar: necesita una solicitud Resuelta y vuelve a En atención. Agrega comentarios y archivos antes del cierre.

Para reservas, crea una reserva y elige **aprobar** o **rechazar**. Cancelar permite una reserva futura Solicitada o Aprobada. La fecha debe ser posterior a hoy en Lima, el horario debe estar dentro del ambiente y no puede solaparse con otra reserva activa.

Para subir evidencias, selecciona un PDF, JPG o PNG en **Body → form-data → file**. El límite depende de la configuración, hasta 5 MB. El backend necesita su almacenamiento configurado. La consulta de contenido comprueba la redirección 302 sin seguirla automáticamente.

## Roles

| Operación | Permisos principales |
| --- | --- |
| Crear solicitudes y reservas | Estudiante, Supervisor y Administrador. |
| Ver solicitudes | Estudiante titular, Técnico asignado, Supervisor del área o Administrador. |
| Editar solicitud | Estudiante titular o Administrador; estado Registrada o En evaluación. |
| Evaluar, asignar, cerrar o devolver | Supervisor del área o Administrador. |
| Iniciar y resolver | Técnico asignado o Administrador. |
| Comentarios internos | Usuario con acceso a la solicitud, salvo Estudiante. |
| Ver reservas | Estudiante titular; Supervisor del área o titular; Administrador. |
| Aprobar o rechazar reserva | Supervisor del área del ambiente o Administrador. |
| Cancelar reserva | Titular o Administrador, con estado y fecha permitidos. |
| Usuarios, catálogos administrativos e historial | Administrador. |

El backend aplica las reglas y devuelve errores si el recurso, estado o rol no corresponde. Los tests de las peticiones esperan el caso exitoso indicado en su descripción; un 403 al probar un rol sin permiso es un resultado de autorización, pero hará fallar ese test de éxito.

## Reset de desarrollo

`SIGESU.dev-reset.postman_collection.json` contiene únicamente `POST /api/admin/reset-demo`. Está separado porque elimina evidencias del almacenamiento y restablece datos demo. Solo existe con perfil `dev` y requiere Administrador.

Si necesitas usarlo en una base descartable, importa esa colección, selecciona el mismo entorno e inicia sesión con la colección principal. Habilita un envío estableciendo `confirmarResetDemo` en `BORRAR_DATOS_DEMO`; el script lo devuelve a `false` antes de enviar. El valor predeterminado omite la petición. Después del reset consulta otra vez los catálogos y actualiza los IDs.

## Validación y alcance

La colección principal contiene 56 peticiones; la de desarrollo contiene una. Juntas cubren las 43 rutas declaradas en los controladores, incluyendo los alias y las acciones concretas de solicitudes y reservas. Ambos archivos de colección se validaron contra el esquema oficial Collection v2.1.

Los ejemplos siguen los contratos del código fuente. Incluyen pruebas de código HTTP y, según la petición, JSON, paginación, estado, ausencia de hash en usuarios, IDs y redirecciones. Se verificaron con simulaciones los scripts de CSRF, su renovación, el bloqueo ante errores, los IDs vacíos, el escape de contraseñas y la protección del reset. No se han ejecutado operaciones contra una base real ni se han probado credenciales.

No exportes ni subas al repositorio el entorno después de completarlo con contraseñas. Los archivos entregados tienen las credenciales vacías y no contienen secretos del backend.

Referencias oficiales: [formato Collection v2.1](https://schema.postman.com/json/collection/v2.1.0/collection.json), [pm.sendRequest](https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/pm-send-request/), [pm.execution](https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/pm-execution/).
