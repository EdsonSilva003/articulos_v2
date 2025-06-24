package mx.Recomendaciones.auth.config;

import mx.Recomendaciones.auth.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()  // Desactiva CSRF para facilitar el acceso desde la aplicación móvil
            .authorizeHttpRequests((requests) -> requests
                // ========== API MÓVIL (Sin autenticación) ==========
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/register").permitAll()
                
                // ========== RECURSOS ESTÁTICOS (Sin autenticación) ==========
                .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()
                .requestMatchers("/static/**", "/webjars/**").permitAll()
                
                // ========== PÁGINAS PÚBLICAS (Sin autenticación) ==========
                .requestMatchers("/", "/register", "/login").permitAll()
                .requestMatchers("/login?error", "/login?logout").permitAll()
                
                // ========== PÁGINAS DE ADMINISTRADOR (Solo ADMIN) ==========
                .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                
                // ========== PÁGINAS DE USUARIO (Requieren autenticación) ==========
                .requestMatchers("/home").authenticated()
                .requestMatchers("/perfil", "/perfil/**").authenticated()
                .requestMatchers("/articulos", "/articulos/**").authenticated()
                .requestMatchers("/usuario/imagen/**").authenticated()
                .requestMatchers("/recomendaciones", "/recomendaciones/**").authenticated()
                
                // ========== CUALQUIER OTRA RUTA (Requiere autenticación) ==========
                .anyRequest().authenticated()
            )
            .formLogin((form) -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/home", true)  // Siempre redirige a /home tras login exitoso
                .failureUrl("/login?error=true")   // Redirige a login con error
                .usernameParameter("username")     // Parámetro del username
                .passwordParameter("password")     // Parámetro de la contraseña
                .permitAll()
            )
            .logout((logout) -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)      // Invalida la sesión
                .deleteCookies("JSESSIONID")       // Elimina cookies de sesión
                .clearAuthentication(true)        // Limpia la autenticación
                .permitAll()
            )
            .sessionManagement((session) -> session
                .maximumSessions(1)               // Máximo 1 sesión por usuario
                .maxSessionsPreventsLogin(false)  // Permite login múltiple (cierra sesiones anteriores)
                .expiredUrl("/login?expired=true") // Redirige si la sesión expira
            )
            .exceptionHandling((exceptions) -> exceptions
                .accessDeniedPage("/login?access-denied=true")  // Página de acceso denegado
            )
            .authenticationProvider(authenticationProvider()); // Configurar el provider personalizado

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}