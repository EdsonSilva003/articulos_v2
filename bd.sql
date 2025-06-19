-- Eliminar la base de datos "" si ya existe
DROP DATABASE IF EXISTS proyecto;

-- Crear la base de datos "proyecto" con codificación UTF-8
CREATE DATABASE proyecto CHARACTER SET utf8 COLLATE utf8_general_ci;

-- Usar la base de datos "proyecto"
USE proyecto;

-- Crear la tabla de usuarios
CREATE TABLE usuarios (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(64) NOT NULL,
    email VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL
);

-- Crear la tabla de roles
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(64) NOT NULL UNIQUE
);

-- Crear la tabla intermedia para la relación muchos a muchos entre usuarios y roles
CREATE TABLE usuario_roles (
    usuario_id BIGINT,
    rol_id BIGINT,
    PRIMARY KEY (usuario_id, rol_id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    FOREIGN KEY (rol_id) REFERENCES roles(id)
);

-- Eliminar el usuario 'admin' si ya existe
DROP USER IF EXISTS 'admin'@'localhost';
FLUSH PRIVILEGES;

-- Crear el usuario 'admin' con la contraseña 'admin'
CREATE USER 'admin'@'localhost' IDENTIFIED BY 'admin';

-- Otorgar todos los permisos sobre la base de datos "proyecto" al usuario 'admin'
GRANT ALL PRIVILEGES ON proyecto.* TO 'admin'@'localhost';

-- Aplicar los cambios
FLUSH PRIVILEGES;


-- Para el BLOB:

ALTER TABLE usuarios ADD COLUMN imagen LONGBLOB;