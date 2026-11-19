package cl.copat.evalua.departamento2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import cl.copat.evalua.departamento2.dominio.GuiaPdf;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Departamento2IntegracionTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  ObjectMapper json;

  @Autowired
  JdbcTemplate jdbc;

  private Cookie csrf;

  @BeforeEach
  void limpiar() throws Exception {
    jdbc.update("DELETE FROM registro_actividad");
    jdbc.update("DELETE FROM rechazo_solicitud");
    jdbc.update("DELETE FROM evidencia_revision");
    jdbc.update("DELETE FROM revision_departamento3");
    jdbc.update("DELETE FROM notificacion");
    jdbc.update("DELETE FROM solicitud_evaluacion");
    jdbc.update("DELETE FROM solicitud_pdf");
    jdbc.update("DELETE FROM accion_evaluacion");
    jdbc.update("DELETE FROM solicitud_log");
    jdbc.update("DELETE FROM solicitud_sede");
    jdbc.update("DELETE FROM solicitud");
    jdbc.update("""
        DELETE FROM usuario
        WHERE email NOT IN ('semilla1@copat.cl', 'semilla2@copat.cl', 'semilla3@copat.cl', 'admin@copat.cl')
        """);
    MvcResult preparacion = mockMvc.perform(get("/api/auth/csrf")).andReturn();
    csrf = preparacion.getResponse().getCookie("XSRF-TOKEN");
    assertThat(csrf).isNotNull();
  }

  @Test
  void elPdfCompletoPasaLaSolicitudARevisionYElTercerDepartamentoLaEvalua() throws Exception {
    MockHttpSession depto1 = ingresar("semilla1@copat.cl", "semilla1");
    MvcResult creada = crear(depto1);
    assertThat(creada.getResponse().getStatus()).isEqualTo(201);
    long id = json.readTree(creada.getResponse().getContentAsString()).get("id").asLong();

    MockHttpSession depto2 = ingresar("semilla2@copat.cl", "semilla2");
    JsonNode sesion = json.readTree(login("semilla2@copat.cl", "semilla2").getResponse().getContentAsString());
    assertThat(sesion.get("departamentoId").asInt()).isEqualTo(1);

    MockHttpSession depto3Ajeno = ingresar("semilla3@copat.cl", "semilla3");
    assertThat(mockMvc.perform(post("/api/departamento1/solicitudes/" + id + "/enviada").session(depto3Ajeno)
            .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
        .andReturn().getResponse().getStatus()).isEqualTo(403);

    MvcResult temprano = enviarPdf(depto2, id, GuiaPdf.conValores(Catalogo.GUIA_1, guiaCompleta()));
    assertThat(temprano.getResponse().getStatus()).isEqualTo(400);
    assertThat(temprano.getResponse().getContentAsString()).contains(Mensajes.ESTADO_ENVIADA);

    MvcResult enviada = mockMvc.perform(post("/api/departamento1/solicitudes/" + id + "/enviada").session(depto2)
            .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
        .andReturn();
    assertThat(json.readTree(enviada.getResponse().getContentAsString()).get("estado").asText())
        .isEqualTo("solicitud enviada a cliente");

    byte[] guia = mockMvc.perform(get("/api/departamento1/solicitudes/" + id + "/guia").session(depto2))
        .andReturn().getResponse().getContentAsByteArray();
    assertThat(new String(guia, 0, 4)).isEqualTo("%PDF");

    MvcResult incompleto = enviarPdf(depto2, id, GuiaPdf.enBlanco(Catalogo.GUIA_1));
    assertThat(incompleto.getResponse().getStatus()).isEqualTo(400);
    assertThat(incompleto.getResponse().getContentAsString()).contains("percepcion_general");
    assertThat(jdbc.queryForObject(
        "select e.nombre from solicitud s join estado_solicitud e on e.id = s.estado_id where s.id = ?",
        String.class, id)).isEqualTo("solicitud enviada a cliente");

    MvcResult revisada = enviarPdf(depto2, id, GuiaPdf.conValores(Catalogo.GUIA_1, guiaCompleta()));
    assertThat(revisada.getResponse().getStatus()).isEqualTo(200);
    assertThat(json.readTree(revisada.getResponse().getContentAsString()).get("estado").asText())
        .isEqualTo("solicitud en revision");
    assertThat(jdbc.queryForObject("select count(*) from notificacion where departamento_id = 3", Integer.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("select desempeno from solicitud_evaluacion where solicitud_id = ?", String.class, id))
        .isEqualTo("Satisfactorio");

    MockHttpSession depto3 = ingresar("semilla3@copat.cl", "semilla3");
    JsonNode panel = json.readTree(mockMvc.perform(get("/api/departamento3/solicitudes").session(depto3))
        .andReturn().getResponse().getContentAsString());
    assertThat(panel).hasSize(1);
    assertThat(panel.get(0).get("desempeno").asText()).isEqualTo("Satisfactorio");
    assertThat(panel.get(0).get("promedio").decimalValue()).isEqualByComparingTo("4.40");
    assertThat(panel.get(0).get("estadoObra").asText()).isEqualTo("Obra en construcción");
    assertThat(panel.get(0).get("encuestado").asText()).isEqualTo("Ana Soto");
    assertThat(panel.get(0).get("notas")).hasSize(10);
    assertThat(panel.get(0).get("notas").get(0).get("valor").asInt()).isEqualTo(5);
    assertThat(panel.get(0).get("creadoPor").asText()).isEqualTo("Semilla Uno");
    assertThat(panel.get(0).get("movimientos").toString()).contains("Solicitud creada", "Semilla Uno");
    assertThat(panel.get(0).get("movimientos").toString()).contains("Solicitud enviada a cliente", "Semilla Dos");
    assertThat(panel.get(0).get("movimientos").toString()).contains("Solicitud enviada a revision", "Semilla Dos");

    MvcResult larga = guardar(depto3, id, "a".repeat(501), null, null);
    assertThat(larga.getResponse().getStatus()).isEqualTo(400);
    assertThat(larga.getResponse().getContentAsString()).contains(Mensajes.DESCRIPCION_REVISION_LARGA);

    byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    MvcResult guardada = guardar(depto3, id, "Nota de la revision", png, "En gestión");
    assertThat(guardada.getResponse().getStatus()).isEqualTo(200);
    JsonNode seguida = json.readTree(guardada.getResponse().getContentAsString());
    assertThat(seguida.get("tieneEvidencia").asBoolean()).isTrue();
    assertThat(seguida.get("descripcion").asText()).isEqualTo("Nota de la revision");
    assertThat(seguida.get("seguimiento").asText()).isEqualTo("En gestión");
    assertThat(seguida.get("notasSeguimiento")).hasSize(1);

    JsonNode operacion = json.readTree(mockMvc.perform(get("/api/departamento1/solicitudes").session(depto2))
        .andReturn().getResponse().getContentAsString());
    JsonNode fila = null;
    for (JsonNode item : operacion) {
      if (item.get("id").asLong() == id) {
        fila = item;
      }
    }
    assertThat(fila).isNotNull();
    assertThat(fila.get("seguimiento").asText()).isEqualTo("En procesamiento");
    assertThat(fila.get("desempeno").asText()).isEqualTo("Satisfactorio");

    MvcResult cerrada = guardar(depto3, id, "Caso resuelto", null, "Cerrada");
    assertThat(cerrada.getResponse().getStatus()).isEqualTo(200);
    assertThat(json.readTree(cerrada.getResponse().getContentAsString()).get("seguimiento").asText()).isEqualTo("Cerrada");
    assertThat(json.readTree(cerrada.getResponse().getContentAsString()).get("notasSeguimiento")).hasSize(2);
    JsonNode despues = json.readTree(mockMvc.perform(get("/api/departamento1/solicitudes").session(depto2))
        .andReturn().getResponse().getContentAsString());
    JsonNode cerradaParaOperacion = null;
    for (JsonNode item : despues) {
      if (item.get("id").asLong() == id) {
        cerradaParaOperacion = item;
      }
    }
    assertThat(cerradaParaOperacion.get("seguimiento").asText()).isEqualTo("Cerrado");
    MvcResult reabierta = guardar(depto3, id, "Intento de reabrir", null, "En gestión");
    assertThat(reabierta.getResponse().getStatus()).isEqualTo(400);
    assertThat(reabierta.getResponse().getContentAsString()).contains(Mensajes.SEGUIMIENTO_CERRADO);

    assertThat(mockMvc.perform(get("/api/departamento3/solicitudes/" + id + "/evidencia").session(depto3))
        .andReturn().getResponse().getStatus()).isEqualTo(200);
    assertThat(mockMvc.perform(get("/api/departamento3/solicitudes").session(depto2))
        .andReturn().getResponse().getStatus()).isEqualTo(403);
  }

  @Test
  void departamento3NoPuedeDevolverUnaEncuestaYaEnviadaAlCliente() throws Exception {
    MockHttpSession depto1 = ingresar("semilla1@copat.cl", "semilla1");
    long id = json.readTree(crear(depto1).getResponse().getContentAsString()).get("id").asLong();
    MockHttpSession depto3 = ingresar("semilla3@copat.cl", "semilla3");
    assertThat(mockMvc.perform(post("/api/departamento3/solicitudes/" + id + "/rechazo").session(depto3)
            .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"descripcion\":\"Falta precisar la obra\"}"))
        .andReturn().getResponse().getStatus()).isEqualTo(404);
  }

  private MvcResult enviarPdf(MockHttpSession sesion, long id, byte[] pdf) throws Exception {
    return mockMvc.perform(multipart("/api/departamento1/solicitudes/" + id + "/pdf")
            .file(new MockMultipartFile("archivo", "guia.pdf", "application/pdf", pdf))
            .session(sesion)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue()))
        .andReturn();
  }

  private MvcResult guardar(MockHttpSession sesion, long id, String descripcion, byte[] png, String estado) throws Exception {
    MockMultipartHttpServletRequestBuilder peticion = multipart("/api/departamento3/solicitudes/" + id + "/revision");
    peticion.param("descripcion", descripcion);
    if (estado != null) {
      peticion.param("estado", estado);
    }
    peticion.session(sesion);
    peticion.cookie(csrf);
    peticion.header("X-XSRF-TOKEN", csrf.getValue());
    if (png != null) {
      peticion.file(new MockMultipartFile("evidencia", "foto.png", "image/png", png));
    }
    return mockMvc.perform(peticion).andReturn();
  }

  private MockHttpSession ingresar(String correo, String contrasena) throws Exception {
    MvcResult resultado = login(correo, contrasena);
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

  private MvcResult crear(MockHttpSession sesion) throws Exception {
    Map<String, Object> cuerpo = new LinkedHashMap<>();
    cuerpo.put("nombreEmpresa", "Empresa Uno");
    cuerpo.put("rut", "12.345.678-5");
    cuerpo.put("correo", "contacto@empresa.cl");
    cuerpo.put("telefono", "912345678");
    cuerpo.put("periodoInicio", "01/04/2026");
    cuerpo.put("periodoFin", "30/04/2026");
    cuerpo.put("plantilla", "Guia evaluacion");
    cuerpo.put("sedes", List.of("Antofagasta"));
    cuerpo.put("descripcion", "Evaluación del período");
    return mockMvc.perform(post("/api/departamento1/solicitudes")
            .session(sesion)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(cuerpo)))
        .andReturn();
  }

  private static Map<String, String> guiaCompleta() {
    return GuiaPdfTest.encuesta();
  }
}
