package mx.Recomendaciones.auth.service;

import mx.Recomendaciones.auth.entity.*;
import mx.Recomendaciones.auth.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;

@Service
public class RecommendationService {

    @Autowired
    private ArticuloFavoritoRepository articuloFavoritoRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    private static final int MAX_RECOMENDACIONES = 12;

    /**
     * Genera recomendaciones personalizadas basadas en favoritos del usuario
     */
    public List<Map<String, Object>> generarRecomendacionesPersonalizadas(Long usuarioId) {
        System.out.println("🔍 Generando recomendaciones personalizadas para usuario: " + usuarioId);
        
        try {
            // Obtener favoritos del usuario
            List<ArticuloFavorito> favoritos = articuloFavoritoRepository.findByUsuarioId(usuarioId);
            System.out.println("📚 Favoritos del usuario: " + favoritos.size());
            
            List<Map<String, Object>> recomendaciones = new ArrayList<>();
            
            if (favoritos.isEmpty()) {
                // Usuario nuevo - recomendaciones generales
                recomendaciones.addAll(generarRecomendacionesIniciales());
            } else {
                // Usuario con historial - recomendaciones personalizadas
                
                // 1. Basado en categorías favoritas
                recomendaciones.addAll(generarPorCategoriasFavoritas(favoritos, usuarioId));
                
                // 2. Basado en autores favoritos
                recomendaciones.addAll(generarPorAutoresFavoritos(favoritos, usuarioId));
                
                // 3. Basado en palabras clave del título
                recomendaciones.addAll(generarPorPalabrasClave(favoritos, usuarioId));
                
                // 4. Usuarios con gustos similares
                recomendaciones.addAll(generarPorUsuariosSimilares(usuarioId, favoritos));
                
                // 5. Artículos trending
                recomendaciones.addAll(generarTrending(usuarioId));
            }
            
            // Eliminar duplicados y limitar cantidad
            List<Map<String, Object>> recomendacionesFinales = eliminarDuplicados(recomendaciones);
            recomendacionesFinales = recomendacionesFinales.stream()
                    .limit(MAX_RECOMENDACIONES)
                    .collect(Collectors.toList());
            
            // Calcular scores personalizados
            calcularScoresPersonalizados(recomendacionesFinales, favoritos);
            
            // Ordenar por score descendente
            recomendacionesFinales.sort((a, b) -> 
                Integer.compare((Integer) b.get("compatibilidad"), (Integer) a.get("compatibilidad")));
            
            System.out.println("✅ Generadas " + recomendacionesFinales.size() + " recomendaciones");
            return recomendacionesFinales;
            
        } catch (Exception e) {
            System.err.println("❌ Error generando recomendaciones: " + e.getMessage());
            e.printStackTrace();
            return generarRecomendacionesIniciales();
        }
    }

    /**
     * Genera recomendaciones basadas en categorías de los favoritos
     */
    private List<Map<String, Object>> generarPorCategoriasFavoritas(List<ArticuloFavorito> favoritos, Long usuarioId) {
        // Analizar categorías más frecuentes
        Map<String, Long> categoriaCount = favoritos.stream()
                .filter(fav -> fav.getCategoria() != null && !fav.getCategoria().trim().isEmpty())
                .collect(Collectors.groupingBy(ArticuloFavorito::getCategoria, Collectors.counting()));
        
        List<Map<String, Object>> recomendaciones = new ArrayList<>();
        
        // Para cada categoría favorita, generar artículos
        categoriaCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3) // Top 3 categorías
                .forEach(entry -> {
                    String categoria = entry.getKey();
                    Long count = entry.getValue();
                    
                    recomendaciones.addAll(crearArticulosPorCategoria(categoria, usuarioId, 
                            "Te recomendamos esto porque tienes " + count + " artículos de " + categoria));
                });
        
        return recomendaciones;
    }

    /**
     * Genera recomendaciones basadas en autores favoritos
     */
    private List<Map<String, Object>> generarPorAutoresFavoritos(List<ArticuloFavorito> favoritos, Long usuarioId) {
        // Extraer autores más frecuentes
        Map<String, Long> autoresCount = new HashMap<>();
        
        favoritos.stream()
                .filter(fav -> fav.getAutores() != null && !fav.getAutores().trim().isEmpty())
                .forEach(fav -> {
                    String[] autores = fav.getAutores().split(",");
                    for (String autor : autores) {
                        String autorLimpio = autor.trim();
                        if (!autorLimpio.isEmpty()) {
                            autoresCount.merge(autorLimpio, 1L, Long::sum);
                        }
                    }
                });
        
        List<Map<String, Object>> recomendaciones = new ArrayList<>();
        
        // Para cada autor favorito
        autoresCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(2) // Top 2 autores
                .forEach(entry -> {
                    String autor = entry.getKey();
                    recomendaciones.addAll(crearArticulosPorAutor(autor, usuarioId,
                            "Basado en tu interés en trabajos de " + autor));
                });
        
        return recomendaciones;
    }

    /**
     * Genera recomendaciones basadas en palabras clave de títulos favoritos
     */
    private List<Map<String, Object>> generarPorPalabrasClave(List<ArticuloFavorito> favoritos, Long usuarioId) {
        // Extraer palabras clave más frecuentes de los títulos
        Map<String, Long> palabrasCount = new HashMap<>();
        
        favoritos.forEach(fav -> {
            if (fav.getTitulo() != null) {
                String[] palabras = fav.getTitulo().toLowerCase()
                        .replaceAll("[^a-záéíóúñ\\s]", "")
                        .split("\\s+");
                
                for (String palabra : palabras) {
                    if (palabra.length() > 4 && !esStopWord(palabra)) {
                        palabrasCount.merge(palabra, 1L, Long::sum);
                    }
                }
            }
        });
        
        List<Map<String, Object>> recomendaciones = new ArrayList<>();
        
        // Generar artículos basados en palabras clave
        palabrasCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3) // Top 3 palabras clave
                .forEach(entry -> {
                    String palabra = entry.getKey();
                    recomendaciones.addAll(crearArticulosPorPalabraClave(palabra, usuarioId,
                            "Basado en tu interés en temas sobre " + palabra));
                });
        
        return recomendaciones;
    }

    /**
     * Genera recomendaciones basadas en usuarios con gustos similares
     */
    private List<Map<String, Object>> generarPorUsuariosSimilares(Long usuarioId, List<ArticuloFavorito> misfovoritos) {
        List<Map<String, Object>> recomendaciones = new ArrayList<>();
        
        try {
            // Buscar usuarios con artículos favoritos en común
            Set<Long> misArticulosIds = misfovoritos.stream()
                    .map(ArticuloFavorito::getArticuloId)
                    .collect(Collectors.toSet());
            
            if (misArticulosIds.isEmpty()) {
                return recomendaciones;
            }
            
            // Buscar otros usuarios que tienen algunos de mis favoritos
            List<ArticuloFavorito> favoritosOtrosUsuarios = articuloFavoritoRepository.findAll()
                    .stream()
                    .filter(fav -> !fav.getUsuarioId().equals(usuarioId))
                    .filter(fav -> misArticulosIds.contains(fav.getArticuloId()))
                    .collect(Collectors.toList());
            
            // Contar coincidencias por usuario
            Map<Long, Long> usuarioCoincidencias = favoritosOtrosUsuarios.stream()
                    .collect(Collectors.groupingBy(ArticuloFavorito::getUsuarioId, Collectors.counting()));
            
            // Obtener usuarios más similares
            List<Long> usuariosSimilares = usuarioCoincidencias.entrySet().stream()
                    .filter(entry -> entry.getValue() >= 2) // Al menos 2 favoritos en común
                    .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            
            // Para cada usuario similar, recomendar sus otros favoritos
            for (Long usuarioSimilar : usuariosSimilares) {
                List<ArticuloFavorito> favoritosUsuarioSimilar = articuloFavoritoRepository.findByUsuarioId(usuarioSimilar);
                
                favoritosUsuarioSimilar.stream()
                        .filter(fav -> !misArticulosIds.contains(fav.getArticuloId()))
                        .limit(2)
                        .forEach(fav -> {
                            recomendaciones.add(crearRecomendacionDeOtroUsuario(fav, usuarioId,
                                    "Usuarios con gustos similares también guardaron esto"));
                        });
            }
            
        } catch (Exception e) {
            System.err.println("Error en recomendaciones por usuarios similares: " + e.getMessage());
        }
        
        return recomendaciones;
    }

    /**
     * Genera artículos trending/populares
     */
    private List<Map<String, Object>> generarTrending(Long usuarioId) {
        List<Map<String, Object>> trending = new ArrayList<>();
        
        // Simular artículos trending basados en datos reales de favoritos
        try {
            List<Object[]> articulosPopulares = articuloFavoritoRepository.getArticulosMasPopulares();
            
            for (Object[] resultado : articulosPopulares) {
                if (trending.size() >= 3) break;
                
                Long articuloId = (Long) resultado[0];
                Long count = (Long) resultado[1];
                
                // Buscar el favorito para obtener datos
                ArticuloFavorito favorito = articuloFavoritoRepository.findAll().stream()
                        .filter(fav -> fav.getArticuloId().equals(articuloId))
                        .findFirst()
                        .orElse(null);
                
                if (favorito != null) {
                    trending.add(crearRecomendacionTrending(favorito, usuarioId, count));
                }
            }
        } catch (Exception e) {
            System.err.println("Error generando trending: " + e.getMessage());
        }
        
        // Si no hay datos suficientes, agregar artículos trending simulados
        if (trending.size() < 3) {
            trending.addAll(generarTrendingSimulado(usuarioId));
        }
        
        return trending;
    }

    /**
     * Genera recomendaciones iniciales para usuarios nuevos
     */
    private List<Map<String, Object>> generarRecomendacionesIniciales() {
        List<Map<String, Object>> recomendaciones = new ArrayList<>();
        
        String[] categorias = {"Inteligencia Artificial", "Machine Learning", "Ciencias de la Computación", 
                              "Ingeniería de Software", "Bases de Datos", "Redes Neuronales"};
        
        String[] tipos = {"Artículo de Investigación", "Paper Académico", "Estudio de Caso", 
                         "Revisión Sistemática", "Tutorial Avanzado", "Estado del Arte"};
        
        for (int i = 0; i < MAX_RECOMENDACIONES; i++) {
            String categoria = categorias[i % categorias.length];
            String tipo = tipos[i % tipos.length];
            
            Map<String, Object> rec = new HashMap<>();
            rec.put("id", System.currentTimeMillis() + i + 2000);
            rec.put("titulo", "Introducción a " + categoria + " - " + tipo);
            rec.put("tipo", tipo);
            rec.put("descripcion", "Explora los fundamentos y aplicaciones de " + categoria.toLowerCase() + 
                   ". Artículo recomendado para comenzar tu viaje en esta área.");
            rec.put("icono", obtenerIconoPorCategoria(categoria));
            rec.put("compatibilidad", String.valueOf(75 + (i * 2)));
            rec.put("url", "https://example.com/" + categoria.toLowerCase().replace(" ", "-"));
            rec.put("autores", "Dr. Experto en " + categoria);
            rec.put("anio", "2024");
            rec.put("categoria", categoria);
            rec.put("razonRecomendacion", "Artículo popular para empezar en " + categoria);
            
            recomendaciones.add(rec);
        }
        
        return recomendaciones;
    }

    // Métodos auxiliares para crear artículos específicos

    private List<Map<String, Object>> crearArticulosPorCategoria(String categoria, Long usuarioId, String razon) {
        List<Map<String, Object>> articulos = new ArrayList<>();
        
        String[] titulos = {
            "Avances Recientes en " + categoria,
            "Metodologías Innovadoras en " + categoria,
            "Estado del Arte: " + categoria + " 2024"
        };
        
        for (int i = 0; i < Math.min(2, titulos.length); i++) {
            Map<String, Object> articulo = new HashMap<>();
            articulo.put("id", System.currentTimeMillis() + usuarioId + i + 3000);
            articulo.put("titulo", titulos[i]);
            articulo.put("tipo", "Artículo Especializado");
            articulo.put("descripcion", "Investigación especializada en " + categoria.toLowerCase() + 
                        " con enfoques metodológicos avanzados y aplicaciones prácticas.");
            articulo.put("icono", obtenerIconoPorCategoria(categoria));
            articulo.put("compatibilidad", String.valueOf(85 + (i * 3)));
            articulo.put("url", "https://example.com/" + categoria.toLowerCase().replace(" ", "-") + "-" + i);
            articulo.put("autores", "Especialistas en " + categoria);
            articulo.put("anio", "2024");
            articulo.put("categoria", categoria);
            articulo.put("razonRecomendacion", razon);
            
            articulos.add(articulo);
        }
        
        return articulos;
    }

    private List<Map<String, Object>> crearArticulosPorAutor(String autor, Long usuarioId, String razon) {
        List<Map<String, Object>> articulos = new ArrayList<>();
        
        String[] titulos = {
            "Nuevas Investigaciones de " + autor,
            "Publicación Reciente: " + autor + " et al."
        };
        
        for (int i = 0; i < Math.min(1, titulos.length); i++) {
            Map<String, Object> articulo = new HashMap<>();
            articulo.put("id", System.currentTimeMillis() + usuarioId + i + 4000);
            articulo.put("titulo", titulos[i]);
            articulo.put("tipo", "Investigación del Autor");
            articulo.put("descripcion", "Último trabajo de investigación del reconocido autor " + autor + 
                        ". Continuación de sus líneas de investigación previas.");
            articulo.put("icono", "fa-user-graduate");
            articulo.put("compatibilidad", String.valueOf(88 + i));
            articulo.put("url", "https://example.com/autor-" + autor.toLowerCase().replace(" ", "-"));
            articulo.put("autores", autor + ", Co-investigadores");
            articulo.put("anio", "2024");
            articulo.put("categoria", "Investigación Especializada");
            articulo.put("razonRecomendacion", razon);
            
            articulos.add(articulo);
        }
        
        return articulos;
    }

    private List<Map<String, Object>> crearArticulosPorPalabraClave(String palabra, Long usuarioId, String razon) {
        List<Map<String, Object>> articulos = new ArrayList<>();
        
        Map<String, Object> articulo = new HashMap<>();
        articulo.put("id", System.currentTimeMillis() + usuarioId + 5000);
        articulo.put("titulo", "Análisis Profundo: " + palabra.substring(0, 1).toUpperCase() + palabra.substring(1));
        articulo.put("tipo", "Análisis Temático");
        articulo.put("descripcion", "Estudio comprehensivo sobre " + palabra + 
                    " y sus aplicaciones en el contexto académico actual.");
        articulo.put("icono", "fa-search");
        articulo.put("compatibilidad", "82");
        articulo.put("url", "https://example.com/tema-" + palabra);
        articulo.put("autores", "Grupo de Investigación en " + palabra.substring(0, 1).toUpperCase() + palabra.substring(1));
        articulo.put("anio", "2024");
        articulo.put("categoria", "Análisis Temático");
        articulo.put("razonRecomendacion", razon);
        
        articulos.add(articulo);
        return articulos;
    }

    private Map<String, Object> crearRecomendacionDeOtroUsuario(ArticuloFavorito favorito, Long usuarioId, String razon) {
        Map<String, Object> recomendacion = new HashMap<>();
        recomendacion.put("id", favorito.getArticuloId());
        recomendacion.put("titulo", favorito.getTitulo());
        recomendacion.put("tipo", "Recomendación Social");
        recomendacion.put("descripcion", "Artículo valorado por usuarios con preferencias similares a las tuyas.");
        recomendacion.put("icono", "fa-users");
        recomendacion.put("compatibilidad", "87");
        recomendacion.put("url", favorito.getUrl());
        recomendacion.put("autores", favorito.getAutores());
        recomendacion.put("anio", favorito.getAnio());
        recomendacion.put("categoria", favorito.getCategoria());
        recomendacion.put("razonRecomendacion", razon);
        
        return recomendacion;
    }

    private Map<String, Object> crearRecomendacionTrending(ArticuloFavorito favorito, Long usuarioId, Long popularidad) {
        Map<String, Object> trending = new HashMap<>();
        trending.put("id", favorito.getArticuloId());
        trending.put("titulo", favorito.getTitulo());
        trending.put("tipo", "Trending");
        trending.put("descripcion", "Artículo popular con " + popularidad + " usuarios que lo han guardado.");
        trending.put("icono", "fa-fire");
        trending.put("compatibilidad", String.valueOf(Math.min(95, 80 + (popularidad * 2))));
        trending.put("url", favorito.getUrl());
        trending.put("autores", favorito.getAutores());
        trending.put("anio", favorito.getAnio());
        trending.put("categoria", favorito.getCategoria());
        trending.put("razonRecomendacion", "Artículo trending - " + popularidad + " usuarios lo han guardado");
        
        return trending;
    }

    private List<Map<String, Object>> generarTrendingSimulado(Long usuarioId) {
        List<Map<String, Object>> trending = new ArrayList<>();
        
        String[] titulosTrending = {
            "Revolución en Inteligencia Artificial: GPT y más allá",
            "Quantum Computing: El Futuro de la Computación",
            "Blockchain en Investigación Académica"
        };
        
        String[] categorias = {"Inteligencia Artificial", "Computación Cuántica", "Blockchain"};
        
        for (int i = 0; i < titulosTrending.length; i++) {
            Map<String, Object> article = new HashMap<>();
            article.put("id", System.currentTimeMillis() + usuarioId + i + 6000);
            article.put("titulo", titulosTrending[i]);
            article.put("tipo", "Trending Global");
            article.put("descripcion", "Artículo altamente popular en la comunidad académica internacional.");
            article.put("icono", "fa-fire");
            article.put("compatibilidad", String.valueOf(90 + i));
            article.put("url", "https://example.com/trending-" + i);
            article.put("autores", "Líderes en " + categorias[i]);
            article.put("anio", "2024");
            article.put("categoria", categorias[i]);
            article.put("razonRecomendacion", "Artículo trending en la comunidad académica");
            
            trending.add(article);
        }
        
        return trending;
    }

    // Métodos auxiliares

    private List<Map<String, Object>> eliminarDuplicados(List<Map<String, Object>> recomendaciones) {
        Set<Object> idsVistos = new HashSet<>();
        return recomendaciones.stream()
                .filter(rec -> idsVistos.add(rec.get("id")))
                .collect(Collectors.toList());
    }

    private void calcularScoresPersonalizados(List<Map<String, Object>> recomendaciones, List<ArticuloFavorito> favoritos) {
        if (favoritos.isEmpty()) return;
        
        // Categorías favoritas del usuario
        Map<String, Long> categoriasPref = favoritos.stream()
                .filter(fav -> fav.getCategoria() != null)
                .collect(Collectors.groupingBy(ArticuloFavorito::getCategoria, Collectors.counting()));
        
        String categoriaPrincipal = categoriasPref.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        
        // Ajustar scores basado en preferencias
        recomendaciones.forEach(rec -> {
            int score = Integer.parseInt((String) rec.get("compatibilidad"));
            
            // Bonus si coincide con categoría principal
            if (categoriaPrincipal != null && categoriaPrincipal.equals(rec.get("categoria"))) {
                score += 8;
            }
            
            // Bonus por tipo de recomendación
            if ("Trending".equals(rec.get("tipo"))) {
                score += 3;
            } else if ("Recomendación Social".equals(rec.get("tipo"))) {
                score += 5;
            }
            
            rec.put("compatibilidad", Math.min(99, score));
        });
    }

    private String obtenerIconoPorCategoria(String categoria) {
        switch (categoria.toLowerCase()) {
            case "inteligencia artificial": return "fa-brain";
            case "machine learning": return "fa-robot";
            case "ciencias de la computación": return "fa-computer";
            case "ingeniería de software": return "fa-code";
            case "bases de datos": return "fa-database";
            case "redes neuronales": return "fa-project-diagram";
            default: return "fa-book";
        }
    }

    private boolean esStopWord(String palabra) {
        Set<String> stopWords = Set.of("para", "sobre", "desde", "hasta", "entre", "durante", 
                                      "mediante", "través", "análisis", "estudio", "investigación", 
                                      "desarrollo", "aplicación", "sistema", "método", "técnica");
        return stopWords.contains(palabra.toLowerCase());
    }
}