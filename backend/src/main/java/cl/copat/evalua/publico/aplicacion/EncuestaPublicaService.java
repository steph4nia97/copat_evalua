package cl.copat.evalua.publico.aplicacion;

import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.dominio.Fechas;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import cl.copat.evalua.departamento1.persistencia.DepartamentoRepository;
import cl.copat.evalua.departamento1.persistencia.EstadoSolicitud;
import cl.copat.evalua.departamento1.persistencia.EstadoSolicitudRepository;
import cl.copat.evalua.departamento1.persistencia.Solicitud;
import cl.copat.evalua.departamento1.persistencia.SolicitudLogRepository;
import cl.copat.evalua.departamento1.persistencia.SolicitudRepository;
import cl.copat.evalua.departamento1.persistencia.Usuario;
import cl.copat.evalua.departamento1.persistencia.UsuarioRepository;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import cl.copat.evalua.departamento2.aplicacion.SeguimientoInicio;
import cl.copat.evalua.departamento2.dominio.Desempeno;
import cl.copat.evalua.departamento2.dominio.Desempeno.Resultado;
import cl.copat.evalua.departamento2.dominio.GuiaPdf;
import cl.copat.evalua.departamento2.dominio.GuiaPdf.Lectura;
import cl.copat.evalua.departamento2.dominio.GuiaPdf.Pregunta;
import cl.copat.evalua.departamento2.persistencia.Notificacion;
import cl.copat.evalua.departamento2.persistencia.NotificacionRepository;
import cl.copat.evalua.departamento2.persistencia.SolicitudEvaluacion;
import cl.copat.evalua.departamento2.persistencia.SolicitudEvaluacionRepository;
import cl.copat.evalua.departamento3.persistencia.RechazoSolicitudRepository;
import cl.copat.evalua.publico.api.EncuestaPublicaResponse;
import cl.copat.evalua.publico.api.EncuestaPublicaResponse.PreguntaPublica;
import cl.copat.evalua.publico.api.EncuestaPublicaResponse.RespuestaGuardada;
import cl.copat.evalua.publico.api.EnlaceResponse;
import cl.copat.evalua.publico.api.VistaEnvioResponse;
import cl.copat.evalua.publico.api.RespuestaEncuestaRequest;
import cl.copat.evalua.publico.persistencia.EnlaceEncuesta;
import cl.copat.evalua.publico.persistencia.EnlaceEncuestaRepository;
import cl.copat.evalua.shared.aplicacion.ActividadService;
import cl.copat.evalua.shared.web.ReglaNegocioException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class EncuestaPublicaService {

  private static final SecureRandom ALEATORIO = new SecureRandom();

  private final EnlaceEncuestaRepository enlaces;
  private final SolicitudRepository solicitudes;
  private final SolicitudEvaluacionRepository evaluaciones;
  private final EstadoSolicitudRepository estados;
  private final UsuarioRepository usuarios;
  private final SolicitudLogRepository bitacora;
  private final NotificacionRepository notificaciones;
  private final RechazoSolicitudRepository rechazos;
  private final DepartamentoRepository departamentos;
  private final ActividadService actividad;
  private final SeguimientoInicio seguimiento;
  private final Clock reloj;
  private final CorreoEncuesta correos;

  public EncuestaPublicaService(
      EnlaceEncuestaRepository enlaces,
      SolicitudRepository solicitudes,
      SolicitudEvaluacionRepository evaluaciones,
      EstadoSolicitudRepository estados,
      UsuarioRepository usuarios,
      SolicitudLogRepository bitacora,
      NotificacionRepository notificaciones,
      RechazoSolicitudRepository rechazos,
      DepartamentoRepository departamentos,
      ActividadService actividad,
      SeguimientoInicio seguimiento,
      Clock reloj,
      CorreoEncuesta correos) {
    this.enlaces = enlaces;
    this.solicitudes = solicitudes;
    this.evaluaciones = evaluaciones;
    this.estados = estados;
    this.usuarios = usuarios;
    this.bitacora = bitacora;
    this.notificaciones = notificaciones;
    this.rechazos = rechazos;
    this.departamentos = departamentos;
    this.actividad = actividad;
    this.seguimiento = seguimiento;
    this.reloj = reloj;
    this.correos = correos;
  }

  @Transactional(readOnly = true)
  public EnlaceResponse existente(long id, UsuarioPrincipal principal) {
    if (!Catalogo.operaSolicitudes(principal.departamentoId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, Mensajes.PROHIBIDO);
    }
    Solicitud solicitud = solicitudes.buscar(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO));
    if (solicitud.getDepartamento().getId() != Catalogo.DEPARTAMENTO_1) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO);
    }
    EnlaceEncuesta enlace = enlaces.findById(id).orElse(null);
    if (enlace == null) {
      return new EnlaceResponse(null, false, false, "", Mensajes.ENLACE_AUSENTE);
    }
    return new EnlaceResponse(enlace.getToken(), abierta(solicitud.getEstado().getNombre()), false, "", "");
  }

  @Transactional(readOnly = true)
  public VistaEnvioResponse vista(long id, UsuarioPrincipal principal) {
    Solicitud solicitud = operable(id, principal);
    EnlaceEncuesta enlace = enlaces.findById(id).orElse(null);
    return correos.vista(solicitud, enlace == null ? null : enlace.getToken());
  }

  @Transactional
  public EnlaceResponse crear(long id, UsuarioPrincipal principal, boolean enviarCorreo) {
    Solicitud solicitud = operable(id, principal);
    if (Catalogo.ESTADO_INICIAL.equals(solicitud.getEstado().getNombre())) {
      Usuario usuario = usuarios.findById(principal.id())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, Mensajes.SESION));
      cambiar(solicitud, Catalogo.ESTADO_ENVIADA_CLIENTE, Catalogo.ACCION_ENVIADA_CLIENTE, usuario);
    }
    String estado = solicitud.getEstado().getNombre();
    EnlaceEncuesta enlace = enlaces.findById(id).orElse(null);
    if (enlace == null) {
      if (!abierta(estado)) {
        throw new ReglaNegocioException(Mensajes.ENLACE_ESTADO);
      }
      enlace = enlaces.saveAndFlush(new EnlaceEncuesta(id, token(), LocalDateTime.now(reloj)));
    }
    if (!enviarCorreo) {
      return new EnlaceResponse(
          enlace.getToken(),
          abierta(estado),
          false,
          "",
          "El enlace quedó listo para compartir por WhatsApp.");
    }
    CorreoEncuesta.Resultado correo = correos.enviar(solicitud, enlace.getToken());
    return new EnlaceResponse(
        enlace.getToken(),
        abierta(estado),
        correo.enviado(),
        correo.destino(),
        correo.aviso());
  }

  public EncuestaPublicaResponse ver(String token) {
    EnlaceEncuesta enlace = buscar(token);
    Solicitud solicitud = solicitudes.buscar(enlace.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO));
    String estado = solicitud.getEstado().getNombre();
    boolean puede = abierta(estado);
    String motivo = null;
    if (Catalogo.ESTADO_RECHAZADA.equals(estado)) {
      motivo = rechazos.findById(solicitud.getId()).map(rechazo -> rechazo.getDescripcion()).orElse(null);
    }
    return new EncuestaPublicaResponse(
        solicitud.getNombreEmpresa(),
        solicitud.getCorreo(),
        solicitud.getTelefono(),
        Fechas.format(solicitud.getPeriodoInicio()),
        Fechas.format(solicitud.getPeriodoFin()),
        solicitud.nombresSedes(),
        puede,
        motivo,
        GuiaPdf.NOTAS.stream().map(pregunta -> new PreguntaPublica(pregunta.clave(), pregunta.etiqueta())).toList(),
        guardada(solicitud.getId(), puede));
  }

  @Transactional
  public void responder(String token, RespuestaEncuestaRequest request) {
    EnlaceEncuesta enlace = buscar(token);
    Solicitud solicitud = solicitudes.buscar(enlace.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO));
    if (!abierta(solicitud.getEstado().getNombre())) {
      throw new ReglaNegocioException(Mensajes.ENCUESTA_CERRADA);
    }
    boolean correccion = Catalogo.ESTADO_RECHAZADA.equals(solicitud.getEstado().getNombre());
    Lectura lectura = leer(request, solicitud);
    LocalDateTime ahora = LocalDateTime.now(reloj);
    SolicitudEvaluacion actual = evaluaciones.findById(solicitud.getId()).orElse(null);
    if (actual == null) {
      evaluaciones.saveAndFlush(new SolicitudEvaluacion(solicitud.getId(), lectura, ahora));
    } else {
      actual.reemplazar(lectura, ahora);
      evaluaciones.saveAndFlush(actual);
    }
    Usuario actor = quienEnvio(solicitud.getId());
    String accion = correccion ? Catalogo.ACCION_REENVIADA : Catalogo.ACCION_EN_REVISION;
    cambiar(solicitud, Catalogo.ESTADO_EN_REVISION, accion, actor);
    seguimiento.abrir(solicitud, actor, lectura.promedio(), lectura.desempeno());
    if (!notificaciones.existsBySolicitud_IdAndDepartamento_Id(solicitud.getId(), Catalogo.DEPARTAMENTO_3)) {
      String mensaje = "Solicitud en revision: " + solicitud.getNombreEmpresa();
      if (mensaje.length() > 200) {
        mensaje = mensaje.substring(0, 200);
      }
      notificaciones.save(new Notificacion(
          solicitud,
          departamentos.getReferenceById(Catalogo.DEPARTAMENTO_3),
          mensaje,
          ahora));
    }
  }

  private RespuestaGuardada guardada(long solicitudId, boolean abierta) {
    if (!abierta) {
      return null;
    }
    SolicitudEvaluacion evaluacion = evaluaciones.findById(solicitudId).orElse(null);
    if (evaluacion == null) {
      return null;
    }
    Map<String, Integer> notas = new LinkedHashMap<>();
    for (Pregunta pregunta : GuiaPdf.NOTAS) {
      notas.put(pregunta.clave(), evaluacion.nota(pregunta.clave()));
    }
    return new RespuestaGuardada(
        evaluacion.getNombreEncuestado(),
        evaluacion.getCargo(),
        evaluacion.getObra(),
        evaluacion.getEstadoObra(),
        evaluacion.getComentarios(),
        notas);
  }

  private Lectura leer(RespuestaEncuestaRequest request, Solicitud solicitud) {
    if (request == null) {
      throw new ReglaNegocioException(Mensajes.CUERPO);
    }
    Map<String, String> campos = new LinkedHashMap<>();
    String nombre = texto(request.nombre());
    String cargo = texto(request.cargo());
    String obra = texto(request.obra());
    String estadoObra = texto(request.estadoObra());
    String comentarios = texto(request.comentarios());
    String empresa = solicitud.getNombreEmpresa();
    if (nombre.isEmpty() || nombre.length() > 120) {
      campos.put("nombre", Mensajes.ENCUESTA_NOMBRE);
    }
    if (cargo.isEmpty() || cargo.length() > 120) {
      campos.put("cargo", Mensajes.ENCUESTA_CARGO);
    }
    if (obra.isEmpty() || obra.length() > 200) {
      campos.put("obra", Mensajes.ENCUESTA_OBRA);
    }
    if (!GuiaPdf.OBRA_FINALIZADA.equals(estadoObra) && !GuiaPdf.OBRA_EN_CONSTRUCCION.equals(estadoObra)) {
      campos.put("estadoObra", Mensajes.ENCUESTA_ESTADO);
    }
    if (comentarios.length() > 1000) {
      campos.put("comentarios", Mensajes.ENCUESTA_COMENTARIO);
    }
    Map<String, Integer> notas = new LinkedHashMap<>();
    Map<String, Integer> recibidas = request.notas() == null ? Map.of() : request.notas();
    for (Pregunta pregunta : GuiaPdf.NOTAS) {
      boolean presente = recibidas.containsKey(pregunta.clave());
      Integer nota = presente ? recibidas.get(pregunta.clave()) : null;
      if (GuiaPdf.admiteSinNota(pregunta.clave()) && presente && nota == null) {
        continue;
      }
      if (nota == null || nota < 1 || nota > 5) {
        campos.put(pregunta.clave(), pregunta.etiqueta() + ": " + Mensajes.ENCUESTA_NOTA);
      } else {
        notas.put(pregunta.clave(), nota);
      }
    }
    if (!campos.isEmpty()) {
      throw new ReglaNegocioException(Mensajes.REVISE, campos);
    }
    Integer[] valores = GuiaPdf.NOTAS.stream().map(pregunta -> notas.get(pregunta.clave())).toArray(Integer[]::new);
    Resultado resultado = Desempeno.calcularOmitiendoVacias(valores);
    return new Lectura(
        nombre,
        empresa,
        cargo,
        obra,
        estadoObra,
        comentarios.isEmpty() ? null : comentarios,
        notas,
        resultado.promedio(),
        resultado.desempeno());
  }

  private Solicitud operable(long id, UsuarioPrincipal principal) {
    if (!Catalogo.operaSolicitudes(principal.departamentoId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, Mensajes.PROHIBIDO);
    }
    Solicitud solicitud = solicitudes.buscar(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO));
    if (solicitud.getDepartamento().getId() != Catalogo.DEPARTAMENTO_1) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO);
    }
    return solicitud;
  }

  private EnlaceEncuesta buscar(String token) {
    if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO);
    }
    return enlaces.findByToken(token)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO));
  }

  private Usuario quienEnvio(long solicitudId) {
    return bitacora.porAcciones(solicitudId, List.of(Catalogo.ACCION_ENVIADA_CLIENTE)).stream()
        .findFirst()
        .map(log -> log.getUsuario())
        .orElseThrow(() -> new IllegalStateException("Falta quien envió la solicitud " + solicitudId));
  }

  private void cambiar(Solicitud solicitud, String estadoNombre, String accion, Usuario usuario) {
    EstadoSolicitud estado = estados.findByNombre(estadoNombre)
        .orElseThrow(() -> new IllegalStateException("Falta el estado " + estadoNombre));
    solicitud.cambiarEstado(estado);
    solicitudes.saveAndFlush(solicitud);
    actividad.anotar(solicitud, usuario, accion, solicitud.getNombreEmpresa());
  }

  private static boolean abierta(String estado) {
    return Catalogo.ESTADO_ENVIADA_CLIENTE.equals(estado) || Catalogo.ESTADO_RECHAZADA.equals(estado);
  }

  private static String texto(String valor) {
    return valor == null ? "" : valor.trim();
  }

  private static String token() {
    byte[] bytes = new byte[32];
    ALEATORIO.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
