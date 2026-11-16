package cl.copat.evalua.departamento2.api;

import cl.copat.evalua.departamento1.api.BitacoraResponse;
import cl.copat.evalua.departamento1.api.SolicitudResponse;
import cl.copat.evalua.departamento1.aplicacion.SolicitudService;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import cl.copat.evalua.departamento2.aplicacion.Departamento2Service;
import cl.copat.evalua.publico.api.EnlaceResponse;
import cl.copat.evalua.publico.aplicacion.EncuestaPublicaService;
import cl.copat.evalua.shared.web.ArchivoDescarga;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/departamento2")
public class Departamento2Controller {

  private final Departamento2Service servicio;
  private final SolicitudService solicitudes;
  private final EncuestaPublicaService encuestas;

  public Departamento2Controller(
      Departamento2Service servicio,
      SolicitudService solicitudes,
      EncuestaPublicaService encuestas) {
    this.servicio = servicio;
    this.solicitudes = solicitudes;
    this.encuestas = encuestas;
  }

  @GetMapping("/estados")
  public List<String> estados(Authentication authentication) {
    return servicio.estados(principal(authentication));
  }

  @GetMapping("/solicitudes")
  public List<SolicitudResponse> listar(
      @RequestParam(required = false) String estado,
      Authentication authentication) {
    return servicio.listar(estado, principal(authentication));
  }

  @GetMapping("/bitacora")
  public List<BitacoraResponse> bitacora(Authentication authentication) {
    return solicitudes.bitacoraSeguimiento(principal(authentication));
  }

  @GetMapping("/solicitudes/{id}/guia")
  public ResponseEntity<byte[]> guia(@PathVariable long id, Authentication authentication) {
    return archivo(servicio.guia(id, principal(authentication)));
  }

  @PostMapping("/solicitudes/{id}/enviada")
  public SolicitudResponse enviada(@PathVariable long id, Authentication authentication) {
    return servicio.marcarEnviada(id, principal(authentication));
  }

  @PostMapping(value = "/solicitudes/{id}/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public SolicitudResponse pdf(
      @PathVariable long id,
      @RequestParam(value = "archivo", required = false) MultipartFile archivo,
      Authentication authentication) {
    return servicio.adjuntar(id, archivo, principal(authentication));
  }

  @PostMapping("/solicitudes/{id}/reenviar")
  public SolicitudResponse reenviar(@PathVariable long id, Authentication authentication) {
    return servicio.reenviar(id, principal(authentication));
  }

  @GetMapping("/solicitudes/{id}/enlace")
  public EnlaceResponse enlaceExistente(@PathVariable long id, Authentication authentication) {
    return encuestas.existente(id, principal(authentication));
  }

  @PostMapping("/solicitudes/{id}/enlace")
  public EnlaceResponse enlace(@PathVariable long id, Authentication authentication) {
    return encuestas.crear(id, principal(authentication));
  }

  private static ResponseEntity<byte[]> archivo(ArchivoDescarga descarga) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(descarga.mediaType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + descarga.nombre() + "\"")
        .body(descarga.contenido());
  }

  private static UsuarioPrincipal principal(Authentication authentication) {
    return (UsuarioPrincipal) authentication.getPrincipal();
  }
}
