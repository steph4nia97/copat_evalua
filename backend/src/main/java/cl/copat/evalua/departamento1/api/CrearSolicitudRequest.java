package cl.copat.evalua.departamento1.api;

import cl.copat.evalua.departamento1.api.validacion.CorreoValido;
import cl.copat.evalua.departamento1.api.validacion.FechaValida;
import cl.copat.evalua.departamento1.api.validacion.NombreEmpresaValido;
import cl.copat.evalua.departamento1.api.validacion.PeriodoCoherente;
import cl.copat.evalua.departamento1.api.validacion.PlantillaValida;
import cl.copat.evalua.departamento1.api.validacion.RutValido;
import cl.copat.evalua.departamento1.api.validacion.TelefonoValido;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import jakarta.validation.constraints.Size;

@PeriodoCoherente
public record CrearSolicitudRequest(
    @NombreEmpresaValido String nombreEmpresa,
    @RutValido String rut,
    @CorreoValido String correo,
    @TelefonoValido String telefono,
    @FechaValida(message = Mensajes.FECHA_INICIO) String periodoInicio,
    @FechaValida(message = Mensajes.FECHA_FIN) String periodoFin,
    @PlantillaValida String plantilla,
    @Size(max = 300, message = Mensajes.DESCRIPCION) String descripcion) {}
