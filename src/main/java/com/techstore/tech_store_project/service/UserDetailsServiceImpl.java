package com.techstore.tech_store_project.service;

import com.techstore.tech_store_project.model.Usuario;
import com.techstore.tech_store_project.repository.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // RF-01: Validar credenciales comparando usuario y contraseña con la base de datos
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // RF-01: Buscar usuario en BD por username
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        // RF-03: Las authorities son los permisos del rol del usuario (RBAC)
        List<GrantedAuthority> authorities = usuario.getRol() == null
                ? List.of()
                : usuario.getRol().getPermisos().stream()
                        .map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p.name()))
                        .toList();

        // La cuenta está habilitada solo si el usuario y su rol están activos
        boolean habilitado = usuario.isActivo()
                && usuario.getRol() != null && usuario.getRol().isActivo();

        return new User(
                usuario.getUsername(),
                // RNF-02: Contraseña almacenada en formato cifrado (BCrypt)
                usuario.getPassword(),
                habilitado,
                true,
                true,
                // RF-02: Bloquear cuenta si tiene 3 intentos fallidos consecutivos
                !usuario.isCuentaBloqueada(),
                authorities);
    }
}
