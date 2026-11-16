package cl.copat.evalua.departamento3.api;

import cl.copat.evalua.departamento1.api.BitacoraResponse;
import cl.copat.evalua.departamento1.aplicacion.SolicitudService;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import cl.copat.evalua.departamento3.aplicacion.Departamento3Service;
import cl.copat.evalua.departamento3.api.IndicadorResponse;
import cl.copat.evalua.publico.api.ReclamoResponse;
import cl.copat.evalua.publico.api.SeguimientoRequest;
import cl.copat.evalua.publico.aplicacion.ReclamoService;
import cl.copat.evalua.shared.web.ArchivoDescarga;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/departamento3")
public class Departamento3Controller {

  private final Departamento3Service servicio;
  private final SolicitudService solicitudes;
  private final ReclamoService reclamoServicio;

  public Departamento3Controller(
      Departamento3Service servicio,
      SolicitudService solicitudes,
      ReclamoService reclamoServicio) {
    this.servicio = servicio;
    this.solicitudes = solicitudes;
    this.reclamoServicio = reclamoServicio;
  }

  @GetMapping("/bitacora")
  public List<BitacoraResponse> bitacora(Authentication authentication) {
    return solicitudes.bitacoraSeguimiento(principal(authentication));
  }

  @GetMapping("/solicitudes")
  public List<RevisionResponse> listar(Authentication authentication) {
    return servicio.listar(principal(authentication));
  }

  @GetMapping("/solicitudes/{id}/pdf")
  public ResponseEntity<byte[]> pdf(@PathVariable long id, Authentication authentication) {
    return archivo(servicio.pdf(id, principal(authentication)));
  }

  @GetMapping("/solicitudes/{id}/evidencia")
  public ResponseEntity<byte[]> evidencia(@PathVariable long id, Authentication authentication) {
    return archivo(servicio.evidencia(id, principal(authentication)));
  }

  @PostMapping(value = "/solicitudes/{id}/revision", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public RevisionResponse revision(
      @PathVariable long id,
      @RequestParam(value = "descripcion", required = false) String descripcion,
      @RequestParam(value = "estado", required = false) String estado,
      @RequestParam(value = "evidencia", required = false) MultipartFile evidencia,
      Authentication authentication) {
    return servicio.guardar(id, descripcion, estado, evidencia, principal(authentication));
  }

  @GetMapping("/indicadores")
  public List<IndicadorResponse> indicadores(Authentication authentication) {
    return servicio.indicadores(principal(authentication));
  }

  @GetMapping("/reclamos")
  public List<ReclamoResponse> reclamos(Authentication authentication) {
    return reclamoServicio.listar(principal(authentication));
  }

  @PutMapping("/reclamos/{id}")
  public ReclamoResponse seguimiento(
      @PathVariable long id,
      @RequestBody SeguimientoRequest body,
      Authentication authentication) {
    return reclamoServicio.seguir(id, body, principal(authentication));
  }

  private static ResponseEntity<byte[]> archivo(ArchivoDescarga descarga) {
    String nombre = descarga.nombre().replace("\"", "").replace("\r", "").replace("\n", "");
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(descarga.mediaType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
        .body(descarga.contenido());
  }

  private static UsuarioPrincipal principal(Authentication authentication) {
    return (UsuarioPrincipal) authentication.getPrincipal();
  }
}
