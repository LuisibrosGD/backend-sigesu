create table usuarios (
 id varchar(80) primary key,
 codigo_universitario text,
 nombre text,
 email text,
 password_hash text,
 rol text,
 facultad_id text,
 telefono text,
 cargo text,
 activo boolean not null default false,
 fecha_registro timestamptz,
 version bigint not null default 0
);
create table areas (
 id varchar(80) primary key,
 nombre text,
 codigo text,
 descripcion text,
 correo_contacto text,
 ubicacion_oficina text,
 activo boolean not null default false
);
create table facultades (
 id varchar(80) primary key,
 nombre text,
 siglas text,
 decano text,
 activo boolean not null default false
);
create table edificios (
 id varchar(80) primary key,
 facultad_id text,
 nombre text,
 pisos integer not null default 0,
 referencia text,
 activo boolean not null default false
);
create table categorias (
 id varchar(80) primary key,
 nombre text,
 tipo_servicio text,
 area_id text,
 prioridad_sugerida text,
 descripcion text,
 activo boolean not null default false
);
create table prioridades (
 nivel varchar(80) primary key,
 plazo_horas integer not null default 0,
 descripcion text,
 orden integer not null default 0
);
create table ambientes (
 id varchar(80) primary key,
 codigo text,
 nombre text,
 tipo text,
 facultad_id text,
 edificio_nombre text,
 ubicacion_piso text,
 capacidad integer not null default 0,
 area_id text,
 horario_inicio time,
 horario_fin time,
 activo boolean not null default false,
 version bigint not null default 0
);
create table configuracion (
 id varchar(80) primary key,
 nombre_institucion text,
 siglas_institucion text,
 periodo_academico text,
 correo_soporte_general text,
 telefono_central text,
 permitir_registro_publico boolean not null default false,
 tamano_maximo_adjunto_mb integer not null default 0,
 dias_maximos_antelacion_reserva integer not null default 0,
 mensaje_institucional text
);
create table solicitudes (
 id varchar(80) primary key,
 codigo text,
 solicitante_id text,
 categoria_id text,
 titulo text,
 descripcion text,
 facultad_id text,
 edificio_id text,
 ubicacion_especifica text,
 prioridad text,
 fecha_registro timestamptz,
 fecha_limite timestamptz,
 estado text,
 area_id text,
 tecnico_id text,
 fecha_evaluacion timestamptz,
 fecha_asignacion timestamptz,
 fecha_inicio_atencion timestamptz,
 fecha_resolucion timestamptz,
 fecha_cierre timestamptz,
 solucion_descripcion text,
 atendida_fuera_de_plazo boolean not null default false,
 version bigint not null default 0
);
create table comentarios (
 id varchar(80) primary key,
 solicitud_id text,
 autor_id text,
 texto text,
 es_interno boolean not null default false,
 fecha timestamptz
);
create table historial (
 id varchar(80) primary key,
 solicitud_id text,
 reserva_id text,
 actor_id text,
 actor_nombre text,
 actor_rol text,
 accion text,
 estado_anterior text,
 estado_nuevo text,
 motivo text,
 detalles text,
 fecha timestamptz
);
create table reservas (
 id varchar(80) primary key,
 codigo text,
 ambiente_id text,
 solicitante_id text,
 fecha date,
 hora_inicio time,
 hora_fin time,
 motivo text,
 descripcion_actividad text,
 cantidad_participantes integer not null default 0,
 estado text,
 motivo_rechazo_o_cancelacion text,
 aprobado_por_id text,
 fecha_registro timestamptz,
 version bigint not null default 0
);
create table notificaciones (
 id varchar(80) primary key,
 usuario_id text,
 tipo text,
 titulo text,
 mensaje text,
 fecha timestamptz,
 leida boolean not null default false,
 referencia_tipo text,
 referencia_id text,
 referencia_codigo text,
 clave_unica_vencimiento text
);
create table evidencias (
 id varchar(80) primary key,
 solicitud_id text,
 nombre_archivo text,
 tipo_mime text,
 tamano_bytes bigint not null default 0,
 storage_path text,
 subido_por_id text,
 fecha_subida timestamptz
);
create table usuario_areas (usuario_id varchar(80) references usuarios(id), area_id varchar(80) references areas(id), primary key(usuario_id,area_id));
create table ambiente_equipamiento (ambiente_id varchar(80) references ambientes(id), equipo text not null, primary key(ambiente_id,equipo));
create table contadores (tipo varchar(8), anio integer, valor bigint not null, primary key(tipo,anio));
alter table usuarios add foreign key (facultad_id) references facultades(id);
alter table edificios add foreign key (facultad_id) references facultades(id);
alter table categorias add foreign key (area_id) references areas(id);
alter table ambientes add foreign key (facultad_id) references facultades(id);
alter table ambientes add foreign key (area_id) references areas(id);
alter table solicitudes add foreign key (solicitante_id) references usuarios(id);
alter table solicitudes add foreign key (categoria_id) references categorias(id);
alter table solicitudes add foreign key (facultad_id) references facultades(id);
alter table solicitudes add foreign key (edificio_id) references edificios(id);
alter table solicitudes add foreign key (area_id) references areas(id);
alter table solicitudes add foreign key (tecnico_id) references usuarios(id);
alter table comentarios add foreign key (solicitud_id) references solicitudes(id);
alter table comentarios add foreign key (autor_id) references usuarios(id);
alter table historial add foreign key (solicitud_id) references solicitudes(id);
alter table historial add foreign key (reserva_id) references reservas(id);
alter table historial add foreign key (actor_id) references usuarios(id);
alter table reservas add foreign key (ambiente_id) references ambientes(id);
alter table reservas add foreign key (solicitante_id) references usuarios(id);
alter table reservas add foreign key (aprobado_por_id) references usuarios(id);
alter table evidencias add foreign key (solicitud_id) references solicitudes(id);
alter table evidencias add foreign key (subido_por_id) references usuarios(id);
alter table notificaciones add foreign key (usuario_id) references usuarios(id);
create unique index usuario_email_uq on usuarios(lower(email));
create unique index usuario_codigo_uq on usuarios(lower(codigo_universitario));
create unique index solicitud_codigo_uq on solicitudes(codigo);
create unique index reserva_codigo_uq on reservas(codigo);
create unique index vencimiento_uq on notificaciones(clave_unica_vencimiento);
alter table usuarios add check (rol in ('Estudiante','Técnico','Supervisor','Administrador'));
alter table solicitudes add check (estado in ('Registrada','En evaluación','Asignada','En atención','Resuelta','Cerrada'));
alter table reservas add check (estado in ('Solicitada','Aprobada','Rechazada','Cancelada','Finalizada'));
alter table reservas add check (hora_fin > hora_inicio and cantidad_participantes > 0);
alter table configuracion add check (tamano_maximo_adjunto_mb between 1 and 5);
alter table prioridades add check (plazo_horas between 1 and 720);
alter table historial add check ((solicitud_id is null) <> (reserva_id is null));
create index solicitudes_area_estado on solicitudes(area_id,estado);
create index solicitudes_tecnico on solicitudes(tecnico_id);
create index solicitudes_solicitante on solicitudes(solicitante_id);
create index reservas_disponibilidad on reservas(ambiente_id,fecha,estado);
create index notificaciones_usuario on notificaciones(usuario_id,fecha desc);
