package cl.copat.evalua.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
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
class AdminIntegracionTest {

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
    jdbc.update("DELETE FROM solicitud_log");
    jdbc.update("DELETE FROM solicitud_sede");
    jdbc.update("DELETE FROM solicitud");
    jdbc.update("""
        DELETE FROM usuario
        WHERE email NOT IN ('semilla1@copat.cl', 'semilla2@copat.cl', 'semilla3@copat.cl', 'admin@copat.cl')
        """);
    MvcResult preparacion = mockMvc.perform(get("/api/auth/csrf")).andReturn();
    csrf = preparacion.getResponse().getCookie("XSRF-TOKEN");
  }

  @Test
  void elAdminCreaUsuariosYElTrabajadorCambiaSuClaveProvisoria() throws Exception {
    MockHttpSession admin = ingresar("admin@copat.cl", "admin123");
    assertThat(json.readTree(mockMvc.perform(get("/api/auth/me").session(admin)).andReturn().getResponse().getContentAsString())
        .get("departamentoId").asInt()).isEqualTo(4);

    assertThat(mockMvc.perform(post("/api/admin/usuarios").session(ingresar("semilla1@copat.cl", "semilla1"))
            .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo("otro@copat.cl", "clave1234", "Otra Persona", 1)))
        .andReturn().getResponse().getStatus()).isEqualTo(403);

    MvcResult creado = mockMvc.perform(post("/api/admin/usuarios").session(admin)
            .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo("visita@copat.cl", "visita123", "Visita", 1)))
        .andReturn();
    assertThat(creado.getResponse().getStatus()).isEqualTo(200);
    assertThat(json.readTree(creado.getResponse().getContentAsString()).get("claveProvisoria").asBoolean()).isTrue();

    MockHttpSession visita = ingresar("visita@copat.cl", "visita123");
    assertThat(mockMvc.perform(put("/api/cuenta/nombre").session(visita)
            .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("nombre", "Visita Ajustes", "clave", "incorrecta"))))
        .andReturn().getResponse().getStatus()).isEqualTo(400);
    MvcResult nombre = mockMvc.perform(put("/api/cuenta/nombre").session(visita)
            .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("nombre", "Visita Ajustes", "clave", "visita123"))))
        .andReturn();
    assertThat(nombre.getResponse().getStatus()).isEqualTo(200);
    assertThat(json.readTree(nombre.getResponse().getContentAsString()).get("nombre").asText()).isEqualTo("Visita Ajustes");

    MvcResult clave = mockMvc.perform(put("/api/cuenta/clave").session(visita)
            .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("claveActual", "visita123", "claveNueva", "visita1234"))))
        .andReturn();
    assertThat(clave.getResponse().getStatus()).isEqualTo(200);
    assertThat(json.readTree(clave.getResponse().getContentAsString()).get("claveProvisoria").asBoolean()).isFalse();
    assertThat(ingresar("visita@copat.cl", "visita1234").isInvalid()).isFalse();

    JsonNode registros = json.readTree(mockMvc.perform(get("/api/admin/registros").session(admin))
        .andReturn().getResponse().getContentAsString());
    assertThat(registros.toString()).contains("Usuario creado", "Nombre actualizado", "Contrasena actualizada");
  }

  private String cuerpo(String correo, String clave, String nombre, int departamento) throws Exception {
    return json.writeValueAsString(Map.of(
        "correo", correo,
        "clave", clave,
        "nombre", nombre,
        "departamentoId", departamento));
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
}
