import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { api, descargar, ErrorApi } from "../api/client";
import { useSesion } from "../auth/sesion";
import { Encabezado } from "../components/Encabezado";

type Movimiento = {
  accion: string;
  trabajador: string;
  fecha: string;
  hora: string;
};

type Nota = {
  etiqueta: string;
  valor: number | null;
};

type NotaSeguimiento = {
  id: number;
  nota: string;
  estado: string;
  usuario: string;
  fecha: string;
  hora: string;
};

const SEGUIMIENTO = ["Recibida", "En análisis", "En gestión", "Cerrada"];

type Revision = {
  id: number;
  nombreEmpresa: string;
  rut: string;
  correo: string;
  telefono: string | null;
  periodoInicio: string;
  periodoFin: string;
  plantilla: string;
  sedes: string[];
  descripcionSolicitud: string | null;
  creadoPor: string;
  movimientos: Movimiento[];
  encuestado: string;
  empresaEncuesta: string;
  cargo: string;
  obra: string;
  estadoObra: string;
  comentarios: string | null;
  notas: Nota[];
  promedio: number;
  desempeno: string;
  escala: string;
  descripcion: string | null;
  tieneEvidencia: boolean;
  tienePdf: boolean;
  seguimiento: string | null;
  notasSeguimiento: NotaSeguimiento[];
};

const ESTADOS_OBRA = ["Obra finalizada", "Obra en construcción"];
const DESEMPENOS = ["Destacado", "Satisfactorio", "Regular", "Insuficiente"];

function notas(valor: number) {
  return valor.toLocaleString("es-CL", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function clasificar(promedio: number) {
  if (promedio >= 4.5) return "Destacado";
  if (promedio >= 3.5) return "Satisfactorio";
  if (promedio >= 2.5) return "Regular";
  return "Insuficiente";
}

function promedioConjunto(valores: number[]) {
  const centavos = valores.reduce((suma, valor) => suma + Math.round(valor * 100), 0);
  return Math.round(centavos / valores.length) / 100;
}

function Tarjeta({ item }: { item: Revision }) {
  const queryClient = useQueryClient();
  const [estado, setEstado] = useState(item.seguimiento ?? "Recibida");
  const [nota, setNota] = useState("");
  const [archivo, setArchivo] = useState<File | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [aviso, setAviso] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    setEstado(item.seguimiento ?? "Recibida");
  }, [item.seguimiento]);

  async function guardar() {
    const texto = nota.trim();
    if (!texto) {
      setError("Escriba una nota de seguimiento.");
      return;
    }
    if (texto.length > 500) {
      setError("La nota admite como máximo 500 caracteres.");
      return;
    }
    setEnviando(true);
    setError("");
    setAviso("");
    const datos = new FormData();
    datos.set("descripcion", texto);
    datos.set("estado", estado);
    if (archivo) {
      datos.set("evidencia", archivo);
    }
    try {
      await api(`/api/departamento3/solicitudes/${item.id}/revision`, { method: "POST", body: datos });
      setNota("");
      setArchivo(null);
      setAviso("Nota agregada.");
      await queryClient.invalidateQueries({ queryKey: ["d3-solicitudes"] });
    } catch (err) {
      setError(err instanceof ErrorApi ? err.message : "No se pudo guardar la revisión.");
    } finally {
      setEnviando(false);
    }
  }

  return (
    <article className="rounded-xl border border-line bg-card p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="font-serif text-2xl">{item.nombreEmpresa}</h2>
          <p
            className={`mt-1 inline-block rounded-full px-2 py-0.5 text-xs font-semibold ${
              item.seguimiento === "Cerrada" ? "bg-teal-100 text-teal-900" : "bg-amber-100 text-amber-950"
            }`}
          >
            {item.seguimiento ?? "Recibida"}
          </p>
          <p className="text-sm text-muted">
            {item.rut} · {item.correo} · {item.telefono ?? "—"}
          </p>
          <p className="text-sm text-muted">
            Sede: {item.sedes.length ? item.sedes.join(", ") : "Sin sede"}
          </p>
          <p className="text-sm text-muted">
            {item.plantilla} · {item.periodoInicio} – {item.periodoFin}
          </p>
          {item.descripcionSolicitud && <p className="mt-1 text-sm">{item.descripcionSolicitud}</p>}
          <p className="mt-1 text-sm">Solicitud creada: {item.creadoPor}</p>
        </div>
        <div className="flex gap-2">
          {item.tienePdf && (
            <button
              type="button"
              className="rounded-md border border-line px-3 py-1 text-sm"
              onClick={() => void descargar(`/api/departamento3/solicitudes/${item.id}/pdf`, "D-SIG-10-01.pdf")}
            >
              Descargar PDF recibido
            </button>
          )}
          {item.tieneEvidencia && (
            <button
              type="button"
              className="rounded-md border border-line px-3 py-1 text-sm"
              onClick={() => void descargar(`/api/departamento3/solicitudes/${item.id}/evidencia`, "evidencia")}
            >
              Descargar evidencia
            </button>
          )}
        </div>
      </div>

      <section className="mt-4 rounded-xl border border-line bg-paper p-4">
        <h3 className="text-sm font-semibold">Notas de seguimiento</h3>
        {item.notasSeguimiento.length === 0 && (
          <p className="mt-2 text-sm text-muted">Aún no hay notas.</p>
        )}
        <ul className="mt-2 space-y-3">
          {[...item.notasSeguimiento].reverse().map((apunte) => (
            <li key={apunte.id} className="rounded-md border border-line bg-card px-3 py-2 text-sm">
              <p className="text-xs text-muted">
                {apunte.estado} · {apunte.usuario} · {apunte.fecha} {apunte.hora}
              </p>
              <p className="mt-1">{apunte.nota}</p>
            </li>
          ))}
        </ul>
      </section>

      <section className="mt-4 grid gap-2 text-sm sm:grid-cols-2">
        <p>
          <span className="text-muted">Encuestado: </span>
          {item.encuestado}
        </p>
        <p>
          <span className="text-muted">Empresa en la encuesta: </span>
          {item.empresaEncuesta}
        </p>
        <p>
          <span className="text-muted">Cargo: </span>
          {item.cargo}
        </p>
        <p>
          <span className="text-muted">Obra: </span>
          {item.obra}
        </p>
        <p>
          <span className="text-muted">Estado de la obra: </span>
          {item.estadoObra}
        </p>
        <p className="sm:col-span-2">
          <span className="text-muted">Comentarios del cliente: </span>
          {item.comentarios ?? "Sin comentarios"}
        </p>
      </section>

      <section className="mt-4 rounded-xl border border-pine/20 bg-pine-soft p-4" aria-label="Resultado de la evaluación">
        <p className="text-xs font-semibold uppercase tracking-[0.14em] text-pine">Resultado de la evaluación</p>
        <p className="mt-1 font-serif text-3xl text-pine">{item.desempeno}</p>
        <p className="text-sm">Promedio {notas(Number(item.promedio))} de 5</p>
        <dl className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-5">
          {item.notas.map((nota) => (
            <div key={nota.etiqueta} className="rounded-md bg-card px-3 py-2">
              <dt className="text-xs text-muted">{nota.etiqueta}</dt>
              <dd className="font-semibold">{nota.valor == null ? "No aplica" : nota.valor}</dd>
            </div>
          ))}
        </dl>
        <p className="mt-3 text-xs text-muted">{item.escala}</p>
      </section>

      <label htmlFor={`estado-${item.id}`} className="mt-4 block text-sm font-semibold">
        Estado del seguimiento
      </label>
      <select
        id={`estado-${item.id}`}
        value={estado}
        onChange={(evento) => setEstado(evento.target.value)}
        className="mt-1 rounded-md border border-line bg-card px-3 py-2"
      >
        {SEGUIMIENTO.map((opcion) => (
          <option key={opcion} value={opcion}>
            {opcion}
          </option>
        ))}
      </select>
      <p className="mt-1 text-xs text-muted">
        Departamento 1 ve Respondida cuando llega la encuesta. Si el seguimiento queda Cerrada, lo ve Cerrado.
      </p>
      <label htmlFor={`nota-${item.id}`} className="mt-3 block text-sm font-semibold">
        Nueva nota
      </label>
      <textarea
        id={`nota-${item.id}`}
        value={nota}
        maxLength={500}
        rows={4}
        onChange={(evento) => setNota(evento.target.value)}
        className="mt-1 w-full rounded-md border border-line px-3 py-2 outline-none ring-copper focus:ring-2"
      />
      <p className="mt-1 text-xs text-muted">{nota.trim().length} / 500</p>

      <label htmlFor={`evidencia-${item.id}`} className="mt-3 block text-sm font-semibold">
        Evidencia adicional (opcional)
      </label>
      <input
        id={`evidencia-${item.id}`}
        type="file"
        accept="application/pdf,.pdf,image/png,image/jpeg,.png,.jpg,.jpeg"
        className="mt-1 block text-sm"
        onChange={(evento) => setArchivo(evento.target.files?.[0] ?? null)}
      />

      {error && (
        <p className="mt-3 text-sm text-copper-dark" role="alert">
          {error}
        </p>
      )}
      {aviso && <p className="mt-3 text-sm text-pine">{aviso}</p>}
      <button
        type="button"
        disabled={enviando}
        className="mt-4 rounded-md bg-pine px-4 py-2 font-semibold text-white disabled:opacity-60"
        onClick={() => void guardar()}
      >
        Agregar nota
      </button>
    </article>
  );
}

export function Departamento3Page() {
  const { usuario, listo } = useSesion();
  const [estadoObra, setEstadoObra] = useState("");
  const [desempeno, setDesempeno] = useState("");
  const [sede, setSede] = useState("");
  const [texto, setTexto] = useState("");
  const sedes = useQuery({
    queryKey: ["sedes"],
    queryFn: () => api<string[]>("/api/departamento1/sedes"),
    enabled: usuario?.departamentoId === 3,
  });
  const solicitudes = useQuery({
    queryKey: ["d3-solicitudes"],
    queryFn: () => api<Revision[]>("/api/departamento3/solicitudes"),
    enabled: usuario?.departamentoId === 3,
    refetchInterval: 15000,
    refetchIntervalInBackground: true,
  });

  const visibles = useMemo(() => {
    const consulta = texto.trim().toLowerCase();
    return (solicitudes.data ?? []).filter((item) => {
      if (estadoObra && item.estadoObra !== estadoObra) return false;
      if (sede && !item.sedes.includes(sede)) return false;
      if (desempeno && item.desempeno !== desempeno) return false;
      if (!consulta) return true;
      const contenido = [item.encuestado, item.empresaEncuesta, item.obra, item.nombreEmpresa, item.rut]
        .join(" ")
        .toLowerCase();
      return contenido.includes(consulta);
    });
  }, [solicitudes.data, estadoObra, sede, desempeno, texto]);

  const conjunto = useMemo(() => {
    if (visibles.length === 0) return null;
    const promedio = promedioConjunto(visibles.map((item) => Number(item.promedio)));
    return { cantidad: visibles.length, promedio, desempeno: clasificar(promedio) };
  }, [visibles]);

  if (!listo) {
    return <p className="p-8 text-muted">Cargando…</p>;
  }
  if (!usuario) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="min-h-screen">
      <Encabezado />
      <main className="mx-auto max-w-6xl px-4 py-6">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h1 className="font-serif text-3xl">Seguimiento</h1>
            <p className="mt-1 max-w-3xl text-sm text-muted">
              Después de recibir la encuesta, el seguimiento avanza con notas hasta cerrarlo.
              Departamento 1 ve Enviada a cliente, luego Respondida, y Cerrado cuando se cierra.
            </p>
          </div>
          <Link to="/indicadores" className="rounded-md bg-pine px-4 py-2 text-sm font-semibold text-white">
            Ver métricas
          </Link>
        </div>

        <div className="mt-5 grid gap-3 rounded-xl border border-line bg-card p-4 sm:grid-cols-2 lg:grid-cols-4">
          <label className="text-sm font-semibold" htmlFor="filtro-obra">
            Estado de la obra
            <select
              id="filtro-obra"
              value={estadoObra}
              onChange={(evento) => setEstadoObra(evento.target.value)}
              className="mt-1 block w-full rounded-md border border-line bg-card px-3 py-2 font-normal"
            >
              <option value="">Todas</option>
              {ESTADOS_OBRA.map((opcion) => (
                <option key={opcion} value={opcion}>
                  {opcion}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold" htmlFor="filtro-sede">
            Sede de despacho
            <select
              id="filtro-sede"
              value={sede}
              onChange={(evento) => setSede(evento.target.value)}
              className="mt-1 block w-full rounded-md border border-line bg-card px-3 py-2 font-normal"
            >
              <option value="">Todas</option>
              {(sedes.data ?? []).map((opcion) => (
                <option key={opcion} value={opcion}>
                  {opcion}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold" htmlFor="filtro-desempeno">
            Evaluación
            <select
              id="filtro-desempeno"
              value={desempeno}
              onChange={(evento) => setDesempeno(evento.target.value)}
              className="mt-1 block w-full rounded-md border border-line bg-card px-3 py-2 font-normal"
            >
              <option value="">Todas</option>
              {DESEMPENOS.map((opcion) => (
                <option key={opcion} value={opcion}>
                  {opcion}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold" htmlFor="filtro-texto">
            Empresa, obra o nombre
            <input
              id="filtro-texto"
              value={texto}
              onChange={(evento) => setTexto(evento.target.value)}
              className="mt-1 block w-full rounded-md border border-line px-3 py-2 font-normal"
            />
          </label>
        </div>

        {conjunto && (
          <section className="mt-4 rounded-xl border border-pine/20 bg-pine-soft p-4" aria-label="Resultado del filtro">
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-pine">Resultado del filtro</p>
            <p className="mt-1 font-serif text-3xl text-pine">{conjunto.desempeno}</p>
            <p className="text-sm">
              {conjunto.cantidad} {conjunto.cantidad === 1 ? "encuesta" : "encuestas"} · promedio{" "}
              {notas(conjunto.promedio)} de 5
            </p>
          </section>
        )}

        <div className="mt-5 flex flex-col gap-4">
          {visibles.map((item) => (
            <Tarjeta key={item.id} item={item} />
          ))}
          {solicitudes.data && visibles.length === 0 && (
            <p className="rounded-xl border border-line bg-card px-3 py-8 text-center text-sm text-muted">
              {solicitudes.data.length === 0
                ? "No hay solicitudes en revisión."
                : "Ninguna encuesta coincide con el filtro."}
            </p>
          )}
        </div>
      </main>
    </div>
  );
}
