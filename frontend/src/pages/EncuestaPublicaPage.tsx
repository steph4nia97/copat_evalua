import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState, type FormEvent } from "react";
import { useParams } from "react-router-dom";
import { api, ErrorApi } from "../api/client";

type Pregunta = { clave: string; etiqueta: string };

type Respuesta = {
  nombre: string;
  cargo: string;
  obra: string;
  estadoObra: string;
  comentarios: string | null;
  notas: Record<string, number | null>;
};

type Encuesta = {
  empresa: string;
  correo: string;
  telefono: string | null;
  periodoInicio: string;
  periodoFin: string;
  sedes: string[];
  abierta: boolean;
  motivoRechazo: string | null;
  preguntas: Pregunta[];
  respuesta: Respuesta | null;
};

const AYUDA: Record<string, string> = {
  gestion_administrativa: "Evalúe la gestión administrativa del servicio.",
  tiempos_entrega: "Indique si se cumplieron los plazos acordados.",
  estado_hormigon: "Evalúe la calidad del hormigón entregado.",
  flexibilidad: "Evalúe la flexibilidad ante cambios de programación.",
  atencion_comercial: "Valore la atención y el trato del equipo comercial.",
  respuesta_reclamos: "Valore la capacidad de respuesta ante inconvenientes.",
  entrega_informacion: "Evalúe la comunicación y la entrega de información.",
  competitividad_precio: "¿Cómo evalúa la relación entre el precio y la calidad?",
  capacitacion_tecnica: "Evalúe la capacitación técnica y el asesoramiento.",
  percepcion_general: "¿Cómo percibe el servicio en general?",
};

const ESTADOS_OBRA = ["Obra en construcción", "Obra finalizada"];
const SIN_NOTA = new Set(["flexibilidad", "respuesta_reclamos"]);

const campo =
  "mt-1 w-full rounded-lg border border-[#d5deef] bg-white px-3 py-2 text-base text-[#1e293b] outline-none focus:border-[#2457d6] focus:ring-2 focus:ring-[#2457d6]/20 disabled:bg-[#eef2f8] disabled:text-[#475569]";

function Nota({
  valor,
  seleccion,
  onElegir,
}: {
  valor: number;
  seleccion: number | null | undefined;
  onElegir: (nota: number) => void;
}) {
  const activo = seleccion === valor;
  return (
    <button
      type="button"
      aria-pressed={activo}
      aria-label={`Nota ${valor}`}
      onClick={() => onElegir(valor)}
      className={`h-8 w-8 rounded-full border text-sm font-semibold ${
        activo
          ? "border-[#2457d6] bg-[#2457d6] text-white"
          : "border-[#c9d4ea] bg-white text-[#2457d6] hover:border-[#2457d6]"
      }`}
    >
      {valor}
    </button>
  );
}

export function EncuestaPublicaPage() {
  const { token } = useParams();
  const queryClient = useQueryClient();
  const [nombre, setNombre] = useState("");
  const [cargo, setCargo] = useState("");
  const [obra, setObra] = useState("");
  const [estadoObra, setEstadoObra] = useState("");
  const [comentarios, setComentarios] = useState("");
  const [notas, setNotas] = useState<Record<string, number | null>>({});
  const [campos, setCampos] = useState<Record<string, string>>({});
  const [error, setError] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [precargada, setPrecargada] = useState(false);

  const encuesta = useQuery({
    queryKey: ["encuesta-publica", token],
    queryFn: () => api<Encuesta>(`/api/publico/encuestas/${token}`),
    enabled: Boolean(token),
  });

  useEffect(() => {
    void api("/api/auth/csrf").catch(() => undefined);
  }, []);

  useEffect(() => {
    const respuesta = encuesta.data?.respuesta;
    if (!respuesta || precargada) return;
    setNombre(respuesta.nombre);
    setCargo(respuesta.cargo);
    setObra(respuesta.obra);
    setEstadoObra(respuesta.estadoObra);
    setComentarios(respuesta.comentarios ?? "");
    setNotas(respuesta.notas);
    setPrecargada(true);
  }, [encuesta.data, precargada]);

  if (!token) {
    return (
      <main className="mx-auto flex min-h-screen max-w-xl items-center px-4">
        <section className="rounded-2xl border border-[#d5deef] bg-white p-8 shadow-card">
          <p className="text-sm font-semibold text-[#2457d6]">D-SIG-10-01</p>
          <h1 className="mt-2 font-serif text-3xl text-[#1e3a8a]">Encuesta de satisfacción</h1>
          <p className="mt-3 text-sm text-[#475569]">
            Cada encuesta tiene un enlace propio. Ábralo desde el mensaje que le envió Copat.
          </p>
        </section>
      </main>
    );
  }

  if (encuesta.isLoading) {
    return <p className="p-8 text-[#64748b]">Cargando la encuesta…</p>;
  }

  if (encuesta.isError || !encuesta.data) {
    const mensaje = encuesta.error instanceof ErrorApi ? encuesta.error.message : "Este enlace no es válido.";
    return (
      <main className="mx-auto flex min-h-screen max-w-xl items-center px-4">
        <p className="rounded-xl bg-white p-6 text-[#7a1d1d]" role="alert">
          {mensaje}
        </p>
      </main>
    );
  }

  const datos = encuesta.data;
  if (!datos.abierta) {
    return (
      <main className="min-h-screen bg-[#f4f7fd] px-4 py-16">
        <section className="mx-auto max-w-xl rounded-2xl border border-[#d5deef] bg-white p-8 text-center shadow-card">
          <h1 className="font-serif text-3xl text-[#1e3a8a]">Recibimos su encuesta</h1>
          <p className="mt-3 text-sm text-[#475569]">
            Gracias por evaluar el servicio de {datos.empresa}. Esta respuesta ya quedó registrada.
          </p>
        </section>
      </main>
    );
  }

  const mitad = Math.ceil(datos.preguntas.length / 2);
  const columnas = [datos.preguntas.slice(0, mitad), datos.preguntas.slice(mitad)];

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    setEnviando(true);
    setError("");
    setCampos({});
    try {
      await api(`/api/publico/encuestas/${token}`, {
        method: "POST",
        body: JSON.stringify({
          nombre,
          cargo,
          obra,
          estadoObra,
          comentarios,
          notas,
        }),
      });
      await queryClient.invalidateQueries({ queryKey: ["encuesta-publica", token] });
    } catch (err) {
      if (err instanceof ErrorApi) {
        setError(err.message);
        setCampos(err.campos);
      } else {
        setError("No se pudo enviar la encuesta.");
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main className="min-h-screen bg-[#f4f7fd] px-4 py-8 text-[#1e293b]">
      <form noValidate onSubmit={enviar} className="mx-auto max-w-5xl">
        <header>
          <h1 className="text-2xl font-bold text-[#1d4ed8] sm:text-3xl">
            Encuesta de Satisfacción del Cliente — D-SIG-10-01
          </h1>
          <p className="mt-1 text-sm text-[#64748b]">Tu opinión nos ayuda a mejorar nuestro servicio.</p>
          <p className="mt-1 text-xs text-[#64748b]">
            Período {datos.periodoInicio} – {datos.periodoFin}
            {datos.sedes.length > 0 ? ` · ${datos.sedes.join(", ")}` : ""}
          </p>
        </header>

        {datos.motivoRechazo && (
          <p className="mt-4 rounded-lg bg-[#fff4e8] px-4 py-3 text-sm text-[#9a3412]" role="status">
            Copat pidió corregir la encuesta: {datos.motivoRechazo}
          </p>
        )}

        <section className="mt-5 rounded-2xl border border-[#e2e8f4] bg-white p-5 shadow-sm">
          <h2 className="flex items-center gap-2 font-semibold text-[#1e3a8a]">
            <span className="grid h-7 w-7 place-items-center rounded-full bg-[#e8efff] text-[#2457d6]" aria-hidden>
              <Persona />
            </span>
            Datos generales
          </h2>
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            <label className="text-base font-semibold md:col-span-2">
              Empresa
              <input className={campo} value={datos.empresa} disabled readOnly />
            </label>
            <label className="text-base font-semibold">
              Nombre *
              <input className={campo} value={nombre} maxLength={120} onChange={(evento) => setNombre(evento.target.value)} />
              {campos.nombre && <Aviso texto={campos.nombre} />}
            </label>
            <label className="text-base font-semibold">
              Cargo *
              <input className={campo} value={cargo} maxLength={120} onChange={(evento) => setCargo(evento.target.value)} />
              {campos.cargo && <Aviso texto={campos.cargo} />}
            </label>
            <label className="text-base font-semibold md:col-span-2">
              Nombre de la obra *
              <input className={campo} value={obra} maxLength={200} onChange={(evento) => setObra(evento.target.value)} />
              {campos.obra && <Aviso texto={campos.obra} />}
            </label>
            <label className="text-base font-semibold md:col-span-2">
              ¿Cuál es el estado de la obra actualmente? *
              <select className={campo} value={estadoObra} onChange={(evento) => setEstadoObra(evento.target.value)}>
                <option value="">Seleccione</option>
                {ESTADOS_OBRA.map((opcion) => (
                  <option key={opcion} value={opcion}>
                    {opcion}
                  </option>
                ))}
              </select>
              {campos.estadoObra && <Aviso texto={campos.estadoObra} />}
            </label>
          </div>
        </section>

        <section className="mt-4 rounded-2xl border border-[#e2e8f4] bg-white p-5 shadow-sm">
          <h2 className="flex items-center gap-2 font-semibold text-[#1e3a8a]">
            <span className="grid h-7 w-7 place-items-center rounded-full bg-[#e8efff] text-[#2457d6]" aria-hidden>
              <Estrella />
            </span>
            Preguntas de la encuesta
          </h2>
          <ul className="mt-3 flex flex-wrap gap-2" aria-label="Escala de respuestas">
            {[
              ["1", "Muy insatisfecho"],
              ["2", "Insatisfecho"],
              ["3", "Indiferente"],
              ["4", "Satisfecho"],
              ["5", "Muy satisfecho"],
            ].map(([numero, etiqueta]) => (
              <li key={numero} className="flex items-center gap-2 rounded-lg border border-[#e2e8f4] bg-[#f8fafc] px-3 py-2">
                <span className="text-2xl font-semibold leading-none text-[#1e293b]">{numero}</span>
                <span className="text-sm font-semibold text-[#1e293b]">{etiqueta}</span>
              </li>
            ))}
          </ul>
          <div className="mt-4 grid gap-3 lg:grid-cols-2">
            {columnas.map((columna, indice) => (
              <div key={indice} className="space-y-3">
                {columna.map((pregunta) => (
                    <div key={pregunta.clave} className="rounded-xl border border-[#e6edf8] bg-[#f8fafc] p-3">
                      <p className="text-lg font-semibold text-[#1e293b]">{pregunta.etiqueta}</p>
                      <p className="mt-1 text-base text-[#475569]">{AYUDA[pregunta.clave]}</p>
                      <div className="mt-3 flex flex-wrap items-center justify-end gap-1.5" role="group" aria-label={pregunta.etiqueta}>
                        {SIN_NOTA.has(pregunta.clave) && (
                          <button
                            type="button"
                            aria-pressed={notas[pregunta.clave] === null}
                            onClick={() => setNotas((actual) => ({ ...actual, [pregunta.clave]: null }))}
                            className={`h-8 rounded-full border px-3 text-sm font-semibold ${
                              notas[pregunta.clave] === null
                                ? "border-[#1e293b] bg-[#1e293b] text-white"
                                : "border-[#c9d4ea] bg-white text-[#1e293b] hover:border-[#1e293b]"
                            }`}
                          >
                            No aplica
                          </button>
                        )}
                        {[1, 2, 3, 4, 5].map((valor) => (
                          <Nota
                            key={valor}
                            valor={valor}
                            seleccion={notas[pregunta.clave]}
                            onElegir={(nota) => setNotas((actual) => ({ ...actual, [pregunta.clave]: nota }))}
                          />
                        ))}
                      </div>
                      {campos[pregunta.clave] && <Aviso texto={campos[pregunta.clave]} />}
                    </div>
                ))}
              </div>
            ))}
          </div>
        </section>

        <section className="mt-4 rounded-2xl border border-[#e2e8f4] bg-white p-5 shadow-sm">
          <h2 className="flex items-center gap-2 font-semibold text-[#1e3a8a]">
            <span className="grid h-7 w-7 place-items-center rounded-full bg-[#e8efff] text-[#2457d6]" aria-hidden>
              <Dialogo />
            </span>
            Comentarios adicionales
          </h2>
          <textarea
            value={comentarios}
            maxLength={1000}
            rows={3}
            placeholder="Cuéntenos su experiencia..."
            onChange={(evento) => setComentarios(evento.target.value)}
            className={`${campo} resize-y`}
          />
          {campos.comentarios && <Aviso texto={campos.comentarios} />}
        </section>

        {error && (
          <p className="mt-4 text-sm text-[#7a1d1d]" role="alert">
            {error}
          </p>
        )}

        <div className="mt-4 flex justify-end">
          <button
            type="submit"
            disabled={enviando}
            className="inline-flex items-center gap-2 rounded-lg bg-[#2457d6] px-5 py-2.5 font-semibold text-white shadow-md disabled:opacity-60"
          >
            <Avion />
            {enviando ? "Enviando…" : "Enviar encuesta"}
          </button>
        </div>
      </form>
    </main>
  );
}

function Aviso({ texto }: { texto: string }) {
  return <span className="mt-1 block text-xs text-[#7a1d1d]">{texto}</span>;
}

function Persona() {
  return (
    <svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth="2">
      <circle cx="12" cy="8" r="3" />
      <path d="M5 19c1.5-3 4-4.5 7-4.5S17.5 16 19 19" />
    </svg>
  );
}

function Estrella() {
  return (
    <svg viewBox="0 0 24 24" className="h-4 w-4" fill="currentColor">
      <path d="m12 3 2.4 5.4 5.9.6-4.4 3.9 1.3 5.7L12 15.8 6.8 18.6l1.3-5.7L3.7 9l5.9-.6L12 3Z" />
    </svg>
  );
}

function Dialogo() {
  return (
    <svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M5 6h14v9H8l-3 3V6Z" />
    </svg>
  );
}

function Avion() {
  return (
    <svg viewBox="0 0 24 24" className="h-4 w-4" fill="currentColor">
      <path d="M3 11.5 21 4l-7.2 16.2-2.3-6.2L3 11.5Z" />
    </svg>
  );
}
