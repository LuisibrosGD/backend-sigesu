# Estado actual

La carga progresiva reemplaza la espera del bootstrap en el frontend. Las mediciones y los contratos actuales están en [CARGA_PROGRESIVA.md](CARGA_PROGRESIVA.md). Autenticación de estudiante con Neon: mediana 2,476 s; panel visible 20,4 ms después de completar la autenticación; indicadores en segundo plano 2,545 s después.

El resto de este documento conserva la medición anterior de la optimización por lotes.

# Optimización de carga e inicio de sesión

## Cambios

- Bootstrap y listados ensamblan sus relaciones desde mapas exclusivos de cada petición, con lotes de hasta 500 identificadores.
- La selección de solicitudes y reservas autorizadas se ejecuta en PostgreSQL. Las áreas de usuarios y el equipamiento se cargan mediante fetch explícito.
- El bootstrap completo, incluida la consulta del usuario actual, se construye dentro de una transacción de solo lectura. No se usa caché compartida de datos privados.
- Se conservan los contratos JSON, comentarios, historial, evidencias y ocupaciones sin datos privados. La configuración pública consulta solo facultades, edificios y configuración.
- El frontend comparte actualizaciones simultáneas, serializa lecturas después de modificaciones y descarta respuestas de sesiones anteriores. Inicializa configuración pública y sesión en paralelo.

## Validación

- 16 pruebas Java: 13 de integración con PostgreSQL temporal, 2 de reglas y 1 de almacenamiento; sin fallos ni omisiones.
- 4 pruebas del coordinador de actualizaciones del frontend; TypeScript y compilación de producción correctos.
- La construcción del bootstrap usa 15 consultas tanto en la carga de prueba como después de añadir 100 solicitudes dentro de una transacción que se revierte. La comprobación del usuario y la persistencia HTTP de sesión se contabilizan aparte.
- Renderizar un snapshot ya cargado ejecuta cero consultas adicionales.
- Equivalencia de las respuestas con las proyecciones anteriores para los cuatro roles, privacidad de comentarios internos y metadatos de evidencias sin storagePath. Eventos con la misma fecha se comparan sin imponer un orden que el contrato anterior no garantizaba.
- Verificación HTTP de las cuatro cuentas demo y revocación de sesión al salir, usando la conexión Neon configurada y sin modificar datos de negocio.

## Medición con Neon (23 de octubre de 2026)

Backend local Java 24, cuenta demo de administrador y 32 solicitudes. Cinco muestras consecutivas. Acceso incluye la obtención de CSRF y la autenticación; carga corresponde a /api/bootstrap. No se registraron contraseñas, cookies ni tokens.

| Muestra | Acceso (s) | Bootstrap (s) | Total (s) |
| --- | ---: | ---: | ---: |
| 1 | 3.927 | 2.991 | 6.918 |
| 2 | 2.721 | 2.795 | 5.516 |
| 3 | 2.275 | 3.191 | 5.467 |
| 4 | 2.274 | 2.822 | 5.095 |
| 5 | 2.266 | 2.776 | 5.042 |
| Mediana | 2.275 | 2.822 | 5.467 |

El objetivo de bootstrap inferior a 3 s se cumplió en la mediana. El objetivo de entrada completa inferior a 5 s no se alcanzó: la mediana fue 5.467 s. Los viajes de red a Neon y la persistencia de sesiones siguen formando parte del tiempo; no se midió por separado su contribución exacta.

El servidor anterior no completó la primera muestra dentro del timeout de 65 s, por lo que no hay una mediana anterior comparable ni se calcula un porcentaje de mejora.

## Aplicar y repetir

Reiniciar SigesuApplication desde IntelliJ y mantener npm run dev activo en el frontend. No hay migraciones nuevas ni cambios de IDs o datos de negocio.

Ejecutar npm run test:refresh, npm run lint y npm run build en el frontend. En el backend ejecutar mvnw.cmd test con TEST_DATABASE_URL, TEST_DATABASE_USERNAME y TEST_DATABASE_PASSWORD apuntando exclusivamente a PostgreSQL de pruebas. Sin TEST_DATABASE_URL, las pruebas de integración se omiten por diseño.