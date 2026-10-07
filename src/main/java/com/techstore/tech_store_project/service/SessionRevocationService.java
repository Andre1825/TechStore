package com.techstore.tech_store_project.service;

import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class SessionRevocationService {
    private final SessionRegistry sessionRegistry;

    public SessionRevocationService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public void revocar(String username) {
        Runnable revoke = () -> sessionRegistry.getAllPrincipals().stream()
                .filter(p -> p instanceof UserDetails u && u.getUsername().equals(username))
                .forEach(p -> sessionRegistry.getAllSessions(p, false)
                        .forEach(session -> session.expireNow()));
        // Evitar cerrar sesiones si falla la modificación de la cuenta o del rol.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { revoke.run(); }
            });
        } else {
            revoke.run();
        }
    }
}
