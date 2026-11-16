import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Navigate } from "react-router-dom";
import { api, ErrorApi } from "../api/client";
import { useSesion } from "../auth/sesion";
import { Encabezado } from "../components/Encabezado";
import { FormularioSolicitud } from "../components/FormularioSolicitud";
import { CargaDatos } from "./ClientesPage";

type Solicitud = {
  id: number;
  nombreEmpresa: string;
  rut: string;
  correo: string;
  telefono: string | null;
  periodoInicio: string;
  periodoFin: string;
  plantilla: string;
  sedes: string[];
  descripcion: string | null;
  estado: string;
  creadoPor: string;
  motivoRechazo: string | null;
  rechazoPara: string | null;
  volvioDeRechazo: boolean;
  desempeno: string | null;
  promedio: string | null;
  seguimiento: string | null;
};

const colores: Record<string, string> = {
  "solicitud ingresada": "bg-emerald-100 text-emerald-900",
  "solicitud enviada a cliente": "bg-sky-100 text-sky-900",
  "solicitud en revision": "bg-amber-100 text-amber-900",
  "solicitud aprobada": "bg-teal-100 text-teal-900",
  "solicitud rechazada": "bg-rose-100 text-rose-900",
  "En procesamiento": "bg-amber-100 text-amber-900",
  "Cerrado": "bg-teal-100 text-teal-900",
};

function estadoVisible(solicitud: Solicitud) {
  return solicitud.seguimiento ?? solicitud.estado;
}

function textoContacto(solicitud: Solicitud) {
  return [
    `Empresa: ${solicitud.nombreEmpresa}`,
    `RUT: ${solicitud.rut}`,
    `Correo: ${solicitud.correo}`,
    `Teléfono: ${solicitud.telefono ?? "—"}`,
    `Sede: ${solicitud.sedes.length ? solicitud.sedes.join(", ") : "Sin sede"}`,
    `Período: ${solicitud.periodoInicio} – ${solicitud.periodoFin}`,
  ].join("\n");
}

function detalleError(err: unknown) {
  if (err instanceof ErrorApi) {
    return { mensaje: err.message, campos: Object.values(err.campos) };
  }
  return { mensaje: "No se pudo completar la acción.", campos: [] as string[] };
}

export function Departamento2Page() {
  const { usuario, listo } = useSesion();
  const queryClient = useQueryClient();
  const [estado, setEstado] = useState("");
  const [copiado, setCopiado] = useState<number | null>(null);
  const [ocupado, setOcupado] = useState<number | null>(null);
  const [enlaces, setEnlaces] = useState<Record<number, string>>({});
  const [avisos, setAvisos] = useState<Record<number, string>>({});
  const [visible, setVisible] = useState<number | null>(null);
  const [consultados, setConsultados] = useState<Record<number, string | null>>({});
  const [errores, setErrores] = useState<Record<number, { mensaje: string; campos: string[] }>>({});
  const [vista, setVista] = useState<"ingreso" | "carga">("ingreso");
  const [formulario, setFormulario] = useState(false);
  const [aviso, setAviso] = useState("");
  const opera = usuario?.departamentoId === 1 || usuario?.departamentoId === 2;

  const estados = useQuery({
    queryKey: ["d2-estados"],
    queryFn: () => api<string[]>("/api/departamento2/estados"),
    enabled: opera,
  });
  const solicitudes = useQuery({
    queryKey: ["d2-solicitudes", estado],
    queryFn: () =>
      api<Solicitud[]>(`/api/departamento2/solicitudes${estado ? `?estado=${encodeURIComponent(estado)}` : ""}`),
    enabled: opera && vista === "ingreso",
  });
  const plantillas = useQuery({
    queryKey: ["plantillas"],
    queryFn: () => api<string[]>("/api/departamento1/plantillas"),
    enabled: opera && formulario,
  });
  const sedes = useQuery({
    queryKey: ["sedes"],
    queryFn: () => api<string[]>("/api/departamento1/sedes"),
    enabled: opera && formulario,
  });

  async function reenviar(id: number) {
    setOcupado(id);
    setErrores((actual) => {
      const siguiente = { ...actual };
      delete siguiente[id];
      return siguiente;
    });
    try {
      await api(`/api/departamento2/solicitudes/${id}/reenviar`, { method: "POST" });
      await queryClient.invalidateQueries({ queryKey: ["d2-solicitudes"] });
    } catch (err) {
      setErrores((actual) => ({ ...actual, [id]: detalleError(err) }));
    } finally {
      setOcupado(null);
    }
  }

  if (!listo) {
    return <p className="p-8 text-muted">Cargando…</p>;
  }
  if (!usuario) {
    return <Navigate to="/login" replace />;
  }

  function limpiarError(id: number) {
    setErrores((actual) => {
      const siguiente = { ...actual };
      delete siguiente[id];
      return siguiente;
    });
  }

  async function copiar(solicitud: Solicitud) {
    try {
      await navigator.clipboard.writeText(textoContacto(solicitud));
      setCopiado(solicitud.id);
      window.setTimeout(() => setCopiado((actual) => (actual === solicitud.id ? null : actual)), 2000);
    } catch {
      setErrores((actual) => ({
        ...actual,
        [solicitud.id]: { mensaje: "No se pudo copiar. Use los datos visibles de la fila.", campos: [] },
      }));
    }
  }

  async function verEnlace(id: number) {
    if (visible === id) {
      setVisible(null);
      return;
    }
    setVisible(id);
    if (enlaces[id] || id in consultados) {
      return;
    }
    setOcupado(id);
    limpiarError(id);
    try {
      const respuesta = await api<{ token: string | null; aviso: string }>(`/api/departamento2/solicitudes/${id}/enlace`);
      setConsultados((actual) => ({
        ...actual,
        [id]: respuesta.token ? `${window.location.origin}/encuesta/${respuesta.token}` : null,
      }));
      if (!respuesta.token && respuesta.aviso) {
        setAvisos((actual) => ({ ...actual, [id]: respuesta.aviso }));
      }
    } catch (err) {
      setErrores((actual) => ({ ...actual, [id]: detalleError(err) }));
      setVisible(null);
    } finally {
      setOcupado(null);
    }
  }

  async function crearEnlace(id: number) {
    setOcupado(id);
    limpiarError(id);
    try {
      const respuesta = await api<{ token: string; correoEnviado: boolean; destino: string; aviso: string }>(
        `/api/departamento2/solicitudes/${id}/enlace`,
        { method: "POST" },
      );
      const url = `${window.location.origin}/encuesta/${respuesta.token}`;
      setEnlaces((actual) => ({ ...actual, [id]: url }));
      setAvisos((actual) => ({ ...actual, [id]: respuesta.aviso }));
      try {
        await navigator.clipboard.writeText(url);
        setCopiado(id);
      } catch {
        setCopiado(null);
      }
      await queryClient.invalidateQueries({ queryKey: ["d2-solicitudes"] });
    } catch (err) {
      setErrores((actual) => ({ ...actual, [id]: detalleError(err) }));
    } finally {
      setOcupado(null);
    }
  }

  async function creada() {
    setFormulario(false);
    setAviso("La solicitud quedó registrada con estado solicitud ingresada.");
    await queryClient.invalidateQueries({ queryKey: ["d2-solicitudes"] });
  }

  const botonVista = (activa: boolean) =>
    `rounded-md px-4 py-2 font-semibold ${activa ? "bg-pine text-white" : "border border-line bg-card"}`;

  return (
    <div className="min-h-screen">
      <Encabezado />
      <main className="mx-auto max-w-6xl px-4 py-6">
        <h1 className="font-serif text-3xl">Solicitudes</h1>
        <p className="mt-1 max-w-3xl text-sm text-muted">
          Desde aquí se ingresa la solicitud, se cargan los clientes y se envía la encuesta.
        </p>
        <div className="mt-5 flex flex-wrap gap-3">
          <button type="button" className={botonVista(vista === "ingreso")} onClick={() => setVista("ingreso")}>
            Ingreso de solicitud
          </button>
          <button type="button" className={botonVista(vista === "carga")} onClick={() => setVista("carga")}>
            Carga de datos
          </button>
        </div>
        {vista === "carga" ? (
          <CargaDatos />
        ) : (
          <>
        <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
          <p className="max-w-3xl text-sm text-muted">
            El correo de la encuesta sale al contacto cuyo RUT coincide con el de la solicitud.
          </p>
          <button
            type="button"
            className="rounded-md bg-copper px-4 py-2 font-semibold text-white hover:bg-copper-dark"
            onClick={() => {
              setAviso("");
              setFormulario(true);
            }}
          >
            Nueva solicitud
          </button>
        </div>
        {aviso && <p className="mt-4 rounded-md bg-pine-soft px-3 py-2 text-sm text-pine">{aviso}</p>}
        {(solicitudes.data ?? []).some((item) => item.estado === "solicitud rechazada" && item.rechazoPara === usuario?.email) && (
          <p className="mt-4 rounded-md bg-rose-100 px-3 py-2 text-sm text-rose-950">
            Departamento 3 rechazó una solicitud que usted envió. Revise el motivo y comparta de nuevo el enlace: el cliente puede corregir la encuesta.
          </p>
        )}

        <div className="mt-5 flex items-center gap-3">
          <label htmlFor="estado" className="text-sm font-semibold">
            Estado
          </label>
          <select
            id="estado"
            value={estado}
            onChange={(evento) => setEstado(evento.target.value)}
            className="rounded-md border border-line bg-card px-3 py-2"
          >
            <option value="">Todas</option>
            {(estados.data ?? []).map((opcion) => (
              <option key={opcion} value={opcion}>
                {opcion}
              </option>
            ))}
          </select>
        </div>

        <div className="mt-4 overflow-x-auto rounded-xl border border-line bg-card">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-paper text-muted">
              <tr>
                {["Empresa", "Contacto", "Período", "Sede", "Guía", "Evaluación", "Estado", "Acciones"].map((columna) => (
                  <th key={columna} className="px-3 py-2 font-semibold">
                    {columna}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {(solicitudes.data ?? []).map((solicitud) => {
                const error = errores[solicitud.id];
                return (
                  <tr key={solicitud.id} className="border-t border-line align-top">
                    <td className="px-3 py-3">
                      <p className="font-semibold">{solicitud.nombreEmpresa}</p>
                      <p className="mt-1 text-muted">{solicitud.rut}</p>
                      {solicitud.descripcion && <p className="mt-1 max-w-xs text-muted">{solicitud.descripcion}</p>}
                      {solicitud.estado === "solicitud rechazada" && solicitud.motivoRechazo && (
                        <p className="mt-2 max-w-xs rounded-md bg-rose-50 px-2 py-1 text-rose-950">
                          Motivo del rechazo: {solicitud.motivoRechazo}
                        </p>
                      )}
                      {solicitud.volvioDeRechazo && (
                        <p className="mt-2 text-xs font-semibold text-copper-dark">Volvió a revisión tras un rechazo</p>
                      )}
                    </td>
                    <td className="px-3 py-3">
                      <p>{solicitud.correo}</p>
                      <p className="mt-1">{solicitud.telefono ?? "—"}</p>
                    </td>
                    <td className="whitespace-nowrap px-3 py-3">
                      {solicitud.periodoInicio} – {solicitud.periodoFin}
                    </td>
                    <td className="px-3 py-3">{solicitud.sedes.length ? solicitud.sedes.join(", ") : "Sin sede"}</td>
                    <td className="px-3 py-3">{solicitud.plantilla}</td>
                    <td className="px-3 py-3">
                      {solicitud.desempeno ? `${solicitud.desempeno} · ${solicitud.promedio}` : "—"}
                    </td>
                    <td className="whitespace-nowrap px-3 py-3">
                      <span className={`rounded-full px-2 py-1 text-xs font-semibold ${colores[estadoVisible(solicitud)] ?? ""}`}>
                        {estadoVisible(solicitud)}
                      </span>
                    </td>
                    <td className="px-3 py-3">
                      <div className="flex min-w-56 flex-col items-start gap-2">
                        <button
                          type="button"
                          className="rounded-md border border-line px-3 py-1"
                          onClick={() => void copiar(solicitud)}
                        >
                          {copiado === solicitud.id && !enlaces[solicitud.id] ? "Copiado" : "Copiar contacto"}
                        </button>
                        <button
                          type="button"
                          disabled={ocupado === solicitud.id}
                          className="rounded-md border border-line px-3 py-1 disabled:opacity-60"
                          aria-expanded={visible === solicitud.id}
                          onClick={() => void verEnlace(solicitud.id)}
                        >
                          {visible === solicitud.id ? "Ocultar enlace" : "Ver enlace"}
                        </button>
                        {(solicitud.estado === "solicitud ingresada" ||
                          solicitud.estado === "solicitud enviada a cliente" ||
                          solicitud.estado === "solicitud rechazada") && (
                          <button
                            type="button"
                            disabled={ocupado === solicitud.id}
                            className="rounded-md bg-pine px-3 py-1 font-semibold text-white disabled:opacity-60"
                            onClick={() => void crearEnlace(solicitud.id)}
                          >
                            {solicitud.estado === "solicitud ingresada" ? "Enviar encuesta por correo" : "Reenviar correo de la encuesta"}
                          </button>
                        )}
                        {(enlaces[solicitud.id] || (visible === solicitud.id && consultados[solicitud.id])) && (
                          <input
                            readOnly
                            value={enlaces[solicitud.id] ?? consultados[solicitud.id] ?? ""}
                            aria-label="Enlace de la encuesta"
                            className="w-full rounded-md border border-line bg-white px-2 py-1 text-xs"
                            onFocus={(evento) => evento.currentTarget.select()}
                          />
                        )}
                        {avisos[solicitud.id] && (
                          <p className="text-xs text-pine">{avisos[solicitud.id]}</p>
                        )}
                        {copiado === solicitud.id && enlaces[solicitud.id] && (
                          <p className="text-xs font-semibold text-pine">Enlace copiado</p>
                        )}
                        {solicitud.estado === "solicitud rechazada" && (
                          <button
                            type="button"
                            disabled={ocupado === solicitud.id}
                            className="rounded-md bg-copper px-3 py-1 font-semibold text-white disabled:opacity-60"
                            onClick={() => void reenviar(solicitud.id)}
                          >
                            Volver a enviar a revisión
                          </button>
                        )}
                        {error && (
                          <div role="alert" className="text-xs text-copper-dark">
                            <p>{error.mensaje}</p>
                            {error.campos.length > 0 && (
                              <ul className="mt-1 list-disc pl-4">
                                {error.campos.map((campo) => (
                                  <li key={campo}>{campo}</li>
                                ))}
                              </ul>
                            )}
                          </div>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
              {solicitudes.data?.length === 0 && (
                <tr>
                  <td colSpan={8} className="px-3 py-8 text-center text-muted">
                    No hay solicitudes para este filtro.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
          </>
        )}
      </main>
      {formulario && (
        <FormularioSolicitud
          plantillas={plantillas.data ?? []}
          sedes={sedes.data ?? []}
          onCerrar={() => setFormulario(false)}
          onCreada={() => void creada()}
        />
      )}
    </div>
  );
}
