import { useLocation, useNavigate } from "react-router-dom";
import { useSesion } from "../auth/sesion";

export function Encabezado() {
  const { usuario, salir } = useSesion();
  const navigate = useNavigate();
  const location = useLocation();

  if (!usuario) {
    return null;
  }

  function cerrarSesion() {
    void salir().finally(() => navigate("/login", { replace: true }));
  }

  const boton = "rounded-md border border-white/30 px-3 py-1 text-sm";

  return (
    <header className="sticky top-0 z-30 bg-pine text-paper">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-3 px-4 py-4">
        <button
          type="button"
          disabled={location.pathname === "/"}
          className={`${boton} disabled:opacity-40`}
          onClick={() => navigate(-1)}
        >
          Atrás
        </button>
        <div className="mr-auto">
          <p className="font-serif text-2xl leading-none">Copat evalua</p>
          <p className="mt-1 text-sm text-pine-soft">
            {usuario.departamento}
            {usuario.cargo ? ` · ${usuario.cargo}` : ""}
          </p>
        </div>
        <nav className="flex flex-wrap items-center gap-2" aria-label="Menú">
          <button type="button" className={boton} onClick={() => navigate("/")}>
            Inicio
          </button>
          {usuario.departamentoId === 1 && (
            <button type="button" className={boton} onClick={() => navigate("/bitacora")}>
              Bitácora
            </button>
          )}
          {usuario.departamentoId === 3 && (
            <>
              <button type="button" className={boton} onClick={() => navigate("/reclamos")}>
                Reclamos
              </button>
              <button type="button" className={boton} onClick={() => navigate("/indicadores")}>
                Métricas
              </button>
              <button type="button" className={boton} onClick={() => navigate("/bitacora")}>
                Bitácora
              </button>
            </>
          )}
          <button type="button" className={boton} onClick={() => navigate("/ajustes")}>
            Ajustes
          </button>
          <button type="button" className={boton} onClick={cerrarSesion}>
            Cerrar sesión
          </button>
        </nav>
      </div>
    </header>
  );
}
