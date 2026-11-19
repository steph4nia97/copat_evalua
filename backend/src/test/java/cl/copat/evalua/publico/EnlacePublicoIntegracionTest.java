package cl.copat.evalua.publico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import javax.imageio.ImageIO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
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
    jdbc.update("DELETE FROM informe_gestion_reclamo");
    jdbc.update("DELETE FROM reclamo");
    jdbc.update("DELETE FROM accion_evaluacion");
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

    MvcResult enlace = mockMvc.perform(post("/api/departamento1/solicitudes/" + id + "/enlace")
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
  void laEvaluacionGuardaConclusionesYAcciones() throws Exception {
    MockHttpSession depto1 = ingresar("semilla1@copat.cl", "semilla1");
    long id = json.readTree(crear(depto1).getResponse().getContentAsString()).get("id").asLong();
    MvcResult enlace = mockMvc.perform(post("/api/departamento1/solicitudes/" + id + "/enlace")
            .session(depto1)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue()))
        .andReturn();
    String token = json.readTree(enlace.getResponse().getContentAsString()).get("token").asText();
    mockMvc.perform(post("/api/publico/encuestas/" + token)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(encuesta("Ana Soto", 5))))
        .andReturn();

    MockHttpSession depto3 = ingresar("semilla3@copat.cl", "semilla3");
    MvcResult conclusiones = mockMvc.perform(put("/api/departamento3/solicitudes/" + id + "/conclusiones")
            .session(depto3)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("texto", "Mantener el horario de entrega."))))
        .andReturn();
    assertThat(conclusiones.getResponse().getStatus()).isEqualTo(200);
    assertThat(json.readTree(conclusiones.getResponse().getContentAsString()).get("conclusiones").asText())
        .isEqualTo("Mantener el horario de entrega.");

    MvcResult creada = mockMvc.perform(post("/api/departamento3/solicitudes/" + id + "/acciones")
            .session(depto3)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of(
                "descripcion", "Confirmar la guía con obra",
                "responsable", "Semilla Tres",
                "plazo", "30/11/2026",
                "estado", "En gestión"))))
        .andReturn();
    assertThat(creada.getResponse().getStatus()).isEqualTo(200);
    JsonNode acciones = json.readTree(creada.getResponse().getContentAsString()).get("acciones");
    assertThat(acciones).hasSize(1);
    assertThat(acciones.get(0).get("estado").asText()).isEqualTo("En gestión");

    MvcResult cerrada = mockMvc.perform(multipart("/api/departamento3/solicitudes/" + id + "/revision")
            .param("descripcion", "Caso resuelto")
            .param("estado", "Cerrada")
            .session(depto3)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue()))
        .andReturn();
    assertThat(cerrada.getResponse().getStatus()).isEqualTo(200);
    JsonNode despues = json.readTree(cerrada.getResponse().getContentAsString());
    assertThat(despues.get("seguimiento").asText()).isEqualTo("Cerrada");
    assertThat(despues.get("acciones").get(0).get("estado").asText()).isEqualTo("Cerrado");
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

  @Test
  void elInformeDeCierreTraeLosDatosYElSeguimiento() throws Exception {
    MvcResult creado = mockMvc.perform(post("/api/publico/reclamos")
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(reclamo())))
        .andReturn();
    assertThat(creado.getResponse().getStatus()).isEqualTo(200);
    long id = json.readTree(creado.getResponse().getContentAsString()).get("id").asLong();

    MockHttpSession depto3 = ingresar("semilla3@copat.cl", "semilla3");
    MvcResult temprano = mockMvc.perform(get("/api/departamento3/reclamos/" + id + "/informe").session(depto3))
        .andReturn();
    assertThat(temprano.getResponse().getStatus()).isEqualTo(400);
    assertThat(temprano.getResponse().getContentAsString()).contains(Mensajes.RECLAMO_INFORME);

    MvcResult cerrado = mockMvc.perform(put("/api/departamento3/reclamos/" + id)
            .session(depto3)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of(
                "estado", "Cerrado",
                "area", "Calidad",
                "responsable", "Semilla Tres",
                "fechaGestion", "19/11/2026",
                "observaciones", "Se reviso la guia",
                "fechaCierre", "19/11/2026",
                "horaCierre", "10:30",
                "solucion", "Se repuso el despacho.",
                "sede", "Antofagasta"))))
        .andReturn();
    assertThat(cerrado.getResponse().getStatus()).isEqualTo(200);

    MvcResult informe = mockMvc.perform(get("/api/departamento3/reclamos/" + id + "/informe").session(depto3))
        .andReturn();
    assertThat(informe.getResponse().getStatus()).isEqualTo(200);
    assertThat(informe.getResponse().getContentType()).isEqualTo("application/pdf");
    assertThat(informe.getResponse().getHeader("Content-Disposition")).contains("D-SIG-09-02-reclamo-" + id + ".pdf");
    try (PDDocument documento = Loader.loadPDF(informe.getResponse().getContentAsByteArray())) {
      String texto = new PDFTextStripper().getText(documento);
      assertThat(texto).contains("D-SIG-09-02");
      assertThat(texto).contains("Constructora Norte");
      assertThat(texto).contains("Edificio Mirador");
      assertThat(texto).contains("GD-123456");
      assertThat(texto).contains("Calidad del hormigón");
      assertThat(texto).contains("Antofagasta");
      assertThat(texto).contains("Se repuso el despacho.");
      assertThat(texto).contains("10:30");
    }
  }

  @Test
  void elEstadoSigueLasObservacionesYElCierre() throws Exception {
    MvcResult creado = mockMvc.perform(post("/api/publico/reclamos")
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(reclamo())))
        .andReturn();
    long id = json.readTree(creado.getResponse().getContentAsString()).get("id").asLong();
    MockHttpSession depto3 = ingresar("semilla3@copat.cl", "semilla3");

    Map<String, String> seguimiento = new LinkedHashMap<>();
    seguimiento.put("estado", "Recibido");
    seguimiento.put("area", "Calidad");
    seguimiento.put("responsable", "Semilla Tres");
    seguimiento.put("fechaGestion", "19/11/2026");
    seguimiento.put("observaciones", "Se llamó a la obra");
    seguimiento.put("sede", "Antofagasta");
    MvcResult gestion = mockMvc.perform(put("/api/departamento3/reclamos/" + id)
            .session(depto3)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(seguimiento)))
        .andReturn();
    assertThat(gestion.getResponse().getStatus()).isEqualTo(200);
    assertThat(json.readTree(gestion.getResponse().getContentAsString()).get("estado").asText()).isEqualTo("En gestión");

    seguimiento.put("estado", "Recibido");
    seguimiento.put("solucion", "Se repuso el despacho.");
    MvcResult cerrado = mockMvc.perform(put("/api/departamento3/reclamos/" + id)
            .session(depto3)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(seguimiento)))
        .andReturn();
    assertThat(cerrado.getResponse().getStatus()).isEqualTo(200);
    assertThat(json.readTree(cerrado.getResponse().getContentAsString()).get("estado").asText()).isEqualTo("Cerrado");
  }

  @Test
  void elInformeDeGestionCalculaLosPorcentajesDelPeriodo() throws Exception {
    mockMvc.perform(post("/api/publico/reclamos")
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(reclamo())))
        .andReturn();
    mockMvc.perform(post("/api/publico/reclamos")
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(reclamo())))
        .andReturn();

    MockHttpSession depto3 = ingresar("semilla3@copat.cl", "semilla3");
    JsonNode lista = json.readTree(mockMvc.perform(get("/api/departamento3/reclamos").session(depto3))
        .andReturn().getResponse().getContentAsString());
    long id = lista.get(0).get("id").asLong();
    String fecha = lista.get(0).get("fecha").asText();
    MvcResult cerrado = mockMvc.perform(put("/api/departamento3/reclamos/" + id)
            .session(depto3)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of(
                "area", "Calidad",
                "responsable", "Semilla Tres",
                "fechaGestion", fecha,
                "observaciones", "Se reviso la guia",
                "fechaCierre", fecha,
                "horaCierre", "10:30",
                "solucion", "Se repuso el despacho.",
                "sede", "Antofagasta"))))
        .andReturn();
    assertThat(cerrado.getResponse().getStatus()).isEqualTo(200);

    MvcResult gestion = mockMvc.perform(get("/api/departamento3/reclamos/gestion")
            .param("desde", "01/01/2020")
            .param("hasta", "31/12/2030")
            .session(depto3))
        .andReturn();
    assertThat(gestion.getResponse().getStatus()).isEqualTo(200);
    JsonNode cuerpo = json.readTree(gestion.getResponse().getContentAsString());
    assertThat(cuerpo.get("total").asInt()).isEqualTo(2);
    assertThat(cuerpo.get("reincidentes").asInt()).isEqualTo(1);
    assertThat(cuerpo.get("porcentajeReincidentes").asText()).isEqualTo("50,0");
    assertThat(cuerpo.get("estadoReincidentes").asText()).isEqualTo("No cumple");
    assertThat(cuerpo.get("cerradosEnPlazo").asInt()).isEqualTo(1);
    assertThat(cuerpo.get("porcentajeCierre").asText()).isEqualTo("50,0");
    assertThat(cuerpo.get("estadoCierre").asText()).isEqualTo("No cumple");
    assertThat(cuerpo.get("tiempoPromedio").asText()).isEqualTo("0,0");
    assertThat(cuerpo.get("estadoTiempo").asText()).isEqualTo("Cumple");
    assertThat(cuerpo.get("principalesCausas").asText()).contains("Calidad del hormigón");
    assertThat(cuerpo.get("causasRecurrentes").asText()).isEqualTo("Calidad del hormigón: 2 (100,0%)");
    assertThat(cuerpo.get("areasFrecuencia").asText()).contains("Calidad: 1 (50,0%)").contains("Sin área: 1 (50,0%)");

    long semilla3 = jdbc.queryForObject("SELECT id FROM usuario WHERE email = 'semilla3@copat.cl'", Long.class);
    Map<String, Object> escrito = new LinkedHashMap<>();
    escrito.put("desde", "01/01/2020");
    escrito.put("hasta", "31/12/2030");
    escrito.put("responsable", "Semilla Tres");
    escrito.put("cargo", "Control Calidad");
    escrito.put("conclusiones", "Reforzar el control de consistencia.");
    escrito.put("comercialUsuarioId", semilla3);
    escrito.put("comercialFecha", "19/11/2026");
    escrito.put("comercialFirmaVirtual", true);
    MvcResult guardado = mockMvc.perform(put("/api/departamento3/reclamos/gestion")
            .session(depto3)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(escrito)))
        .andReturn();
    assertThat(guardado.getResponse().getStatus()).isEqualTo(200);
    JsonNode guardadoJson = json.readTree(guardado.getResponse().getContentAsString());
    assertThat(guardadoJson.get("conclusiones").asText()).isEqualTo("Reforzar el control de consistencia.");
    assertThat(guardadoJson.get("comercialNombre").asText()).isEqualTo("Semilla Tres");
    assertThat(guardadoJson.get("comercialCargo").asText()).isEqualTo("Por asignar");
    assertThat(guardadoJson.get("comercialFirmaVirtual").asBoolean()).isTrue();

    MvcResult informe = mockMvc.perform(get("/api/departamento3/reclamos/gestion/informe")
            .param("desde", "01/01/2020")
            .param("hasta", "31/12/2030")
            .session(depto3))
        .andReturn();
    assertThat(informe.getResponse().getStatus()).isEqualTo(200);
    assertThat(informe.getResponse().getHeader("Content-Disposition")).contains("D-SIG-09-03-");
    try (PDDocument documento = Loader.loadPDF(informe.getResponse().getContentAsByteArray())) {
      String texto = new PDFTextStripper().getText(documento);
      assertThat(texto).contains("D-SIG-09-03");
      assertThat(texto).contains("50,0");
      assertThat(texto).contains("Reforzar el control de consistencia.");
      assertThat(texto).contains("Firma virtual");
      assertThat(texto).contains("Por asignar");
      assertThat(imagenes(documento)).isZero();
    }

    MvcResult invalida = mockMvc.perform(multipart("/api/departamento3/reclamos/gestion/firma")
            .file(new MockMultipartFile("archivo", "firma.txt", "text/plain", "hola".getBytes()))
            .param("desde", "01/01/2020")
            .param("hasta", "31/12/2030")
            .param("rol", "comercial")
            .session(depto3)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue()))
        .andReturn();
    assertThat(invalida.getResponse().getStatus()).isEqualTo(400);

    BufferedImage pixel = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream pngSalida = new ByteArrayOutputStream();
    ImageIO.write(pixel, "png", pngSalida);
    MvcResult subida = mockMvc.perform(multipart("/api/departamento3/reclamos/gestion/firma")
            .file(new MockMultipartFile("archivo", "firma.png", "image/png", pngSalida.toByteArray()))
            .param("desde", "01/01/2020")
            .param("hasta", "31/12/2030")
            .param("rol", "comercial")
            .session(depto3)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue()))
        .andReturn();
    assertThat(subida.getResponse().getStatus()).isEqualTo(200);
    assertThat(json.readTree(subida.getResponse().getContentAsString()).get("comercialTieneFirma").asBoolean()).isTrue();

    MvcResult archivo = mockMvc.perform(get("/api/departamento3/reclamos/gestion/firma")
            .param("desde", "01/01/2020")
            .param("hasta", "31/12/2030")
            .param("rol", "comercial")
            .session(depto3))
        .andReturn();
    assertThat(archivo.getResponse().getStatus()).isEqualTo(200);
    assertThat(archivo.getResponse().getContentType()).isEqualTo("image/png");

    MvcResult conFirma = mockMvc.perform(get("/api/departamento3/reclamos/gestion/informe")
            .param("desde", "01/01/2020")
            .param("hasta", "31/12/2030")
            .session(depto3))
        .andReturn();
    try (PDDocument documento = Loader.loadPDF(conFirma.getResponse().getContentAsByteArray())) {
      assertThat(imagenes(documento)).isEqualTo(1);
    }

    escrito.put("comercialFirmaVirtual", false);
    escrito.put("comercialQuitarFirma", true);
    MvcResult sinFirma = mockMvc.perform(put("/api/departamento3/reclamos/gestion")
            .session(depto3)
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(escrito)))
        .andReturn();
    assertThat(sinFirma.getResponse().getStatus()).isEqualTo(200);
    assertThat(json.readTree(sinFirma.getResponse().getContentAsString()).get("comercialTieneFirma").asBoolean()).isFalse();

    MockHttpSession depto1 = ingresar("semilla1@copat.cl", "semilla1");
    assertThat(mockMvc.perform(get("/api/departamento3/reclamos/gestion")
            .param("desde", "01/01/2020")
            .param("hasta", "31/12/2030")
            .session(depto1))
        .andReturn().getResponse().getStatus()).isEqualTo(403);
  }

  private static int imagenes(PDDocument documento) throws Exception {
    int total = 0;
    for (PDPage pagina : documento.getPages()) {
      if (pagina.getResources() == null) {
        continue;
      }
      for (COSName nombre : pagina.getResources().getXObjectNames()) {
        if (pagina.getResources().getXObject(nombre) instanceof PDImageXObject) {
          total++;
        }
      }
    }
    return total;
  }

  private Map<String, String> reclamo() {
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
    return cuerpo;
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
