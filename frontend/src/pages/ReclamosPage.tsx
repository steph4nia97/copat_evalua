import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { api, ErrorApi } from "../api/client";
import { useSesion } from "../auth/sesion";
import { CampoFecha } from "../components/CampoFecha";
import { Encabezado } from "../components/Encabezado";
import { compararFechas, fechaValida } from "../domain/reglas";

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
const campo = "mt-1 w-full rounded-md border border-line bg-white px-3 py-2 text-sm";

function instante(reclamo: Reclamo) {
  const [dia, mes, anio] = reclamo.fecha.split("/").map(Number);
  const [hora, minuto] = reclamo.hora.split(":").map(Number);
  return new Date(anio, (mes || 1) - 1, dia || 1, hora || 0, minuto || 0).getTime();
}

function mesDe(fecha: string) {
  return fecha.slice(3);
}

function porcentaje(parte: number, total: number) {
  if (total === 0) return 0;
  return Math.round((parte / total) * 100);
}

function diasEntre(inicio: string, fin: string) {
  if (!fechaValida(inicio) || !fechaValida(fin)) return null;
  const [diaInicio, mesInicio, anioInicio] = inicio.split("/").map(Number);
  const [diaFin, mesFin, anioFin] = fin.split("/").map(Number);
  const milisegundos = new Date(anioFin, mesFin - 1, diaFin).getTime() - new Date(anioInicio, mesInicio - 1, diaInicio).getTime();
  return Math.round(milisegundos / 86400000);
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
  const enlace = `${window.location.origin}/reclamo`;
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
        <h1 className="font-serif text-3xl">Seguimiento de reclamos</h1>
        <p className="mt-1 max-w-3xl text-sm text-muted">
          D-SIG-09-02. El listado se acota por tipo, sede y fecha de recepción. Los porcentajes de abajo corresponden a ese corte.
        </p>
        <div className="mt-4 flex flex-wrap items-center gap-2">
          <input readOnly value={enlace} aria-label="Enlace de reclamos" className="w-full max-w-md rounded-md border border-line bg-card px-3 py-2 text-sm" onFocus={(evento) => evento.currentTarget.select()} />
          <button type="button" onClick={() => void copiar()} className="rounded-md bg-pine px-3 py-2 text-sm font-semibold text-white">
            {copiado ? "Enlace copiado" : "Copiar enlace"}
          </button>
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
              Fecha recibida · {recientes ? "más recientes" : "más antiguas"}
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
                    <th className="px-3 py-2 font-semibold" aria-sort={recientes ? "descending" : "ascending"}>Fecha recibida</th>
                    <th className="px-3 py-2 font-semibold">Empresa</th>
                    <th className="px-3 py-2 font-semibold">Tipo</th>
                    <th className="px-3 py-2 font-semibold">Sede responsable</th>
                    <th className="px-3 py-2 font-semibold">Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {visibles.map((reclamo) => (
                    <tr
                      key={reclamo.id}
                      className="cursor-pointer border-t border-line hover:bg-paper"
                      onClick={() => setAbierto(reclamo.id)}
                    >
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
                      <td className="px-3 py-3">{reclamo.sede ?? "Sin sede"}</td>
                      <td className="px-3 py-3">
                        <span className="rounded-full bg-paper px-2 py-1 text-xs font-semibold">{reclamo.estado}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <Metricas reclamos={visibles} totalGeneral={datos.length} sedes={sedes.data ?? []} />
      </main>
      {seleccionado && <Detalle reclamo={seleccionado} onCerrar={() => setAbierto(null)} />}
    </div>
  );
}

function Metricas({ reclamos, totalGeneral, sedes }: { reclamos: Reclamo[]; totalGeneral: number; sedes: string[] }) {
  const total = reclamos.length;
  const porTipo = TIPOS.map((nombre) => ({ nombre, cantidad: reclamos.filter((reclamo) => reclamo.tipo === nombre).length }));
  const nombresSede = [...sedes, SIN_SEDE];
  const porSede = nombresSede.map((nombre) => ({
    nombre,
    cantidad: reclamos.filter((reclamo) => (nombre === SIN_SEDE ? !reclamo.sede : reclamo.sede === nombre)).length,
  })).filter((item) => item.cantidad > 0);
  const porEstado = ESTADOS.map((nombre) => ({ nombre, cantidad: reclamos.filter((reclamo) => reclamo.estado === nombre).length }));
  const lider = [...porTipo].sort((a, b) => b.cantidad - a.cantidad)[0];
  const cerrados = reclamos.filter((reclamo) => reclamo.estado === "Cerrado" && reclamo.fechaCierre);
  const dias = cerrados
    .map((reclamo) => diasEntre(reclamo.fecha, reclamo.fechaCierre ?? ""))
    .filter((valor): valor is number => valor != null);
  const promedioDias = dias.length === 0 ? null : Math.round(dias.reduce((suma, valor) => suma + valor, 0) / dias.length);
  const meses = new Map<string, number>();
  for (const reclamo of reclamos) {
    const clave = mesDe(reclamo.fecha);
    meses.set(clave, (meses.get(clave) ?? 0) + 1);
  }
  const serie = [...meses.entries()].sort((a, b) => {
    const [mesA, anioA] = a[0].split("/").map(Number);
    const [mesB, anioB] = b[0].split("/").map(Number);
    return anioA - anioB || mesA - mesB;
  });
  const maximoMes = Math.max(...serie.map((item) => item[1]), 1);
  const lectura = total === 0
    ? "No hay reclamos en este corte."
    : `${lider.nombre} es el ${porcentaje(lider.cantidad, total)}% del corte. ${porcentaje(porEstado.find((item) => item.nombre === "Cerrado")?.cantidad ?? 0, total)}% está cerrado.`;

  return (
    <section className="mt-8" aria-label="Porcentajes del corte">
      <h2 className="font-serif text-2xl">Porcentajes</h2>
      <p className="mt-1 text-sm text-muted">{lectura} {totalGeneral > 0 ? `El corte representa el ${porcentaje(total, totalGeneral)}% de todos los reclamos.` : ""}</p>
      <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Resumen etiqueta="En el corte" valor={String(total)} detalle={`${porcentaje(total, totalGeneral)}% del total`} />
        {porEstado.map((item) => (
          <Resumen key={item.nombre} etiqueta={item.nombre} valor={`${porcentaje(item.cantidad, total)}%`} detalle={`${item.cantidad} ${item.cantidad === 1 ? "caso" : "casos"}`} />
        ))}
        <Resumen etiqueta="Días al cierre" valor={promedioDias == null ? "—" : String(promedioDias)} detalle={dias.length ? `Promedio de ${dias.length} cerrados` : "Sin cierres en el corte"} />
        <Resumen etiqueta="Sin sede" valor={`${porcentaje(porSede.find((item) => item.nombre === SIN_SEDE)?.cantidad ?? 0, total)}%`} detalle={`${porSede.find((item) => item.nombre === SIN_SEDE)?.cantidad ?? 0} ${(porSede.find((item) => item.nombre === SIN_SEDE)?.cantidad ?? 0) === 1 ? "caso" : "casos"}`} />
      </div>
      <div className="mt-4 grid gap-4 lg:grid-cols-2">
        <Distribucion titulo="Por tipo de reclamo" items={porTipo} total={total} />
        <Distribucion titulo="Por sede responsable" items={porSede.length ? porSede : [{ nombre: SIN_SEDE, cantidad: 0 }]} total={total} />
      </div>
      {serie.length > 0 && (
        <div className="mt-4 rounded-xl border border-line bg-card p-4">
          <h3 className="font-semibold">Llegadas por mes</h3>
          <ul className="mt-3 flex items-end gap-3 overflow-x-auto">
            {serie.map(([mes, cantidad]) => (
              <li key={mes} className="flex w-16 shrink-0 flex-col items-center gap-1">
                <span className="text-xs font-semibold">{cantidad}</span>
                <span className="text-[11px] text-muted">{porcentaje(cantidad, total)}%</span>
                <span className="flex h-20 w-full items-end rounded-md bg-paper">
                  <span className="block w-full rounded-md bg-copper" style={{ height: `${Math.max(8, (cantidad / maximoMes) * 100)}%` }} />
                </span>
                <span className="text-center text-[11px] text-muted">{mes}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}

function Resumen({ etiqueta, valor, detalle }: { etiqueta: string; valor: string; detalle: string }) {
  return (
    <article className="rounded-xl border border-line bg-card p-4">
      <p className="text-xs uppercase tracking-wide text-muted">{etiqueta}</p>
      <p className="font-serif text-3xl">{valor}</p>
      <p className="text-sm text-muted">{detalle}</p>
    </article>
  );
}

function Distribucion({ titulo, items, total }: { titulo: string; items: { nombre: string; cantidad: number }[]; total: number }) {
  const maximo = Math.max(...items.map((item) => item.cantidad), 1);
  return (
    <article className="rounded-xl border border-line bg-card p-4">
      <h3 className="font-semibold">{titulo}</h3>
      <ul className="mt-3 space-y-3">
        {items.map((item) => (
          <li key={item.nombre}>
            <div className="flex items-baseline justify-between gap-3 text-sm">
              <span className="font-semibold">{item.nombre}</span>
              <span className="text-muted">{item.cantidad} · {porcentaje(item.cantidad, total)}%</span>
            </div>
            <div className="mt-1 h-2 overflow-hidden rounded-full bg-paper">
              <div className="h-full rounded-full bg-pine" style={{ width: `${(item.cantidad / maximo) * 100}%` }} />
            </div>
          </li>
        ))}
      </ul>
    </article>
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
            <p className="text-xs font-semibold uppercase tracking-wide text-muted">Reclamo {reclamo.id}</p>
            <h2 id="titulo-reclamo" className="font-serif text-2xl">{reclamo.empresa}</h2>
          </div>
          <button type="button" className="rounded-md border border-line bg-card px-3 py-1 text-sm" onClick={onCerrar}>
            Cerrar
          </button>
        </div>
        <dl className="mt-4 grid gap-2 text-sm sm:grid-cols-2">
          <Dato etiqueta="Fecha recibida" valor={`${reclamo.fecha} ${reclamo.hora}`} />
          <Dato etiqueta="Estado" valor={reclamo.estado} />
          <Dato etiqueta="Sede responsable" valor={reclamo.sede ?? "Sin sede"} />
          <Dato etiqueta="Tipo de reclamo" valor={reclamo.tipo} />
          <Dato etiqueta="Nombre" valor={reclamo.nombre} />
          <Dato etiqueta="Correo electrónico" valor={reclamo.correo} />
          <Dato etiqueta="Teléfono de contacto" valor={reclamo.telefono} />
          <Dato etiqueta="Nombre de la obra" valor={reclamo.obra} />
          <Dato etiqueta="Ubicación" valor={reclamo.ubicacion} />
          <Dato etiqueta="Fecha del servicio" valor={reclamo.fechaServicio} />
          <Dato etiqueta="Guía de despacho / orden" valor={reclamo.guiaDespacho ?? "—"} />
          <Dato etiqueta="Hora de finalización" valor={reclamo.horaCierre ?? "—"} />
          <Dato etiqueta="¿Cómo prefiere que lo contactemos?" valor={reclamo.preferenciaContacto} />
          <Dato etiqueta="Horario preferido" valor={reclamo.horario ?? "—"} />
          <div className="sm:col-span-2">
            <Dato etiqueta="Descripción del reclamo" valor={reclamo.descripcion} />
          </div>
          <div className="sm:col-span-2">
            <Dato etiqueta="Comentarios adicionales" valor={reclamo.comentarios ?? "—"} />
          </div>
        </dl>
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
  const [estado, setEstado] = useState(reclamo.estado);
  const [sede, setSede] = useState(reclamo.sede ?? "");
  const [area, setArea] = useState(reclamo.area ?? "");
  const [responsable, setResponsable] = useState(reclamo.responsable ?? usuario?.nombre ?? "");
  const [fechaGestion, setFechaGestion] = useState(reclamo.fechaGestion ?? "");
  const [observaciones, setObservaciones] = useState(reclamo.observaciones ?? "");
  const [fechaCierre, setFechaCierre] = useState(reclamo.fechaCierre ?? "");
  const [horaCierre, setHoraCierre] = useState(reclamo.horaCierre ?? "");
  const [solucion, setSolucion] = useState(reclamo.solucion ?? "");
  const [error, setError] = useState("");
  const [campos, setCampos] = useState<Record<string, string>>({});
  const [aviso, setAviso] = useState("");
  const [guardando, setGuardando] = useState(false);
  const gestiona = estado === "En gestión" || estado === "Cerrado";

  useEffect(() => {
    setEstado(reclamo.estado);
    setSede(reclamo.sede ?? "");
    setArea(reclamo.area ?? "");
    setResponsable(reclamo.responsable ?? usuario?.nombre ?? "");
    setFechaGestion(reclamo.fechaGestion ?? "");
    setObservaciones(reclamo.observaciones ?? "");
    setFechaCierre(reclamo.fechaCierre ?? "");
    setHoraCierre(reclamo.horaCierre ?? "");
    setSolucion(reclamo.solucion ?? "");
  }, [firma, reclamo, usuario?.nombre]);

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
          fechaGestion,
          observaciones,
          fechaCierre,
          horaCierre,
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
      className="mt-4 grid gap-3 border-t border-line pt-4 sm:grid-cols-2"
      onSubmit={(evento) => {
        evento.preventDefault();
        void guardar();
      }}
    >
      <label className="text-sm font-semibold">
        Estado del reclamo
        <select className={campo} value={estado} onChange={(evento) => setEstado(evento.target.value)}>
          {ESTADOS.map((opcion) => (
            <option key={opcion} value={opcion}>
              {opcion}
            </option>
          ))}
        </select>
        {campos.estado && <Aviso texto={campos.estado} />}
      </label>
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
        Área responsable
        <input className={campo} value={area} maxLength={80} onChange={(evento) => setArea(evento.target.value)} />
        {campos.area && <Aviso texto={campos.area} />}
      </label>
      <label className="text-sm font-semibold">
        Persona responsable del reclamo
        <input className={campo} value={responsable} maxLength={80} onChange={(evento) => setResponsable(evento.target.value)} />
        {campos.responsable && <Aviso texto={campos.responsable} />}
      </label>
      <label className="text-sm font-semibold">
        Fecha de gestión
        <input className={campo} value={fechaGestion} placeholder="dd/mm/aaaa" onChange={(evento) => setFechaGestion(evento.target.value)} />
        {campos.fechaGestion && <Aviso texto={campos.fechaGestion} />}
      </label>
      <label className="text-sm font-semibold sm:col-span-2">
        Observaciones internas
        <textarea className={campo} rows={2} maxLength={500} value={observaciones} onChange={(evento) => setObservaciones(evento.target.value)} />
        {campos.observaciones && <Aviso texto={campos.observaciones} />}
      </label>
      {estado === "Cerrado" && (
        <>
          <label className="text-sm font-semibold">
            Fecha de cierre
            <input className={campo} value={fechaCierre} placeholder="dd/mm/aaaa" onChange={(evento) => setFechaCierre(evento.target.value)} />
            {campos.fechaCierre && <Aviso texto={campos.fechaCierre} />}
          </label>
          <label className="text-sm font-semibold">
            Hora de finalización
            <input className={campo} value={horaCierre} placeholder="hh:mm" onChange={(evento) => setHoraCierre(evento.target.value)} />
            {campos.horaCierre && <Aviso texto={campos.horaCierre} />}
          </label>
          <label className="text-sm font-semibold sm:col-span-2">
            Solución
            <textarea className={campo} rows={3} maxLength={1000} value={solucion} onChange={(evento) => setSolucion(evento.target.value)} />
            {campos.solucion && <Aviso texto={campos.solucion} />}
          </label>
        </>
      )}
      {error && (
        <p className="text-sm text-copper-dark sm:col-span-2" role="alert">
          {error}
        </p>
      )}
      {aviso && <p className="text-sm text-pine sm:col-span-2">{aviso}</p>}
      <div className="sm:col-span-2">
        <button type="submit" disabled={guardando} className="rounded-md bg-pine px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">
          {guardando ? "Guardando…" : "Guardar seguimiento"}
        </button>
      </div>
    </form>
  );
}

function Dato({ etiqueta, valor }: { etiqueta: string; valor: string }) {
  return (
    <div>
      <dt className="text-xs text-muted">{etiqueta}</dt>
      <dd>{valor}</dd>
    </div>
  );
}

function Aviso({ texto }: { texto: string }) {
  return <span className="mt-1 block text-xs font-normal text-copper-dark">{texto}</span>;
}
