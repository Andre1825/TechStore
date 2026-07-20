package com.techstore.tech_store_project.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/** Sirve la SPA de React y reenvía las rutas de React Router a index.html. */
/*configura Spring Boot para que funcione correctamente con una Single Page Application (SPA) como React.*/
@Component
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // No reenviar API/actuator/export ni peticiones de archivos (con extensión)
                        if (resourcePath.startsWith("api/")
                                || resourcePath.startsWith("actuator/")
                                || resourcePath.startsWith("export/")
                                || resourcePath.contains(".")) {
                            return null;
                        }
                        // Ruta de la SPA → devolver index.html (React Router se encarga)
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
/*
Sirve los archivos estáticos (HTML, CSS, JS, imágenes) desde classpath:/static/.
Si el archivo solicitado existe, lo devuelve normalmente.
Si la ruta es de la API (/api), Actuator (/actuator), Export (/export) o parece un archivo
(tiene una extensión como .js o .css), no la redirige, dejando que Spring la procese o responda con 404.
Si la ruta no existe y no es una API ni un archivo, devuelve index.html. Esto permite que React Router
gestione la navegación del lado del cliente (por ejemplo, /usuarios, /perfil, /dashboard).
*/