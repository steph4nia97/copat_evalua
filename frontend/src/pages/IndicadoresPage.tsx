import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { Encabezado } from "../components/Encabezado";

type Nota = { etiqueta: string; valor: number | null };

type Indicador = {
  id: number;
  empresa: string;
  sedes: string[];
  obra: string;
  estadoObra: string;
  estado: string;
  desempeno: string;
  promedio: number;
  notas: Nota[];
  fecha: string;
  vecesRechazada: number;
};

const DESEMPENOS = ["Destacado", "Satisfactorio", "Regular", "Insuficiente"];
const COLORES: Record<string, string> = {
  Destacado: "#1f6b4a",
  Satisfactorio: "#2f6f8f",
  Regular: "#c4842a",
  Insuficiente: "#9a3d2f",
};

function texto(valor: number) {
  return valor.toLocaleString("es-CL", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function promedioDe(valores: number[]) {
  if (valores.length === 0) return 0;
  const centavos = valores.reduce((suma, valor) => suma + Math.round(valor * 100), 0);
  return Math.round(centavos / valores.length) / 100;
}

function clasificar(promedio: number) {
  if (promedio >= 4.5) return "Destacado";
  if (promedio >= 3.5) return "Satisfactorio";
  if (promedio >= 2.5) return "Regular";
  return "Insuficiente";
}

export function IndicadoresPage() {
  const [estadoObra, setEstadoObra] = useState("");
  const [sede, setSede] = useState("");
  const [desempeno, setDesempeno] = useState("");
  const [activo, setActivo] = useState<number | null>(null);
  const [pregunta, setPregunta] = useState<number | null>(null);
  const sedes = useQuery({
    queryKey: ["sedes"],
    queryFn: () => api<string[]>("/api/departamento1/sedes"),
  });
  const indicadores = useQuery({
    queryKey: ["d3-indicadores"],
    queryFn: () => api<Indicador[]>("/api/departamento3/indicadores"),
  });
  const datos = indicadores.data ?? [];
  const obras = [...new Set(datos.map((item) => item.estadoObra))];
  const visibles = useMemo(
    () => datos.filter((item) => (!estadoObra || item.estadoObra === estadoObra) && (!sede || item.sedes.includes(sede)) && (!desempeno || item.desempeno === desempeno)),
    [datos, estadoObra, sede, desempeno],
  );
  const promedio = promedioDe(visibles.map((item) => Number(item.promedio)));
  const etiquetas = datos[0]?.notas.map((nota) => nota.etiqueta) ?? [];
  const porPregunta = etiquetas.map((etiqueta, indice) => {
    const aplicables = visibles.flatMap((item) => {
      const valor = item.notas[indice]?.valor;
      return valor == null ? [] : [valor];
    });
    return { etiqueta, promedio: promedioDe(aplicables), cantidad: aplicables.length };
  });
  const porNivel = DESEMPENOS.map((nivel) => ({
    nivel,
    cantidad: visibles.filter((item) => item.desempeno === nivel).length,
  }));
  const maximo = Math.max(...porNivel.map((nivel) => nivel.cantidad), 1);
  const seleccionado = visibles.find((item) => item.id === activo) ?? null;

  function alternarNivel(nivel: string) {
    setDesempeno((actual) => (actual === nivel ? "" : nivel));
  }

  function alternarObra(obra: string) {
    setEstadoObra((actual) => (actual === obra ? "" : obra));
  }

  return (
    <div className="min-h-screen">
      <Encabezado />
      <main className="mx-auto max-w-6xl px-4 py-6">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h1 className="font-serif text-3xl">Métricas de la encuesta</h1>
            <p className="mt-1 max-w-3xl text-sm text-muted">
              El registro usa las notas del 1 al 5. No aplica queda fuera del promedio. Pulse una sede, un nivel, un estado de obra o una encuesta para acotar el resultado.
            </p>
          </div>
          <Link to="/" className="rounded-md border border-line px-3 py-2 text-sm">Volver a las solicitudes</Link>
        </div>

        <div className="mt-4 flex flex-wrap gap-2">
          <button type="button" className={`rounded-full px-3 py-1 text-sm ${estadoObra === "" ? "bg-pine text-white" : "bg-card"}`} onClick={() => setEstadoObra("")}>
            Todas las obras
          </button>
          {obras.map((obra) => (
            <button key={obra} type="button" className={`rounded-full px-3 py-1 text-sm ${estadoObra === obra ? "bg-pine text-white" : "bg-card"}`} onClick={() => alternarObra(obra)}>
              {obra}
            </button>
          ))}
        </div>
        <div className="mt-3 flex flex-wrap gap-2">
          <button type="button" className={`rounded-full px-3 py-1 text-sm ${sede === "" ? "bg-pine text-white" : "bg-card"}`} onClick={() => setSede("")}>
            Todas las sedes
          </button>
          {(sedes.data ?? []).map((nombre) => (
            <button key={nombre} type="button" className={`rounded-full px-3 py-1 text-sm ${sede === nombre ? "bg-pine text-white" : "bg-card"}`} onClick={() => setSede((actual) => (actual === nombre ? "" : nombre))}>
              {nombre}
            </button>
          ))}
        </div>

        <section className="mt-4 grid gap-3 sm:grid-cols-4">
          <article className="rounded-xl border border-line bg-card p-4">
            <p className="text-xs uppercase tracking-wide text-muted">Encuestas</p>
            <p className="font-serif text-4xl">{visibles.length}</p>
          </article>
          <article className="rounded-xl border border-pine/20 bg-pine-soft p-4 sm:col-span-2">
            <p className="text-xs uppercase tracking-wide text-pine">Promedio del filtro</p>
            <p className="font-serif text-4xl text-pine">{visibles.length ? texto(promedio) : "—"}</p>
            <p className="text-sm">{visibles.length ? clasificar(promedio) : "Sin encuestas"}</p>
          </article>
          <article className="rounded-xl border border-line bg-card p-4">
            <p className="text-xs uppercase tracking-wide text-muted">Volvieron de un rechazo</p>
            <p className="font-serif text-4xl">{visibles.filter((item) => item.vecesRechazada > 0).length}</p>
          </article>
        </section>

        <section className="mt-4 grid gap-4 lg:grid-cols-5">
          <div className="rounded-xl border border-line bg-card p-4 lg:col-span-3">
            <h2 className="font-semibold">Promedio por pregunta</h2>
            <ul className="mt-3 space-y-3">
              {porPregunta.map((item, indice) => (
                <li key={item.etiqueta}>
                  <button type="button" className="w-full text-left" onClick={() => setPregunta(pregunta === indice ? null : indice)}>
                    <span className="flex justify-between gap-3 text-sm">
                      <span>{item.etiqueta}</span>
                      <span className="font-semibold">{item.cantidad ? texto(item.promedio) : "—"}</span>
                    </span>
                    <span className="mt-1 block h-3 overflow-hidden rounded-full bg-paper">
                      <span
                        className="block h-full rounded-full bg-pine transition-all duration-500"
                        style={{ width: `${item.cantidad ? (item.promedio / 5) * 100 : 0}%`, opacity: pregunta === null || pregunta === indice ? 1 : 0.35 }}
                      />
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          </div>
          <div className="rounded-xl border border-line bg-card p-4 lg:col-span-2">
            <h2 className="font-semibold">Desempeño</h2>
            <ul className="mt-3 space-y-3">
              {porNivel.map((nivel) => (
                <li key={nivel.nivel}>
                  <button type="button" className="w-full text-left" onClick={() => alternarNivel(nivel.nivel)}>
                    <span className="flex justify-between text-sm">
                      <span>{nivel.nivel}</span>
                      <span>{nivel.cantidad}</span>
                    </span>
                    <span className="mt-1 block h-3 overflow-hidden rounded-full bg-paper">
                      <span
                        className="block h-full rounded-full transition-all duration-500"
                        style={{
                          width: `${(nivel.cantidad / maximo) * 100}%`,
                          background: COLORES[nivel.nivel],
                          outline: desempeno === nivel.nivel ? "2px solid #16332f" : "none",
                        }}
                      />
                    </span>
                  </button>
                </li>
              ))}
            </ul>
            {desempeno && <button type="button" className="mt-3 text-sm text-muted" onClick={() => setDesempeno("")}>Quitar filtro de evaluación</button>}
          </div>
        </section>

        {seleccionado && (
          <section className="mt-4 rounded-xl border border-copper/30 bg-card p-4">
            <p className="text-xs uppercase tracking-wide text-copper-dark">Encuesta seleccionada frente al filtro</p>
            <p className="mt-1 font-serif text-2xl">{seleccionado.empresa}</p>
            <p className="text-sm text-muted">{seleccionado.sedes.length ? seleccionado.sedes.join(", ") : "Sin sede"} · {seleccionado.obra} · {seleccionado.fecha}</p>
            <p className="mt-2 text-sm">
              Esta encuesta tiene {texto(Number(seleccionado.promedio))} ({seleccionado.desempeno}). El filtro está en {visibles.length ? texto(promedio) : "—"}.
            </p>
          </section>
        )}

        <section className="mt-4 overflow-x-auto rounded-xl border border-line bg-card">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-paper text-muted">
              <tr>
                {["Fecha", "Empresa", "Sede", "Obra", "Estado de la obra", "Promedio", "Evaluación"].map((columna) => (
                  <th key={columna} className="px-3 py-2 font-semibold">{columna}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {visibles.map((item) => (
                <tr
                  key={item.id}
                  className={`cursor-pointer border-t border-line ${activo === item.id ? "bg-pine-soft" : ""}`}
                  onClick={() => setActivo(activo === item.id ? null : item.id)}
                >
                  <td className="whitespace-nowrap px-3 py-2">{item.fecha}</td>
                  <td className="px-3 py-2">{item.empresa}</td>
                  <td className="px-3 py-2">{item.sedes.length ? item.sedes.join(", ") : "Sin sede"}</td>
                  <td className="px-3 py-2">{item.obra}</td>
                  <td className="px-3 py-2">{item.estadoObra}</td>
                  <td className="px-3 py-2">{texto(Number(item.promedio))}</td>
                  <td className="px-3 py-2">
                    {item.desempeno}
                    {item.vecesRechazada > 0 && item.estado === "solicitud en revision" && (
                      <span className="ml-2 rounded-full bg-copper px-2 py-0.5 text-xs text-white">Volvió tras un rechazo</span>
                    )}
                  </td>
                </tr>
              ))}
              {visibles.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-3 py-8 text-center text-muted">No hay encuestas para este filtro.</td>
                </tr>
              )}
            </tbody>
          </table>
        </section>
      </main>
    </div>
  );
}
