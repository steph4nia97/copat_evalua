import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useState, type ComponentProps } from "react";
import { useForm } from "react-hook-form";
import { api, ErrorApi } from "../api/client";
import { normalizarNombre } from "../domain/reglas";
import { formularioVacio, solicitudSchema, type SolicitudFormulario } from "../domain/solicitud";
import { CampoFecha } from "./CampoFecha";

type Props = {
  plantillas: string[];
  onCerrar: () => void;
  onCreada: () => void;
};

export function FormularioSolicitud({ plantillas, onCerrar, onCreada }: Props) {
  const [mensaje, setMensaje] = useState("");
  const {
    register,
    handleSubmit,
    setError,
    setValue,
    watch,
    formState: { errors },
  } = useForm<SolicitudFormulario>({
    resolver: zodResolver(solicitudSchema),
    defaultValues: formularioVacio,
  });
  const descripcion = watch("descripcion");
  const inicio = watch("periodoInicio");
  const fin = watch("periodoFin");

  const crear = useMutation({
    mutationFn: (valores: SolicitudFormulario) =>
      api("/api/departamento1/solicitudes", {
        method: "POST",
        body: JSON.stringify({
          nombreEmpresa: normalizarNombre(valores.nombreEmpresa),
          rut: valores.rut.trim(),
          correo: valores.correo.trim(),
          telefono: valores.telefono.trim(),
          periodoInicio: valores.periodoInicio.trim(),
          periodoFin: valores.periodoFin.trim(),
          plantilla: valores.plantilla,
          descripcion: valores.descripcion.trim(),
        }),
      }),
    onSuccess: () => onCreada(),
    onError: (error) => {
      if (!(error instanceof ErrorApi)) {
        setMensaje("No se pudo guardar la solicitud.");
        return;
      }
      Object.entries(error.campos).forEach(([campo, texto]) => {
        setError(campo as keyof SolicitudFormulario, { message: texto });
      });
      setMensaje(error.message);
    },
  });

  return (
    <div className="fixed inset-0 z-20 overflow-y-auto bg-ink/40 p-4">
      <form
        noValidate
        onSubmit={handleSubmit((valores) => {
          setMensaje("");
          crear.mutate(valores);
        })}
        className="mx-auto my-6 w-full max-w-2xl rounded-xl bg-card p-5 shadow-card sm:p-6"
      >
        <div className="mb-4 flex items-start justify-between gap-4">
          <div>
            <h2 className="font-serif text-2xl">Ingresar solicitud</h2>
            <p className="text-sm text-muted">La solicitud queda en estado solicitud creada.</p>
          </div>
          <button type="button" className="text-sm text-muted" onClick={onCerrar}>
            Cerrar
          </button>
        </div>

        {mensaje && (
          <p className="mb-4 rounded-md border border-copper/40 bg-copper/10 px-3 py-2 text-sm text-copper-dark" role="alert">
            {mensaje}
          </p>
        )}

        <div className="grid gap-4 sm:grid-cols-2">
          <Campo
            id="nombreEmpresa"
            label="Nombre de la empresa"
            hint="Letras y números, máximo 100."
            error={errors.nombreEmpresa?.message}
            className="sm:col-span-2"
            {...register("nombreEmpresa")}
          />
          <Campo id="rut" label="RUT" hint="Con dígito verificador módulo 11." error={errors.rut?.message} {...register("rut")} />
          <Campo id="correo" label="Correo" type="email" error={errors.correo?.message} {...register("correo")} />
          <Campo
            id="telefono"
            label="Teléfono"
            hint="Opcional. Solo dígitos: 9 nacionales o 11 con 56."
            error={errors.telefono?.message}
            {...register("telefono")}
          />
          <CampoFecha
            id="periodoInicio"
            label="Período de inicio"
            value={inicio}
            error={errors.periodoInicio?.message}
            onBlur={() => undefined}
            onChange={(valor) => setValue("periodoInicio", valor, { shouldValidate: Boolean(errors.periodoInicio) })}
          />
          <CampoFecha
            id="periodoFin"
            label="Período de término"
            value={fin}
            error={errors.periodoFin?.message}
            onBlur={() => undefined}
            onChange={(valor) => setValue("periodoFin", valor, { shouldValidate: Boolean(errors.periodoFin) })}
          />
        </div>

        <fieldset className="mt-4">
          <legend className="mb-2 text-sm font-semibold">Plantilla</legend>
          <div className="grid gap-2">
            {plantillas.map((plantilla) => (
              <label key={plantilla} className="flex items-center gap-3 rounded-md border border-line bg-white px-3 py-2">
                <input type="radio" value={plantilla} {...register("plantilla")} />
                <span>{plantilla}</span>
              </label>
            ))}
          </div>
          {errors.plantilla && <p className="mt-1 text-sm text-copper-dark">{errors.plantilla.message}</p>}
        </fieldset>

        <label htmlFor="descripcion" className="mt-4 block text-sm font-semibold">
          Descripción
        </label>
        <textarea
          id="descripcion"
          maxLength={300}
          rows={3}
          placeholder="Opcional"
          className="mt-1 w-full rounded-md border border-line bg-white px-3 py-2 outline-none ring-copper focus:ring-2"
          {...register("descripcion")}
        />
        <div className="mt-1 flex justify-between text-sm">
          <span className="text-copper-dark">{errors.descripcion?.message}</span>
          <span className="text-muted">{descripcion.trim().length}/300</span>
        </div>

        <div className="mt-5 flex justify-end gap-3">
          <button type="button" className="rounded-md px-4 py-2 text-sm" onClick={onCerrar}>
            Cancelar
          </button>
          <button
            type="submit"
            disabled={crear.isPending}
            className="rounded-md bg-copper px-4 py-2 text-sm font-semibold text-white hover:bg-copper-dark disabled:opacity-60"
          >
            Guardar solicitud
          </button>
        </div>
      </form>
    </div>
  );
}

type CampoProps = ComponentProps<"input"> & {
  label: string;
  hint?: string;
  error?: string;
};

function Campo({ id, label, hint, error, className = "", ...props }: CampoProps) {
  return (
    <div className={className}>
      <label htmlFor={id} className="mb-1 block text-sm font-semibold">
        {label}
      </label>
      <input
        id={id}
        aria-invalid={Boolean(error)}
        className="w-full rounded-md border border-line bg-white px-3 py-2 outline-none ring-copper focus:ring-2"
        {...props}
      />
      {hint && !error && <p className="mt-1 text-xs text-muted">{hint}</p>}
      {error && <p className="mt-1 text-sm text-copper-dark">{error}</p>}
    </div>
  );
}
