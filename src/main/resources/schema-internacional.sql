-- Schema para MySQL 8.0 (Banco Internacional)
-- IMPORTANTE: Sintaxis diferente a PostgreSQL

CREATE TABLE IF NOT EXISTS cuenta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_cuenta VARCHAR(20) UNIQUE NOT NULL,
    titular VARCHAR(100) NOT NULL,
    saldo DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    activa BOOLEAN DEFAULT true,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cuenta_numero (numero_cuenta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS movimiento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL COMMENT 'DEBITO, CREDITO',
    monto DECIMAL(15, 2) NOT NULL,
    saldo_anterior DECIMAL(15, 2) NOT NULL,
    saldo_nuevo DECIMAL(15, 2) NOT NULL,
    descripcion VARCHAR(255),
    referencia_transferencia VARCHAR(50),
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_movimiento_cuenta (cuenta_id),
    INDEX idx_movimiento_referencia (referencia_transferencia),
    CONSTRAINT fk_movimiento_cuenta FOREIGN KEY (cuenta_id) 
        REFERENCES cuenta(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Cuentas del Banco Internacional
INSERT INTO cuenta (numero_cuenta, titular, saldo) VALUES
('BI-001', 'Juan Trejos', 8000.00),
('BI-002', 'Pedro Gracia', 3000.00),
('BI-003', 'Harold Vargas', 12000.00),
('BI-004', 'Diego Torres', 6000.00);

Commit;

-- Nota: MySQL usa AUTO_INCREMENT en lugar de SERIAL
-- MySQL usa TIMESTAMP con ON UPDATE CURRENT_TIMESTAMP para auto-actualización
-- MySQL usa ENGINE=InnoDB para soporte de transacciones y foreign keys
