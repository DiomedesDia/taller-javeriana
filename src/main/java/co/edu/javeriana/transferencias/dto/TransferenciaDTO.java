package co.edu.javeriana.transferencias.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

public class TransferenciaDTO {

    @Data
    public static class Request {

        @NotBlank(message = "La cuenta origen es requerida")
        private String cuentaOrigen;

        @NotBlank(message = "La cuenta destino es requerida")
        private String cuentaDestino;

        @NotNull(message = "El monto es requerido")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        private BigDecimal monto;

        private String descripcion;
    }

    @Data
    public static class Response {
        private String referencia;
        private String cuentaOrigen;
        private String cuentaDestino;
        private BigDecimal monto;
        private String estado;
        private String mensaje;
        private String errorDetalle;
        private String fechaCreacion;
    }

    @Data
    public static class CuentaInfo {
        private Long id;
        private String numeroCuenta;
        private String titular;
        private BigDecimal saldo;
        private Boolean activa;
    }

    @Data
    public static class EstadisticasResponse {
        private long totalTransferencias;
        private long completadas;
        private long fallidas;
        private long revertidas;
        private BigDecimal montoTotal;
    }
}
