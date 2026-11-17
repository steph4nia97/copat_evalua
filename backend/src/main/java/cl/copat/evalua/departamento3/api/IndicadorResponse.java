package cl.copat.evalua.departamento3.api;

import cl.copat.evalua.departamento3.api.RevisionResponse.NotaResponse;
import java.math.BigDecimal;
import java.util.List;

public record IndicadorResponse(
    long id,
    String empresa,
    List<String> sedes,
    String obra,
    String estadoObra,
    String estado,
    String desempeno,
    BigDecimal promedio,
    List<NotaResponse> notas,
    String fecha,
    String correo,
    String telefono,
    int vecesRechazada) {}
