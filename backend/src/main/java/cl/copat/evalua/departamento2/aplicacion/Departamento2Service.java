package cl.copat.evalua.departamento2.aplicacion;

import cl.copat.evalua.departamento1.api.SolicitudResponse;
import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.dominio.Fechas;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import cl.copat.evalua.departamento1.dominio.Rut;
import cl.copat.evalua.departamento1.persistencia.DepartamentoRepository;
import cl.copat.evalua.departamento1.persistencia.EstadoSolicitud;
import cl.copat.evalua.departamento1.persistencia.EstadoSolicitudRepository;
import cl.copat.evalua.departamento1.persistencia.Solicitud;
import cl.copat.evalua.departamento1.persistencia.SolicitudRepository;
import cl.copat.evalua.departamento1.persistencia.Usuario;
import cl.copat.evalua.departamento1.persistencia.UsuarioRepository;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import cl.copat.evalua.departamento2.dominio.GuiaPdf;
import cl.copat.evalua.departamento2.dominio.GuiaPdf.Lectura;
import cl.copat.evalua.departamento2.persistencia.Notificacion;
import cl.copat.evalua.departamento2.persistencia.NotificacionRepository;
import cl.copat.evalua.departamento2.persistencia.SolicitudEvaluacion;
import cl.copat.evalua.departamento2.persistencia.SolicitudEvaluacionRepository;
import cl.copat.evalua.departamento2.persistencia.SolicitudPdf;
import cl.copat.evalua.departamento2.persistencia.SolicitudPdfRepository;
import cl.copat.evalua.departamento3.persistencia.RechazoSolicitud;
import cl.copat.evalua.departamento3.persistencia.RechazoSolicitudRepository;
import cl.copat.evalua.shared.aplicacion.ActividadService;
import cl.copat.evalua.shared.web.ArchivoDescarga;
import cl.copat.evalua.shared.web.ReglaNegocioException;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class Departamento2Service {

  private static final int ARCHIVO_MAX = 8_000_000;

  private final SolicitudRepository solicitudes;
  private final SolicitudPdfRepository pdfs;
  private final SolicitudEvaluacionRepository evaluaciones;
  private final NotificacionRepository notificaciones;
  private final EstadoSolicitudRepository estados;
  private final UsuarioRepository usuarios;
  private final DepartamentoRepository departamentos;
  private final RechazoSolicitudRepository rechazos;
  private final ActividadService actividad;
  private final SeguimientoInicio seguimiento;
  private final Clock reloj;

  public Departamento2Service(
      SolicitudRepository solicitudes,
      SolicitudPdfRepository pdfs,
      SolicitudEvaluacionRepository evaluaciones,
      NotificacionRepository notificaciones,
      EstadoSolicitudRepository estados,
      UsuarioRepository usuarios,
      DepartamentoRepository departamentos,
      RechazoSolicitudRepository rechazos,
      ActividadService actividad,
      SeguimientoInicio seguimiento,
      Clock reloj) {
    this.solicitudes = solicitudes;
    this.pdfs = pdfs;
    this.evaluaciones = evaluaciones;
    this.notificaciones = notificaciones;
    this.estados = estados;
    this.usuarios = usuarios;
    this.departamentos = departamentos;
    this.rechazos = rechazos;
    this.actividad = actividad;
    this.seguimiento = seguimiento;
    this.reloj = reloj;
  }

  public List<String> estados(UsuarioPrincipal principal) {
    exigir(principal);
    return Catalogo.ESTADOS;
  }

  public List<SolicitudResponse> listar(String estado, UsuarioPrincipal principal) {
    exigir(principal);
    List<Solicitud> lista;
    if (estado == null || estado.isBlank()) {
      lista = solicitudes.listarTodas(Catalogo.DEPARTAMENTO_1);
    } else {
      String filtro = estado.trim();
      if (!Catalogo.ESTADOS.contains(filtro)) {
        throw new ReglaNegocioException(Mensajes.ESTADO);
      }
      lista = solicitudes.listarPorEstado(Catalogo.DEPARTAMENTO_1, filtro);
    }
    List<Long> ids = lista.stream().map(Solicitud::getId).toList();
    var porId = rechazos.findAllById(ids).stream()
        .collect(Collectors.toMap(RechazoSolicitud::getId, Function.identity()));
    var evaluacionesPorId = evaluaciones.findAllById(ids).stream()
        .collect(Collectors.toMap(SolicitudEvaluacion::getId, Function.identity()));
    return lista.stream()
        .map(solicitud -> aRespuesta(solicitud, porId.get(solicitud.getId()), evaluacionesPorId.get(solicitud.getId())))
        .toList();
  }

  public ArchivoDescarga guia(long id, UsuarioPrincipal principal) {
    exigir(principal);
    Solicitud solicitud = buscarDeDepartamento1(id);
    String plantilla = solicitud.getPlantilla().getNombre();
    return new ArchivoDescarga(GuiaPdf.nombreArchivo(plantilla), "application/pdf", GuiaPdf.enBlanco(plantilla));
  }

  @Transactional
  public SolicitudResponse marcarEnviada(long id, UsuarioPrincipal principal) {
    exigir(principal);
    Solicitud solicitud = buscarDeDepartamento1(id);
    if (!Catalogo.ESTADO_INICIAL.equals(solicitud.getEstado().getNombre())) {
      throw new ReglaNegocioException(Mensajes.ESTADO_CREADA);
    }
    cambiar(solicitud, Catalogo.ESTADO_ENVIADA_CLIENTE, Catalogo.ACCION_ENVIADA_CLIENTE, principal);
    return aRespuesta(solicitud, null, null);
  }

  @Transactional
  public SolicitudResponse adjuntar(long id, MultipartFile archivo, UsuarioPrincipal principal) {
    exigir(principal);
    Solicitud solicitud = buscarDeDepartamento1(id);
    if (!Catalogo.ESTADO_ENVIADA_CLIENTE.equals(solicitud.getEstado().getNombre())) {
      throw new ReglaNegocioException(Mensajes.ESTADO_ENVIADA);
    }
    byte[] contenido = contenido(archivo);
    Lectura lectura = GuiaPdf.leer(contenido, solicitud.getPlantilla().getNombre());
    pdfs.save(new SolicitudPdf(solicitud.getId(), nombre(archivo, GuiaPdf.ARCHIVO), contenido));
    SolicitudEvaluacion evaluacion = evaluaciones.save(
        new SolicitudEvaluacion(solicitud.getId(), lectura, LocalDateTime.now(reloj)));
    Usuario usuario = usuarios.findById(principal.id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, Mensajes.SESION));
    cambiar(solicitud, Catalogo.ESTADO_EN_REVISION, Catalogo.ACCION_EN_REVISION, principal);
    seguimiento.abrir(solicitud, usuario, lectura.promedio(), lectura.desempeno());
    String mensaje = "Solicitud en revision: " + solicitud.getNombreEmpresa();
    if (mensaje.length() > 200) {
      mensaje = mensaje.substring(0, 200);
    }
    notificaciones.save(new Notificacion(
        solicitud,
        departamentos.getReferenceById(Catalogo.DEPARTAMENTO_3),
        mensaje,
        LocalDateTime.now(reloj)));
    return aRespuesta(solicitud, null, evaluacion);
  }

  @Transactional
  public SolicitudResponse reenviar(long id, UsuarioPrincipal principal) {
    exigir(principal);
    Solicitud solicitud = buscarDeDepartamento1(id);
    if (!Catalogo.ESTADO_RECHAZADA.equals(solicitud.getEstado().getNombre())) {
      throw new ReglaNegocioException(Mensajes.REENVIO_ESTADO);
    }
    if (pdfs.findById(id).isEmpty() && evaluaciones.findById(id).isEmpty()) {
      throw new IllegalStateException("Falta la encuesta de la solicitud " + id);
    }
    cambiar(solicitud, Catalogo.ESTADO_EN_REVISION, Catalogo.ACCION_REENVIADA, principal);
    return aRespuesta(solicitud, rechazos.findById(id).orElse(null), evaluaciones.findById(id).orElse(null));
  }

  private void cambiar(Solicitud solicitud, String estadoNombre, String accion, UsuarioPrincipal principal) {
    EstadoSolicitud estado = estados.findByNombre(estadoNombre)
        .orElseThrow(() -> new IllegalStateException("Falta el estado " + estadoNombre));
    Usuario usuario = usuarios.findById(principal.id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, Mensajes.SESION));
    solicitud.cambiarEstado(estado);
    solicitudes.saveAndFlush(solicitud);
    actividad.anotar(solicitud, usuario, accion, solicitud.getNombreEmpresa());
  }

  private Solicitud buscarDeDepartamento1(long id) {
    Solicitud solicitud = solicitudes.buscar(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO));
    if (solicitud.getDepartamento().getId() != Catalogo.DEPARTAMENTO_1) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO);
    }
    return solicitud;
  }

  private static byte[] contenido(MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty()) {
      throw new ReglaNegocioException(Mensajes.PDF);
    }
    byte[] bytes;
    try {
      bytes = archivo.getBytes();
    } catch (IOException ex) {
      throw new ReglaNegocioException(Mensajes.PDF_GUIA);
    }
    if (bytes.length > ARCHIVO_MAX) {
      throw new ReglaNegocioException(Mensajes.ARCHIVO_GRANDE);
    }
    return bytes;
  }

  public static String nombre(MultipartFile archivo, String respaldo) {
    String nombre = archivo.getOriginalFilename();
    if (nombre == null || nombre.isBlank()) {
      return respaldo;
    }
    nombre = nombre.replace('\\', '/').trim();
    int corte = nombre.lastIndexOf('/');
    if (corte >= 0) {
      nombre = nombre.substring(corte + 1).trim();
    }
    if (nombre.isEmpty() || nombre.length() > 200) {
      return respaldo;
    }
    return nombre;
  }

  private void exigir(UsuarioPrincipal principal) {
    if (!Catalogo.operaSolicitudes(principal.departamentoId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, Mensajes.PROHIBIDO);
    }
  }

  private SolicitudResponse aRespuesta(Solicitud solicitud, RechazoSolicitud rechazo, SolicitudEvaluacion evaluacion) {
    boolean rechazada = Catalogo.ESTADO_RECHAZADA.equals(solicitud.getEstado().getNombre());
    return new SolicitudResponse(
        solicitud.getId(),
        solicitud.getNombreEmpresa(),
        Rut.formatear(solicitud.getRut()),
        solicitud.getCorreo(),
        solicitud.getTelefono(),
        Fechas.format(solicitud.getPeriodoInicio()),
        Fechas.format(solicitud.getPeriodoFin()),
        solicitud.getPlantilla().getNombre(),
        solicitud.nombresSedes(),
        solicitud.getDescripcion(),
        solicitud.getEstado().getNombre(),
        solicitud.getCreadoPor().nombreVisible() + " (" + solicitud.getCreadoPor().getEmail() + ")",
        rechazada && rechazo != null ? rechazo.getDescripcion() : null,
        rechazada && rechazo != null ? rechazo.getDestinatario().getEmail() : null,
        solicitud.getVecesRechazada() > 0 && Catalogo.ESTADO_EN_REVISION.equals(solicitud.getEstado().getNombre()),
        evaluacion == null ? null : evaluacion.getDesempeno(),
        evaluacion == null ? null : Catalogo.promedio(evaluacion.getPromedio()),
        Catalogo.vistaOperacion(solicitud.getSeguimiento()));
  }
}
