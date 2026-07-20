import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Proxy hacia el backend Spring Boot (misma sesión HTTP, sin CORS)
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": "http://localhost:8080",
      "/export": "http://localhost:8080",
    },
  },
  build: {
    outDir: "dist",
  },
});
//Gracias a esto, el navegador ve todas las peticiones como si fueran al mismo origen (localhost:5173),
//por lo que no hace falta configurar CORS ni cabeceras especiales, y la cookie de sesión que emite Spring
//se comporta como same-origin.
