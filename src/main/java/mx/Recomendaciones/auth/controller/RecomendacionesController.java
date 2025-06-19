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
import mx.Recomendaciones.auth.service.RecommendationService;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/recomendaciones")
public class RecomendacionesController {

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private ArticuloFavoritoRepository favoritoRepository;
    
    @Autowired
    private RecommendationService recommendationService;

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
            
            // Obtener favoritos del usuario para contexto
            List<ArticuloFavorito> favoritos = favoritoRepository.findAllByUsuarioIdOrderByFechaGuardadoDesc(usuarioId);
            System.out.println("Favoritos encontrados: " + favoritos.size());
            
            // **USAR EL NUEVO SERVICIO DE RECOMENDACIONES**
            List<Map<String, Object>> recomendaciones = recommendationService.generarRecomendacionesPersonalizadas(usuarioId);
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
                    articulo.put("razonRecomendacion", rec.get("razonRecomendacion"));
                    return articulo;
                })
                .collect(Collectors.toList());
            
            model.addAttribute("articulos", articulos);
            
            // Estadísticas mejoradas
            model.addAttribute("totalFavoritos", favoritos.size());
            model.addAttribute("totalRecomendaciones", recomendaciones.size());
            
            // Agregar tipo de recomendación basado en el perfil del usuario
            String tipoRecomendacion = favoritos.isEmpty() ? 
                "Recomendaciones para comenzar" : 
                "Recomendaciones personalizadas basadas en tus " + favoritos.size() + " favoritos";
            model.addAttribute("tipoRecomendacion", tipoRecomendacion);
            
            // Información adicional para debug
            model.addAttribute("debug", Map.of(
                "usuarioId", usuarioId,
                "cantidadFavoritos", favoritos.size(),
                "cantidadRecomendaciones", recomendaciones.size(),
                "tipoUsuario", favoritos.isEmpty() ? "NUEVO" : "EXPERIMENTADO"
            ));
            
            System.out.println("✅ Datos cargados correctamente para recomendaciones");
            
        } catch (Exception e) {
            System.err.println("❌ Error al cargar recomendaciones: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al cargar las recomendaciones: " + e.getMessage());
            
            // Proporcionar datos mínimos para evitar errores en la vista
            model.addAttribute("totalFavoritos", 0);
            model.addAttribute("totalRecomendaciones", 0);
            model.addAttribute("articulos", new ArrayList<>());
            model.addAttribute("recomendaciones", new ArrayList<>());
        }
        
        return "recomendaciones";
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