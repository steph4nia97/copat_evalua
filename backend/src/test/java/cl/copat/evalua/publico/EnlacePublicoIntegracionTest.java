package cl.copat.evalua.publico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import cl.copat.evalua.departamento1.dominio.Catalogo;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnlacePublicoIntegracionTest {

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
    jdbc.update("DELETE FROM enlace_encuesta");
    jdbc.update("DELETE FROM reclamo");
    jdbc.update("DELETE FROM solicitud_log");
    jdbc.update("DELETE FROM solicitud_sede");
    jdbc.update("DELETE FROM solicitud");
    MvcResult preparacion = mockMvc.perform(get("/api/auth/csrf")).andReturn();
    csrf = preparacion.getResponse().getCookie("XSRF-TOKEN");
    assertThat(csrf).isNotNull();
  }

  @Test
  void elClienteRespondePorElEnlaceYPuedeCorregirSiLaRechazan() throws Exception {
    MockHttpSession depto1 = ingresar("semilla1@copat.cl", "semilla1");
    long id = json.readTree(crear(depto1).getResponse().getContentAsString()).get("id").asLong();
    MockHttpSession depto2 = ingresar("semilla2@copat.cl", "semilla2");

    MvcResult enlace = mockMvc.perform(post("/api/departamento2/solicitudes/" + id + "/enlace")
            .session(depto2)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue()))
        .andReturn();
    assertThat(enlace.getResponse().getStatus()).isEqualTo(200);
    String token = json.readTree(enlace.getResponse().getContentAsString()).get("token").asText();
    assertThat(jdbc.queryForObject("select nombre from estado_solicitud e join solicitud s on s.estado_id = e.id where s.id = ?", String.class, id))
        .isEqualTo(Catalogo.ESTADO_ENVIADA_CLIENTE);

    MvcResult publica = mockMvc.perform(get("/api/publico/encuestas/" + token)).andReturn();
    assertThat(publica.getResponse().getStatus()).isEqualTo(200);
    JsonNode formulario = json.readTree(publica.getResponse().getContentAsString());
    assertThat(formulario.get("abierta").asBoolean()).isTrue();
    assertThat(formulario.get("empresa").asText()).isEqualTo("Empresa Uno");
    assertThat(formulario.get("preguntas")).hasSize(10);

    MvcResult respuesta = mockMvc.perform(post("/api/publico/encuestas/" + token)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(encuesta("Ana Soto", 5))))
        .andReturn();
    assertThat(respuesta.getResponse().getStatus()).isEqualTo(204);
    assertThat(jdbc.queryForObject("select nombre from estado_solicitud e join solicitud s on s.estado_id = e.id where s.id = ?", String.class, id))
        .isEqualTo(Catalogo.ESTADO_EN_REVISION);
    assertThat(jdbc.queryForObject("select nombre_encuestado from solicitud_evaluacion where solicitud_id = ?", String.class, id))
        .isEqualTo("Ana Soto");

    MvcResult cerrada = mockMvc.perform(post("/api/publico/encuestas/" + token)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(encuesta("Ana Soto", 5))))
        .andReturn();
    assertThat(cerrada.getResponse().getStatus()).isEqualTo(400);
    assertThat(jdbc.queryForObject(
            "select count(*) from notificacion where solicitud_id = ? and departamento_id = 3", Integer.class, id))
        .isEqualTo(1);
  }

  @Test
  void unClienteEnviaUnReclamoSinCuentaYDepartamento3LoVe() throws Exception {
    Map<String, String> cuerpo = new LinkedHashMap<>();
    cuerpo.put("nombre", "Juan Pérez");
    cuerpo.put("telefono", "+56 9 1234 5678");
    cuerpo.put("obra", "Edificio Mirador");
    cuerpo.put("fechaServicio", "02/11/2026");
    cuerpo.put("empresa", "Constructora Norte");
    cuerpo.put("correo", "juan.perez@empresa.cl");
    cuerpo.put("ubicacion", "Av. Industrial 123, Antofagasta");
    cuerpo.put("guiaDespacho", "GD-123456");
    cuerpo.put("tipo", "Calidad del hormigón");
    cuerpo.put("descripcion", "El hormigón llegó con una consistencia distinta a la pedida.");
    cuerpo.put("preferenciaContacto", "Teléfono");
    cuerpo.put("horario", "Mañana, 08:00 a 12:00");
    cuerpo.put("comentarios", "Pueden llamar al jefe de obra.");

    MvcResult creado = mockMvc.perform(post("/api/publico/reclamos")
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(cuerpo)))
        .andReturn();
    assertThat(creado.getResponse().getStatus()).isEqualTo(200);
    assertThat(jdbc.queryForObject("select telefono from reclamo where correo = ?", String.class, "juan.perez@empresa.cl"))
        .isEqualTo("56912345678");

    MockHttpSession depto3 = ingresar("semilla3@copat.cl", "semilla3");
    JsonNode lista = json.readTree(mockMvc.perform(get("/api/departamento3/reclamos").session(depto3))
        .andReturn().getResponse().getContentAsString());
    assertThat(lista).hasSize(1);
    assertThat(lista.get(0).get("tipo").asText()).isEqualTo("Calidad del hormigón");

    MockHttpSession depto2 = ingresar("semilla2@copat.cl", "semilla2");
    assertThat(mockMvc.perform(get("/api/departamento3/reclamos").session(depto2))
        .andReturn().getResponse().getStatus()).isEqualTo(403);
  }

  private Map<String, Object> encuesta(String nombre, int general) {
    Map<String, Integer> notas = new LinkedHashMap<>();
    notas.put("gestion_administrativa", 5);
    notas.put("tiempos_entrega", 5);
    notas.put("estado_hormigon", 5);
    notas.put("flexibilidad", 5);
    notas.put("atencion_comercial", 4);
    notas.put("respuesta_reclamos", 4);
    notas.put("entrega_informacion", 4);
    notas.put("competitividad_precio", 4);
    notas.put("capacitacion_tecnica", 4);
    notas.put("percepcion_general", general);
    Map<String, Object> cuerpo = new LinkedHashMap<>();
    cuerpo.put("nombre", nombre);
    cuerpo.put("cargo", "Jefa de obra");
    cuerpo.put("obra", "Edificio Costanera");
    cuerpo.put("estadoObra", "Obra en construcción");
    cuerpo.put("comentarios", "Entrega puntual");
    cuerpo.put("notas", notas);
    return cuerpo;
  }

  private MockHttpSession ingresar(String correo, String contrasena) throws Exception {
    MvcResult resultado = mockMvc.perform(post("/api/auth/login")
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("correo", correo, "contrasena", contrasena))))
        .andReturn();
    assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    return (MockHttpSession) resultado.getRequest().getSession(false);
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
    MvcResult creada = mockMvc.perform(post("/api/departamento1/solicitudes")
            .session(sesion)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(cuerpo)))
        .andReturn();
    assertThat(creada.getResponse().getStatus()).isEqualTo(201);
    return creada;
  }
}
