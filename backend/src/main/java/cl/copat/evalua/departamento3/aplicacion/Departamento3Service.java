package cl.copat.evalua.departamento3.aplicacion;

import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.dominio.Fechas;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import cl.copat.evalua.departamento1.dominio.Rut;
import cl.copat.evalua.departamento1.persistencia.Solicitud;
import cl.copat.evalua.departamento1.persistencia.SolicitudLog;
import cl.copat.evalua.departamento1.persistencia.SolicitudLogRepository;
import cl.copat.evalua.departamento1.persistencia.SolicitudRepository;
import cl.copat.evalua.departamento1.persistencia.Usuario;
import cl.copat.evalua.departamento1.persistencia.UsuarioRepository;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import cl.copat.evalua.departamento2.aplicacion.Departamento2Service;
import cl.copat.evalua.departamento2.dominio.Desempeno;
import cl.copat.evalua.departamento2.dominio.GuiaPdf;
import cl.copat.evalua.departamento2.persistencia.Notificacion;
import cl.copat.evalua.shared.aplicacion.ActividadService;
import cl.copat.evalua.departamento2.persistencia.NotificacionRepository;
import cl.copat.evalua.departamento2.persistencia.SolicitudEvaluacion;
import cl.copat.evalua.departamento2.persistencia.SolicitudEvaluacionRepository;
import cl.copat.evalua.departamento2.persistencia.SolicitudPdf;
import cl.copat.evalua.departamento2.persistencia.SolicitudPdfRepository;
import cl.copat.evalua.departamento3.api.IndicadorResponse;
import cl.copat.evalua.departamento3.api.RevisionResponse;
import cl.copat.evalua.departamento3.api.RevisionResponse.MovimientoResponse;
import cl.copat.evalua.departamento3.api.RevisionResponse.NotaResponse;
import cl.copat.evalua.departamento3.api.RevisionResponse.NotaSeguimientoResponse;
import cl.copat.evalua.departamento3.persistencia.EvidenciaRevision;
import cl.copat.evalua.departamento3.persistencia.EvidenciaRevisionRepository;
import cl.copat.evalua.departamento3.persistencia.RevisionDepartamento3;
import cl.copat.evalua.departamento3.persistencia.RevisionDepartamento3Repository;
import cl.copat.evalua.departamento3.persistencia.SeguimientoNota;
import cl.copat.evalua.departamento3.persistencia.SeguimientoNotaRepository;
import cl.copat.evalua.shared.web.ArchivoDescarga;
import cl.copat.evalua.shared.web.ReglaNegocioException;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class Departamento3Service {

  private static final int ARCHIVO_MAX = 8_000_000;
  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter MARCA = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm");

  private final NotificacionRepository notificaciones;
  private final SolicitudEvaluacionRepository evaluaciones;
  private final SolicitudPdfRepository pdfs;
  private final RevisionDepartamento3Repository revisiones;
  private final EvidenciaRevisionRepository evidencias;
  private final SolicitudRepository solicitudes;
  private final SolicitudLogRepository bitacora;
  private final UsuarioRepository usuarios;
  private final ActividadService actividad;
  private final SeguimientoNotaRepository notasSeguimiento;
  private final Clock reloj;

  public Departamento3Service(
      NotificacionRepository notificaciones,
      SolicitudEvaluacionRepository evaluaciones,
      SolicitudPdfRepository pdfs,
      RevisionDepartamento3Repository revisiones,
      EvidenciaRevisionRepository evidencias,
      SolicitudRepository solicitudes,
      SolicitudLogRepository bitacora,
      UsuarioRepository usuarios,
      ActividadService actividad,
      SeguimientoNotaRepository notasSeguimiento,
      Clock reloj) {
    this.notificaciones = notificaciones;
    this.evaluaciones = evaluaciones;
    this.pdfs = pdfs;
    this.revisiones = revisiones;
    this.evidencias = evidencias;
    this.solicitudes = solicitudes;
    this.bitacora = bitacora;
    this.usuarios = usuarios;
    this.actividad = actividad;
    this.notasSeguimiento = notasSeguimiento;
    this.reloj = reloj;
  }

  public List<RevisionResponse> listar(UsuarioPrincipal principal) {
    exigir(principal);
    List<Notificacion> avisos = notificaciones.listar(Catalogo.DEPARTAMENTO_3).stream()
        .filter(aviso -> Catalogo.ESTADO_EN_REVISION.equals(aviso.getSolicitud().getEstado().getNombre()))
        .toList();
    List<Long> ids = avisos.stream().map(aviso -> aviso.getSolicitud().getId()).toList();
    if (ids.isEmpty()) {
      return List.of();
    }
    Map<Long, SolicitudEvaluacion> porSolicitud = evaluaciones.findAllById(ids).stream()
        .collect(Collectors.toMap(SolicitudEvaluacion::getId, Function.identity()));
    Map<Long, RevisionDepartamento3> notas = revisiones.findAllById(ids).stream()
        .collect(Collectors.toMap(RevisionDepartamento3::getId, Function.identity()));
    Map<Long, Solicitud> cargadas = solicitudes.porIds(ids).stream()
        .collect(Collectors.toMap(Solicitud::getId, Function.identity(), (primera, segunda) -> primera));
    HashSet<Long> conEvidencia = new HashSet<>(evidencias.identificadoresPresentes(ids));
    HashSet<Long> conPdf = pdfs.findAllById(ids).stream()
        .map(SolicitudPdf::getId)
        .collect(Collectors.toCollection(HashSet::new));
    Map<Long, List<MovimientoResponse>> movimientos = bitacora.porSolicitudes(ids).stream()
        .collect(Collectors.groupingBy(log -> log.getSolicitud().getId(), Collectors.mapping(this::movimiento, Collectors.toList())));
    Map<Long, List<NotaSeguimientoResponse>> notasDeSeguimiento = notasSeguimiento.porSolicitudes(ids).stream()
        .collect(Collectors.groupingBy(nota -> nota.getSolicitud().getId(), Collectors.mapping(this::notaSeguimiento, Collectors.toList())));
    return avisos.stream().map(aviso -> {
      Solicitud solicitud = aviso.getSolicitud();
      SolicitudEvaluacion evaluacion = porSolicitud.get(solicitud.getId());
      if (evaluacion == null) {
        throw new IllegalStateException("Falta la evaluacion de la solicitud " + solicitud.getId());
      }
      RevisionDepartamento3 revision = notas.get(solicitud.getId());
      List<NotaResponse> puntajes = GuiaPdf.NOTAS.stream()
          .map(pregunta -> new NotaResponse(pregunta.etiqueta(), evaluacion.nota(pregunta.clave())))
          .toList();
      return new RevisionResponse(
          solicitud.getId(),
          solicitud.getNombreEmpresa(),
          Rut.formatear(solicitud.getRut()),
          solicitud.getCorreo(),
          solicitud.getTelefono(),
          Fechas.format(solicitud.getPeriodoInicio()),
          Fechas.format(solicitud.getPeriodoFin()),
          solicitud.getPlantilla().getNombre(),
          sedesDe(cargadas.get(solicitud.getId())),
          solicitud.getDescripcion(),
          solicitud.getCreadoPor().getNombre(),
          movimientos.getOrDefault(solicitud.getId(), List.of()),
          evaluacion.getNombreEncuestado(),
          evaluacion.getEmpresaEncuesta(),
          evaluacion.getCargo(),
          evaluacion.getObra(),
          evaluacion.getEstadoObra(),
          evaluacion.getComentarios(),
          puntajes,
          evaluacion.getPromedio(),
          evaluacion.getDesempeno(),
          Desempeno.ESCALA,
          revision == null ? null : revision.getDescripcion(),
          conEvidencia.contains(solicitud.getId()),
          conPdf.contains(solicitud.getId()),
          solicitud.getVecesRechazada() > 0,
          solicitud.getSeguimiento() == null ? Catalogo.SEGUIMIENTO_RECIBIDA : solicitud.getSeguimiento(),
          notasDeSeguimiento.getOrDefault(solicitud.getId(), List.of()));
    }).toList();
  }

  private MovimientoResponse movimiento(SolicitudLog log) {
    return new MovimientoResponse(
        log.getAccion(),
        log.getUsuario().getNombre(),
        Fechas.format(log.getFecha()),
        log.getHora().format(HORA));
  }

  public ArchivoDescarga pdf(long id, UsuarioPrincipal principal) {
    exigir(principal);
    exigirNotificada(id);
    SolicitudPdf pdf = pdfs.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO));
    return new ArchivoDescarga(pdf.getNombreArchivo(), "application/pdf", pdf.getContenido());
  }

  public ArchivoDescarga evidencia(long id, UsuarioPrincipal principal) {
    exigir(principal);
    exigirNotificada(id);
    EvidenciaRevision evidencia = evidencias.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO));
    return new ArchivoDescarga(evidencia.getNombreArchivo(), evidencia.getMediaType(), evidencia.getContenido());
  }

  private NotaSeguimientoResponse notaSeguimiento(SeguimientoNota nota) {
    return new NotaSeguimientoResponse(
        nota.getId(),
        nota.getNota(),
        nota.getEstado(),
        nota.getUsuario().getNombre(),
        Fechas.format(nota.getCreadaEn().toLocalDate()),
        nota.getCreadaEn().toLocalTime().format(HORA));
  }

  @Transactional
  public RevisionResponse guardar(
      long id,
      String descripcion,
      String estadoPedido,
      MultipartFile evidencia,
      UsuarioPrincipal principal) {
    exigir(principal);
    exigirNotificada(id);
    Solicitud solicitud = solicitudes.buscar(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO));
    if (!Catalogo.ESTADO_EN_REVISION.equals(solicitud.getEstado().getNombre())) {
      throw new ReglaNegocioException(Mensajes.REVISION_AUSENTE);
    }
    String texto = descripcion == null ? "" : descripcion.trim();
    if (texto.isEmpty()) {
      throw new ReglaNegocioException(Mensajes.REVISE, Map.of("descripcion", Mensajes.DESCRIPCION_REVISION));
    }
    if (texto.length() > 500) {
      throw new ReglaNegocioException(Mensajes.REVISE, Map.of("descripcion", Mensajes.DESCRIPCION_REVISION_LARGA));
    }
    String vigente = solicitud.getSeguimiento() == null ? Catalogo.SEGUIMIENTO_RECIBIDA : solicitud.getSeguimiento();
    String estado = estadoPedido == null || estadoPedido.isBlank() ? vigente : estadoPedido.trim();
    if (!Catalogo.seguimientoValido(estado)) {
      throw new ReglaNegocioException(Mensajes.REVISE, Map.of("estado", Mensajes.SEGUIMIENTO));
    }
    byte[] bytes = null;
    String media = null;
    String nombre = null;
    if (evidencia != null && !evidencia.isEmpty()) {
      bytes = leer(evidencia);
      media = media(bytes);
      nombre = Departamento2Service.nombre(evidencia, "evidencia");
    }
    Usuario usuario = usuarios.findById(principal.id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, Mensajes.SESION));
    LocalDateTime ahora = LocalDateTime.now(reloj);
    boolean cambio = !estado.equals(solicitud.getSeguimiento());
    solicitud.definirSeguimiento(estado);
    solicitudes.saveAndFlush(solicitud);
    notasSeguimiento.saveAndFlush(new SeguimientoNota(solicitud, usuario, estado, texto, ahora));
    RevisionDepartamento3 revision = revisiones.findById(id).orElse(null);
    if (revision == null) {
      revisiones.saveAndFlush(new RevisionDepartamento3(id, texto, usuario, ahora));
    } else {
      revision.actualizar(texto, usuario, ahora);
      revisiones.saveAndFlush(revision);
    }
    if (bytes != null) {
      EvidenciaRevision actual = evidencias.findById(id).orElse(null);
      if (actual == null) {
        evidencias.saveAndFlush(new EvidenciaRevision(id, nombre, media, bytes));
      } else {
        actual.reemplazar(nombre, media, bytes);
        evidencias.saveAndFlush(actual);
      }
    }
    if (cambio) {
      actividad.anotar(solicitud, usuario, Catalogo.seguimiento(estado), solicitud.getNombreEmpresa());
    }
    return listar(principal).stream()
        .filter(item -> item.id() == id)
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO));
  }

  public List<IndicadorResponse> indicadores(UsuarioPrincipal principal) {
    exigir(principal);
    List<SolicitudEvaluacion> evaluacionesHechas = evaluaciones.findAll();
    if (evaluacionesHechas.isEmpty()) {
      return List.of();
    }
    List<Long> ids = evaluacionesHechas.stream().map(SolicitudEvaluacion::getId).toList();
    Map<Long, Solicitud> porSolicitud = solicitudes.porIds(ids).stream()
        .collect(Collectors.toMap(Solicitud::getId, Function.identity()));
    return evaluacionesHechas.stream()
        .map(evaluacion -> indicador(evaluacion, porSolicitud.get(evaluacion.getId())))
        .filter(item -> item != null)
        .toList();
  }

  private static List<String> sedesDe(Solicitud solicitud) {
    return solicitud == null ? List.of() : solicitud.nombresSedes();
  }

  private IndicadorResponse indicador(SolicitudEvaluacion evaluacion, Solicitud solicitud) {
    if (solicitud == null) {
      return null;
    }
    List<NotaResponse> puntajes = GuiaPdf.NOTAS.stream()
        .map(pregunta -> new NotaResponse(pregunta.etiqueta(), evaluacion.nota(pregunta.clave())))
        .toList();
    return new IndicadorResponse(
        solicitud.getId(),
        solicitud.getNombreEmpresa(),
        solicitud.nombresSedes(),
        evaluacion.getObra(),
        evaluacion.getEstadoObra(),
        solicitud.getEstado().getNombre(),
        evaluacion.getDesempeno(),
        evaluacion.getPromedio(),
        puntajes,
        evaluacion.getRecibidaEn().format(MARCA),
        solicitud.getVecesRechazada());
  }

  private void exigirNotificada(long id) {
    if (!notificaciones.existsBySolicitud_IdAndDepartamento_Id(id, Catalogo.DEPARTAMENTO_3)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO);
    }
  }

  private static byte[] leer(MultipartFile archivo) {
    try {
      byte[] bytes = archivo.getBytes();
      if (bytes.length > ARCHIVO_MAX) {
        throw new ReglaNegocioException(Mensajes.ARCHIVO_GRANDE);
      }
      return bytes;
    } catch (IOException ex) {
      throw new ReglaNegocioException(Mensajes.EVIDENCIA);
    }
  }

  private static String media(byte[] bytes) {
    if (esPdf(bytes)) {
      return "application/pdf";
    }
    if (bytes.length >= 8
        && (bytes[0] & 0xFF) == 0x89
        && bytes[1] == 0x50
        && bytes[2] == 0x4E
        && bytes[3] == 0x47) {
      return "image/png";
    }
    if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
      return "image/jpeg";
    }
    throw new ReglaNegocioException(Mensajes.EVIDENCIA);
  }

  private static boolean esPdf(byte[] bytes) {
    return bytes.length >= 5 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
  }

  private void exigir(UsuarioPrincipal principal) {
    if (principal.departamentoId() != Catalogo.DEPARTAMENTO_3) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, Mensajes.PROHIBIDO);
    }
  }
}
