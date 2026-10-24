# Carga progresiva de SIGESU

## Comportamiento actual

La autenticación y la recuperación de sesión guardan directamente el usuario devuelto por el servidor. El frontend no solicita `/api/bootstrap`. La estructura de navegación se muestra al autenticar; cada pantalla descarga sus datos y su código de forma independiente. El formulario de login sigue apareciendo sin esperar a la API.

| Recurso | Carga |
| --- | --- |
| Panel | Agregaciones de PostgreSQL y hasta diez pendientes prioritarios; sin reconstruir todas las solicitudes |
| Solicitudes | Una página con filtros, búsqueda, orden y conteos en PostgreSQL |
| Detalle | Comentarios, historial y evidencias solo al abrir una solicitud |
| Reservas | Página de reservas propias o del área; ocupaciones únicamente para las fechas consultadas |
| Notificaciones | Página filtrada y contador independiente de no leídas |
| Administración | Usuarios paginados, historial paginado y trabajo pendiente del usuario que se edita |
| Catálogos | Solo los grupos que necesita la pantalla o el formulario |

Los filtros y la posición de navegación se conservan, incluida la página de notificaciones. Un enlace de notificación resuelve el detalle autorizado aunque el registro nunca haya estado en una página descargada. Marcar todas las notificaciones actúa sobre todas las del usuario, no solo sobre la página visible, y conserva la opción de deshacer.

## Caché y modificaciones

La caché vive únicamente en memoria y se reinicia al cambiar usuario, rol o áreas, al salir o al caducar la sesión. Los recursos se identifican por URL con filtros, orden y página. Los resultados y detalles duran 30 segundos; los catálogos, cinco minutos.

Al volver se muestran los datos almacenados y se revalida en segundo plano si vencieron. Las consultas simultáneas comparten su petición. Las respuestas de sesiones anteriores o invalidadas se descartan. Al guardar se publica el registro devuelto y se invalidan los recursos afectados; solo los recursos activos vuelven a consultar. No se recarga un snapshot global.

## API

- `GET /api/solicitudes` y `GET /api/reservas`: `{content,page,size,totalElements}`, página base cero, tamaño inicial 10 y máximo 100. Los listados no contienen comentarios, historial ni evidencias. Los detalles existentes conservan esos campos y sus permisos.
- `GET /api/dashboard/resumen` e `/indicadores`: agregaciones y datos para las tarjetas, distribuciones, evolución temporal, técnicos y pendientes prioritarios. `totalVisibles` permite distinguir ausencia de registros de ausencia de coincidencias con los filtros. `countsOnly=true` omite los pendientes para los contadores de la bandeja.
- `GET /api/solicitudes/exportar`: todos los resultados autorizados y filtrados, leídos por lotes; permite exportar sin limitarse a la página visible.
- `GET /api/catalogos?include=areas,categorias,...`: grupos seleccionados. Omitir `include` conserva la respuesta anterior.
- `GET /api/reservas/ocupaciones?fechaDesde=YYYY-MM-DD&fechaHasta=YYYY-MM-DD`: intervalo de hasta 62 días y ambiente opcional. Solo expone ID, ambiente, fecha, horas y estado.
- `GET /api/notificaciones/pagina` y `/resumen`: página filtrada y conteos independientes. Las rutas anteriores siguen disponibles.
- `GET /api/admin/usuarios`, `/usuarios/{id}/pendientes` y `/historial`: lecturas específicas con autorización de administrador.
- `/api/bootstrap` permanece por compatibilidad, sin uso desde la interfaz actual.

Se mantienen cookies HttpOnly, CSRF, autorización por propietario, técnico y área, y filtros de comentarios internos. No se añadieron migraciones ni se cambiaron IDs.

## Validación del 23 de octubre de 2026

- Backend: 22 pruebas aprobadas, incluidas 19 de integración en PostgreSQL aislado. Validan los cuatro roles, filtros, exportación completa, fechas de Lima, promedios, catálogos, orden por última actualización con historial y privacidad de las ocupaciones.
- Con 1.032 solicitudes, pedir diez filas ejecutó siete consultas y cargó 40 entidades, contando las relaciones de catálogo; no cargó las 1.032 solicitudes.
- Frontend: 15 pruebas de navegador, seis de caché y cuatro de regresión de concurrencia aprobadas. TypeScript y la compilación de producción también pasaron.
- Se verificó la navegación real con Neon para estudiante, técnico, supervisor y administrador, sin errores JavaScript ni respuestas fallidas de los recursos consultados.
- Con datos retrasados diez segundos, el panel permaneció visible. En la prueba controlada final, la mediana de aparición del panel después de autenticar fue 22 ms; el login se mostró en 84,5 ms con caché de navegador vacía.

### Cinco accesos de estudiante con Neon

Backend local Java 24 y frontend de producción servido mediante un proxy temporal. La autenticación se mide hasta recibir el cuerpo completo de la respuesta e incluye CSRF. El panel se mide desde esa respuesta hasta el primer render de la estructura de navegación. Los indicadores se miden hasta recibir su respuesta completa; no bloquean el panel.

| Muestra | Autenticación (ms) | Panel después de autenticar (ms) | Indicadores después de autenticar (ms) |
| --- | ---: | ---: | ---: |
| 1 | 3488,4 | 23,9 | 2809,6 |
| 2 | 2456,5 | 18,4 | 2545,0 |
| 3 | 2657,7 | 23,4 | 2514,0 |
| 4 | 2475,5 | 20,4 | 2574,2 |
| 5 | 2337,8 | 18,0 | 2525,4 |
| Mediana | **2475,5** | **20,4** | **2545,0** |

Los tiempos varían con la red y el estado de Neon. Estas cifras no significan que la autenticación sea instantánea: ahora termina sin esperar la carga del panel completo.

## Repetir las comprobaciones

Frontend: `npm run lint`, `npm run build`, `npm run test:cache`, `npm run test:refresh`. Para las pruebas de navegador, servir `dist` con `npm run preview -- --host 127.0.0.1 --port 4175 --strictPort` y ejecutar `npm run test:login` en otra terminal. Estas pruebas interceptan la API y no requieren Neon.

Backend: `mvnw.cmd test` con `TEST_DATABASE_URL`, `TEST_DATABASE_USERNAME` y `TEST_DATABASE_PASSWORD` apuntando exclusivamente a una base de pruebas. Sin `TEST_DATABASE_URL`, las pruebas de integración se omiten por diseño.

`scripts/benchmark-student.mjs` y `scripts/progressive-smoke.mjs` son comprobaciones manuales que usan las cuentas demo reales contra `LOGIN_TEST_URL` (por defecto, el proxy temporal de pruebas en el puerto 4176). No forman parte de las pruebas automáticas y requieren un proxy real a la API. No utilizarlos contra una base de producción con cuentas distintas.

Para usar los cambios, detener y volver a ejecutar `SigesuApplication` en IntelliJ y recargar el frontend. La compilación del backend no sustituye por sí sola las clases cargadas en un proceso Java que ya está ejecutándose.
