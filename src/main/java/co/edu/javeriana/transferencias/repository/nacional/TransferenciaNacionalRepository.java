package co.edu.javeriana.transferencias.repository.nacional;

import co.edu.javeriana.transferencias.model.Transferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransferenciaNacionalRepository extends JpaRepository<Transferencia, Long> {

    Optional<Transferencia> findByReferencia(String referencia);

    List<Transferencia> findAllByOrderByFechaCreacionDesc();
}
