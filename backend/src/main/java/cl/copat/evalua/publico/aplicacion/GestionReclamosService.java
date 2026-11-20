package cl.copat.evalua.publico.aplicacion;

import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.dominio.Fechas;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import cl.copat.evalua.departamento1.persistencia.Usuario;
import cl.copat.evalua.departamento1.persistencia.UsuarioRepository;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import cl.copat.evalua.publico.api.FirmanteResponse;
import cl.copat.evalua.publico.api.GestionReclamosRequest;
import cl.copat.evalua.publico.api.GestionReclamosResponse;
import cl.copat.evalua.publico.api.GestionReclamosResponse.Corte;
import cl.copat.evalua.publico.dominio.InformeGestionPdf;
import cl.copat.evalua.publico.dominio.ReclamoCatalogo;
import cl.copat.evalua.publico.persistencia.InformeGestion;
import cl.copat.evalua.publico.persistencia.InformeGestionRepository;
import cl.copat.evalua.publico.persistencia.Reclamo;
import cl.copat.evalua.publico.persistencia.ReclamoRepository;
import cl.copat.evalua.shared.web.ArchivoDescarga;
import cl.copat.evalua.shared.web.ReglaNegocioException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class GestionReclamosService {

  static final String CUMPLE = "Cumple";
  static final String NO_CUMPLE = "No cumple";
  static final String SIN_DATOS = "Sin datos";
  static final String SUBGERENTE_CORREO = "hugomunoz@copat.cl";
  static final String SUBGERENTE_NOMBRE = "Hugo Muñoz";
  static final String SUBGERENTE_CARGO = "Subgerente comercial";
  private static final String SIN_AREA = "Sin área";
  private static final int FIRMA_MAX = 1_000_000;

  private final ReclamoRepository reclamos;
  private final InformeGestionRepository informes;
  private final UsuarioRepository usuarios;
  private final Clock reloj;

  public GestionReclamosService(
      ReclamoRepository reclamos,
      InformeGestionRepository informes,
      UsuarioRepository usuarios,
      Clock reloj) {
    this.reclamos = reclamos;
    this.informes = informes;
    this.usuarios = usuarios;
    this.reloj = reloj;
  }

  public List<FirmanteResponse> firmantes(UsuarioPrincipal principal) {
    exigir(principal);
    return usuarios.findAllByOrderByNombreAsc().stream()
        .filter(Usuario::isActivo)
        .sorted(Comparator.comparing(Usuario::getCargo, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Usuario::getNombre, String.CASE_INSENSITIVE_ORDER))
        .map(usuario -> new FirmanteResponse(
            usuario.getId(),
            usuario.getNombre(),
            usuario.getCargo(),
            usuario.getDepartamento().getNombre()))
        .toList();
  }

  public GestionReclamosResponse consultar(String desde, String hasta, UsuarioPrincipal principal) {
    exigir(principal);
    Periodo periodo = periodo(desde, hasta);
    return armar(periodo, informes.findByPeriodoInicioAndPeriodoFin(periodo.inicio(), periodo.fin()).orElse(null));
  }

  @Transactional
  public GestionReclamosResponse guardar(GestionReclamosRequest request, UsuarioPrincipal principal) {
    exigir(principal);
    if (request == null) {
      throw new ReglaNegocioException(Mensajes.CUERPO);
    }
    Periodo periodo = periodo(request.desde(), request.hasta());
    Texto texto = texto(request);
    InformeGestion informe = informes.findByPeriodoInicioAndPeriodoFin(periodo.inicio(), periodo.fin()).orElse(null);
    boolean esHugo = esSubgerente(principal);
    Firma comercial = firmarComercial(request.comercialFirmaVirtual(), informe, esHugo);
    Firma gerencia = firmar(request.gerenciaUsuarioId(), request.gerenciaFirmaVirtual(), "gerenciaUsuarioId", informe, false);
    if (informe == null) {
      informe = new InformeGestion(periodo.inicio(), periodo.fin());
    }
    informe.completar(
        texto.responsable(),
        texto.cargo(),
        texto.aprobadoPor(),
        null,
        null,
        texto.efectividad(),
        texto.conclusiones(),
        comercial.usuario(),
        comercial.nombre(),
        comercial.cargo(),
        comercial.fecha(),
        comercial.virtual(),
        gerencia.usuario(),
        gerencia.nombre(),
        gerencia.cargo(),
        gerencia.fecha(),
        gerencia.virtual());
    if (esHugo && (!comercial.virtual() || request.comercialQuitarFirma())) {
      informe.quitarFirmaComercial();
    }
    if (!gerencia.virtual() || request.gerenciaQuitarFirma()) {
      informe.quitarFirmaGerencia();
    }
    informes.saveAndFlush(informe);
    return armar(periodo, informe);
  }

  @Transactional
  public GestionReclamosResponse subirFirma(
      String desde, String hasta, String rol, MultipartFile archivo, UsuarioPrincipal principal) {
    exigir(principal);
    Periodo periodo = periodo(desde, hasta);
    InformeGestion informe = informes.findByPeriodoInicioAndPeriodoFin(periodo.inicio(), periodo.fin())
        .orElseThrow(() -> new ReglaNegocioException(Mensajes.GESTION_FIRMA_PREVIA));
    boolean esComercial = esComercial(rol);
    if (esComercial && !esSubgerente(principal)) {
      throw new ReglaNegocioException(Mensajes.REVISE, Map.of("archivo", Mensajes.GESTION_SUBGERENTE));
    }
    if (esComercial ? !informe.isComercialFirmaVirtual() : !informe.isGerenciaFirmaVirtual()) {
      throw new ReglaNegocioException(Mensajes.REVISE, Map.of("archivo", Mensajes.GESTION_FIRMA_PREVIA));
    }
    Imagen imagen = imagen(archivo);
    if (esComercial) {
      informe.asignarFirmaComercial(imagen.contenido(), imagen.tipo());
    } else {
      informe.asignarFirmaGerencia(imagen.contenido(), imagen.tipo());
    }
    informes.saveAndFlush(informe);
    return armar(periodo, informe);
  }

  public ArchivoDescarga firma(String desde, String hasta, String rol, UsuarioPrincipal principal) {
    exigir(principal);
    Periodo periodo = periodo(desde, hasta);
    InformeGestion informe = informes.findByPeriodoInicioAndPeriodoFin(periodo.inicio(), periodo.fin())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO));
    boolean esComercial = esComercial(rol);
    byte[] contenido = esComercial ? informe.getComercialFirma() : informe.getGerenciaFirma();
    String tipo = esComercial ? informe.getComercialFirmaTipo() : informe.getGerenciaFirmaTipo();
    if (contenido == null || contenido.length == 0 || tipo == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO);
    }
    String extension = "image/png".equals(tipo) ? "png" : "jpg";
    return new ArchivoDescarga("firma-" + rol + "." + extension, tipo, contenido);
  }

  public ArchivoDescarga informe(String desde, String hasta, UsuarioPrincipal principal) {
    exigir(principal);
    Periodo periodo = periodo(desde, hasta);
    InformeGestion guardado = informes.findByPeriodoInicioAndPeriodoFin(periodo.inicio(), periodo.fin()).orElse(null);
    GestionReclamosResponse datos = armar(periodo, guardado);
    byte[] comercial = firmaSubgerente(guardado) ? guardado.getComercialFirma() : null;
    String nombre = InformeGestionPdf.nombreArchivo(datos.desde(), datos.hasta());
    return new ArchivoDescarga(nombre, "application/pdf", InformeGestionPdf.generar(datos, comercial));
  }

  private GestionReclamosResponse armar(Periodo periodo, InformeGestion informe) {
    List<Reclamo> delPeriodo = reclamos.findAllByOrderByIdDesc().stream()
        .filter(reclamo -> !reclamo.getRecibidoEn().toLocalDate().isBefore(periodo.inicio()))
        .filter(reclamo -> !reclamo.getRecibidoEn().toLocalDate().isAfter(periodo.fin()))
        .sorted(Comparator.comparing(Reclamo::getRecibidoEn).thenComparing(Reclamo::getId))
        .toList();
    int total = delPeriodo.size();
    Map<String, Integer> tipos = contar(delPeriodo.stream().map(Reclamo::getTipo).toList());
    List<Corte> porArea = porcentajesDeArea(delPeriodo.stream().map(Reclamo::getAreaResponsable).toList());
    int reincidentes = reincidentes(delPeriodo);
    int cerradosEnPlazo = 0;
    long sumaDias = 0;
    int cerrados = 0;
    for (Reclamo reclamo : delPeriodo) {
      if (!ReclamoCatalogo.ESTADO_CERRADO.equals(reclamo.getEstado()) || reclamo.getFechaCierre() == null) {
        continue;
      }
      long dias = Math.max(0, ChronoUnit.DAYS.between(reclamo.getRecibidoEn().toLocalDate(), reclamo.getFechaCierre()));
      sumaDias += dias;
      cerrados++;
      if (dias <= 5) {
        cerradosEnPlazo++;
      }
    }
    String porcentajeCierre = total == 0 ? "—" : porcentaje(cerradosEnPlazo, total);
    String porcentajeReincidentes = total == 0 ? "—" : porcentaje(reincidentes, total);
    String tiempo = cerrados == 0 ? "—" : decimal(sumaDias / (double) cerrados);
    String principales = causas(tipos, total);
    String causasRecurrentes = recurrentes(tipos, total);
    String areasFrecuencia = areasMayores(porArea);
    String cumplimiento = cumplimiento(total, porcentajeCierre, tiempo);
    boolean subgerente = firmaSubgerente(informe);
    return new GestionReclamosResponse(
        Fechas.format(periodo.inicio()),
        Fechas.format(periodo.fin()),
        total,
        cortes(tipos, total),
        porArea,
        reincidentes,
        porcentajeReincidentes,
        estadoTope(total, reincidentes, 5),
        cerradosEnPlazo,
        porcentajeCierre,
        estadoMinimo(total, cerradosEnPlazo, 90),
        tiempo,
        cerrados == 0 ? SIN_DATOS : (sumaDias / (double) cerrados <= 5 ? CUMPLE : NO_CUMPLE),
        principales,
        cumplimiento,
        informe == null ? null : informe.getResponsable(),
        informe == null ? null : informe.getCargo(),
        informe == null ? null : informe.getAprobadoPor(),
        causasRecurrentes,
        areasFrecuencia,
        informe == null ? null : informe.getEfectividad(),
        informe == null ? null : informe.getConclusiones(),
        SUBGERENTE_NOMBRE,
        SUBGERENTE_CARGO,
        subgerente ? informe.getComercial().getId() : null,
        subgerente && informe.getComercialFecha() != null ? Fechas.format(informe.getComercialFecha()) : null,
        subgerente,
        informe == null ? null : informe.getGerenciaNombre(),
        informe == null ? null : informe.getGerenciaCargo(),
        informe == null || informe.getGerencia() == null ? null : informe.getGerencia().getId(),
        informe == null || informe.getGerenciaFecha() == null ? null : Fechas.format(informe.getGerenciaFecha()),
        informe != null && informe.isGerenciaFirmaVirtual(),
        subgerente && informe.tieneFirmaComercial(),
        informe != null && informe.tieneFirmaGerencia());
  }

  private static int reincidentes(List<Reclamo> delPeriodo) {
    Map<String, Integer> vistos = new LinkedHashMap<>();
    int repetidos = 0;
    for (Reclamo reclamo : delPeriodo) {
      String clave = reclamo.getEmpresa().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ")
          + "|" + reclamo.getTipo();
      int previos = vistos.getOrDefault(clave, 0);
      if (previos > 0) {
        repetidos++;
      }
      vistos.put(clave, previos + 1);
    }
    return repetidos;
  }

  static List<Corte> porcentajesDeArea(List<String> guardadas) {
    List<String> seleccionadas = new ArrayList<>();
    for (String guardada : guardadas) {
      seleccionadas.addAll(separarArea(guardada));
    }
    return cortes(contar(seleccionadas), seleccionadas.size());
  }

  static List<String> separarArea(String area) {
    if (area == null || area.isBlank()) {
      return List.of(SIN_AREA);
    }
    List<String> partes = new ArrayList<>();
    for (String parte : area.split(",")) {
      String limpia = parte.trim();
      if (!limpia.isEmpty()) {
        partes.add(limpia);
      }
    }
    return partes.isEmpty() ? List.of(SIN_AREA) : partes;
  }

  private static Map<String, Integer> contar(List<String> nombres) {
    Map<String, Integer> conteo = new LinkedHashMap<>();
    for (String nombre : nombres) {
      conteo.merge(nombre, 1, Integer::sum);
    }
    return conteo;
  }

  private static List<Corte> cortes(Map<String, Integer> conteo, int total) {
    return conteo.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
        .map(entrada -> new Corte(entrada.getKey(), entrada.getValue(), total == 0 ? "—" : porcentaje(entrada.getValue(), total)))
        .toList();
  }

  private static String causas(Map<String, Integer> tipos, int total) {
    if (total == 0) {
      return "No hay reclamos en el periodo.";
    }
    List<String> lineas = new ArrayList<>();
    cortes(tipos, total).forEach(corte -> lineas.add(corte.nombre() + ": " + corte.cantidad() + " (" + corte.porcentaje() + "%)"));
    return String.join("\n", lineas);
  }

  private static String recurrentes(Map<String, Integer> tipos, int total) {
    if (total == 0) {
      return "No hay reclamos en el periodo.";
    }
    List<String> lineas = new ArrayList<>();
    cortes(tipos, total).stream()
        .filter(corte -> corte.cantidad() > 1)
        .forEach(corte -> lineas.add(corte.nombre() + ": " + corte.cantidad() + " (" + corte.porcentaje() + "%)"));
    if (lineas.isEmpty()) {
      return "Ninguna causa se repite en el periodo.";
    }
    return String.join("\n", lineas);
  }

  private static String areasMayores(List<Corte> orden) {
    if (orden.isEmpty()) {
      return "No hay reclamos en el periodo.";
    }
    int maximo = orden.getFirst().cantidad();
    List<String> lineas = new ArrayList<>();
    orden.stream()
        .filter(corte -> corte.cantidad() == maximo)
        .forEach(corte -> lineas.add(corte.nombre() + ": " + corte.cantidad() + " (" + corte.porcentaje() + "%)"));
    return String.join("\n", lineas);
  }

  private static String cumplimiento(int total, String porcentajeCierre, String tiempo) {
    if (total == 0) {
      return "No hay reclamos en el periodo.";
    }
    String dias = "—".equals(tiempo) ? "sin cierres en el periodo" : tiempo + " días";
    return "El " + porcentajeCierre + "% de los reclamos se cerró en 5 días o menos. La meta es 90% o más. Tiempo promedio de los cerrados: " + dias + ".";
  }

  private static String estadoMinimo(int total, int parte, double meta) {
    if (total == 0) {
      return SIN_DATOS;
    }
    return parte * 100.0 / total >= meta ? CUMPLE : NO_CUMPLE;
  }

  private static String estadoTope(int total, int parte, double meta) {
    if (total == 0) {
      return SIN_DATOS;
    }
    return parte * 100.0 / total <= meta ? CUMPLE : NO_CUMPLE;
  }

  private static String porcentaje(int parte, int total) {
    return decimal(parte * 100.0 / total);
  }

  private static String decimal(double valor) {
    return String.format(Locale.forLanguageTag("es-CL"), "%.1f", valor);
  }

  private static Periodo periodo(String desde, String hasta) {
    String inicio = desde == null ? "" : desde.trim();
    String fin = hasta == null ? "" : hasta.trim();
    Map<String, String> campos = new LinkedHashMap<>();
    if (!Fechas.esValida(inicio)) {
      campos.put("desde", Mensajes.GESTION_PERIODO);
    }
    if (!Fechas.esValida(fin)) {
      campos.put("hasta", Mensajes.GESTION_PERIODO);
    }
    if (!campos.isEmpty()) {
      throw new ReglaNegocioException(Mensajes.REVISE, campos);
    }
    LocalDate fechaInicio = Fechas.parse(inicio);
    LocalDate fechaFin = Fechas.parse(fin);
    if (fechaFin.isBefore(fechaInicio)) {
      throw new ReglaNegocioException(Mensajes.REVISE, Map.of("hasta", Mensajes.PERIODO));
    }
    return new Periodo(fechaInicio, fechaFin);
  }

  private static Texto texto(GestionReclamosRequest request) {
    String responsable = recortar(request.responsable());
    String cargo = recortar(request.cargo());
    String aprobado = recortar(request.aprobadoPor());
    String causas = recortar(request.causasRecurrentes());
    String areas = recortar(request.areasFrecuencia());
    String efectividad = recortar(request.efectividad());
    String conclusiones = recortar(request.conclusiones());
    Map<String, String> campos = new LinkedHashMap<>();
    largo(campos, "responsable", responsable, 80, Mensajes.GESTION_NOMBRE);
    largo(campos, "cargo", cargo, 80, Mensajes.GESTION_NOMBRE);
    largo(campos, "aprobadoPor", aprobado, 80, Mensajes.GESTION_NOMBRE);
    largo(campos, "causasRecurrentes", causas, 1000, Mensajes.GESTION_ANALISIS);
    largo(campos, "areasFrecuencia", areas, 1000, Mensajes.GESTION_ANALISIS);
    largo(campos, "efectividad", efectividad, 1000, Mensajes.GESTION_ANALISIS);
    largo(campos, "conclusiones", conclusiones, 2000, Mensajes.GESTION_CONCLUSIONES);
    if (!campos.isEmpty()) {
      throw new ReglaNegocioException(Mensajes.REVISE, campos);
    }
    return new Texto(
        vacio(responsable),
        vacio(cargo),
        vacio(aprobado),
        vacio(causas),
        vacio(areas),
        vacio(efectividad),
        vacio(conclusiones));
  }

  private static boolean esComercial(String rol) {
    if ("comercial".equals(rol)) {
      return true;
    }
    if ("gerencia".equals(rol)) {
      return false;
    }
    throw new ReglaNegocioException(Mensajes.REVISE, Map.of("rol", Mensajes.GESTION_FIRMA));
  }

  private static Imagen imagen(MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty()) {
      throw new ReglaNegocioException(Mensajes.REVISE, Map.of("archivo", Mensajes.GESTION_FIRMA));
    }
    byte[] bytes;
    try {
      bytes = archivo.getBytes();
    } catch (IOException ex) {
      throw new ReglaNegocioException(Mensajes.REVISE, Map.of("archivo", Mensajes.GESTION_FIRMA));
    }
    if (bytes.length > FIRMA_MAX) {
      throw new ReglaNegocioException(Mensajes.REVISE, Map.of("archivo", Mensajes.GESTION_FIRMA));
    }
    String tipo = tipoImagen(bytes);
    try {
      BufferedImage leida = ImageIO.read(new ByteArrayInputStream(bytes));
      if (leida == null || leida.getWidth() < 1 || leida.getHeight() < 1 || leida.getWidth() > 4000 || leida.getHeight() > 4000) {
        throw new ReglaNegocioException(Mensajes.REVISE, Map.of("archivo", Mensajes.GESTION_FIRMA));
      }
    } catch (IOException ex) {
      throw new ReglaNegocioException(Mensajes.REVISE, Map.of("archivo", Mensajes.GESTION_FIRMA));
    }
    return new Imagen(bytes, tipo);
  }

  private static String tipoImagen(byte[] bytes) {
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
    throw new ReglaNegocioException(Mensajes.REVISE, Map.of("archivo", Mensajes.GESTION_FIRMA));
  }

  private Firma firmarComercial(boolean virtual, InformeGestion actual, boolean esHugo) {
    if (!esHugo) {
      return comercialGuardada(actual);
    }
    if (!virtual) {
      return Firma.vacia();
    }
    Usuario usuario = usuarios.findByEmailIgnoreCase(SUBGERENTE_CORREO)
        .filter(Usuario::isActivo)
        .orElseThrow(() -> new ReglaNegocioException(Mensajes.REVISE, Map.of("comercialUsuarioId", Mensajes.GESTION_SUBGERENTE)));
    Usuario anterior = actual == null ? null : actual.getComercial();
    boolean yaFirmaba = actual != null && actual.isComercialFirmaVirtual();
    LocalDate anteriorFecha = actual == null ? null : actual.getComercialFecha();
    boolean mismaFirma = yaFirmaba && anteriorFecha != null && anterior != null && usuario.getId().equals(anterior.getId());
    LocalDate dia = mismaFirma ? anteriorFecha : LocalDate.now(reloj);
    return new Firma(usuario, SUBGERENTE_NOMBRE, SUBGERENTE_CARGO, dia, true);
  }

  private static Firma comercialGuardada(InformeGestion actual) {
    if (actual == null) {
      return Firma.vacia();
    }
    return new Firma(
        actual.getComercial(),
        actual.getComercialNombre(),
        actual.getComercialCargo(),
        actual.getComercialFecha(),
        actual.isComercialFirmaVirtual());
  }

  private static boolean firmaSubgerente(InformeGestion informe) {
    return informe != null
        && informe.isComercialFirmaVirtual()
        && informe.getComercial() != null
        && SUBGERENTE_CORREO.equalsIgnoreCase(informe.getComercial().getEmail());
  }

  private static boolean esSubgerente(UsuarioPrincipal principal) {
    return principal.email() != null && SUBGERENTE_CORREO.equalsIgnoreCase(principal.email());
  }

  private Firma firmar(Long id, boolean virtual, String campoPersona, InformeGestion actual, boolean comercial) {
    if (id == null) {
      if (virtual) {
        throw new ReglaNegocioException(Mensajes.REVISE, Map.of(campoPersona, Mensajes.GESTION_FIRMANTE));
      }
      return Firma.vacia();
    }
    Usuario usuario = usuarios.findById(id)
        .filter(Usuario::isActivo)
        .orElseThrow(() -> new ReglaNegocioException(Mensajes.REVISE, Map.of(campoPersona, Mensajes.GESTION_FIRMANTE)));
    if (!virtual) {
      return new Firma(usuario, usuario.getNombre(), usuario.getCargo(), null, false);
    }
    Usuario anterior = actual == null ? null : (comercial ? actual.getComercial() : actual.getGerencia());
    boolean yaFirmaba = actual != null && (comercial ? actual.isComercialFirmaVirtual() : actual.isGerenciaFirmaVirtual());
    LocalDate anteriorFecha = actual == null ? null : (comercial ? actual.getComercialFecha() : actual.getGerenciaFecha());
    boolean mismaFirma = yaFirmaba && anteriorFecha != null && anterior != null && id.equals(anterior.getId());
    LocalDate dia = mismaFirma ? anteriorFecha : LocalDate.now(reloj);
    return new Firma(usuario, usuario.getNombre(), usuario.getCargo(), dia, true);
  }

  private static void largo(Map<String, String> campos, String clave, String valor, int maximo, String mensaje) {
    if (valor.length() > maximo) {
      campos.put(clave, mensaje);
    }
  }

  private static String recortar(String valor) {
    return valor == null ? "" : valor.trim();
  }

  private static String vacio(String valor) {
    return valor.isEmpty() ? null : valor;
  }

  private static void exigir(UsuarioPrincipal principal) {
    if (principal.departamentoId() != Catalogo.DEPARTAMENTO_3) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, Mensajes.PROHIBIDO);
    }
  }

  private record Periodo(LocalDate inicio, LocalDate fin) {}

  private record Imagen(byte[] contenido, String tipo) {}

  private record Texto(
      String responsable,
      String cargo,
      String aprobadoPor,
      String causas,
      String areas,
      String efectividad,
      String conclusiones) {}

  private record Firma(Usuario usuario, String nombre, String cargo, LocalDate fecha, boolean virtual) {
    private static Firma vacia() {
      return new Firma(null, null, null, null, false);
    }
  }
}
