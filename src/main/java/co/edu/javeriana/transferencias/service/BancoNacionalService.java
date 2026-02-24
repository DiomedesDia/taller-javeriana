package co.edu.javeriana.transferencias.service;

import co.edu.javeriana.transferencias.exception.TransferenciaException;
import co.edu.javeriana.transferencias.model.Cuenta;
import co.edu.javeriana.transferencias.model.Movimiento;
import co.edu.javeriana.transferencias.repository.nacional.CuentaNacionalRepository;
import co.edu.javeriana.transferencias.repository.nacional.MovimientoNacionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BancoNacionalService {

    private final CuentaNacionalRepository cuentaRepository;
    private final MovimientoNacionalRepository movimientoRepository;

    /**
     * PASO 1 del SAGA — Debitar fondos en PostgreSQL.
     * Usa lock pesimista para evitar race conditions.
     */
    @Transactional(transactionManager = "nacionalTransactionManager")
    public void debitar(String numeroCuenta, BigDecimal monto, String referencia) {
        log.info("[BancoNacional] Debitando ${} de cuenta {} | ref: {}", monto, numeroCuenta, referencia);

        Cuenta cuenta = cuentaRepository.findByNumeroCuentaWithLock(numeroCuenta)
                .orElseThrow(() -> new TransferenciaException("Cuenta origen no encontrada: " + numeroCuenta));

        if (!cuenta.getActiva()) {
            throw new TransferenciaException("La cuenta origen está inactiva: " + numeroCuenta);
        }

        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new TransferenciaException(
                    String.format("Saldo insuficiente. Disponible: $%.2f | Requerido: $%.2f",
                            cuenta.getSaldo(), monto));
        }

        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal saldoNuevo = saldoAnterior.subtract(monto);
        cuenta.setSaldo(saldoNuevo);
        cuentaRepository.save(cuenta);

        Movimiento movimiento = new Movimiento();
        movimiento.setCuentaId(cuenta.getId());
        movimiento.setTipo("DEBITO");
        movimiento.setMonto(monto);
        movimiento.setSaldoAnterior(saldoAnterior);
        movimiento.setSaldoNuevo(saldoNuevo);
        movimiento.setDescripcion("Transferencia internacional - débito");
        movimiento.setReferenciaTransferencia(referencia);
        movimientoRepository.save(movimiento);

        log.info("[BancoNacional] Débito OK. Saldo: ${} → ${}", saldoAnterior, saldoNuevo);
    }

    /**
     * COMPENSACIÓN del SAGA — Revertir el débito si el crédito falló.
     */
    @Transactional(transactionManager = "nacionalTransactionManager")
    public void revertirDebito(String numeroCuenta, BigDecimal monto, String referencia) {
        log.warn("[BancoNacional][COMPENSACION] Revirtiendo débito de ${} en cuenta {} | ref: {}",
                monto, numeroCuenta, referencia);

        Cuenta cuenta = cuentaRepository.findByNumeroCuentaWithLock(numeroCuenta)
                .orElseThrow(() -> new TransferenciaException(
                        "Cuenta no encontrada para compensación: " + numeroCuenta));

        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal saldoNuevo = saldoAnterior.add(monto);
        cuenta.setSaldo(saldoNuevo);
        cuentaRepository.save(cuenta);

        Movimiento movimiento = new Movimiento();
        movimiento.setCuentaId(cuenta.getId());
        movimiento.setTipo("CREDITO");
        movimiento.setMonto(monto);
        movimiento.setSaldoAnterior(saldoAnterior);
        movimiento.setSaldoNuevo(saldoNuevo);
        movimiento.setDescripcion("COMPENSACION - Reversión de transferencia fallida");
        movimiento.setReferenciaTransferencia(referencia + "_REV");
        movimientoRepository.save(movimiento);

        log.warn("[BancoNacional][COMPENSACION] Saldo restaurado: ${}", saldoNuevo);
    }

    @Transactional(transactionManager = "nacionalTransactionManager", readOnly = true)
    public List<Cuenta> listarCuentas() {
        return cuentaRepository.findByActivaTrue();
    }

    @Transactional(transactionManager = "nacionalTransactionManager", readOnly = true)
    public Cuenta obtenerCuenta(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new TransferenciaException("Cuenta no encontrada: " + numeroCuenta));
    }
}
