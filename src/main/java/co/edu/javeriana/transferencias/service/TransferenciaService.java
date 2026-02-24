package co.edu.javeriana.transferencias.service;

import co.edu.javeriana.transferencias.dto.TransferenciaDTO;
import co.edu.javeriana.transferencias.exception.TransferenciaException;
import co.edu.javeriana.transferencias.model.EstadoTransferencia;
import co.edu.javeriana.transferencias.model.Transferencia;
import co.edu.javeriana.transferencias.repository.nacional.TransferenciaNacionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orquestador del patron SAGA.
 *
 * Flujo:
 *   PASO 0 - Crear registro INICIADA
 *   PASO 1 - Debitar Banco Nacional (PostgreSQL)   -> falla: FALLIDA
 *   PASO 2 - Estado: DEBITO_COMPLETADO
 *   PASO 3 - Acreditar Banco Internacional (MySQL) -> falla: compensar -> REVERTIDA
 *   PASO 4 - Estado: CREDITO_COMPLETADO
 *   PASO 5 - Estado: COMPLETADA
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferenciaService {

    private final BancoNacionalService bancoNacionalService;
    private final BancoInternacionalService bancoInternacionalService;
    private final TransferenciaNacionalRepository transferenciaRepository;
    private final SagaLogService sagaLogService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Sin @Transactional aqui — cada paso maneja su propia transaccion via SagaLogService
    public TransferenciaDTO.Response ejecutarTransferencia(TransferenciaDTO.Request request) {
        String referencia = "TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("========== INICIO SAGA | {} ==========", referencia);
        log.info("Origen: {} -> Destino: {} | Monto: ${}", request.getCuentaOrigen(),
                request.getCuentaDestino(), request.getMonto());

        // PASO 0: Crear registro
        Transferencia transferencia = new Transferencia();
        transferencia.setReferencia(referencia);
        transferencia.setCuentaOrigen(request.getCuentaOrigen());
        transferencia.setCuentaDestino(request.getCuentaDestino());
        transferencia.setMonto(request.getMonto());
        transferencia.setEstado(EstadoTransferencia.INICIADA);
        transferencia.setDescripcion(
                request.getDescripcion() != null ? request.getDescripcion() : "Transferencia interbancaria");
        transferencia = sagaLogService.guardar(transferencia);
        log.info("[PASO 0] Registro creado: {}", EstadoTransferencia.INICIADA);

        // PASO 1: Debito en PostgreSQL
        try {
            log.info("[PASO 1] Debitando en Banco Nacional...");
            bancoNacionalService.debitar(request.getCuentaOrigen(), request.getMonto(), referencia);
        } catch (TransferenciaException e) {
            // Saldo insuficiente, cuenta inactiva, etc — no hay nada que compensar
            log.error("[PASO 1] Fallo en debito: {}", e.getMessage());
            transferencia.setEstado(EstadoTransferencia.FALLIDA);
            transferencia.setErrorMensaje(e.getMessage());
            sagaLogService.guardar(transferencia);
            return buildResponse(transferencia);
        } catch (Exception e) {
            log.error("[PASO 1] Error inesperado: {}", e.getMessage(), e);
            transferencia.setEstado(EstadoTransferencia.FALLIDA);
            transferencia.setErrorMensaje("Error en debito: " + e.getMessage());
            sagaLogService.guardar(transferencia);
            return buildResponse(transferencia);
        }

        // PASO 2: Estado DEBITO_COMPLETADO
        transferencia.setEstado(EstadoTransferencia.DEBITO_COMPLETADO);
        transferencia = sagaLogService.guardar(transferencia);
        log.info("[PASO 2] Estado: {}", EstadoTransferencia.DEBITO_COMPLETADO);

        // PASO 3: Credito en MySQL
        try {
            log.info("[PASO 3] Acreditando en Banco Internacional...");
            bancoInternacionalService.acreditar(request.getCuentaDestino(), request.getMonto(), referencia);
        } catch (Exception e) {
            // COMPENSACION: el credito fallo, revertir el debito
            log.error("[COMPENSACION] Fallo en credito: {}. Revirtiendo debito...", e.getMessage());
            try {
                bancoNacionalService.revertirDebito(request.getCuentaOrigen(), request.getMonto(), referencia);
                transferencia.setEstado(EstadoTransferencia.REVERTIDA);
                transferencia.setErrorMensaje("Credito fallo: " + e.getMessage() + " | Debito revertido correctamente.");
                sagaLogService.guardar(transferencia);
                log.warn("[COMPENSACION] Debito revertido exitosamente.");
            } catch (Exception compEx) {
                log.error("[COMPENSACION] ERROR CRITICO - no se pudo revertir: {}", compEx.getMessage());
                transferencia.setEstado(EstadoTransferencia.FALLIDA);
                transferencia.setErrorMensaje("ERROR CRITICO - compensacion fallida: " + compEx.getMessage());
                sagaLogService.guardar(transferencia);
            }
            return buildResponse(transferencia);
        }

        // PASO 4: Estado CREDITO_COMPLETADO
        transferencia.setEstado(EstadoTransferencia.CREDITO_COMPLETADO);
        transferencia = sagaLogService.guardar(transferencia);
        log.info("[PASO 4] Estado: {}", EstadoTransferencia.CREDITO_COMPLETADO);

        // PASO 5: COMPLETADA
        transferencia.setEstado(EstadoTransferencia.COMPLETADA);
        transferencia = sagaLogService.guardar(transferencia);
        log.info("========== SAGA COMPLETADO | {} ==========", referencia);

        return buildResponse(transferencia);
    }

    @Transactional(transactionManager = "nacionalTransactionManager", readOnly = true)
    public List<TransferenciaDTO.Response> listarTransferencias() {
        return transferenciaRepository.findAllByOrderByFechaCreacionDesc()
                .stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    @Transactional(transactionManager = "nacionalTransactionManager", readOnly = true)
    public TransferenciaDTO.EstadisticasResponse obtenerEstadisticas() {
        List<Transferencia> todas = transferenciaRepository.findAll();
        TransferenciaDTO.EstadisticasResponse stats = new TransferenciaDTO.EstadisticasResponse();

        stats.setTotalTransferencias(todas.size());
        stats.setCompletadas(todas.stream()
                .filter(t -> EstadoTransferencia.COMPLETADA.equals(t.getEstado())).count());
        stats.setFallidas(todas.stream()
                .filter(t -> EstadoTransferencia.FALLIDA.equals(t.getEstado())).count());
        stats.setRevertidas(todas.stream()
                .filter(t -> EstadoTransferencia.REVERTIDA.equals(t.getEstado())).count());
        stats.setMontoTotal(todas.stream()
                .filter(t -> EstadoTransferencia.COMPLETADA.equals(t.getEstado()))
                .map(Transferencia::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return stats;
    }

    private TransferenciaDTO.Response buildResponse(Transferencia t) {
        TransferenciaDTO.Response r = new TransferenciaDTO.Response();
        r.setReferencia(t.getReferencia());
        r.setCuentaOrigen(t.getCuentaOrigen());
        r.setCuentaDestino(t.getCuentaDestino());
        r.setMonto(t.getMonto());
        r.setEstado(t.getEstado().name());
        r.setErrorDetalle(t.getErrorMensaje());
        r.setMensaje(getMensaje(t.getEstado()));
        if (t.getFechaCreacion() != null) r.setFechaCreacion(t.getFechaCreacion().format(FMT));
        return r;
    }

    private String getMensaje(EstadoTransferencia estado) {
        return switch (estado) {
            case COMPLETADA         -> "Transferencia completada exitosamente";
            case FALLIDA            -> "Transferencia fallida";
            case REVERTIDA          -> "Transferencia revertida — debito compensado";
            case INICIADA           -> "Iniciada";
            case DEBITO_COMPLETADO  -> "Debito completado";
            case CREDITO_COMPLETADO -> "Credito completado";
        };
    }
}
