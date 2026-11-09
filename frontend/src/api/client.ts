export class ErrorApi extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly campos: Record<string, string> = {},
  ) {
    super(message);
  }
}

function leerCookie(nombre: string): string | null {
  const prefijo = `${nombre}=`;
  const encontrada = document.cookie.split("; ").find((parte) => parte.startsWith(prefijo));
  return encontrada ? decodeURIComponent(encontrada.slice(prefijo.length)) : null;
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const csrf = leerCookie("XSRF-TOKEN");
  if (csrf) {
    headers.set("X-XSRF-TOKEN", csrf);
  }
  let response: Response;
  try {
    response = await fetch(path, { ...options, headers, credentials: "include" });
  } catch {
    throw new ErrorApi("No se pudo conectar con el servidor.", 0);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  const texto = await response.text();
  const cuerpo = texto ? (JSON.parse(texto) as { mensaje?: string; campos?: Record<string, string> }) : {};
  if (!response.ok) {
    throw new ErrorApi(cuerpo.mensaje ?? "Ocurrió un error inesperado.", response.status, cuerpo.campos ?? {});
  }
  return cuerpo as T;
}
