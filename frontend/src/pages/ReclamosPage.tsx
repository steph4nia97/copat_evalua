import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useRef, useState } from "react";
import { api, descargar, ErrorApi } from "../api/client";
import { useSesion } from "../auth/sesion";
import { CampoFecha } from "../components/CampoFecha";
import { Encabezado } from "../components/Encabezado";
import { compararFechas, fechaValida } from "../domain/reglas";
import { InformeGestion } from "./InformeGestion";

type Reclamo = {
  id: number;
  fecha: string;
  hora: string;
  nombre: string;
  telefono: string;
  empresa: string;
  correo: string;
  obra: string;
  fechaServicio: string;
  ubicacion: string;
  guiaDespacho: string | null;
  tipo: string;
  descripcion: string;
  preferenciaContacto: string;
  horario: string | null;
  comentarios: string | null;
  estado: string;
  area: string | null;
  responsable: string | null;
  fechaGestion: string | null;
  observaciones: string | null;
  horaCierre: string | null;
  fechaCierre: string | null;
  solucion: string | null;
  sede: string | null;
};

const ESTADOS = ["Recibido", "En gestión", "Cerrado"] as const;
const TIPOS = [
  "Calidad del hormigón",
  "Retraso en la entrega",
  "Atención del personal",
  "Facturación / documentación",
  "Otro",
];

const SIN_SEDE = "Sin sede";

function estadoAutomatico(observaciones: string, solucion: string) {
  if (solucion.trim()) return "Cerrado";
  if (observaciones.trim()) return "En gestión";
  return "Recibido";
}
const campo = "mt-1 w-full rounded-md border border-line bg-white px-3 py-2 text-sm";

const AREAS_RESPONSABLES = ["Ventas", "Calidad", "Operaciones", "Administración", "Finanzas"];

function areasDesdeTexto(valor: string | null): string[] {
  const partes = (valor ?? "")
    .split(",")
    .map((parte) => parte.trim())
    .filter(Boolean);
  return AREAS_RESPONSABLES.filter((opcion) =>
    partes.some((parte) => parte.localeCompare(opcion, "es", { sensitivity: "base" }) === 0),
  );
}

function MenuAreas({
  seleccion,
  texto,
  onChange,
  error,
}: {
  seleccion: string[];
  texto: string;
  onChange: (seleccion: string[]) => void;
  error?: string;
}) {
  const [abierto, setAbierto] = useState(false);
  const caja = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!abierto) return;
    function fuera(evento: MouseEvent) {
      if (caja.current && !caja.current.contains(evento.target as Node)) setAbierto(false);
    }
    function tecla(evento: KeyboardEvent) {
      if (evento.key === "Escape") setAbierto(false);
    }
    document.addEventListener("mousedown", fuera);
    document.addEventListener("keydown", tecla);
    return () => {
      document.removeEventListener("mousedown", fuera);
      document.removeEventListener("keydown", tecla);
    };
  }, [abierto]);

  function alternar(opcion: string) {
    const siguiente = seleccion.includes(opcion)
      ? seleccion.filter((item) => item !== opcion)
      : AREAS_RESPONSABLES.filter((item) => item === opcion || seleccion.includes(item));
    onChange(siguiente);
  }

  return (
    <div ref={caja} className="relative">
      <button
        type="button"
        aria-expanded={abierto}
        aria-haspopup="listbox"
        className={`${campo} flex items-center justify-between gap-2 text-left font-normal`}
        onClick={() => setAbierto((valor) => !valor)}
      >
        <span className={texto ? "" : "text-muted"}>{texto || "Seleccione el área"}</span>
        <span aria-hidden="true">▾</span>
      </button>
      {abierto && (
        <ul className="absolute z-20 mt-1 w-full rounded-md border border-line bg-card p-1 shadow-card">
          {AREAS_RESPONSABLES.map((opcion) => (
            <li key={opcion}>
              <label className="flex cursor-pointer items-center gap-2 rounded px-2 py-1.5 text-sm font-normal hover:bg-paper">
                <input type="checkbox" checked={seleccion.includes(opcion)} onChange={() => alternar(opcion)} />
                {opcion}
              </label>
            </li>
          ))}
        </ul>
      )}
      {error && <Aviso texto={error} />}
    </div>
  );
}

function instante(reclamo: Reclamo) {
  const [dia, mes, anio] = reclamo.fecha.split("/").map(Number);
  const [hora, minuto] = reclamo.hora.split(":").map(Number);
  return new Date(anio, (mes || 1) - 1, dia || 1, hora || 0, minuto || 0).getTime();
}

function dentroDelRango(fecha: string, desde: string, hasta: string) {
  if (fechaValida(desde) && compararFechas(fecha, desde) < 0) return false;
  if (fechaValida(hasta) && compararFechas(fecha, hasta) > 0) return false;
  return true;
}

export function ReclamosPage() {
  const [copiado, setCopiado] = useState(false);
  const [estado, setEstado] = useState("");
  const [tipo, setTipo] = useState("");
  const [sede, setSede] = useState("");
  const [desde, setDesde] = useState("");
  const [hasta, setHasta] = useState("");
  const [recientes, setRecientes] = useState(true);
  const [abierto, setAbierto] = useState<number | null>(null);
  const [vista, setVista] = useState<"seguimiento" | "gestion">("seguimiento");
  const publico = useQuery({
    queryKey: ["reclamo-enlace"],
    queryFn: () => api<{ enlace: string }>("/api/departamento3/reclamos/enlace"),
  });
  const enlace = publico.data?.enlace ?? "";
  const reclamos = useQuery({
    queryKey: ["reclamos"],
    queryFn: () => api<Reclamo[]>("/api/departamento3/reclamos"),
  });
  const sedes = useQuery({
    queryKey: ["sedes"],
    queryFn: () => api<string[]>("/api/departamento1/sedes"),
  });
  const datos = reclamos.data ?? [];
  const rangoInvalido = fechaValida(desde) && fechaValida(hasta) && compararFechas(desde, hasta) > 0;
  const visibles = useMemo(() => {
    if (rangoInvalido) return [];
    const lista = datos.filter((reclamo) => {
      if (estado && reclamo.estado !== estado) return false;
      if (tipo && reclamo.tipo !== tipo) return false;
      if (sede === SIN_SEDE && reclamo.sede) return false;
      if (sede && sede !== SIN_SEDE && reclamo.sede !== sede) return false;
      return dentroDelRango(reclamo.fecha, desde, hasta);
    });
    return [...lista].sort((a, b) => (recientes ? instante(b) - instante(a) : instante(a) - instante(b)));
  }, [datos, estado, tipo, sede, desde, hasta, rangoInvalido, recientes]);
  const seleccionado = datos.find((reclamo) => reclamo.id === abierto) ?? null;
  const hayFiltro = Boolean(estado || tipo || sede || desde || hasta);

  async function copiar() {
    try {
      await navigator.clipboard.writeText(enlace);
      setCopiado(true);
    } catch {
      setCopiado(false);
    }
  }

  function limpiar() {
    setEstado("");
    setTipo("");
    setSede("");
    setDesde("");
    setHasta("");
  }

  return (
    <div className="min-h-screen">
      <Encabezado />
      <main className="mx-auto max-w-6xl px-4 py-6">
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            aria-pressed={vista === "seguimiento"}
            className={vista === "seguimiento" ? "rounded-md bg-pine px-3 py-2 text-sm font-semibold text-white" : "rounded-md border border-line bg-card px-3 py-2 text-sm font-semibold"}
            onClick={() => setVista("seguimiento")}
          >
            Seguimiento
          </button>
          <button
            type="button"
            aria-pressed={vista === "gestion"}
            className={vista === "gestion" ? "rounded-md bg-pine px-3 py-2 text-sm font-semibold text-white" : "rounded-md border border-line bg-card px-3 py-2 text-sm font-semibold"}
            onClick={() => setVista("gestion")}
          >
            Informe de gestión
          </button>
        </div>
        {vista === "seguimiento" && (
          <>
        <h1 className="mt-4 font-serif text-3xl">Seguimiento de reclamos</h1>
        <p className="mt-1 max-w-3xl text-sm text-muted">
          Planilla D-SIG-09-02. Los datos los ingresa el cliente. El estado cambia solo: Recibido al llegar, En gestión al escribir observaciones, y Cerrado al escribir la solución.
        </p>
        <div className="mt-4">
          <p className="text-sm font-semibold">Enlace para la página oficial</p>
          <p className="mt-1 max-w-3xl text-sm text-muted">Cópielo en el sitio de Copat. El cliente entra por ahí, sin la intranet.</p>
          <div className="mt-2 flex flex-wrap items-center gap-2">
            <input readOnly value={enlace} aria-label="Enlace de reclamos" className="w-full max-w-md rounded-md border border-line bg-card px-3 py-2 text-sm" onFocus={(evento) => evento.currentTarget.select()} />
            <button type="button" disabled={!enlace} onClick={() => void copiar()} className="rounded-md bg-pine px-3 py-2 text-sm font-semibold text-white disabled:opacity-60">
              {copiado ? "Enlace copiado" : "Copiar enlace"}
            </button>
          </div>
        </div>

        <section className="relative z-20 mt-5 rounded-xl border border-line bg-card p-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="font-semibold">Filtros</h2>
            {hayFiltro && (
              <button type="button" className="text-sm font-semibold text-pine" onClick={limpiar}>
                Quitar filtros
              </button>
            )}
          </div>
          <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <label className="text-sm font-semibold">
              Tipo de reclamo
              <select className={campo} value={tipo} onChange={(evento) => setTipo(evento.target.value)}>
                <option value="">Todos</option>
                {TIPOS.map((nombre) => (
                  <option key={nombre} value={nombre}>{nombre}</option>
                ))}
              </select>
            </label>
            <label className="text-sm font-semibold">
              Sede responsable
              <select className={campo} value={sede} onChange={(evento) => setSede(evento.target.value)}>
                <option value="">Todas</option>
                <option value={SIN_SEDE}>{SIN_SEDE}</option>
                {(sedes.data ?? []).map((nombre) => (
                  <option key={nombre} value={nombre}>{nombre}</option>
                ))}
              </select>
            </label>
            <label className="text-sm font-semibold">
              Estado
              <select className={campo} value={estado} onChange={(evento) => setEstado(evento.target.value)}>
                <option value="">Todos</option>
                {ESTADOS.map((nombre) => (
                  <option key={nombre} value={nombre}>{nombre}</option>
                ))}
              </select>
            </label>
            <div className="grid gap-3 sm:col-span-2 sm:grid-cols-[1fr_1fr_auto] sm:items-end lg:col-span-3">
              <CampoFecha id="filtro-desde" label="Desde" value={desde} onChange={setDesde} onBlur={() => undefined} error={desde && !fechaValida(desde) ? "Use el formato dd/mm/aaaa." : undefined} />
              <CampoFecha id="filtro-hasta" label="Hasta" value={hasta} onChange={setHasta} onBlur={() => undefined} error={rangoInvalido ? "La fecha final es anterior a la inicial." : hasta && !fechaValida(hasta) ? "Use el formato dd/mm/aaaa." : undefined} />
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
        </section>

        <section className="mt-5">
          <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
            <h2 className="font-semibold">Listado</h2>
            <button
              type="button"
              className="rounded-md border border-line bg-card px-3 py-2 text-sm font-semibold"
              aria-pressed={recientes}
              onClick={() => setRecientes((actual) => !actual)}
            >
              Hora de inicio · {recientes ? "más recientes" : "más antiguas"}
            </button>
          </div>
          {datos.length === 0 && <p className="text-sm text-muted">Todavía no llegan reclamos.</p>}
          {datos.length > 0 && visibles.length === 0 && (
            <p className="text-sm text-muted">Ningún reclamo coincide con este filtro.</p>
          )}
          {visibles.length > 0 && (
            <div className="overflow-x-auto rounded-xl border border-line bg-card">
              <table className="w-full min-w-[720px] text-left text-sm">
                <thead className="border-b border-line text-xs uppercase tracking-wide text-muted">
                  <tr>
                    <th className="px-3 py-2 font-semibold">Id</th>
                    <th className="px-3 py-2 font-semibold" aria-sort={recientes ? "descending" : "ascending"}>Hora de inicio</th>
                    <th className="px-3 py-2 font-semibold">Empresa</th>
                    <th className="px-3 py-2 font-semibold">Tipo de reclamo</th>
                    <th className="px-3 py-2 font-semibold">Estado del reclamo</th>
                    <th className="px-3 py-2 font-semibold">Persona responsable</th>
                  </tr>
                </thead>
                <tbody>
                  {visibles.map((reclamo) => (
                    <tr
                      key={reclamo.id}
                      className="cursor-pointer border-t border-line hover:bg-paper"
                      onClick={() => setAbierto(reclamo.id)}
                    >
                      <td className="px-3 py-3">{reclamo.id}</td>
                      <td className="px-3 py-3">
                        <button type="button" className="text-left font-semibold" onClick={() => setAbierto(reclamo.id)}>
                          {reclamo.fecha} {reclamo.hora}
                        </button>
                      </td>
                      <td className="px-3 py-3">
                        <p>{reclamo.empresa}</p>
                        <p className="text-xs text-muted">{reclamo.obra}</p>
                      </td>
                      <td className="px-3 py-3">{reclamo.tipo}</td>
                      <td className="px-3 py-3">
                        <span className="rounded-full bg-paper px-2 py-1 text-xs font-semibold">{reclamo.estado}</span>
                      </td>
                      <td className="px-3 py-3">{reclamo.responsable ?? "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

          </>
        )}
        {vista === "gestion" && <InformeGestion />}
      </main>
      {seleccionado && <Detalle reclamo={seleccionado} onCerrar={() => setAbierto(null)} />}
    </div>
  );
}

function Detalle({ reclamo, onCerrar }: { reclamo: Reclamo; onCerrar: () => void }) {
  useEffect(() => {
    function tecla(evento: KeyboardEvent) {
      if (evento.key === "Escape") {
        onCerrar();
      }
    }
    window.addEventListener("keydown", tecla);
    return () => window.removeEventListener("keydown", tecla);
  }, [onCerrar]);

  return (
    <div className="fixed inset-0 z-40 overflow-y-auto bg-paper px-4 py-8" onClick={onCerrar}>
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-reclamo"
        className="mx-auto w-full max-w-6xl rounded-xl border border-line bg-card p-5"
        onClick={(evento) => evento.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-muted">Planilla D-SIG-09-02 · Reclamo {reclamo.id}</p>
            <h2 id="titulo-reclamo" className="font-serif text-2xl">{reclamo.empresa}</h2>
            <p className="mt-1 text-sm text-muted">Los datos ingresados no se vuelven a escribir. El seguimiento se completa abajo.</p>
          </div>
          <button type="button" className="rounded-md border border-line bg-card px-3 py-1 text-sm" onClick={onCerrar}>
            Cerrar
          </button>
        </div>
        <section className="mt-4 rounded-lg border border-line bg-paper p-4">
          <h3 className="text-base font-semibold text-ink">Datos ingresados</h3>
          <dl className="mt-3 grid items-stretch gap-x-4 gap-y-3 sm:grid-cols-2">
            <Dato etiqueta="Id" valor={String(reclamo.id)} />
            <Dato etiqueta="Hora de ingreso del reclamo" valor={`${reclamo.fecha} ${reclamo.hora}`} />
            <Dato etiqueta="Empresa / razón social" valor={reclamo.empresa} />
            <Dato etiqueta="Nombre de la obra" valor={reclamo.obra} />
            <Dato etiqueta="Ubicación (dirección, comuna, ciudad)" valor={reclamo.ubicacion} />
            <Dato etiqueta="Guía de despacho / orden" valor={reclamo.guiaDespacho ?? "—"} />
            <Dato etiqueta="Nombre de contacto" valor={reclamo.nombre} />
            <Dato etiqueta="Correo electrónico" valor={reclamo.correo} />
            <Dato etiqueta="Teléfono de contacto" valor={reclamo.telefono} />
            <Dato etiqueta="Horario preferido para contacto" valor={reclamo.horario ?? "—"} />
            <Dato etiqueta="¿Cómo prefiere que lo contactemos?" valor={reclamo.preferenciaContacto} />
            <div className="hidden sm:block" aria-hidden="true" />
            <Dato etiqueta="Tipo de reclamo" valor={reclamo.tipo} />
            <Dato etiqueta="Fecha del servicio" valor={reclamo.fechaServicio} />
            <div className="sm:col-span-2">
              <Dato etiqueta="Descripción del reclamo" valor={reclamo.descripcion} relato />
            </div>
            <div className="sm:col-span-2">
              <Dato etiqueta="Comentarios adicionales" valor={reclamo.comentarios ?? "—"} relato />
            </div>
          </dl>
        </section>
        <Formulario reclamo={reclamo} />
      </div>
    </div>
  );
}

function Formulario({ reclamo }: { reclamo: Reclamo }) {
  const { usuario } = useSesion();
  const queryClient = useQueryClient();
  const sedes = useQuery({
    queryKey: ["sedes"],
    queryFn: () => api<string[]>("/api/departamento1/sedes"),
  });
  const firma = [
    reclamo.estado,
    reclamo.sede,
    reclamo.area,
    reclamo.responsable,
    reclamo.fechaGestion,
    reclamo.observaciones,
    reclamo.fechaCierre,
    reclamo.horaCierre,
    reclamo.solucion,
  ].join("|");
  const [sede, setSede] = useState(reclamo.sede ?? "");
  const [area, setArea] = useState(reclamo.area ?? "");
  const [areas, setAreas] = useState<string[]>(() => areasDesdeTexto(reclamo.area));
  const [areaTocada, setAreaTocada] = useState(false);
  const [responsable, setResponsable] = useState(reclamo.responsable ?? usuario?.nombre ?? "");
  const [observaciones, setObservaciones] = useState(reclamo.observaciones ?? "");
  const [solucion, setSolucion] = useState(reclamo.solucion ?? "");
  const [error, setError] = useState("");
  const [campos, setCampos] = useState<Record<string, string>>({});
  const [aviso, setAviso] = useState("");
  const [guardando, setGuardando] = useState(false);
  const estado = estadoAutomatico(observaciones, solucion);
  const gestiona = estado !== "Recibido";

  useEffect(() => {
    setSede(reclamo.sede ?? "");
    setArea(reclamo.area ?? "");
    setAreas(areasDesdeTexto(reclamo.area));
    setAreaTocada(false);
    setResponsable(reclamo.responsable ?? usuario?.nombre ?? "");
    setObservaciones(reclamo.observaciones ?? "");
    setSolucion(reclamo.solucion ?? "");
  }, [firma, reclamo, usuario?.nombre]);

  async function bajarInforme() {
    setError("");
    try {
      await descargar(
        `/api/departamento3/reclamos/${reclamo.id}/informe`,
        `D-SIG-09-02-reclamo-${reclamo.id}.pdf`,
      );
    } catch (err) {
      setError(err instanceof ErrorApi ? err.message : "No se pudo descargar el informe.");
    }
  }

  async function guardar() {
    setGuardando(true);
    setError("");
    setCampos({});
    setAviso("");
    try {
      await api(`/api/departamento3/reclamos/${reclamo.id}`, {
        method: "PUT",
        body: JSON.stringify({
          estado,
          area,
          responsable,
          observaciones,
          solucion,
          sede,
        }),
      });
      await queryClient.invalidateQueries({ queryKey: ["reclamos"] });
      setAviso("Seguimiento guardado.");
    } catch (err) {
      if (err instanceof ErrorApi) {
        setError(err.message);
        setCampos(err.campos);
      } else {
        setError("No se pudo guardar el seguimiento.");
      }
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form
      className="mt-5 grid gap-3 border-t border-line pt-4 sm:grid-cols-2"
      onSubmit={(evento) => {
        evento.preventDefault();
        void guardar();
      }}
    >
      <h3 className="text-sm font-semibold sm:col-span-2">Seguimiento</h3>
      <p className="text-sm sm:col-span-2">
        <span className="font-semibold">Estado del reclamo</span>
        <span className="mt-1 block">{estado}</span>
        <span className="mt-1 block text-xs font-normal text-muted">
          Al recibirlo queda Recibido. Si escribes observaciones internas, pasa a En gestión. Si escribes la solución, el caso se cierra. Las fechas quedan solo en el informe.
        </span>
      </p>
      <div className="text-sm font-semibold">
        Área responsable{gestiona ? " *" : ""}
        <MenuAreas
          seleccion={areas}
          texto={areaTocada || areas.length > 0 ? areas.join(", ") : area}
          error={campos.area}
          onChange={(seleccion) => {
            setAreas(seleccion);
            setArea(seleccion.join(", "));
            setAreaTocada(true);
          }}
        />
      </div>
      <label className="text-sm font-semibold">
        Sede responsable{gestiona ? " *" : ""}
        <select className={campo} value={sede} onChange={(evento) => setSede(evento.target.value)}>
          <option value="">Seleccione la sede</option>
          {(sedes.data ?? []).map((nombre) => (
            <option key={nombre} value={nombre}>
              {nombre}
            </option>
          ))}
        </select>
        {campos.sede && <Aviso texto={campos.sede} />}
      </label>
      <label className="text-sm font-semibold">
        Persona responsable del reclamo{gestiona ? " *" : ""}
        <input className={campo} value={responsable} maxLength={80} onChange={(evento) => setResponsable(evento.target.value)} />
        {campos.responsable && <Aviso texto={campos.responsable} />}
      </label>
      <label className="text-sm font-semibold sm:col-span-2">
        Observaciones internas
        <textarea className={campo} rows={2} maxLength={500} value={observaciones} onChange={(evento) => setObservaciones(evento.target.value)} />
        {campos.observaciones && <Aviso texto={campos.observaciones} />}
      </label>
      <label className="text-sm font-semibold sm:col-span-2">
        Solución{estado === "Cerrado" ? " *" : ""}
        <textarea className={campo} rows={3} maxLength={1000} value={solucion} onChange={(evento) => setSolucion(evento.target.value)} />
        {campos.solucion && <Aviso texto={campos.solucion} />}
      </label>
      {error && (
        <p className="text-sm text-copper-dark sm:col-span-2" role="alert">
          {error}
        </p>
      )}
      {aviso && <p className="text-sm text-pine sm:col-span-2">{aviso}</p>}
      {reclamo.estado !== "Cerrado" && estado === "Cerrado" && (
        <p className="text-sm text-muted sm:col-span-2">Guarde el cierre para descargar el informe.</p>
      )}
      <div className="flex flex-wrap gap-2 sm:col-span-2">
        <button type="submit" disabled={guardando} className="rounded-md bg-pine px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">
          {guardando ? "Guardando…" : "Guardar seguimiento"}
        </button>
        {reclamo.estado === "Cerrado" && (
          <button type="button" className="rounded-md border border-line bg-card px-4 py-2 text-sm font-semibold" onClick={() => void bajarInforme()}>
            Descargar informe
          </button>
        )}
      </div>
    </form>
  );
}

function Dato({ etiqueta, valor, relato = false }: { etiqueta: string; valor: string; relato?: boolean }) {
  return (
    <div className="flex h-full flex-col gap-1.5">
      <dt className="text-sm font-semibold leading-5 text-ink">{etiqueta}</dt>
      <dd
        className={
          relato
            ? "min-h-24 flex-1 whitespace-pre-wrap rounded-md border border-pine/25 bg-pine-soft px-3 py-3 text-sm leading-6 text-ink"
            : "min-h-10 flex-1 rounded-md border border-line bg-card px-3 py-2 text-xs leading-5 text-muted"
        }
      >
        {valor}
      </dd>
    </div>
  );
}

function Aviso({ texto }: { texto: string }) {
  return <span className="mt-1 block text-xs font-normal text-copper-dark">{texto}</span>;
}
