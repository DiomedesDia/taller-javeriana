package co.edu.javeriana.transferencias.service;

import co.edu.javeriana.transferencias.model.Transferencia;
import co.edu.javeriana.transferencias.repository.nacional.TransferenciaNacionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaLogService {

    private final TransferenciaNacionalRepository transferenciaRepository;

    /**
     * Guarda el estado del SAGA en una transacción INDEPENDIENTE (REQUIRES_NEW).
     * Esto garantiza que el log persiste aunque la transacción principal haga rollback.
     */
    @Transactional(transactionManager = "nacionalTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public Transferencia guardar(Transferencia transferencia) {
        Transferencia saved = transferenciaRepository.save(transferencia);
        log.info("[SAGA-LOG] Estado guardado: {} | ref: {}", saved.getEstado(), saved.getReferencia());
        return saved;
    }
}
