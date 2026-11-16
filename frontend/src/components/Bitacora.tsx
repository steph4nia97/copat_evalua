import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { api } from "../api/client";
import { compararFechas, fechaValida } from "../domain/reglas";
import { CampoFecha } from "./CampoFecha";

type Movimiento = {
  id: number;
  solicitudId: number;
  nombreEmpresa: string;
  periodoInicio: string;
  periodoFin: string;
  fecha: string;
  hora: string;
  usuario: string;
  accion: string;
};

const ORDEN_ACCION = [
  "Datos de clientes cargados",
  "Contactos de clientes cargados",
  "Envío de solicitud",
  "Recepción de solicitud",
  "Evaluación",
  "Seguimiento: Recibida",
  "Seguimiento: En análisis",
  "Seguimiento: En gestión",
  "Seguimiento: En procesamiento",
  "Seguimiento: Cerrada",
  "Seguimiento: Cerrado",
];

function tipoAccion(accion: string) {
  if (accion.startsWith("Evaluación")) return "Evaluación";
  if (accion.startsWith("Datos de clientes")) return "Datos de clientes cargados";
  if (accion.startsWith("Contactos de clientes")) return "Contactos de clientes cargados";
  return accion;
}

function dentroDelRango(fecha: string, desde: string, hasta: string) {
  if (fechaValida(desde) && compararFechas(fecha, desde) < 0) return false;
  if (fechaValida(hasta) && compararFechas(fecha, hasta) > 0) return false;
  return true;
}

export function Bitacora({ ruta, encabezado = true }: { ruta: string; encabezado?: boolean }) {
  const [desde, setDesde] = useState("");
  const [hasta, setHasta] = useState("");
  const [usuario, setUsuario] = useState("");
  const [accion, setAccion] = useState("");
  const [empresa, setEmpresa] = useState("");
  const bitacora = useQuery({
    queryKey: ["bitacora", ruta],
    queryFn: () => api<Movimiento[]>(ruta),
  });
  const movimientos = bitacora.data ?? [];
  const rangoInvalido = fechaValida(desde) && fechaValida(hasta) && compararFechas(desde, hasta) > 0;

  const enviadoPor = useMemo(() => {
    const mapa = new Map<number, string>();
    for (const movimiento of movimientos) {
      if (movimiento.solicitudId > 0 && movimiento.accion === "Envío de solicitud") {
        mapa.set(movimiento.solicitudId, movimiento.usuario);
      }
    }
    return mapa;
  }, [movimientos]);

  const usuarios = useMemo(
    () => [...new Set(enviadoPor.values())].sort((a, b) => a.localeCompare(b, "es")),
    [enviadoPor],
  );
  const acciones = useMemo(() => {
    const presentes = new Set(movimientos.map((movimiento) => tipoAccion(movimiento.accion)));
    return [...presentes].sort((a, b) => {
      const ia = ORDEN_ACCION.indexOf(a);
      const ib = ORDEN_ACCION.indexOf(b);
      return (ia === -1 ? ORDEN_ACCION.length : ia) - (ib === -1 ? ORDEN_ACCION.length : ib);
    });
  }, [movimientos]);
  const empresas = useMemo(
    () =>
      [...new Set(movimientos.map((movimiento) => movimiento.nombreEmpresa))]
        .filter((nombre) => nombre && nombre !== "—")
        .sort((a, b) => a.localeCompare(b, "es")),
    [movimientos],
  );

  const visibles = useMemo(() => {
    if (rangoInvalido) return [];
    return movimientos.filter((movimiento) => {
      if (!dentroDelRango(movimiento.fecha, desde, hasta)) return false;
      if (usuario && enviadoPor.get(movimiento.solicitudId) !== usuario) return false;
      if (accion && tipoAccion(movimiento.accion) !== accion) return false;
      if (empresa && movimiento.nombreEmpresa !== empresa) return false;
      return true;
    });
  }, [movimientos, desde, hasta, usuario, accion, empresa, enviadoPor, rangoInvalido]);

  const hayFiltro = Boolean(desde || hasta || usuario || accion || empresa);
  const campo = "mt-1 block w-full rounded-md border border-line bg-card px-3 py-2 font-normal";

  return (
    <section className={encabezado ? "mt-8" : "mt-4"}>
      {encabezado && (
        <>
          <h2 className="font-serif text-2xl">Bitácora</h2>
          <p className="text-sm text-muted">Registro de cada paso, con fecha, hora y quien lo hizo.</p>
        </>
      )}
      <div className="mt-3 grid gap-3 rounded-xl border border-line bg-card p-4 sm:grid-cols-2 lg:grid-cols-3">
        <label className="text-sm font-semibold">
          Usuario que envió la encuesta
          <select className={campo} value={usuario} onChange={(evento) => setUsuario(evento.target.value)}>
            <option value="">Todos</option>
            {usuarios.map((nombre) => (
              <option key={nombre} value={nombre}>
                {nombre}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm font-semibold">
          Acción
          <select className={campo} value={accion} onChange={(evento) => setAccion(evento.target.value)}>
            <option value="">Todas</option>
            {acciones.map((nombre) => (
              <option key={nombre} value={nombre}>
                {nombre}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm font-semibold">
          Empresa
          <select className={campo} value={empresa} onChange={(evento) => setEmpresa(evento.target.value)}>
            <option value="">Todas</option>
            {empresas.map((nombre) => (
              <option key={nombre} value={nombre}>
                {nombre}
              </option>
            ))}
          </select>
        </label>
        <div className="grid gap-3 sm:col-span-2 sm:grid-cols-[1fr_1fr_auto] sm:items-end lg:col-span-3">
          <CampoFecha
            id="bitacora-desde"
            label="Desde"
            value={desde}
            onChange={setDesde}
            onBlur={() => undefined}
            error={desde && !fechaValida(desde) ? "Use el formato dd/mm/aaaa." : undefined}
          />
          <CampoFecha
            id="bitacora-hasta"
            label="Hasta"
            value={hasta}
            onChange={setHasta}
            onBlur={() => undefined}
            error={
              rangoInvalido
                ? "La fecha final es anterior a la inicial."
                : hasta && !fechaValida(hasta)
                  ? "Use el formato dd/mm/aaaa."
                  : undefined
            }
          />
          <button
            type="button"
            className="rounded-md border border-line bg-white px-3 py-2 text-sm font-semibold disabled:opacity-50"
            disabled={!desde && !hasta}
            onClick={() => {
              setDesde("");
              setHasta("");
            }}
          >
            Todas las fechas
          </button>
        </div>
      </div>
      <div className="mt-3 overflow-x-auto rounded-xl border border-line bg-card">
        <table className="min-w-full text-left text-sm">
          <thead className="bg-paper text-muted">
            <tr>
              {["Fecha", "Hora", "Usuario", "Acción", "Empresa", "Período"].map((columna) => (
                <th key={columna} className="px-3 py-2 font-semibold">
                  {columna}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {visibles.map((movimiento) => (
              <tr key={movimiento.id} className="border-t border-line">
                <td className="whitespace-nowrap px-3 py-3">{movimiento.fecha}</td>
                <td className="whitespace-nowrap px-3 py-3">{movimiento.hora}</td>
                <td className="px-3 py-3">{movimiento.usuario}</td>
                <td className="whitespace-nowrap px-3 py-3">{movimiento.accion}</td>
                <td className="px-3 py-3">{movimiento.nombreEmpresa}</td>
                <td className="whitespace-nowrap px-3 py-3">
                  {movimiento.periodoInicio} – {movimiento.periodoFin}
                </td>
              </tr>
            ))}
            {bitacora.data && visibles.length === 0 && (
              <tr>
                <td colSpan={6} className="px-3 py-8 text-center text-muted">
                  {hayFiltro || rangoInvalido
                    ? "Ningún movimiento coincide con el filtro."
                    : "Aún no hay movimientos en la bitácora."}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
