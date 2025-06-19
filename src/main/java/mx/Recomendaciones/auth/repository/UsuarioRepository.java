package mx.Recomendaciones.auth.repository;

import mx.Recomendaciones.auth.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // ===== MÉTODOS BÁSICOS NECESARIOS =====
    
    /**
     * Buscar usuario por email (para autenticación por email)
     */
    Optional<Usuario> findByEmail(String email);
    
    /**
     * Buscar usuario por nombre (para autenticación por username)
     */
    Optional<Usuario> findByNombre(String nombre);
    
    /**
     * Verificar si existe un usuario con el email dado
     */
    boolean existsByEmail(String email);
    
    /**
     * Verificar si existe un usuario con el nombre dado
     */
    boolean existsByNombre(String nombre);
    
    // ===== MÉTODOS ADICIONALES ÚTILES =====
    
    /**
     * Buscar usuarios por nombre que contenga el texto dado (case insensitive)
     */
    List<Usuario> findByNombreContainingIgnoreCase(String nombre);
    
    /**
     * Buscar usuario por email (case insensitive)
     */
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<Usuario> findByEmailIgnoreCase(@Param("email") String email);
    
    /**
     * Buscar usuario por nombre (case insensitive)
     */
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.nombre) = LOWER(:nombre)")
    Optional<Usuario> findByNombreIgnoreCase(@Param("nombre") String nombre);
    
    /**
     * Buscar usuarios que tengan un rol específico
     */
    @Query("SELECT u FROM Usuario u JOIN u.roles r WHERE r.nombre = :rolNombre")
    List<Usuario> findByRolNombre(@Param("rolNombre") String rolNombre);
    
    /**
     * Buscar usuarios administradores
     */
    @Query("SELECT u FROM Usuario u JOIN u.roles r WHERE r.nombre = 'ROLE_ADMIN'")
    List<Usuario> findAdministradores();
    
    /**
     * Contar usuarios por rol
     */
    @Query("SELECT COUNT(u) FROM Usuario u JOIN u.roles r WHERE r.nombre = :rolNombre")
    Long countByRolNombre(@Param("rolNombre") String rolNombre);
    
    /**
     * Contar total de usuarios
     */
    @Query("SELECT COUNT(u) FROM Usuario u")
    Long countTotalUsuarios();
    
    /**
     * Obtener usuarios ordenados por ID (más recientes primero)
     */
    List<Usuario> findAllByOrderByIdDesc();
    
    /**
     * Buscar usuarios por múltiples criterios
     */
    @Query("SELECT u FROM Usuario u WHERE " +
           "(:nombre IS NULL OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))) AND " +
           "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))")
    List<Usuario> buscarUsuarios(@Param("nombre") String nombre, @Param("email") String email);
    
    /**
     * Buscar usuarios que contengan texto en nombre o email
     */
    @Query("SELECT u FROM Usuario u WHERE " +
           "LOWER(u.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :busqueda, '%'))")
    List<Usuario> buscarPorTexto(@Param("busqueda") String busqueda);
    
    /**
     * Verificar si existe otro usuario con el mismo email (útil para actualizaciones)
     */
    @Query("SELECT COUNT(u) > 0 FROM Usuario u WHERE u.email = :email AND u.id != :id")
    boolean existsOtroUsuarioConEmail(@Param("email") String email, @Param("id") Long id);
    
    /**
     * Verificar si el usuario tiene un rol específico
     */
    @Query("SELECT COUNT(r) > 0 FROM Usuario u JOIN u.roles r WHERE u.id = :usuarioId AND r.nombre = :rolNombre")
    boolean usuarioTieneRol(@Param("usuarioId") Long usuarioId, @Param("rolNombre") String rolNombre);
    
    /**
     * Obtener usuarios que tienen artículos en favoritos
     */
    @Query("SELECT DISTINCT u FROM Usuario u WHERE u.id IN " +
           "(SELECT af.usuarioId FROM ArticuloFavorito af)")
    List<Usuario> findUsuariosConFavoritos();
    
    /**
     * Contar favoritos por usuario
     */
    @Query("SELECT COUNT(af) FROM ArticuloFavorito af WHERE af.usuarioId = :usuarioId")
    Long countFavoritosByUsuario(@Param("usuarioId") Long usuarioId);
    
    /**
     * Buscar usuarios inactivos (sin favoritos)
     */
    @Query("SELECT u FROM Usuario u WHERE u.id NOT IN " +
           "(SELECT DISTINCT af.usuarioId FROM ArticuloFavorito af)")
    List<Usuario> findUsuariosInactivos();
}