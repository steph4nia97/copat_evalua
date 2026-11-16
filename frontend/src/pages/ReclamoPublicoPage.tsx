import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import { api, ErrorApi } from "../api/client";

const TIPOS = [
  "Calidad del hormigón",
  "Retraso en la entrega",
  "Atención del personal",
  "Facturación / documentación",
  "Otro",
] as const;

const HORARIOS = ["Mañana, 08:00 a 12:00", "Tarde, 12:00 a 17:00", "Cualquier horario"];

const campo =
  "mt-1 w-full rounded-lg border border-[#d7dee8] bg-white px-3 py-2 text-sm outline-none focus:border-[#f26522] focus:ring-2 focus:ring-[#f26522]/20";

export function ReclamoPublicoPage() {
  const [nombre, setNombre] = useState("");
  const [telefono, setTelefono] = useState("");
  const [obra, setObra] = useState("");
  const [fechaServicio, setFechaServicio] = useState("");
  const [empresa, setEmpresa] = useState("");
  const [correo, setCorreo] = useState("");
  const [ubicacion, setUbicacion] = useState("");
  const [guiaDespacho, setGuiaDespacho] = useState("");
  const [tipo, setTipo] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [preferenciaContacto, setPreferenciaContacto] = useState("");
  const [horario, setHorario] = useState("");
  const [comentarios, setComentarios] = useState("");
  const [campos, setCampos] = useState<Record<string, string>>({});
  const [error, setError] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [listo, setListo] = useState(false);

  useEffect(() => {
    void api("/api/auth/csrf").catch(() => undefined);
  }, []);

  function limpiar() {
    setNombre("");
    setTelefono("");
    setObra("");
    setFechaServicio("");
    setEmpresa("");
    setCorreo("");
    setUbicacion("");
    setGuiaDespacho("");
    setTipo("");
    setDescripcion("");
    setPreferenciaContacto("");
    setHorario("");
    setComentarios("");
    setCampos({});
    setError("");
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    setEnviando(true);
    setError("");
    setCampos({});
    try {
      await api("/api/publico/reclamos", {
        method: "POST",
        body: JSON.stringify({
          nombre,
          telefono,
          obra,
          fechaServicio,
          empresa,
          correo,
          ubicacion,
          guiaDespacho,
          tipo,
          descripcion,
          preferenciaContacto,
          horario,
          comentarios,
        }),
      });
      setListo(true);
    } catch (err) {
      if (err instanceof ErrorApi) {
        setError(err.message);
        setCampos(err.campos);
      } else {
        setError("No se pudo enviar el reclamo.");
      }
    } finally {
      setEnviando(false);
    }
  }

  if (listo) {
    return (
      <main className="flex min-h-screen items-center bg-[#f4f7fb] px-4">
        <section className="mx-auto max-w-xl rounded-2xl bg-white p-8 text-center shadow-card">
          <p className="text-sm font-semibold text-[#f26522]">D-SIG-09-01</p>
          <h1 className="mt-2 text-3xl font-semibold text-[#0b1f33]">Recibimos su reclamo</h1>
          <p className="mt-3 text-sm text-[#526070]">
            El equipo de Copat lo revisará y le responderá dentro de 24 horas hábiles, al teléfono o al correo que
            indicó.
          </p>
        </section>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-[#f3f6fa] text-[#1c2834]">
      <header className="bg-[#0b1f33] text-white">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center gap-4 px-4 py-4">
          <div className="flex items-center gap-3">
            <span className="grid h-12 w-12 place-items-center rounded-lg bg-[#f26522]" aria-hidden>
              <Mixer />
            </span>
            <div className="leading-tight">
              <p className="text-xl font-bold tracking-wide">COPAT</p>
              <p className="text-sm font-semibold text-[#f26522]">Hormigón</p>
            </div>
          </div>
          <div className="mr-auto sm:ml-6">
            <h1 className="text-xl font-semibold sm:text-2xl">Formulario de Reclamo de Clientes</h1>
            <p className="text-sm text-white/70">D-SIG-09-01</p>
          </div>
          <div
            className="hidden h-16 w-44 rounded-md bg-gradient-to-br from-slate-300 via-slate-500 to-[#0b1f33] md:block"
            aria-hidden
          />
        </div>
      </header>

      <form noValidate onSubmit={enviar} className="mx-auto max-w-5xl px-4 py-6">
        <p className="rounded-xl border border-[#d7e3f2] bg-[#f3f8fd] px-4 py-3 text-sm text-[#334155]">
          En Copat valoramos tu opinión y nos comprometemos a atender su reclamo con seriedad. Complete este formulario
          para que nuestro equipo lo gestione. Le responderemos dentro de 24 horas hábiles.
        </p>

        <ol className="mt-5 grid gap-3 sm:grid-cols-3">
          {[
            ["1", "Datos del reclamo"],
            ["2", "Detalle"],
            ["3", "Contacto y envío"],
          ].map(([numero, etiqueta], indice) => (
            <li key={etiqueta} className="flex items-center gap-3 rounded-xl bg-white px-3 py-2 shadow-sm">
              <span
                className={`grid h-8 w-8 place-items-center rounded-full text-sm font-bold ${
                  indice === 0 ? "bg-[#2457d6] text-white" : "bg-[#e8eef6] text-[#64748b]"
                }`}
              >
                {numero}
              </span>
              <span className={indice === 0 ? "font-semibold" : "text-[#64748b]"}>{etiqueta}</span>
            </li>
          ))}
        </ol>

        <section className="mt-4 rounded-2xl border border-[#e4ebf3] bg-white p-5 shadow-sm">
          <h2 className="font-semibold">1. Datos generales</h2>
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            <Campo etiqueta="Nombre completo *" error={campos.nombre}>
              <input className={campo} value={nombre} maxLength={120} placeholder="Ingrese su nombre" onChange={(evento) => setNombre(evento.target.value)} />
            </Campo>
            <Campo etiqueta="Empresa / razón social *" error={campos.empresa}>
              <input className={campo} value={empresa} maxLength={150} placeholder="Ingrese la empresa" onChange={(evento) => setEmpresa(evento.target.value)} />
            </Campo>
            <Campo etiqueta="Teléfono de contacto *" error={campos.telefono}>
              <input className={campo} value={telefono} inputMode="tel" placeholder="+56 9 0000 0000" onChange={(evento) => setTelefono(evento.target.value)} />
            </Campo>
            <Campo etiqueta="Correo electrónico *" error={campos.correo}>
              <input className={campo} type="email" value={correo} placeholder="ejemplo@empresa.cl" onChange={(evento) => setCorreo(evento.target.value)} />
            </Campo>
            <Campo etiqueta="Nombre de la obra *" error={campos.obra}>
              <input className={campo} value={obra} maxLength={200} placeholder="Ej: Edificio Mirador" onChange={(evento) => setObra(evento.target.value)} />
            </Campo>
            <Campo etiqueta="Ubicación (dirección, comuna, ciudad) *" error={campos.ubicacion}>
              <input className={campo} value={ubicacion} maxLength={200} placeholder="Ej: Av. Industrial 123, Antofagasta" onChange={(evento) => setUbicacion(evento.target.value)} />
            </Campo>
            <Campo etiqueta="Fecha del servicio *" error={campos.fechaServicio}>
              <input className={campo} value={fechaServicio} placeholder="dd/mm/aaaa" onChange={(evento) => setFechaServicio(evento.target.value)} />
            </Campo>
            <Campo etiqueta="Guía de despacho / orden (si aplica)" error={campos.guiaDespacho}>
              <input className={campo} value={guiaDespacho} maxLength={40} placeholder="Ej: GD-123456" onChange={(evento) => setGuiaDespacho(evento.target.value)} />
            </Campo>
          </div>
        </section>

        <section className="mt-4 rounded-2xl border border-[#e4ebf3] bg-white p-5 shadow-sm">
          <h2 className="font-semibold">2. Tipo de reclamo</h2>
          <p className="mt-1 text-sm text-[#64748b]">Seleccione el motivo de su reclamo *</p>
          <div className="mt-3 grid gap-2 sm:grid-cols-2 lg:grid-cols-5">
            {TIPOS.map((opcion) => {
              const activo = tipo === opcion;
              return (
                <button
                  key={opcion}
                  type="button"
                  aria-pressed={activo}
                  onClick={() => setTipo(opcion)}
                  className={`rounded-xl border px-3 py-3 text-left text-sm font-semibold ${
                    activo ? "border-[#f26522] bg-[#fff4ee] text-[#9a3412]" : "border-[#e4ebf3] bg-white"
                  }`}
                >
                  {opcion}
                </button>
              );
            })}
          </div>
          {campos.tipo && <p className="mt-2 text-xs text-[#7a1d1d]">{campos.tipo}</p>}
        </section>

        <section className="mt-4 rounded-2xl border border-[#e4ebf3] bg-white p-5 shadow-sm">
          <h2 className="font-semibold">3. Descripción del reclamo</h2>
          <Campo etiqueta="Describa su reclamo *" error={campos.descripcion}>
            <textarea
              className={`${campo} resize-y`}
              rows={4}
              maxLength={1000}
              value={descripcion}
              placeholder="Explique en detalle su reclamo..."
              onChange={(evento) => setDescripcion(evento.target.value)}
            />
            <span className="mt-1 block text-right text-xs text-[#94a3b8]">{descripcion.length} / 1.000</span>
          </Campo>
        </section>

        <section className="mt-4 rounded-2xl border border-[#e4ebf3] bg-white p-5 shadow-sm">
          <h2 className="font-semibold">4. Contacto y comentarios</h2>
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            <fieldset>
              <legend className="text-sm font-semibold">¿Cómo prefiere que lo contactemos? *</legend>
              <div className="mt-2 flex gap-2">
                {["Teléfono", "Mail"].map((opcion) => {
                  const activo = preferenciaContacto === opcion;
                  return (
                    <button
                      key={opcion}
                      type="button"
                      aria-pressed={activo}
                      onClick={() => setPreferenciaContacto(opcion)}
                      className={`rounded-lg border px-4 py-2 text-sm font-semibold ${
                        activo ? "border-[#2457d6] bg-[#eef3ff] text-[#1d4ed8]" : "border-[#e4ebf3]"
                      }`}
                    >
                      {opcion}
                    </button>
                  );
                })}
              </div>
              {campos.preferenciaContacto && <p className="mt-1 text-xs text-[#7a1d1d]">{campos.preferenciaContacto}</p>}
            </fieldset>
            <Campo etiqueta="Horario preferido para contacto" error={campos.horario}>
              <select className={campo} value={horario} onChange={(evento) => setHorario(evento.target.value)}>
                <option value="">Seleccione un horario</option>
                {HORARIOS.map((opcion) => (
                  <option key={opcion} value={opcion}>
                    {opcion}
                  </option>
                ))}
              </select>
            </Campo>
          </div>
          <div className="mt-4">
            <Campo etiqueta="Comentarios adicionales" error={campos.comentarios}>
              <textarea
                className={`${campo} resize-y`}
                rows={3}
                maxLength={500}
                value={comentarios}
                placeholder="Agregue comentarios o antecedentes que considere relevantes (opcional)..."
                onChange={(evento) => setComentarios(evento.target.value)}
              />
              <span className="mt-1 block text-right text-xs text-[#94a3b8]">{comentarios.length} / 500</span>
            </Campo>
          </div>
        </section>

        {error && (
          <p className="mt-4 text-sm text-[#7a1d1d]" role="alert">
            {error}
          </p>
        )}

        <div className="mt-4 flex justify-end gap-3">
          <button type="button" onClick={limpiar} className="rounded-lg border border-[#d7dee8] bg-white px-4 py-2 text-sm font-semibold">
            Cancelar
          </button>
          <button
            type="submit"
            disabled={enviando}
            className="inline-flex items-center gap-2 rounded-lg bg-[#f26522] px-5 py-2.5 font-semibold text-white disabled:opacity-60"
          >
            <Avion />
            {enviando ? "Enviando…" : "Enviar reclamo"}
          </button>
        </div>
      </form>
    </main>
  );
}

function Campo({
  etiqueta,
  error,
  children,
}: {
  etiqueta: string;
  error?: string;
  children: ReactNode;
}) {
  return (
    <label className="block text-sm font-semibold">
      {etiqueta}
      {children}
      {error && <span className="mt-1 block text-xs font-normal text-[#7a1d1d]">{error}</span>}
    </label>
  );
}

function Mixer() {
  return (
    <svg viewBox="0 0 24 24" className="h-7 w-7 text-white" fill="currentColor" aria-hidden>
      <path d="M3 16h10l2-5h4l2 3v3h-1.2a2.2 2.2 0 0 1-4.1 0H9.3a2.2 2.2 0 0 1-4.1 0H3v-1Zm2.2 2.2a1.2 1.2 0 1 0 0-2.4 1.2 1.2 0 0 0 0 2.4Zm10.5 0a1.2 1.2 0 1 0 0-2.4 1.2 1.2 0 0 0 0 2.4ZM14 8l2 3h-3l1-3Zm-1-4h2v3h-2V4Z" />
    </svg>
  );
}

function Avion() {
  return (
    <svg viewBox="0 0 24 24" className="h-4 w-4" fill="currentColor" aria-hidden>
      <path d="M3 11.5 21 4l-7.2 16.2-2.3-6.2L3 11.5Z" />
    </svg>
  );
}
