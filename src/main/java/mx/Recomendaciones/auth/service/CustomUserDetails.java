package mx.Recomendaciones.auth.service;

import mx.Recomendaciones.auth.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CustomUserDetails implements UserDetails {

    private Usuario usuario;

    public CustomUserDetails(Usuario usuario) {
        this.usuario = usuario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Convertir los roles del usuario a GrantedAuthority
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        usuario.getRoles().forEach(rol -> {
            authorities.add(new SimpleGrantedAuthority(rol.getNombre()));
        });
        return authorities;
    }

    @Override
    public String getPassword() {
        return usuario.getPassword();
    }

    @Override
    public String getUsername() {
        // CAMBIO IMPORTANTE: Devolver el email como username para consistencia
        return usuario.getEmail();
    }
    
    // Método adicional para obtener el nombre real del usuario
    public String getNombre() {
        return usuario.getNombre();
    }
    
    // Método para obtener el ID del usuario
    public Long getId() {
        return usuario.getId();
    }
    
    // Método para obtener el usuario completo
    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}