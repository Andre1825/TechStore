package com.techstore.tech_store_project.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import com.techstore.tech_store_project.service.PasswordPolicy;
import java.math.BigDecimal;

import com.techstore.tech_store_project.model.Categoria;
import com.techstore.tech_store_project.model.Marca;
import com.techstore.tech_store_project.model.Permiso;
import com.techstore.tech_store_project.model.Producto;
import com.techstore.tech_store_project.model.Rol;
import com.techstore.tech_store_project.model.Usuario;

import java.util.EnumSet;
import java.util.Set;
import com.techstore.tech_store_project.repository.CategoriaRepository;
import com.techstore.tech_store_project.repository.MarcaRepository;
import com.techstore.tech_store_project.repository.ProductoRepository;
import com.techstore.tech_store_project.repository.RolRepository;
import com.techstore.tech_store_project.repository.UsuarioRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final ProductoRepository productoRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final boolean seedDemoData;
    private final Environment environment;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           CategoriaRepository categoriaRepository,
                           MarcaRepository marcaRepository,
                           ProductoRepository productoRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${techstore.bootstrap.admin-password:}") String adminPassword,
                           @Value("${techstore.seed-demo-data:false}") boolean seedDemoData,
                           Environment environment) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
        this.productoRepository = productoRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.seedDemoData = seedDemoData;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        seedRoles();
        seedUsuarios();
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            usuarioRepository.findByUsername("admin").ifPresent(admin -> {
                if (passwordEncoder.matches("123456", admin.getPassword())) {
                    throw new IllegalStateException("Cambia la contraseña antigua de admin desde Mi Perfil en desarrollo antes de activar prod.");
                }
            });
            return;
        }
        if (!seedDemoData) return;
        try { seedCategorias();                                  } catch (Exception e) { log.error("seedCategorias failed: {}", e.getMessage()); }
        try { migrateCategoriaNombre("Laptops", "Computadoras", "Laptops, ultrabooks y equipos de escritorio"); } catch (Exception e) { log.warn("migrateCategoriaNombre skipped: {}", e.getMessage()); }
        try { seedMarcas();                                      } catch (Exception e) { log.error("seedMarcas failed: {}", e.getMessage()); }
        try { seedProductos();                                   } catch (Exception e) { log.error("seedProductos failed: {}", e.getMessage()); }
    }

    private void migrateCategoriaNombre(String oldNombre, String newNombre, String newDescripcion) {
        categoriaRepository.findAll().stream()
                .filter(c -> oldNombre.equalsIgnoreCase(c.getNombre()))
                .findFirst()
                .ifPresent(c -> {
                    if (!categoriaRepository.existsByNombreIgnoreCase(newNombre)) {
                        c.setNombre(newNombre);
                        c.setDescripcion(newDescripcion);
                        categoriaRepository.save(c);
                        log.info("Migrated categoria '{}' → '{}'", oldNombre, newNombre);
                    }
                });
    }

    private void seedRoles() {
        if (rolRepository.count() > 0) return;
        // RF-03: Roles por defecto con su conjunto de permisos (RBAC)
        rolRepository.save(rol("Administrador", "Acceso completo al sistema.",
                EnumSet.allOf(Permiso.class)));
        rolRepository.save(rol("Almacenero", "Gestión de inventario, productos y movimientos de stock.",
                EnumSet.of(Permiso.VER_DASHBOARD, Permiso.GESTIONAR_PRODUCTOS, Permiso.GESTIONAR_CATEGORIAS,
                        Permiso.GESTIONAR_MARCAS, Permiso.REGISTRAR_ENTRADAS, Permiso.REGISTRAR_SALIDAS,
                        Permiso.VER_MOVIMIENTOS)));
        rolRepository.save(rol("Vendedor", "Registro de salidas y consulta de movimientos.",
                EnumSet.of(Permiso.VER_DASHBOARD, Permiso.REGISTRAR_SALIDAS, Permiso.VER_MOVIMIENTOS)));
    }

    private void seedUsuarios() {
        if (usuarioRepository.count() > 0) return;
        if (adminPassword.isBlank()) {
            throw new IllegalStateException("Define ADMIN_PASSWORD para crear el administrador inicial (al menos 12 caracteres; máximo 72 bytes UTF-8).");
        }
        PasswordPolicy.validar(adminPassword);
        Rol rolAdmin = rolRepository.findByNombreIgnoreCase("Administrador")
                .orElseThrow(() -> new IllegalStateException("Rol 'Administrador' no encontrado. Verifique seedRoles."));
        Usuario admin = new Usuario();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRol(rolAdmin);
        admin.setNombreCompleto("Administrador del Sistema");
        admin.setActivo(true);
        usuarioRepository.save(admin);
    }

    private void seedCategorias() {
        saveCategoria("CAT-01", "Computadoras", "Laptops, ultrabooks y equipos de escritorio");
        saveCategoria("CAT-02", "Smartphones",  "Teléfonos inteligentes y accesorios");
        saveCategoria("CAT-03", "Tablets",     "Tabletas y e-readers");
        saveCategoria("CAT-04", "Monitores",   "Pantallas y monitores para PC");
        saveCategoria("CAT-05", "Periféricos", "Teclados, mouse, auriculares y webcams");
        saveCategoria("CAT-06", "Componentes", "Hardware interno: RAM, SSD, GPU");
    }

    private void saveCategoria(String preferredCodigo, String nombre, String descripcion) {
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) return;
        Categoria c = cat(preferredCodigo, nombre, descripcion);
        if (!categoriaRepository.existsByCodigoIgnoreCase(preferredCodigo)) {
            c.setCodigo(preferredCodigo);
        } else {
            String fallback = nombre.toUpperCase()
                    .replace("Á","A").replace("É","E").replace("Í","I")
                    .replace("Ó","O").replace("Ú","U").replace("Ñ","N")
                    .replaceAll("[^A-Z0-9]", "");
            c.setCodigo(fallback.substring(0, Math.min(20, fallback.length())));
        }
        try {
            categoriaRepository.save(c);
            log.info("Seeded categoria: {} ({})", nombre, c.getCodigo());
        } catch (Exception e) {
            log.warn("Could not seed categoria '{}': {}", nombre, e.getMessage());
        }
    }

    private void seedMarcas() {
        saveMarca("HP",       "Hewlett-Packard — laptops y periféricos");
        saveMarca("Samsung",  "Electrónica de consumo y componentes");
        saveMarca("Apple",    "Dispositivos premium iOS y macOS");
        saveMarca("Dell",     "Computadoras y monitores empresariales");
        saveMarca("Lenovo",   "Laptops y equipos corporativos");
        saveMarca("Asus",     "Laptops, monitores y componentes gaming");
        saveMarca("Sony",     "Audio, video y electrónica premium");
        saveMarca("Logitech", "Periféricos y accesorios para PC");
        saveMarca("Acer",     "Computadoras, monitores y tablets — fabricante taiwanés");
    }

    private void saveMarca(String nombre, String descripcion) {
        if (marcaRepository.existsByNombreIgnoreCase(nombre)) return;
        try {
            marcaRepository.save(marca(nombre, descripcion));
            log.info("Seeded marca: {}", nombre);
        } catch (Exception e) {
            log.warn("Could not seed marca '{}': {}", nombre, e.getMessage());
        }
    }

    private void seedProductos() {
        if (productoRepository.existsBySkuIgnoreCase("LAP-HP-001")) return;

        Categoria laptops     = cat("Computadoras");
        Categoria smartphones = cat("Smartphones");
        Categoria tablets     = cat("Tablets");
        Categoria monitores   = cat("Monitores");
        Categoria perifericos = cat("Periféricos");
        Categoria componentes = cat("Componentes");

        Marca hp       = marc("HP");
        Marca samsung  = marc("Samsung");
        Marca apple    = marc("Apple");
        Marca dell     = marc("Dell");
        Marca lenovo   = marc("Lenovo");
        Marca asus     = marc("Asus");
        Marca sony     = marc("Sony");
        Marca logitech = marc("Logitech");

        // Laptops
        productoRepository.save(prod("LAP-HP-001",  "Laptop HP Envy x360 15\"",          "Core i7, 16GB RAM, 512GB SSD, FHD Touch",      3299.00, 5,  2, laptops,   hp));
        productoRepository.save(prod("LAP-DL-001",  "Laptop Dell Inspiron 15 3520",       "Core i5, 8GB RAM, 512GB SSD, FHD",             2799.00, 8,  3, laptops,   dell));
        productoRepository.save(prod("LAP-LN-001",  "Laptop Lenovo IdeaPad 5 Pro",        "Ryzen 5, 16GB RAM, 512GB SSD, 2.8K OLED",      2499.00, 3,  2, laptops,   lenovo));
        productoRepository.save(prod("LAP-AS-001",  "Laptop Asus VivoBook 14",            "Core i5, 8GB RAM, 256GB SSD, FHD",             2199.00, 12, 4, laptops,   asus));
        productoRepository.save(prod("LAP-HP-002",  "Laptop HP Pavilion 15",              "Core i3, 8GB RAM, 256GB SSD, HD",              1799.00, 6,  2, laptops,   hp));

        // Smartphones
        productoRepository.save(prod("SMT-SM-001",  "Samsung Galaxy S24 Ultra",           "6.8\" QHD+, S Pen, 200MP, 5G",                4499.00, 7,  3, smartphones, samsung));
        productoRepository.save(prod("SMT-AP-001",  "iPhone 15 Pro",                      "6.1\" Super Retina XDR, Chip A17, 48MP",       5299.00, 4,  2, smartphones, apple));
        productoRepository.save(prod("SMT-SM-002",  "Samsung Galaxy A54 5G",              "6.4\" Super AMOLED, 50MP, 5000mAh",            1299.00, 15, 5, smartphones, samsung));
        productoRepository.save(prod("SMT-SM-003",  "Samsung Galaxy S23 FE",              "6.4\" Dynamic AMOLED, 50MP, 5G",              1899.00, 6,  2, smartphones, samsung));

        // Tablets
        productoRepository.save(prod("TAB-AP-001",  "iPad Air M2 11\"",                   "Chip M2, 256GB, Wi-Fi + Cellular",             3199.00, 6,  2, tablets,   apple));
        productoRepository.save(prod("TAB-SM-001",  "Samsung Galaxy Tab S9",              "11\" AMOLED, Snapdragon 8 Gen 2, 256GB",       2899.00, 4,  2, tablets,   samsung));

        // Monitores
        productoRepository.save(prod("MON-DL-001",  "Monitor Dell P2422H 24\"",           "IPS FHD 1080p, 60Hz, DisplayPort + HDMI",      899.00,  10, 3, monitores, dell));
        productoRepository.save(prod("MON-SM-001",  "Monitor Samsung C27G75T 27\"",       "VA Curved, 240Hz, 1ms, HDR10, 1440p",          1299.00, 7,  3, monitores, samsung));
        productoRepository.save(prod("MON-AS-001",  "Monitor Asus ProArt PA329CV 32\"",   "4K UHD, IPS, 90W USB-C, Thunderbolt 3",        2199.00, 3,  2, monitores, asus));

        // Periféricos
        productoRepository.save(prod("PER-LG-001",  "Mouse Logitech MX Master 3S",        "Ergonómico, scroll electromagnético, 8K DPI",  399.00,  20, 5, perifericos, logitech));
        productoRepository.save(prod("PER-LG-002",  "Teclado Logitech MX Keys S",         "Teclado inalámbrico retroiluminado, multi-OS",  349.00,  18, 5, perifericos, logitech));
        productoRepository.save(prod("PER-SN-001",  "Auriculares Sony WH-1000XM5",        "ANC líder, 30h batería, Hi-Res Audio",          1199.00, 8,  3, perifericos, sony));
        productoRepository.save(prod("PER-LG-003",  "Webcam Logitech C920 HD Pro",        "1080p 30fps, micrófono estéreo, autofocus",     279.00,  2,  4, perifericos, logitech));
        productoRepository.save(prod("PER-LG-004",  "Hub USB-C Logitech 7-en-1",          "HDMI 4K, 3× USB-A, SD, MicroSD, USB-C PD",     149.00,  3,  5, perifericos, logitech));

        // Componentes
        productoRepository.save(prod("CMP-SM-001",  "SSD Samsung 990 Pro 1TB NVMe",       "PCIe 4.0, 7450 MB/s lectura, M.2 2280",        349.00,  25, 8, componentes, samsung));
        productoRepository.save(prod("CMP-AS-001",  "GPU Asus Dual RTX 4060 OC 8GB",      "GDDR6, DLSS 3, HDMI 2.1, 3× DisplayPort 1.4", 1899.00, 5,  2, componentes, asus));
    }

    private Categoria cat(String nombre) {
        return categoriaRepository.findAll().stream()
                .filter(c -> nombre.equalsIgnoreCase(c.getNombre()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Categoría '" + nombre + "' no encontrada. Verifique seedCategorias."));
    }

    private Marca marc(String nombre) {
        return marcaRepository.findAll().stream()
                .filter(m -> nombre.equalsIgnoreCase(m.getNombre()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Marca '" + nombre + "' no encontrada. Verifique seedMarcas."));
    }

    private Rol rol(String nombre, String descripcion, Set<Permiso> permisos) {
        Rol r = new Rol();
        r.setNombre(nombre);
        r.setDescripcion(descripcion);
        r.setPermisos(EnumSet.copyOf(permisos));
        r.setActivo(true);
        return r;
    }

    private Categoria cat(String codigo, String nombre, String descripcion) {
        Categoria c = new Categoria();
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setDescripcion(descripcion);
        c.setActiva(true);
        return c;
    }

    private Marca marca(String nombre, String descripcion) {
        Marca m = new Marca();
        m.setNombre(nombre);
        m.setDescripcion(descripcion);
        m.setActiva(true);
        return m;
    }

    private Producto prod(String sku, String nombre, String descripcion,
                           double precio, int stock, int stockMinimo,
                           Categoria categoria, Marca marca) {
        Producto p = new Producto();
        p.setSku(sku);
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(BigDecimal.valueOf(precio).setScale(2));
        p.setStock(stock);
        p.setStockMinimo(stockMinimo);
        p.setCategoria(categoria);
        p.setMarca(marca);
        p.setActivo(true);
        return p;
    }
}
