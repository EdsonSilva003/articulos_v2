package mx.Recomendaciones.auth.service;

import mx.Recomendaciones.auth.entity.Usuario;
import mx.Recomendaciones.auth.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("customUserDetailsService")
@Transactional
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        System.out.println("=== AUTENTICACIÓN ===");
        System.out.println("Buscando usuario: " + usernameOrEmail);
        
        Usuario usuario = null;
        
        try {
            // Primero buscar por email
            usuario = usuarioRepository.findByEmail(usernameOrEmail).orElse(null);
            
            // Si no se encuentra por email, buscar por nombre
            if (usuario == null) {
                usuario = usuarioRepository.findByNombre(usernameOrEmail).orElse(null);
                System.out.println("  - Buscando por nombre: " + usernameOrEmail);
            } else {
                System.out.println("  - Encontrado por email: " + usernameOrEmail);
            }
            
            if (usuario == null) {
                System.err.println("Usuario no encontrado: " + usernameOrEmail);
                throw new UsernameNotFoundException("Usuario no encontrado: " + usernameOrEmail);
            }
            
            // Verificar que el usuario tenga roles cargados
            if (usuario.getRoles() == null || usuario.getRoles().isEmpty()) {
                System.err.println("⚠️ Usuario sin roles: " + usuario.getNombre());
                // No lanzar excepción, simplemente loggear
            }
            
            System.out.println("Usuario encontrado: " + usuario.getNombre() + " (ID: " + usuario.getId() + ")");
            System.out.println("Roles: " + usuario.getRoles().size());
            
            return new CustomUserDetails(usuario);
            
        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Error durante la autenticación: " + e.getMessage());
            e.printStackTrace();
            throw new UsernameNotFoundException("Error del sistema durante la autenticación", e);
        }
    }
}