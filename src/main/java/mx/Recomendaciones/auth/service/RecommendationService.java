package mx.Recomendaciones.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// Importa tus entidades exactas
import mx.Recomendaciones.auth.entity.articulo;
import mx.Recomendaciones.auth.entity.ArticuloFavorito;
import mx.Recomendaciones.auth.repository.ArticuloFavoritoRepository;
import mx.Recomendaciones.auth.repository.ArticuloRepository;

@Service
public class RecommendationService {
    
    @Autowired
    private ArticuloRepository articuloRepository;
    
    @Autowired
    private ArticuloFavoritoRepository articuloFavoritoRepository;

    public List<articulo> getRecomendacionesParaUsuario(Long usuarioId, int limite) {
        List<articulo> recomendaciones = new ArrayList<>();
        
        // 1. Obtener favoritos del usuario
        List<ArticuloFavorito> favoritos = articuloFavoritoRepository.findByUsuarioId(usuarioId);
        
        if (favoritos.isEmpty()) {
            // Si no tiene favoritos, mostrar artículos recientes
            return articuloRepository.findAllByOrderByIdDesc().stream()
                .limit(limite)
                .collect(Collectors.toList());
        }
        
        // 2. Obtener categorías más frecuentes
        List<String> categoriasFavoritas = favoritos.stream()
            .map(ArticuloFavorito::getCategoria)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(
                Function.identity(), 
                Collectors.counting()
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
            
        // 3. Buscar artículos similares por categoría
        if (!categoriasFavoritas.isEmpty()) {
            List<articulo> similares = articuloRepository
                .findByCategoriaInOrderByIdDesc(categoriasFavoritas);
            recomendaciones.addAll(similares);
        }
        
        // 4. Eliminar duplicados y artículos ya en favoritos
        Set<Long> favoritosIds = favoritos.stream()
            .map(ArticuloFavorito::getArticuloId)
            .collect(Collectors.toSet());
            
        return recomendaciones.stream()
            .filter(a -> !favoritosIds.contains(a.getId()))
            .distinct()
            .limit(limite)
            .collect(Collectors.toList());
    }
}