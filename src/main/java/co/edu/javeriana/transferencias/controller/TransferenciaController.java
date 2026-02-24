package co.edu.javeriana.transferencias.controller;

import co.edu.javeriana.transferencias.dto.TransferenciaDTO;
import co.edu.javeriana.transferencias.exception.TransferenciaException;
import co.edu.javeriana.transferencias.model.Cuenta;
import co.edu.javeriana.transferencias.service.BancoInternacionalService;
import co.edu.javeriana.transferencias.service.BancoNacionalService;
import co.edu.javeriana.transferencias.service.TransferenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransferenciaController {

    private final TransferenciaService transferenciaService;
    private final BancoNacionalService bancoNacionalService;
    private final BancoInternacionalService bancoInternacionalService;

    /** POST /api/transferencias — Ejecutar transferencia SAGA */
    @PostMapping("/transferencias")
    public ResponseEntity<TransferenciaDTO.Response> realizar(
            @Valid @RequestBody TransferenciaDTO.Request request) {

        TransferenciaDTO.Response response = transferenciaService.ejecutarTransferencia(request);
        HttpStatus status = "COMPLETADA".equals(response.getEstado()) ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    /** GET /api/transferencias — Historial */
    @GetMapping("/transferencias")
    public ResponseEntity<List<TransferenciaDTO.Response>> historial() {
        return ResponseEntity.ok(transferenciaService.listarTransferencias());
    }

    /** GET /api/cuentas — Cuentas de ambos bancos */
    @GetMapping("/cuentas")
    public ResponseEntity<Map<String, Object>> cuentas() {
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("bancoNacional", bancoNacionalService.listarCuentas()
                .stream().map(this::toCuentaInfo).collect(Collectors.toList()));
        resultado.put("bancoInternacional", bancoInternacionalService.listarCuentas()
                .stream().map(this::toCuentaInfo).collect(Collectors.toList()));
        return ResponseEntity.ok(resultado);
    }

    /** GET /api/stats — Estadísticas */
    @GetMapping("/stats")
    public ResponseEntity<TransferenciaDTO.EstadisticasResponse> stats() {
        return ResponseEntity.ok(transferenciaService.obtenerEstadisticas());
    }

    /** POST /api/config/simular-fallo — Activar/desactivar fallos */
    @PostMapping("/config/simular-fallo")
    public ResponseEntity<Map<String, Object>> simularFallo(@RequestParam boolean activo) {
        bancoInternacionalService.setSimularFallo(activo);
        Map<String, Object> res = new HashMap<>();
        res.put("simulacionFallos", activo);
        res.put("mensaje", activo ? "⚠️ Fallos activados (25%)" : "✅ Fallos desactivados");
        return ResponseEntity.ok(res);
    }

    /** GET /api/config/estado */
    @GetMapping("/config/estado")
    public ResponseEntity<Map<String, Object>> estadoConfig() {
        Map<String, Object> res = new HashMap<>();
        res.put("simulacionFallos", bancoInternacionalService.isSimularFallo());
        return ResponseEntity.ok(res);
    }

    @ExceptionHandler(TransferenciaException.class)
    public ResponseEntity<Map<String, String>> handleTransferenciaEx(TransferenciaException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericEx(Exception ex) {
        log.error("Error no manejado: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body(Map.of("error", "Error interno del servidor"));
    }

    private TransferenciaDTO.CuentaInfo toCuentaInfo(Cuenta c) {
        TransferenciaDTO.CuentaInfo info = new TransferenciaDTO.CuentaInfo();
        info.setId(c.getId());
        info.setNumeroCuenta(c.getNumeroCuenta());
        info.setTitular(c.getTitular());
        info.setSaldo(c.getSaldo());
        info.setActiva(c.getActiva());
        return info;
    }
}
