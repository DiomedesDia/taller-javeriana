package co.edu.javeriana.transferencias.service;

import co.edu.javeriana.transferencias.exception.TransferenciaException;
import co.edu.javeriana.transferencias.model.Cuenta;
import co.edu.javeriana.transferencias.model.Movimiento;
import co.edu.javeriana.transferencias.repository.internacional.CuentaInternacionalRepository;
import co.edu.javeriana.transferencias.repository.internacional.MovimientoInternacionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class BancoInternacionalService {

    private final CuentaInternacionalRepository cuentaRepository;
    private final MovimientoInternacionalRepository movimientoRepository;

    private boolean simularFallo = false;
    private final Random random = new Random();

    /**
     * PASO 3 del SAGA — Acreditar fondos en MySQL.
     * Si llega a fallar, el orquestador SAGA debe compensar revirtiendo el débito.
     */
    @Transactional(transactionManager = "internacionalTransactionManager")
    public void acreditar(String numeroCuenta, BigDecimal monto, String referencia) {
        log.info("[BancoInternacional] Acreditando ${} en cuenta {} | ref: {}", monto, numeroCuenta, referencia);

        // Simular fallo aleatorio del 90%
        if (simularFallo && random.nextInt(10) < 9) {
            log.error("[BancoInternacional] FALLO SIMULADO activado!");
            throw new TransferenciaException("Fallo simulado en Banco Internacional");
        }

        Cuenta cuenta = cuentaRepository.findByNumeroCuentaWithLock(numeroCuenta)
                .orElseThrow(() -> new TransferenciaException("Cuenta destino no encontrada: " + numeroCuenta));

        if (!cuenta.getActiva()) {
            throw new TransferenciaException("La cuenta destino está inactiva: " + numeroCuenta);
        }

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
        movimiento.setDescripcion("Transferencia internacional - crédito");
        movimiento.setReferenciaTransferencia(referencia);
        movimientoRepository.save(movimiento);

        log.info("[BancoInternacional] Crédito OK. Saldo: ${} → ${}", saldoAnterior, saldoNuevo);
    }

    @Transactional(transactionManager = "internacionalTransactionManager", readOnly = true)
    public List<Cuenta> listarCuentas() {
        return cuentaRepository.findByActivaTrue();
    }

    @Transactional(transactionManager = "internacionalTransactionManager", readOnly = true)
    public Cuenta obtenerCuenta(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new TransferenciaException("Cuenta no encontrada: " + numeroCuenta));
    }

    public void setSimularFallo(boolean simularFallo) {
        this.simularFallo = simularFallo;
        log.warn("[CONFIG] Simulación de fallos: {}", simularFallo ? "ACTIVADA (25%)" : "DESACTIVADA");
    }

    public boolean isSimularFallo() {
        return simularFallo;
    }
}
