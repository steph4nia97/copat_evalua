package cl.copat.evalua.departamento1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.dominio.Fechas;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Departamento1IntegracionTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  ObjectMapper json;

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  PasswordEncoder passwordEncoder;

  private Cookie csrf;

  @BeforeEach
  void limpiar() throws Exception {
    jdbc.update("DELETE FROM solicitud_log");
    jdbc.update("DELETE FROM solicitud");
    MvcResult preparacion = mockMvc.perform(get("/api/auth/csrf")).andReturn();
    csrf = preparacion.getResponse().getCookie("XSRF-TOKEN");
    assertThat(csrf).isNotNull();
  }

  @Test
  void credencialesIncorrectasUsanUnSoloMensaje() throws Exception {
    String desconocido = cuerpo(login("nadie@copat.cl", "semilla1"));
    String claveMala = cuerpo(login("semilla1@copat.cl", "clave-mala"));
    String vacio = cuerpo(login("semilla1@copat.cl", ""));
    assertThat(desconocido).isEqualTo(claveMala).isEqualTo(vacio);
    assertThat(desconocido).contains(Mensajes.CREDENCIALES);
    assertThat(desconocido.toLowerCase()).doesNotContain("no existe", "usuario no");
    assertThat(login("semilla1@copat.cl", "clave-mala").getResponse().getStatus()).isEqualTo(401);
    assertThat(login("nadie@copat.cl", "semilla1").getResponse().getStatus()).isEqualTo(401);
  }

  @Test
  void laContrasenaSemillaQuedaHasheada() {
    String hash = jdbc.queryForObject(
        "select password_hash from usuario where email = ?",
        String.class,
        "semilla1@copat.cl");
    assertThat(hash).isNotEqualTo("semilla1");
    assertThat(passwordEncoder.matches("semilla1", hash)).isTrue();
  }

  @Test
  void creaSoloSolicitudCreadaYRegistraLaBitacora() throws Exception {
    MockHttpSession sesion = ingresar();
    MvcResult creada = crear(sesion, solicitud("Empresa Uno", "12.345.678-5", "01/01/2026", "31/01/2026"));
    assertThat(creada.getResponse().getStatus()).isEqualTo(201);
    JsonNode cuerpo = json.readTree(creada.getResponse().getContentAsString());
    assertThat(cuerpo.get("estado").asText()).isEqualTo("solicitud creada");
    assertThat(cuerpo.get("rut").asText()).isEqualTo("12.345.678-5");
    assertThat(cuerpo.get("plantilla").asText()).isEqualTo("Guia evaluacion");

    String estadoEnBase = jdbc.queryForObject(
        "select e.nombre from solicitud s join estado_solicitud e on e.id = s.estado_id",
        String.class);
    assertThat(estadoEnBase).isEqualTo(Catalogo.ESTADO_INICIAL);

    JsonNode log = json.readTree(mockMvc.perform(get("/api/departamento1/bitacora").session(sesion))
        .andReturn().getResponse().getContentAsString());
    assertThat(log).hasSize(1);
    assertThat(log.get(0).get("accion").asText()).isEqualTo("Solicitud creada");
    assertThat(log.get(0).get("usuario").asText()).isEqualTo("Semilla Uno (semilla1@copat.cl)");
    assertThat(log.get(0).get("fecha").asText())
        .isEqualTo(Fechas.format(LocalDate.now(ZoneId.of("America/Santiago"))));
    assertThat(log.get(0).get("hora").asText()).matches("\\d{2}:\\d{2}:\\d{2}");
  }

  @Test
  void ignoraUnEstadoEnviadoPorElCliente() throws Exception {
    MockHttpSession sesion = ingresar();
    Map<String, Object> cuerpo = solicitud("Empresa Uno", "12.345.678-5", "01/03/2026", "31/03/2026");
    cuerpo.put("estado", "solicitud aprobada");
    MvcResult creada = crear(sesion, cuerpo);
    assertThat(json.readTree(creada.getResponse().getContentAsString()).get("estado").asText())
        .isEqualTo("solicitud creada");
  }

  @Test
  void permiteOtroPeriodoYBloqueaLasMismasFechas() throws Exception {
    MockHttpSession sesion = ingresar();
    assertThat(crear(sesion, solicitud("Empresa Uno", "12.345.678-5", "01/01/2026", "31/01/2026"))
        .getResponse().getStatus()).isEqualTo(201);
    assertThat(crear(sesion, solicitud("Empresa Uno", "12345678-5", "01/02/2026", "28/02/2026"))
        .getResponse().getStatus()).isEqualTo(201);

    MvcResult repetida = crear(sesion, solicitud("Otro nombre", "12.345.678-5", "01/01/2026", "31/01/2026"));
    assertThat(repetida.getResponse().getStatus()).isEqualTo(409);
    assertThat(repetida.getResponse().getContentAsString()).contains(Mensajes.DUPLICADO);

    JsonNode todas = json.readTree(mockMvc.perform(get("/api/departamento1/solicitudes").session(sesion))
        .andReturn().getResponse().getContentAsString());
    assertThat(todas).hasSize(2);
  }

  @Test
  void filtraPorLosEstadosDelCatalogo() throws Exception {
    MockHttpSession sesion = ingresar();
    crear(sesion, solicitud("Empresa Uno", "12.345.678-5", "01/04/2026", "30/04/2026"));

    JsonNode estados = json.readTree(mockMvc.perform(get("/api/departamento1/estados").session(sesion))
        .andReturn().getResponse().getContentAsString());
    assertThat(estados).hasSize(5);
    assertThat(json.convertValue(estados, String[].class)).containsExactly(Catalogo.ESTADOS.toArray(String[]::new));

    JsonNode creadas = json.readTree(mockMvc.perform(get("/api/departamento1/solicitudes")
            .param("estado", "solicitud creada").session(sesion))
        .andReturn().getResponse().getContentAsString());
    assertThat(creadas).hasSize(1);

    for (String estado : new String[] {
        "solicitud enviada a cliente", "solicitud en revision", "solicitud aprobada", "rechazada"}) {
      JsonNode vacia = json.readTree(mockMvc.perform(get("/api/departamento1/solicitudes")
              .param("estado", estado).session(sesion))
          .andReturn().getResponse().getContentAsString());
      assertThat(vacia).isEmpty();
    }
  }

  @Test
  void elServidorRepiteLasValidaciones() throws Exception {
    MockHttpSession sesion = ingresar();
    Map<String, Object> invalida = Map.of(
        "nombreEmpresa", "Empresa-1",
        "rut", "12.345.678-9",
        "correo", "no-es-correo",
        "telefono", "+56912345678",
        "periodoInicio", "31/02/2026",
        "periodoFin", "01/13/2026",
        "plantilla", "Otra guia",
        "descripcion", "x".repeat(301));
    MvcResult respuesta = crear(sesion, invalida);
    assertThat(respuesta.getResponse().getStatus()).isEqualTo(400);
    JsonNode campos = json.readTree(respuesta.getResponse().getContentAsString()).get("campos");
    assertThat(campos.get("nombreEmpresa").asText()).isEqualTo(Mensajes.NOMBRE);
    assertThat(campos.get("rut").asText()).isEqualTo(Mensajes.RUT);
    assertThat(campos.get("correo").asText()).isEqualTo(Mensajes.CORREO);
    assertThat(campos.get("telefono").asText()).isEqualTo(Mensajes.TELEFONO);
    assertThat(campos.get("periodoInicio").asText()).isEqualTo(Mensajes.FECHA_INICIO);
    assertThat(campos.get("periodoFin").asText()).isEqualTo(Mensajes.FECHA_FIN);
    assertThat(campos.get("plantilla").asText()).isEqualTo(Mensajes.PLANTILLA);
    assertThat(campos.get("descripcion").asText()).isEqualTo(Mensajes.DESCRIPCION);

    Map<String, Object> periodo = solicitud("Empresa Uno", "12.345.678-5", "31/03/2026", "01/01/2026");
    JsonNode periodoCampos = json.readTree(crear(sesion, periodo).getResponse().getContentAsString()).get("campos");
    assertThat(periodoCampos.get("periodoFin").asText()).isEqualTo(Mensajes.PERIODO);
    assertThat(jdbc.queryForObject("select count(*) from solicitud", Integer.class)).isZero();
  }

  @Test
  void laBaseImpideRutInvalidoYNombreConSimbolos() {
    Long usuarioId = jdbc.queryForObject("select id from usuario", Long.class);
    assertThatThrownBy(() -> jdbc.update("""
        insert into solicitud (
          nombre_empresa, rut, correo, periodo_inicio, periodo_fin,
          plantilla_id, estado_id, departamento_id, creado_por_id)
        values ('Empresa Uno', '12345678-9', 'a@b.cl', '2026-01-01', '2026-01-31', 1, 1, 1, ?)
        """, usuarioId)).isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update("""
        insert into solicitud (
          nombre_empresa, rut, correo, periodo_inicio, periodo_fin,
          plantilla_id, estado_id, departamento_id, creado_por_id)
        values ('Empresa-1', '12345678-5', 'a@b.cl', '2026-01-01', '2026-01-31', 1, 1, 1, ?)
        """, usuarioId)).isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void sinSesionNoListaSolicitudes() throws Exception {
    mockMvc.perform(get("/api/departamento1/solicitudes"))
        .andExpect(status().isUnauthorized());
  }

  private MockHttpSession ingresar() throws Exception {
    MvcResult resultado = login("semilla1@copat.cl", "semilla1");
    assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    return (MockHttpSession) resultado.getRequest().getSession(false);
  }

  private MvcResult login(String correo, String contrasena) throws Exception {
    return mockMvc.perform(post("/api/auth/login")
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("correo", correo, "contrasena", contrasena))))
        .andReturn();
  }

  private MvcResult crear(MockHttpSession sesion, Map<String, Object> cuerpo) throws Exception {
    return mockMvc.perform(post("/api/departamento1/solicitudes")
            .session(sesion)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(cuerpo)))
        .andReturn();
  }

  private static String cuerpo(MvcResult resultado) throws Exception {
    return resultado.getResponse().getContentAsString();
  }

  private static Map<String, Object> solicitud(String empresa, String rut, String inicio, String fin) {
    Map<String, Object> cuerpo = new java.util.LinkedHashMap<>();
    cuerpo.put("nombreEmpresa", empresa);
    cuerpo.put("rut", rut);
    cuerpo.put("correo", "contacto@empresa.cl");
    cuerpo.put("telefono", "912345678");
    cuerpo.put("periodoInicio", inicio);
    cuerpo.put("periodoFin", fin);
    cuerpo.put("plantilla", "Guia evaluacion");
    cuerpo.put("descripcion", "Evaluación del período");
    return cuerpo;
  }
}
