package cl.copat.evalua.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
            .content(cuerpo("otro@copat.cl", "clave1234", "Otra Persona", "Analista", 1)))
        .andReturn().getResponse().getStatus()).isEqualTo(403);

    MvcResult creado = mockMvc.perform(post("/api/admin/usuarios").session(admin)
            .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo("visita@copat.cl", "visita123", "Visita", "Jefa de obra", 1)))
        .andReturn();
    assertThat(creado.getResponse().getStatus()).isEqualTo(200);
    assertThat(json.readTree(creado.getResponse().getContentAsString()).get("claveProvisoria").asBoolean()).isTrue();
    assertThat(json.readTree(creado.getResponse().getContentAsString()).get("cargo").asText()).isEqualTo("Jefa de obra");

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
    assertThat(json.readTree(nombre.getResponse().getContentAsString()).get("cargo").asText()).isEqualTo("Jefa de obra");

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

    MvcResult temporal = mockMvc.perform(post("/api/admin/usuarios").session(admin)
            .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo("temporal@copat.cl", "temporal1", "Temporal", "Analista", 1)))
        .andReturn();
    long temporalId = json.readTree(temporal.getResponse().getContentAsString()).get("id").asLong();
    MvcResult editado = mockMvc.perform(put("/api/admin/usuarios/" + temporalId).session(admin)
            .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of(
                "correo", "temporal2@copat.cl",
                "nombre", "Temporal Dos",
                "cargo", "Jefe de obra",
                "departamentoId", 3,
                "clave", "temporal12"))))
        .andReturn();
    assertThat(editado.getResponse().getStatus()).isEqualTo(200);
    JsonNode cuenta = json.readTree(editado.getResponse().getContentAsString());
    assertThat(cuenta.get("email").asText()).isEqualTo("temporal2@copat.cl");
    assertThat(cuenta.get("nombre").asText()).isEqualTo("Temporal Dos");
    assertThat(cuenta.get("cargo").asText()).isEqualTo("Jefe de obra");
    assertThat(cuenta.get("departamentoId").asInt()).isEqualTo(3);
    assertThat(cuenta.get("claveProvisoria").asBoolean()).isTrue();
    assertThat(ingresar("temporal2@copat.cl", "temporal12").isInvalid()).isFalse();

    assertThat(mockMvc.perform(delete("/api/admin/usuarios/" + temporalId).session(admin)
            .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
        .andReturn().getResponse().getStatus()).isEqualTo(204);
    assertThat(mockMvc.perform(get("/api/admin/usuarios").session(admin)).andReturn().getResponse().getContentAsString())
        .doesNotContain("temporal2@copat.cl");

    long adminId = adminId(admin);
    MvcResult propio = mockMvc.perform(delete("/api/admin/usuarios/" + adminId).session(admin)
            .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
        .andReturn();
    assertThat(propio.getResponse().getStatus()).isEqualTo(400);
    assertThat(propio.getResponse().getContentAsString()).contains("No puede eliminar su propia cuenta.");

    long visitaId = json.readTree(creado.getResponse().getContentAsString()).get("id").asLong();
    MvcResult conMovimientos = mockMvc.perform(delete("/api/admin/usuarios/" + visitaId).session(admin)
            .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
        .andReturn();
    assertThat(conMovimientos.getResponse().getStatus()).isEqualTo(400);
    assertThat(conMovimientos.getResponse().getContentAsString()).contains("movimientos registrados");

    MvcResult desactivado = mockMvc.perform(post("/api/admin/usuarios/" + visitaId + "/desactivar").session(admin)
            .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
        .andReturn();
    assertThat(desactivado.getResponse().getStatus()).isEqualTo(200);
    assertThat(json.readTree(desactivado.getResponse().getContentAsString()).get("activo").asBoolean()).isFalse();
    MvcResult ingreso = mockMvc.perform(post("/api/auth/login")
            .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("correo", "visita@copat.cl", "contrasena", "visita1234"))))
        .andReturn();
    assertThat(ingreso.getResponse().getStatus()).isEqualTo(401);
    assertThat(ingreso.getResponse().getContentAsString()).contains("desactivada");
    assertThat(mockMvc.perform(get("/api/admin/registros").session(admin)).andReturn().getResponse().getContentAsString())
        .contains("Visita Ajustes (usuario desactivado)");

    MvcResult reactivado = mockMvc.perform(post("/api/admin/usuarios/" + visitaId + "/reactivar").session(admin)
            .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
        .andReturn();
    assertThat(reactivado.getResponse().getStatus()).isEqualTo(200);
    assertThat(json.readTree(reactivado.getResponse().getContentAsString()).get("activo").asBoolean()).isTrue();
    assertThat(ingresar("visita@copat.cl", "visita1234").isInvalid()).isFalse();
  }

  private long adminId(MockHttpSession admin) throws Exception {
    JsonNode lista = json.readTree(mockMvc.perform(get("/api/admin/usuarios").session(admin))
        .andReturn().getResponse().getContentAsString());
    for (JsonNode usuario : lista) {
      if ("admin@copat.cl".equals(usuario.get("email").asText())) {
        return usuario.get("id").asLong();
      }
    }
    throw new AssertionError("No está la cuenta admin");
  }

  private String cuerpo(String correo, String clave, String nombre, String cargo, int departamento) throws Exception {
    return json.writeValueAsString(Map.of(
        "correo", correo,
        "clave", clave,
        "nombre", nombre,
        "cargo", cargo,
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
