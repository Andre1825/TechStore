package com.techstore.tech_store_project.service;

import com.techstore.tech_store_project.model.Permiso;
import com.techstore.tech_store_project.model.Usuario;
import com.techstore.tech_store_project.notification.StockBajoEvent;
import com.techstore.tech_store_project.notification.StockNotificationGateway;
import com.techstore.tech_store_project.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class StockAlertService {
    private static final Logger log = LoggerFactory.getLogger(StockAlertService.class);
    private final UsuarioRepository users;
    private final StockNotificationGateway gateway;
    private final TaskExecutor executor;

    public StockAlertService(UsuarioRepository users, StockNotificationGateway gateway,
                             @Qualifier("stockAlertExecutor") TaskExecutor executor) {
        this.users = users;
        this.gateway = gateway;
        this.executor = executor;
    }

    public boolean disponible() { return gateway.enabled(); }

    @Transactional
    public void activar(String username) {
        Usuario user = users.findByUsernameForUpdate(username).orElseThrow();
        if (!esAdministrador(user)) throw new IllegalArgumentException("Esta cuenta no puede recibir avisos administrativos.");
        if (user.getCorreo() == null || user.getCorreo().isBlank()) {
            throw new IllegalArgumentException("Guarda primero un correo válido en Mi Perfil.");
        }
        if (!gateway.enabled()) throw new ServicioNoDisponibleException("Los avisos por correo aún no están configurados en el servidor.");
        try {
            gateway.subscribe(user.getCorreo(), recipientKey(user));
        } catch (RuntimeException e) {
            log.warn("No se pudo solicitar la suscripción SNS para usuario {} ({})", user.getId(), e.getClass().getSimpleName());
            throw new ServicioNoDisponibleException("No se pudo solicitar la suscripción. Revisa la configuración de SNS y vuelve a intentarlo.");
        }
        user.setStockAlertasActivas(true);
        users.save(user);
    }

    @Transactional
    public void desactivar(String username) {
        Usuario user = users.findByUsernameForUpdate(username).orElseThrow();
        user.setStockAlertasActivas(false);
        users.save(user);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmarSalida(StockBajoEvent event) {
        if (!gateway.enabled()) return;
        try {
            executor.execute(() -> publicar(event));
        } catch (RuntimeException e) {
            log.error("No se pudo encolar aviso de stock para movimiento {}", event.movimientoId());
        }
    }

    private void publicar(StockBajoEvent event) {
        try {
            String message = "Un producto alcanzó el nivel de stock bajo.\n\n"
                    + "Producto: " + event.nombre() + "\nSKU: " + event.sku()
                    + "\nStock anterior: " + event.stockAnterior() + "\nStock actual: " + event.stockActual()
                    + "\nStock mínimo: " + event.stockMinimo() + "\nMovimiento: " + event.movimientoId()
                    + "\n\nRevisa el inventario en TechStore y planifica la reposición.";
            for (Usuario user : users.findByActivoTrueAndCuentaBloqueadaFalseAndStockAlertasActivasTrue()) {
                if (!esAdministrador(user) || user.getCorreo() == null || user.getCorreo().isBlank()) continue;
                try {
                    gateway.publish(message, recipientKey(user));
                    log.info("Aviso SNS publicado: movimiento {}, usuario {}", event.movimientoId(), user.getId());
                } catch (RuntimeException e) {
                    log.error("Falló aviso SNS: movimiento {}, usuario {} ({})", event.movimientoId(), user.getId(), e.getClass().getSimpleName());
                }
            }
        } catch (RuntimeException e) {
            log.error("No se pudieron procesar los destinatarios del aviso de stock: movimiento {}", event.movimientoId());
        }
    }

    private boolean esAdministrador(Usuario user) {
        return user.isActivo() && !user.isCuentaBloqueada() && user.getRol() != null && user.getRol().isActivo()
                && user.getRol().getPermisos().contains(Permiso.GESTIONAR_USUARIOS);
    }

    public static String recipientKey(Usuario user) {
        try {
            byte[] input = (user.getId() + ":" + user.getCorreo()).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
