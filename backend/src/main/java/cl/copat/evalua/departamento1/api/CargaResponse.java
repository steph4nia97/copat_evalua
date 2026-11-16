package cl.copat.evalua.departamento1.api;

import java.util.List;

public record CargaResponse(int guardados, int omitidos, List<String> avisos) {}
