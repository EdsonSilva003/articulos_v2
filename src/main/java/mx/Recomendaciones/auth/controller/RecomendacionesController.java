package mx.Recomendaciones.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import mx.Recomendaciones.auth.entity.Usuario;
import mx.Recomendaciones.auth.repository.UsuarioRepository;
import mx.Recomendaciones.auth.entity.ArticuloFavorito;
import mx.Recomendaciones.auth.repository.ArticuloFavoritoRepository;
import mx.Recomendaciones.auth.service.CustomUserDetails;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.function.Function;

@Controller
@RequestMapping("/recomendaciones")
public class RecomendacionesController {

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private ArticuloFavoritoRepository favoritoRepository;

    @GetMapping
    public String mostrarRecomendaciones(Model model, Authentication authentication) {
        System.out.println("=== ACCEDIENDO A RECOMENDACIONES ===");
        
        try {
            // Obtener el ID del usuario actual
            Long usuarioId = obtenerUsuarioActualId();
            System.out.println("Usuario ID: " + usuarioId);
            
            if (usuarioId == null) {
                model.addAttribute("error", "Error: Usuario no autenticado");
                return "recomendaciones";
            }
            
            // Obtener información del usuario
            String username = authentication != null ? authentication.getName() : "Usuario";
            model.addAttribute("usuario", username);
            System.out.println("Username: " + username);
            
            // Obtener favoritos del usuario para generar recomendaciones basadas en sus intereses
            List<ArticuloFavorito> favoritos = favoritoRepository.findAllByUsuarioIdOrderByFechaGuardadoDesc(usuarioId);
            System.out.println("Favoritos encontrados: " + favoritos.size());
            
            // Generar recomendaciones (simuladas pero basadas en favoritos)
            List<Map<String, Object>> recomendaciones = generarRecomendacionesPersonalizadas(favoritos, usuarioId);
            model.addAttribute("recomendaciones", recomendaciones);
            
            // Convertir recomendaciones a lista de artículos para la plantilla
            List<Map<String, Object>> articulos = recomendaciones.stream()
                .map(rec -> {
                    Map<String, Object> articulo = new HashMap<>();
                    articulo.put("id", rec.get("id"));
                    articulo.put("titulo", rec.get("titulo"));
                    articulo.put("autores", rec.get("autores"));
                    articulo.put("anio", rec.get("anio"));
                    articulo.put("categoria", rec.get("tipo"));
                    articulo.put("contenido", rec.get("descripcion"));
                    articulo.put("url", rec.get("url"));
                    articulo.put("scoreRecomendacion", rec.get("compatibilidad"));
                    articulo.put("esFavorito", false); // Las recomendaciones inicialmente no son favoritas
                    articulo.put("razonRecomendacion", "Recomendado basado en tus favoritos");
                    return articulo;
                })
                .collect(Collectors.toList());
            
            model.addAttribute("articulos", articulos);
            
            // Estadísticas
            model.addAttribute("totalFavoritos", favoritos.size());
            model.addAttribute("totalRecomendaciones", recomendaciones.size());
            
            System.out.println("✅ Datos cargados correctamente para recomendaciones");
            
        } catch (Exception e) {
            System.err.println("❌ Error al cargar recomendaciones: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al cargar las recomendaciones: " + e.getMessage());
        }
        
        return "recomendaciones";
    }
    
    /**
     * Genera recomendaciones basadas en los favoritos del usuario
     */
    private List<Map<String, Object>> generarRecomendacionesPersonalizadas(List<ArticuloFavorito> favoritos, Long usuarioId) {
        List<Map<String, Object>> recomendaciones = new ArrayList<>();
        
        try {
            System.out.println("Generando recomendaciones basadas en " + favoritos.size() + " favoritos...");
            
            // Analizar categorías favoritas del usuario
            Map<String, Long> categoriasFavoritas = favoritos.stream()
                .filter(fav -> fav.getCategoria() != null)
                .collect(Collectors.groupingBy(
                    ArticuloFavorito::getCategoria,
                    Collectors.counting()
                ));
            
            String categoriaPrincipal = categoriasFavoritas.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Ciencias de la Computación");
            
            System.out.println("Categoría principal del usuario: " + categoriaPrincipal);
            
            // Recomendaciones base personalizadas
            String[][] recomendacionesBase = {
                {
                    "Inteligencia Artificial en " + categoriaPrincipal, 
                    "Investigación", 
                    "Un análisis profundo sobre cómo la IA está transformando " + categoriaPrincipal.toLowerCase() + ".",
                    "fa-brain",
                    "95",
                    "https://example.com/ai-" + categoriaPrincipal.toLowerCase().replace(" ", "-")
                },
                {
                    "Machine Learning para " + categoriaPrincipal, 
                    "Artículo Científico", 
                    "Técnicas avanzadas de aprendizaje automático aplicadas a " + categoriaPrincipal.toLowerCase() + ".",
                    "fa-robot",
                    "92",
                    "https://example.com/ml-" + categoriaPrincipal.toLowerCase().replace(" ", "-")
                },
                {
                    "Metodologías Avanzadas en " + categoriaPrincipal, 
                    "Paper de Investigación", 
                    "Nuevas metodologías y enfoques innovadores en el campo de " + categoriaPrincipal.toLowerCase() + ".",
                    "fa-flask",
                    "88",
                    "https://example.com/metodologias-" + categoriaPrincipal.toLowerCase().replace(" ", "-")
                },
                {
                    "Tendencias Futuras en " + categoriaPrincipal, 
                    "Estudio de Caso", 
                    "Análisis de las tendencias emergentes y el futuro de " + categoriaPrincipal.toLowerCase() + ".",
                    "fa-chart-line",
                    "85",
                    "https://example.com/tendencias-" + categoriaPrincipal.toLowerCase().replace(" ", "-")
                },
                {
                    "Análisis de Big Data en " + categoriaPrincipal, 
                    "Artículo Técnico", 
                    "Metodologías para el procesamiento y análisis de grandes volúmenes de datos en " + categoriaPrincipal.toLowerCase() + ".",
                    "fa-database",
                    "82",
                    "https://example.com/bigdata-" + categoriaPrincipal.toLowerCase().replace(" ", "-")
                },
                {
                    "Aplicaciones Prácticas en " + categoriaPrincipal, 
                    "Guía Práctica", 
                    "Casos prácticos y aplicaciones reales de " + categoriaPrincipal.toLowerCase() + " en la industria.",
                    "fa-tools",
                    "79",
                    "https://example.com/aplicaciones-" + categoriaPrincipal.toLowerCase().replace(" ", "-")
                }
            };
            
            // Crear las recomendaciones personalizadas
            for (int i = 0; i < recomendacionesBase.length; i++) {
                String[] rec = recomendacionesBase[i];
                
                Map<String, Object> recomendacion = new HashMap<>();
                recomendacion.put("id", System.currentTimeMillis() + i + 1000); // IDs únicos para recomendaciones
                recomendacion.put("titulo", rec[0]);
                recomendacion.put("tipo", rec[1]);
                recomendacion.put("descripcion", rec[2]);
                recomendacion.put("icono", rec[3]);
                recomendacion.put("compatibilidad", rec[4]);
                recomendacion.put("url", rec[5]);
                recomendacion.put("autores", "Dr. Sistema de Recomendaciones IA");
                recomendacion.put("anio", "2024");
                
                // Ajustar puntuación basada en favoritos del usuario
                int puntuacion = Integer.parseInt(rec[4]);
                if (!favoritos.isEmpty()) {
                    // Bonus si hay favoritos relacionados con la categoría
                    long favoritosEnCategoria = favoritos.stream()
                        .filter(fav -> categoriaPrincipal.equals(fav.getCategoria()))
                        .count();
                    
                    if (favoritosEnCategoria > 0) {
                        puntuacion = Math.min(99, puntuacion + (int)(favoritosEnCategoria * 2));
                    }
                }
                recomendacion.put("compatibilidad", String.valueOf(puntuacion));
                
                recomendaciones.add(recomendacion);
            }
            
            System.out.println("✅ " + recomendaciones.size() + " recomendaciones personalizadas generadas");
            
        } catch (Exception e) {
            System.err.println("❌ Error generando recomendaciones: " + e.getMessage());
            e.printStackTrace();
        }
        
        return recomendaciones;
    }
    
    /**
     * Obtiene el ID del usuario actual de la sesión
     */
    private Long obtenerUsuarioActualId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated() && 
                    !"anonymousUser".equals(authentication.getPrincipal())) {
                
                // Verificar si es CustomUserDetails
                if (authentication.getPrincipal() instanceof CustomUserDetails) {
                    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                    Long userId = userDetails.getId();
                    System.out.println("✅ Usuario obtenido de CustomUserDetails: ID = " + userId);
                    return userId;
                }
                
                // Si no es CustomUserDetails, buscar por email/username
                String identifier = authentication.getName();
                System.out.println("Buscando usuario por identificador: " + identifier);
                
                // Buscar por email primero
                Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(identifier);
                
                // Si no se encuentra por email, buscar por nombre
                if (!usuarioOpt.isPresent()) {
                    usuarioOpt = usuarioRepository.findByNombre(identifier);
                }
                
                if (usuarioOpt.isPresent()) {
                    Usuario usuario = usuarioOpt.get();
                    System.out.println("✅ Usuario encontrado: ID = " + usuario.getId());
                    return usuario.getId();
                } else {
                    System.err.println("❌ Usuario no encontrado para: " + identifier);
                }
            } else {
                System.out.println("⚠️ Usuario no autenticado o es anónimo");
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("❌ Error al obtener el usuario actual: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}