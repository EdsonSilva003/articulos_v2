package mx.Recomendaciones.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import mx.Recomendaciones.auth.entity.articulo;

@Repository
public interface ArticuloRepository extends JpaRepository<articulo, Long> {
    
    // ===== MÉTODOS BÁSICOS =====
    
    // Buscar por ID
    Optional<articulo> findById(Long id);
    
    // Obtener todos los artículos
    List<articulo> findAll();
    
    // Buscar por título (para búsquedas)
    List<articulo> findByTituloContainingIgnoreCase(String titulo);
    
    // Buscar por categoría
    List<articulo> findByCategoria(String categoria);
    
    // Buscar por autor
    List<articulo> findByAutoresContainingIgnoreCase(String autor);
    
    // ===== MÉTODOS PARA RECOMENDACIONES =====
    
    // Buscar por categoría específica ordenado por ID descendente (más recientes primero)
    List<articulo> findByCategoriaOrderByIdDesc(String categoria);
    
    // Buscar por autor (contiene el nombre del autor) ordenado por ID descendente
    List<articulo> findByAutoresContainingIgnoreCaseOrderByIdDesc(String autor);
    
    // Obtener todos ordenados por ID más reciente
    List<articulo> findAllByOrderByIdDesc();
    
    // Buscar por múltiples categorías
    @Query("SELECT a FROM articulo a WHERE a.categoria IN :categorias ORDER BY a.id DESC")
    List<articulo> findByCategoriaInOrderByIdDesc(@Param("categorias") List<String> categorias);
    
    // Buscar excluyendo artículos específicos (útil para no repetir favoritos)
    @Query("SELECT a FROM articulo a WHERE a.id NOT IN :excluirIds ORDER BY a.id DESC")
    List<articulo> findByIdNotInOrderByIdDesc(@Param("excluirIds") List<Long> excluirIds);
    
    // ===== MÉTODOS PARA BÚSQUEDAS AVANZADAS =====
    
    // Buscar por título o contenido
    @Query("SELECT a FROM articulo a WHERE " +
           "LOWER(a.titulo) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(a.contenido) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "ORDER BY a.id DESC")
    List<articulo> findByTituloOrContenidoContainingIgnoreCase(@Param("busqueda") String busqueda);
    
    // Buscar por título específicamente
    @Query("SELECT a FROM articulo a WHERE " +
           "LOWER(a.titulo) LIKE LOWER(CONCAT('%', :titulo, '%')) " +
           "ORDER BY a.id DESC")
    List<articulo> findByTituloContainingIgnoreCaseOrderByIdDesc(@Param("titulo") String titulo);
    
    // Buscar por rango de años
    @Query("SELECT a FROM articulo a WHERE a.anio BETWEEN :anioInicio AND :anioFin ORDER BY a.anio DESC")
    List<articulo> findByAnioBetween(@Param("anioInicio") String anioInicio, @Param("anioFin") String anioFin);
    
    // ===== MÉTODOS PARA ESTADÍSTICAS =====
    
    // Contar artículos por categoría
    @Query("SELECT a.categoria, COUNT(a) FROM articulo a WHERE a.categoria IS NOT NULL GROUP BY a.categoria ORDER BY COUNT(a) DESC")
    List<Object[]> countByCategoria();
    
    // Obtener categorías únicas
    @Query("SELECT DISTINCT a.categoria FROM articulo a WHERE a.categoria IS NOT NULL ORDER BY a.categoria")
    List<String> findDistinctCategorias();
    
    // Contar total de artículos
    @Query("SELECT COUNT(a) FROM articulo a")
    Long countTotalArticulos();
    
    // ===== MÉTODOS DE UTILIDAD =====
    
    // Verificar si existe un artículo con DOI específico
    boolean existsByDoi(String doi);
    
    // Buscar por DOI
    Optional<articulo> findByDoi(String doi);
    
    // Buscar por URL
    Optional<articulo> findByUrl(String url);
}