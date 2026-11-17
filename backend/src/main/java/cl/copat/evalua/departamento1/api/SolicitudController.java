package cl.copat.evalua.departamento1.api;

import cl.copat.evalua.departamento1.aplicacion.SolicitudService;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import cl.copat.evalua.departamento2.aplicacion.Departamento2Service;
import cl.copat.evalua.publico.api.EnlaceResponse;
import cl.copat.evalua.publico.api.VistaEnvioResponse;
import cl.copat.evalua.publico.aplicacion.EncuestaPublicaService;
import cl.copat.evalua.shared.web.ArchivoDescarga;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/departamento1")
public class SolicitudController {

  private final SolicitudService solicitudes;
  private final Departamento2Service operacion;
  private final EncuestaPublicaService encuestas;

  public SolicitudController(
      SolicitudService solicitudes,
      Departamento2Service operacion,
      EncuestaPublicaService encuestas) {
    this.solicitudes = solicitudes;
    this.operacion = operacion;
    this.encuestas = encuestas;
  }

  @GetMapping("/estados")
  public List<String> estados() {
    return solicitudes.estados();
  }

  @GetMapping("/plantillas")
  public List<String> plantillas() {
    return solicitudes.plantillas();
  }

  @GetMapping("/sedes")
  public List<String> sedes() {
    return solicitudes.sedes();
  }

  @GetMapping("/solicitudes")
  public List<SolicitudResponse> listar(@RequestParam(required = false) String estado, Authentication authentication) {
    return operacion.listar(estado, principal(authentication));
  }

  @PostMapping("/solicitudes")
  public ResponseEntity<SolicitudResponse> crear(
      @Valid @RequestBody CrearSolicitudRequest request,
      Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED).body(solicitudes.crear(request, principal(authentication)));
  }

  @PutMapping("/solicitudes/{id}")
  public SolicitudResponse editar(
      @PathVariable long id,
      @Valid @RequestBody CrearSolicitudRequest request,
      Authentication authentication) {
    return solicitudes.editar(id, request, principal(authentication));
  }

  @GetMapping("/bitacora")
  public List<BitacoraResponse> bitacora(Authentication authentication) {
    return solicitudes.bitacora(principal(authentication));
  }

  @GetMapping("/solicitudes/{id}/guia")
  public ResponseEntity<byte[]> guia(@PathVariable long id, Authentication authentication) {
    return archivo(operacion.guia(id, principal(authentication)));
  }

  @PostMapping("/solicitudes/{id}/enviada")
  public SolicitudResponse enviada(@PathVariable long id, Authentication authentication) {
    return operacion.marcarEnviada(id, principal(authentication));
  }

  @PostMapping(value = "/solicitudes/{id}/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public SolicitudResponse pdf(
      @PathVariable long id,
      @RequestParam(value = "archivo", required = false) MultipartFile archivo,
      Authentication authentication) {
    return operacion.adjuntar(id, archivo, principal(authentication));
  }

  @PostMapping("/solicitudes/{id}/reenviar")
  public SolicitudResponse reenviar(@PathVariable long id, Authentication authentication) {
    return operacion.reenviar(id, principal(authentication));
  }

  @GetMapping("/solicitudes/{id}/enlace")
  public EnlaceResponse enlaceExistente(@PathVariable long id, Authentication authentication) {
    return encuestas.existente(id, principal(authentication));
  }

  @GetMapping("/solicitudes/{id}/vista-envio")
  public VistaEnvioResponse vistaEnvio(@PathVariable long id, Authentication authentication) {
    return encuestas.vista(id, principal(authentication));
  }

  @PostMapping("/solicitudes/{id}/enlace")
  public EnlaceResponse enlace(
      @PathVariable long id,
      @RequestParam(name = "correo", defaultValue = "true") boolean correo,
      Authentication authentication) {
    return encuestas.crear(id, principal(authentication), correo);
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
