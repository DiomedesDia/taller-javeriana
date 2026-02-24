package co.edu.javeriana.transferencias.repository.internacional;

import co.edu.javeriana.transferencias.model.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoInternacionalRepository extends JpaRepository<Movimiento, Long> {

    List<Movimiento> findByCuentaIdOrderByFechaDesc(Long cuentaId);

    List<Movimiento> findByReferenciaTransferencia(String referencia);
}
