import { useState } from "react";
import { enmascararFecha, fechaValida } from "../domain/reglas";
import { Calendario } from "./Calendario";

type Props = {
  id: string;
  label: string;
  value: string;
  error?: string;
  onChange: (valor: string) => void;
  onBlur: () => void;
};

export function CampoFecha({ id, label, value, error, onChange, onBlur }: Props) {
  const hoy = new Date();
  const inicial = fechaValida(value) ? partir(value) : { anio: hoy.getFullYear(), mes: hoy.getMonth() };
  const [abierto, setAbierto] = useState(false);
  const [anio, setAnio] = useState(inicial.anio);
  const [mes, setMes] = useState(inicial.mes);

  function abrir() {
    const actual = fechaValida(value) ? partir(value) : { anio: hoy.getFullYear(), mes: hoy.getMonth() };
    setAnio(actual.anio);
    setMes(actual.mes);
    setAbierto(true);
  }

  return (
    <div className="relative">
      <label htmlFor={id} className="mb-1 block text-sm font-semibold">
        {label}
      </label>
      <div className="flex">
        <input
          id={id}
          inputMode="numeric"
          placeholder="dd/mm/aaaa"
          autoComplete="off"
          value={value}
          aria-invalid={Boolean(error)}
          onChange={(evento) => onChange(enmascararFecha(evento.target.value))}
          onBlur={onBlur}
          className="w-full rounded-l-md border border-line bg-white px-3 py-2 outline-none ring-copper focus:ring-2"
        />
        <button
          type="button"
          className="rounded-r-md border border-l-0 border-line bg-pine-soft px-3 text-pine"
          aria-label={`Abrir calendario de ${label.toLowerCase()}`}
          onClick={() => (abierto ? setAbierto(false) : abrir())}
        >
          <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true">
            <path
              fill="currentColor"
              d="M7 2h2v2h6V2h2v2h3v18H4V4h3V2zm12 8H5v10h14V10zM5 8h14V6H5v2z"
            />
          </svg>
        </button>
      </div>
      {abierto && (
        <Calendario
          anio={anio}
          mes={mes}
          seleccionado={value}
          onCambiarMes={(nuevoAnio, nuevoMes) => {
            setAnio(nuevoAnio);
            setMes(nuevoMes);
          }}
          onElegir={(fecha) => {
            onChange(fecha);
            setAbierto(false);
          }}
          onCerrar={() => setAbierto(false)}
        />
      )}
      {error && <p className="mt-1 text-sm text-copper-dark">{error}</p>}
    </div>
  );
}

function partir(fecha: string) {
  const [, mes, anio] = fecha.split("/").map(Number);
  return { anio, mes: mes - 1 };
}
