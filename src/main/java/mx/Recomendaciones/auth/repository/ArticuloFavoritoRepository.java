package mx.Recomendaciones.auth.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import mx.Recomendaciones.auth.entity.ArticuloFavorito;

@Repository
public interface ArticuloFavoritoRepository extends JpaRepository<ArticuloFavorito, Long> {
    
    // ===== MÉTODOS BÁSICOS =====
    
    // Encontrar favoritos por usuario
    List<ArticuloFavorito> findByUsuarioId(Long usuarioId);
    
    // Verificar si un artículo es favorito del usuario
    boolean existsByUsuarioIdAndArticuloId(Long usuarioId, Long articuloId);
    
    // Buscar favorito específico
    Optional<ArticuloFavorito> findByUsuarioIdAndArticuloId(Long usuarioId, Long articuloId);
    
    // Eliminar favorito específico
    void deleteByUsuarioIdAndArticuloId(Long usuarioId, Long articuloId);
    
    // ===== MÉTODOS PARA RECOMENDACIONES =====
    
    // Obtener categorías favoritas del usuario con conteo
    @Query("SELECT af.categoria, COUNT(af.categoria) FROM ArticuloFavorito af " +
           "WHERE af.usuarioId = :usuarioId AND af.categoria IS NOT NULL " +
           "GROUP BY af.categoria ORDER BY COUNT(af.categoria) DESC")
    List<Object[]> getCategoriasFavoritasPorUsuario(@Param("usuarioId") Long usuarioId);
    
    // Obtener autores favoritos del usuario con conteo
    @Query("SELECT af.autores, COUNT(af.autores) FROM ArticuloFavorito af " +
           "WHERE af.usuarioId = :usuarioId AND af.autores IS NOT NULL " +
           "GROUP BY af.autores ORDER BY COUNT(af.autores) DESC")
    List<Object[]> getAutoresFavoritosPorUsuario(@Param("usuarioId") Long usuarioId);
    
    // Obtener IDs de artículos favoritos del usuario
    @Query("SELECT af.articuloId FROM ArticuloFavorito af WHERE af.usuarioId = :usuarioId")
    List<Long> getArticulosIdsFavoritosPorUsuario(@Param("usuarioId") Long usuarioId);
    
    // ===== MÉTODOS PARA ESTADÍSTICAS =====
    
    // Contar favoritos por usuario
    @Query("SELECT COUNT(af) FROM ArticuloFavorito af WHERE af.usuarioId = :usuarioId")
    Long countFavoritosByUsuario(@Param("usuarioId") Long usuarioId);
    
    // Obtener artículos más populares (más veces agregados a favoritos)
    @Query("SELECT af.articuloId, COUNT(af.articuloId) FROM ArticuloFavorito af " +
           "GROUP BY af.articuloId ORDER BY COUNT(af.articuloId) DESC")
    List<Object[]> getArticulosMasPopulares();
    
    // Contar total de favoritos en el sistema
    @Query("SELECT COUNT(af) FROM ArticuloFavorito af")
    Long countTotalFavoritos();
    
    // ===== MÉTODOS POR FECHA =====
    
    // Favoritos agregados en un rango de fechas
    List<ArticuloFavorito> findByUsuarioIdAndFechaGuardadoBetween(
            Long usuarioId, LocalDateTime fechaInicio, LocalDateTime fechaFin);
    
    // Favoritos recientes del usuario (últimos N días)
    @Query("SELECT af FROM ArticuloFavorito af WHERE af.usuarioId = :usuarioId " +
           "AND af.fechaGuardado >= :fechaLimite ORDER BY af.fechaGuardado DESC")
    List<ArticuloFavorito> getFavoritosRecientes(@Param("usuarioId") Long usuarioId, 
                                               @Param("fechaLimite") LocalDateTime fechaLimite);
    
    // Favoritos ordenados por fecha (más recientes primero)
    List<ArticuloFavorito> findByUsuarioIdOrderByFechaGuardadoDesc(Long usuarioId);
    
    // ===== MÉTODOS POR CATEGORÍA =====
    
    // Favoritos por categoría específica
    List<ArticuloFavorito> findByUsuarioIdAndCategoria(Long usuarioId, String categoria);
    
    // Contar favoritos por categoría para un usuario
    @Query("SELECT COUNT(af) FROM ArticuloFavorito af WHERE af.usuarioId = :usuarioId AND af.categoria = :categoria")
    Long countByUsuarioIdAndCategoria(@Param("usuarioId") Long usuarioId, @Param("categoria") String categoria);
    
    // ===== MÉTODOS POR AUTOR =====
    
    // Favoritos que contienen un autor específico
    List<ArticuloFavorito> findByUsuarioIdAndAutoresContainingIgnoreCase(Long usuarioId, String autor);
    
    // ===== MÉTODOS DE BÚSQUEDA =====
    
    // Buscar favoritos por título
    @Query("SELECT af FROM ArticuloFavorito af WHERE af.usuarioId = :usuarioId " +
           "AND LOWER(af.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))")
    List<ArticuloFavorito> buscarFavoritosPorTitulo(@Param("usuarioId") Long usuarioId, 
                                                  @Param("titulo") String titulo);
    
    // Buscar favoritos por múltiples criterios
    @Query("SELECT af FROM ArticuloFavorito af WHERE af.usuarioId = :usuarioId " +
           "AND (LOWER(af.titulo) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(af.autores) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(af.categoria) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    List<ArticuloFavorito> buscarFavoritos(@Param("usuarioId") Long usuarioId, 
                                         @Param("busqueda") String busqueda);
    
    // ===== MÉTODOS PARA ANÁLISIS DE USUARIOS SIMILARES =====
    
    // Encontrar usuarios que tienen favoritos en común
    @Query("SELECT af2.usuarioId, COUNT(af2.articuloId) FROM ArticuloFavorito af1 " +
           "JOIN ArticuloFavorito af2 ON af1.articuloId = af2.articuloId " +
           "WHERE af1.usuarioId = :usuarioId AND af2.usuarioId != :usuarioId " +
           "GROUP BY af2.usuarioId " +
           "HAVING COUNT(af2.articuloId) >= :minCoincidencias " +
           "ORDER BY COUNT(af2.articuloId) DESC")
    List<Object[]> findUsuariosSimilares(@Param("usuarioId") Long usuarioId, 
                                       @Param("minCoincidencias") Long minCoincidencias);
    
    // Obtener favoritos de usuarios similares que el usuario actual no tiene
    @Query("SELECT DISTINCT af.articuloId FROM ArticuloFavorito af " +
           "WHERE af.usuarioId IN :usuariosSimilares " +
           "AND af.articuloId NOT IN (SELECT af2.articuloId FROM ArticuloFavorito af2 WHERE af2.usuarioId = :usuarioId)")
    List<Long> getRecomendacionesDeUsuariosSimilares(@Param("usuarioId") Long usuarioId, 
                                                   @Param("usuariosSimilares") List<Long> usuariosSimilares);
    
    // ===== MÉTODOS DE LIMPIEZA =====
    
    // Eliminar todos los favoritos de un usuario
    void deleteByUsuarioId(Long usuarioId);
    
    // Eliminar favoritos antiguos (más de X días)
    @Query("DELETE FROM ArticuloFavorito af WHERE af.fechaGuardado < :fechaLimite")
    void deleteOldFavorites(@Param("fechaLimite") LocalDateTime fechaLimite);
    
    List<ArticuloFavorito> findAllByUsuarioIdOrderByFechaGuardadoDesc(Long usuarioId);
    boolean existsByArticuloIdAndUsuarioId(Long articuloId, Long usuarioId);
    void deleteByArticuloIdAndUsuarioId(Long articuloId, Long usuarioId);
    
    Optional<ArticuloFavorito> findByArticuloIdAndUsuarioId(Long articuloId, Long usuarioId);
}