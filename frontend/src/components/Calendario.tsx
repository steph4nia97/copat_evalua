import { useEffect, useRef } from "react";
import { compararFechas, fechaValida } from "../domain/reglas";

const MESES = [
  "enero",
  "febrero",
  "marzo",
  "abril",
  "mayo",
  "junio",
  "julio",
  "agosto",
  "septiembre",
  "octubre",
  "noviembre",
  "diciembre",
];

type Props = {
  anio: number;
  mes: number;
  seleccionado: string;
  hasta?: string;
  acento?: "pine" | "copat";
  onCambiarMes: (anio: number, mes: number) => void;
  onElegir: (fecha: string) => void;
  onCerrar: () => void;
};

export function Calendario({ anio, mes, seleccionado, hasta, acento = "pine", onCambiarMes, onElegir, onCerrar }: Props) {
  const ref = useRef<HTMLDivElement>(null);
  const primero = new Date(anio, mes, 1);
  const desplazamiento = (primero.getDay() + 6) % 7;
  const dias = new Date(anio, mes + 1, 0).getDate();
  const celdas = [...Array.from({ length: desplazamiento }, () => null), ...Array.from({ length: dias }, (_, i) => i + 1)];

  useEffect(() => {
    function cerrar(evento: MouseEvent) {
      if (ref.current && !ref.current.contains(evento.target as Node)) {
        onCerrar();
      }
    }
    function tecla(evento: KeyboardEvent) {
      if (evento.key === "Escape") {
        onCerrar();
      }
    }
    document.addEventListener("mousedown", cerrar);
    document.addEventListener("keydown", tecla);
    return () => {
      document.removeEventListener("mousedown", cerrar);
      document.removeEventListener("keydown", tecla);
    };
  }, [onCerrar]);

  function mover(delta: number) {
    const siguiente = new Date(anio, mes + delta, 1);
    onCambiarMes(siguiente.getFullYear(), siguiente.getMonth());
  }

  const limite = hasta && fechaValida(hasta) ? hasta : undefined;
  const siguienteBloqueado = limite != null && mesSiguienteSupera(anio, mes, limite);

  return (
    <div
      ref={ref}
      className="absolute right-0 z-30 mt-2 w-64 rounded-lg border border-line bg-card p-3 shadow-card"
    >
      <div className="mb-2 flex items-center justify-between gap-2">
        <button type="button" className={`rounded px-2 py-1 ${acento === "copat" ? "text-[#4D8AC2] hover:bg-[#E8F2FA]" : "text-pine hover:bg-pine-soft"}`} onClick={() => mover(-1)} aria-label="Mes anterior">
          ‹
        </button>
        <p className={`text-sm font-semibold capitalize ${acento === "copat" ? "text-[#2F6A9E]" : ""}`}>
          {MESES[mes]} {anio}
        </p>
        <button
          type="button"
          className={`rounded px-2 py-1 disabled:text-muted disabled:hover:bg-transparent ${acento === "copat" ? "text-[#4D8AC2] hover:bg-[#E8F2FA]" : "text-pine hover:bg-pine-soft"}`}
          onClick={() => mover(1)}
          disabled={siguienteBloqueado}
          aria-label="Mes siguiente"
        >
          ›
        </button>
      </div>
      <div className={`grid grid-cols-7 gap-1 text-center text-xs ${acento === "copat" ? "text-[#4D8AC2]" : "text-muted"}`}>
        {["lu", "ma", "mi", "ju", "vi", "sá", "do"].map((dia) => (
          <span key={dia} className="py-1">
            {dia}
          </span>
        ))}
        {celdas.map((dia, indice) => {
          if (!dia) {
            return <span key={`vacio-${indice}`} />;
          }
          const fecha = `${String(dia).padStart(2, "0")}/${String(mes + 1).padStart(2, "0")}/${anio}`;
          const activo = fecha === seleccionado;
          const bloqueado = limite != null && compararFechas(fecha, limite) > 0;
          return (
            <button
              key={fecha}
              type="button"
              disabled={bloqueado}
              className={`rounded py-1 text-sm ${
                activo
                  ? acento === "copat"
                    ? "bg-[#4D8AC2] text-white"
                    : "bg-pine text-white"
                  : bloqueado
                    ? "cursor-not-allowed bg-[#eef1f4] text-[#b7c0cb] line-through"
                    : acento === "copat"
                      ? "text-ink hover:bg-[#E8F2FA]"
                      : "text-ink hover:bg-pine-soft"
              }`}
              onClick={() => onElegir(fecha)}
            >
              {dia}
            </button>
          );
        })}
      </div>
    </div>
  );
}

function mesSiguienteSupera(anio: number, mes: number, hasta: string) {
  const [, mesHasta, anioHasta] = hasta.split("/").map(Number);
  const siguiente = new Date(anio, mes + 1, 1);
  return siguiente > new Date(anioHasta, mesHasta - 1, 1);
}
