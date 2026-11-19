import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { api, ErrorApi } from "../api/client";
import {
  componerTelefono,
  enmascararRut,
  enmascararTelefono,
  esNoAplica,
  NO_APLICA,
  normalizarNombre,
  type TipoTelefono,
} from "../domain/reglas";
import { formularioVacio, solicitudSchema, type SolicitudFormulario } from "../domain/solicitud";
import { CampoFecha } from "./CampoFecha";

type Cruce = {
  rut: string;
  empresa: string;
  nombreContacto: string | null;
  correo: string | null;
  telefono: string | null;
};

function claveCliente(fila: { rut: string; correo: string | null; nombreContacto: string | null }) {
  return `${fila.rut}|${fila.correo ?? ""}|${fila.nombreContacto ?? ""}`;
}

function telefonoDesdeExcel(valor: string | null): { tipo: TipoTelefono; local: string } {
  const digitos = (valor ?? "").replace(/\D/g, "");
  if (digitos.startsWith("569") && digitos.length >= 11) {
    return { tipo: "celular", local: digitos.slice(3, 11) };
  }
  if (digitos.length === 9 && digitos.startsWith("9")) {
    return { tipo: "celular", local: digitos.slice(1) };
  }
  if (digitos.length === 8) {
    return { tipo: "celular", local: digitos };
  }
  if (digitos.startsWith("56") && /^[2-8]\d{8}$/.test(digitos.slice(2, 11))) {
    return { tipo: "fijo", local: digitos.slice(2, 11) };
  }
  if (/^[2-8]\d{8}$/.test(digitos)) {
    return { tipo: "fijo", local: digitos };
  }
  return { tipo: "celular", local: "" };
}

type Props = {
  plantillas: string[];
  sedes: string[];
  solicitudId?: number;
  valores?: SolicitudFormulario;
  onCerrar: () => void;
  onGuardada: () => void;
};

const campo = "mt-1 w-full rounded-md border border-line bg-white px-3 py-2 outline-none ring-copper focus:ring-2";

export function FormularioSolicitud({ plantillas, sedes, solicitudId, valores, onCerrar, onGuardada }: Props) {
  const [mensaje, setMensaje] = useState("");
  const [modo, setModo] = useState<"cruce" | "manual">("cruce");
  const [cliente, setCliente] = useState(valores ? "actual" : "");
  const [telefonoExcel, setTelefonoExcel] = useState("");
  const cruce = useQuery({
    queryKey: ["cruce-clientes"],
    queryFn: () => api<Cruce[]>("/api/departamento1/cruce"),
  });
  const opciones = (cruce.data ?? []).filter((fila) => fila.nombreContacto && fila.correo);
  const {
    register,
    handleSubmit,
    setError,
    setValue,
    watch,
    formState: { errors },
  } = useForm<SolicitudFormulario>({
    resolver: zodResolver(solicitudSchema),
    defaultValues: valores ?? formularioVacio,
  });
  const sedesElegidas = watch("sedes");
  const empresa = watch("nombreEmpresa");
  const rut = watch("rut");
  const correo = watch("correo");
  const telefono = watch("telefono");
  const tipo = watch("tipoTelefono");

  function aplicarCliente(fila: Cruce) {
    const correo = (fila.correo ?? "").trim();
    const sinTelefono = !(fila.telefono ?? "").trim() || esNoAplica(fila.telefono);
    const tel = telefonoDesdeExcel(sinTelefono ? null : fila.telefono);
    setTelefonoExcel(sinTelefono || tel.local ? "" : (fila.telefono ?? "").trim());
    setValue("nombreEmpresa", normalizarNombre(fila.empresa), { shouldValidate: true });
    setValue("rut", fila.rut, { shouldValidate: true });
    setValue("correo", esNoAplica(correo) || correo === "" ? NO_APLICA : correo, { shouldValidate: true });
    setValue("tipoTelefono", tel.tipo, { shouldValidate: true });
    setValue("telefono", sinTelefono ? NO_APLICA : tel.local, { shouldValidate: true });
  }

  useEffect(() => {
    if (!valores) {
      return;
    }
    const encontrada = opciones.find((fila) => fila.rut === valores.rut && fila.correo === valores.correo);
    if (encontrada) {
      setCliente(claveCliente(encontrada));
    }
  }, [opciones, valores]);
  const descripcion = watch("descripcion");
  const inicio = watch("periodoInicio");
  const fin = watch("periodoFin");

  const crear = useMutation({
    mutationFn: (datos: SolicitudFormulario) =>
      api(solicitudId == null ? "/api/departamento1/solicitudes" : `/api/departamento1/solicitudes/${solicitudId}`, {
        method: solicitudId == null ? "POST" : "PUT",
        body: JSON.stringify({
          nombreEmpresa: normalizarNombre(datos.nombreEmpresa),
          rut: datos.rut.trim(),
          correo: datos.correo.trim(),
          telefono: componerTelefono(datos.telefono, datos.tipoTelefono),
          periodoInicio: datos.periodoInicio.trim(),
          periodoFin: datos.periodoFin.trim(),
          plantilla: datos.plantilla,
          sedes: datos.sedes,
          descripcion: datos.descripcion.trim(),
        }),
      }),
    onSuccess: () => onGuardada(),
    onError: (error) => {
      if (!(error instanceof ErrorApi)) {
        setMensaje("No se pudo guardar la solicitud.");
        return;
      }
      Object.entries(error.campos).forEach(([campo, texto]) => {
        setError(campo as keyof SolicitudFormulario, { message: texto });
      });
      setMensaje(error.message);
    },
  });

  return (
    <div className="fixed inset-x-0 bottom-0 top-20 z-20 overflow-y-auto bg-paper p-4">
      <form
        noValidate
        onSubmit={handleSubmit((valores) => {
          setMensaje("");
          crear.mutate(valores);
        })}
        className="mx-auto my-6 w-full max-w-2xl rounded-md border border-line bg-card p-5 sm:p-6"
      >
        <div className="mb-4 flex items-start justify-between gap-4">
          <div>
            <h2 className="font-serif text-2xl">{solicitudId == null ? "Ingresar solicitud" : "Editar solicitud"}</h2>
            <p className="text-sm text-muted">
              {solicitudId == null
                ? "Puede tomar la empresa y el contacto del cruce, o escribirlos a mano. El período, la sede y la plantilla se eligen aquí."
                : "Solo se puede corregir mientras sigue ingresada. Los datos del cliente se pueden cambiar a mano."}
            </p>
          </div>
          <button type="button" className="text-sm text-muted" onClick={onCerrar}>
            Cerrar
          </button>
        </div>

        {mensaje && (
          <p className="mb-4 rounded-md border border-copper/40 bg-copper/10 px-3 py-2 text-sm text-copper-dark" role="alert">
            {mensaje}
          </p>
        )}

        <div className="mb-3 flex flex-wrap gap-2">
          <button
            type="button"
            aria-pressed={modo === "cruce"}
            className={`rounded-md px-3 py-2 text-sm font-semibold ${modo === "cruce" ? "bg-pine text-white" : "border border-line bg-card"}`}
            onClick={() => setModo("cruce")}
          >
            Desde el cruce
          </button>
          <button
            type="button"
            aria-pressed={modo === "manual"}
            className={`rounded-md px-3 py-2 text-sm font-semibold ${modo === "manual" ? "bg-pine text-white" : "border border-line bg-card"}`}
            onClick={() => {
              setModo("manual");
              setCliente("");
              setTelefonoExcel("");
            }}
          >
            Ingreso manual
          </button>
        </div>

        {modo === "manual" ? (
          <div className="grid gap-4 sm:grid-cols-2">
            <label htmlFor="nombreEmpresa" className="text-sm font-semibold sm:col-span-2">
              Empresa
              <input id="nombreEmpresa" className={campo} maxLength={100} {...register("nombreEmpresa")} />
              {errors.nombreEmpresa && <span className="mt-1 block font-normal text-copper-dark">{errors.nombreEmpresa.message}</span>}
            </label>
            <label htmlFor="rut" className="text-sm font-semibold">
              RUT
              <input
                id="rut"
                className={campo}
                value={rut}
                onChange={(evento) => setValue("rut", enmascararRut(evento.target.value), { shouldValidate: Boolean(errors.rut) })}
              />
              {errors.rut && <span className="mt-1 block font-normal text-copper-dark">{errors.rut.message}</span>}
            </label>
            <label htmlFor="correo" className="text-sm font-semibold">
              Correo
              <input id="correo" className={campo} {...register("correo")} placeholder="correo@empresa.cl o No aplica" />
              {errors.correo && <span className="mt-1 block font-normal text-copper-dark">{errors.correo.message}</span>}
            </label>
            <fieldset className="sm:col-span-2">
              <legend className="text-sm font-semibold">Teléfono</legend>
              <div className="mt-2 flex flex-wrap gap-4 text-sm">
                <label className="flex items-center gap-2">
                  <input type="radio" value="celular" {...register("tipoTelefono")} />
                  Celular
                </label>
                <label className="flex items-center gap-2">
                  <input type="radio" value="fijo" {...register("tipoTelefono")} />
                  Fijo
                </label>
              </div>
              <input
                id="telefono"
                className={campo}
                value={telefono}
                placeholder={tipo === "fijo" ? "9 dígitos, o No aplica" : "8 dígitos sin el 9, o No aplica"}
                onChange={(evento) => {
                  const texto = evento.target.value;
                  const siguiente = /[a-zA-ZáéíóúñÁÉÍÓÚÑ]/.test(texto) ? texto : enmascararTelefono(texto, tipo);
                  setValue("telefono", siguiente, { shouldValidate: Boolean(errors.telefono) });
                }}
              />
              {errors.telefono && <span className="mt-1 block text-sm text-copper-dark">{errors.telefono.message}</span>}
            </fieldset>
          </div>
        ) : (
          <>
        <label htmlFor="cliente" className="mb-1 block text-sm font-semibold">
          Cliente del Excel
        </label>
        <select
          id="cliente"
          value={cliente}
          onChange={(evento) => {
            const elegido = evento.target.value;
            setCliente(elegido);
            if (elegido === "actual" && valores) {
              setTelefonoExcel("");
              setValue("nombreEmpresa", valores.nombreEmpresa, { shouldValidate: true });
              setValue("rut", valores.rut, { shouldValidate: true });
              setValue("correo", valores.correo, { shouldValidate: true });
              setValue("tipoTelefono", valores.tipoTelefono, { shouldValidate: true });
              setValue("telefono", valores.telefono, { shouldValidate: true });
              return;
            }
            const fila = opciones.find((item) => claveCliente(item) === elegido);
            if (fila) {
              aplicarCliente(fila);
            }
          }}
          className="w-full rounded-md border border-line bg-card px-3 py-2"
        >
          <option value="">{opciones.length === 0 ? "No hay clientes con contacto" : "Elija un cliente"}</option>
          {valores && <option value="actual">Datos ya guardados · {valores.nombreEmpresa}</option>}
          {opciones.map((fila) => (
            <option key={claveCliente(fila)} value={claveCliente(fila)}>
              {fila.empresa} · {fila.nombreContacto} · {fila.correo}
            </option>
          ))}
        </select>
        {cruce.isSuccess && opciones.length === 0 && (
          <p className="mt-1 text-sm text-muted">No hay contactos en el cruce. Puede usar el ingreso manual.</p>
        )}
        {(errors.nombreEmpresa || errors.rut || errors.correo) && (
          <p className="mt-1 text-sm text-copper-dark">
            {errors.nombreEmpresa?.message ?? errors.rut?.message ?? errors.correo?.message}
          </p>
        )}
        {cliente !== "" && (
          <dl className="mt-3 grid gap-2 rounded-md border border-line bg-paper p-3 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-muted">Empresa</dt>
              <dd>{empresa || "—"}</dd>
            </div>
            <div>
              <dt className="text-muted">RUT</dt>
              <dd>{rut || "—"}</dd>
            </div>
            <div>
              <dt className="text-muted">Correo</dt>
              <dd>{correo || "—"}</dd>
            </div>
            <div>
              <dt className="text-muted">Teléfono</dt>
              <dd>
                {esNoAplica(telefono)
                  ? NO_APLICA
                  : telefono
                    ? `${tipo === "fijo" ? "+56" : "+569"} ${telefono}`
                    : telefonoExcel
                      ? `No se usará (${telefonoExcel})`
                      : "Sin teléfono"}
              </dd>
            </div>
          </dl>
        )}
          </>
        )}

        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <CampoFecha
            id="periodoInicio"
            label="Período de inicio"
            value={inicio}
            error={errors.periodoInicio?.message}
            onBlur={() => undefined}
            onChange={(valor) => setValue("periodoInicio", valor, { shouldValidate: Boolean(errors.periodoInicio) })}
          />
          <CampoFecha
            id="periodoFin"
            label="Período de término"
            value={fin}
            error={errors.periodoFin?.message}
            onBlur={() => undefined}
            onChange={(valor) => setValue("periodoFin", valor, { shouldValidate: Boolean(errors.periodoFin) })}
          />
        </div>

        <fieldset className="mt-4">
          <legend className="mb-2 text-sm font-semibold">Sede que despachó</legend>
          <p className="mb-2 text-xs text-muted">Marque una o más. Suele ser una sola sede.</p>
          <div className="grid gap-2 sm:grid-cols-2">
            {sedes.map((sede) => (
              <label key={sede} className="flex items-center gap-3 rounded-md border border-line bg-white px-3 py-2">
                <input
                  type="checkbox"
                  checked={sedesElegidas.includes(sede)}
                  onChange={(evento) => {
                    const siguientes = evento.target.checked
                      ? [...sedesElegidas, sede]
                      : sedesElegidas.filter((actual) => actual !== sede);
                    setValue("sedes", siguientes, { shouldValidate: Boolean(errors.sedes) });
                  }}
                />
                <span>{sede}</span>
              </label>
            ))}
          </div>
          {errors.sedes && <p className="mt-1 text-sm text-copper-dark">{errors.sedes.message}</p>}
        </fieldset>

        <fieldset className="mt-4">
          <legend className="mb-2 text-sm font-semibold">Plantilla</legend>
          <div className="grid gap-2">
            {plantillas.map((plantilla) => (
              <label key={plantilla} className="flex items-center gap-3 rounded-md border border-line bg-white px-3 py-2">
                <input type="radio" value={plantilla} {...register("plantilla")} />
                <span>{plantilla}</span>
              </label>
            ))}
          </div>
          {errors.plantilla && <p className="mt-1 text-sm text-copper-dark">{errors.plantilla.message}</p>}
        </fieldset>

        <label htmlFor="descripcion" className="mt-4 block text-sm font-semibold">
          Descripción
        </label>
        <textarea
          id="descripcion"
          maxLength={300}
          rows={3}
          placeholder="Opcional"
          className="mt-1 w-full rounded-md border border-line bg-white px-3 py-2 outline-none ring-copper focus:ring-2"
          {...register("descripcion")}
        />
        <div className="mt-1 flex justify-between text-sm">
          <span className="text-copper-dark">{errors.descripcion?.message}</span>
          <span className="text-muted">{descripcion.trim().length}/300</span>
        </div>

        <div className="mt-5 flex justify-end gap-3">
          <button type="button" className="rounded-md px-4 py-2 text-sm" onClick={onCerrar}>
            Cancelar
          </button>
          <button
            type="submit"
            disabled={crear.isPending || (solicitudId == null && modo === "cruce" && cliente === "")}
            className="rounded-md bg-copper px-4 py-2 text-sm font-semibold text-white hover:bg-copper-dark disabled:opacity-60"
          >
            {solicitudId == null ? "Guardar solicitud" : "Guardar cambios"}
          </button>
        </div>
      </form>
    </div>
  );
}
