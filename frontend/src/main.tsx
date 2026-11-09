import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { App } from "./App";
import { ProveedorSesion } from "./auth/sesion";
import "./index.css";

const consultas = new QueryClient({
  defaultOptions: {
    queries: { retry: false, refetchOnWindowFocus: false },
  },
});

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={consultas}>
      <ProveedorSesion>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </ProveedorSesion>
    </QueryClientProvider>
  </StrictMode>,
);
