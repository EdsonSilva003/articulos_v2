package mx.Recomendaciones.auth.service;

import mx.Recomendaciones.auth.entity.Usuario;
import mx.Recomendaciones.auth.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("customUserDetailsService")
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        System.out.println("=== AUTENTICACIÓN ===");
        System.out.println("Buscando usuario: " + usernameOrEmail);
        
        Usuario usuario = null;
        
        // Primero buscar por email
        usuario = usuarioRepository.findByEmail(usernameOrEmail).orElse(null);
        
        // Si no se encuentra por email, buscar por nombre
        if (usuario == null) {
            usuario = usuarioRepository.findByNombre(usernameOrEmail).orElse(null);
        }
        
        if (usuario == null) {
            System.err.println("Usuario no encontrado: " + usernameOrEmail);
            throw new UsernameNotFoundException("Usuario no encontrado: " + usernameOrEmail);
        }
        
        System.out.println("Usuario encontrado: " + usuario.getNombre() + " (ID: " + usuario.getId() + ")");
        return new CustomUserDetails(usuario);
    }
}