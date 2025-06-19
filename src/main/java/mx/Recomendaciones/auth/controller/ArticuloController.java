package mx.Recomendaciones.auth.controller;

import mx.Recomendaciones.auth.entity.ArticuloFavorito;
import mx.Recomendaciones.auth.entity.Usuario;
import mx.Recomendaciones.auth.entity.articulo;
import mx.Recomendaciones.auth.repository.ArticuloFavoritoRepository;
import mx.Recomendaciones.auth.repository.UsuarioRepository;
import mx.Recomendaciones.auth.service.CustomUserDetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/articulos")
public class ArticuloController {

    @Autowired
    private ArticuloFavoritoRepository articuloFavoritoRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    // Cache para almacenar artículos temporalmente
    private Map<Long, articulo> articulosCache = new HashMap<>();
    private static final int TIEMPO_ESPERA_SEGUNDOS = 3;

    // ===== MÉTODOS AUXILIARES CON DEBUG =====
    
    /**
     * Obtiene el usuario actualmente autenticado CON DEBUG DETALLADO
     */
    private Usuario getUsuarioActual() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("🔍 DEBUG - getUsuarioActual():");
            System.out.println("  - Authentication: " + (authentication != null ? "EXISTS" : "NULL"));
            
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getPrincipal())) {
                System.out.println("  - Usuario NO autenticado");
                return null;
            }
            
            System.out.println("  - Principal type: " + authentication.getPrincipal().getClass().getSimpleName());
            System.out.println("  - Name: " + authentication.getName());
            
            String email = authentication.getName();
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
            
            if (usuarioOpt.isEmpty()) {
                usuarioOpt = usuarioRepository.findByNombre(email);
                System.out.println("  - Buscando por nombre: " + email);
            } else {
                System.out.println("  - Encontrado por email: " + email);
            }
            
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                System.out.println("  - ✅ Usuario encontrado: ID=" + usuario.getId() + ", Nombre=" + usuario.getNombre());
                return usuario;
            } else {
                System.out.println("  - ❌ Usuario NO encontrado en BD");
                return null;
            }
        } catch (Exception e) {
            System.err.println("❌ Error en getUsuarioActual: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ===== ENDPOINTS PRINCIPALES =====
    
    @GetMapping
    public String mostrarBusqueda(Model model) {
        model.addAttribute("buscarPor", "titulo");
        return "articulos";
    }

    @PostMapping
    public String buscarArticulos(
            @RequestParam("query") String query,
            @RequestParam(value = "buscarPor", defaultValue = "titulo") String buscarPor,
            Model model, 
            HttpSession session) {
        
        System.out.println("🔍 BÚSQUEDA DE ARTÍCULOS:");
        System.out.println("  - Query: " + query);
        System.out.println("  - Buscar por: " + buscarPor);
        
        // Control de spam
        LocalDateTime ultimaBusqueda = (LocalDateTime) session.getAttribute("ultimaBusqueda");
        LocalDateTime ahora = LocalDateTime.now();
        
        if (ultimaBusqueda != null && 
            ultimaBusqueda.plusSeconds(TIEMPO_ESPERA_SEGUNDOS).isAfter(ahora)) {
            
            int segundosRestantes = TIEMPO_ESPERA_SEGUNDOS - 
                                   (int) java.time.Duration.between(ultimaBusqueda, ahora).getSeconds();
            
            model.addAttribute("error", "Por favor espera " + segundosRestantes + 
                              " segundo(s) antes de realizar otra búsqueda.");
            model.addAttribute("query", query);
            model.addAttribute("buscarPor", buscarPor);
            return "articulos";
        }
        
        session.setAttribute("ultimaBusqueda", ahora);
        
        try {
            Usuario usuario = getUsuarioActual();
            
            if (usuario == null) {
                model.addAttribute("error", "Error: Usuario no autenticado correctamente.");
                return "articulos";
            }
            
            // Crear artículos de prueba
            List<articulo> articulos = crearArticulosDePrueba(query, usuario.getId());
            
            if (!articulos.isEmpty()) {
                model.addAttribute("articulos", articulos);
                model.addAttribute("mensaje", "Se encontraron " + articulos.size() + " artículos");
                System.out.println("✅ " + articulos.size() + " artículos creados y agregados al modelo");
            } else {
                model.addAttribute("error", "No se encontraron resultados para '" + query + "'.");
            }
            
            model.addAttribute("query", query);
            model.addAttribute("buscarPor", buscarPor);
            
        } catch (Exception e) {
            System.err.println("❌ Error en búsqueda: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al buscar artículos: " + e.getMessage());
            model.addAttribute("query", query);
            model.addAttribute("buscarPor", buscarPor);
        }

        return "articulos";
    }
    
    @GetMapping("/ver/{id}")
    public String verDetallesArticulo(@PathVariable("id") Long id, Model model) {
        System.out.println("🔍 VER DETALLES - ID: " + id);
        
        try {
            Usuario usuario = getUsuarioActual();
            
            if (usuario == null) {
                model.addAttribute("error", "Error: Usuario no autenticado.");
                return "viewPdf";
            }
            
            articulo articuloSeleccionado = articulosCache.get(id);
            System.out.println("  - Artículo en cache: " + (articuloSeleccionado != null ? "SÍ" : "NO"));
            
            if (articuloSeleccionado != null) {
                // Verificar si es favorito
                boolean esFavorito = articuloFavoritoRepository.existsByArticuloIdAndUsuarioId(id, usuario.getId());
                articuloSeleccionado.setEsFavorito(esFavorito);
                
                model.addAttribute("articulo", articuloSeleccionado);
                model.addAttribute("mensaje", "Artículo cargado correctamente");
                
                System.out.println("✅ Artículo: " + articuloSeleccionado.getTitulo());
                System.out.println("  - Es favorito: " + esFavorito);
            } else {
                System.err.println("❌ Artículo no encontrado en cache");
                model.addAttribute("error", "El artículo no se encontró. Realiza una nueva búsqueda.");
            }
            
            model.addAttribute("id", id);
            
        } catch (Exception e) {
            System.err.println("❌ Error al cargar artículo: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al cargar el artículo: " + e.getMessage());
            model.addAttribute("id", id);
        }
        
        return "viewPdf";
    }

    // ===== FAVORITOS CON DEBUG DETALLADO =====
    
    @PostMapping("/favorito/agregar/{id}")
    public String agregarFavorito(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        System.out.println("🔍 AGREGAR A FAVORITOS:");
        System.out.println("  - Artículo ID: " + id);
        
        try {
            // Step 1: Verificar usuario
            Usuario usuario = getUsuarioActual();
            if (usuario == null) {
                System.err.println("❌ Usuario no autenticado");
                redirectAttributes.addFlashAttribute("error", "Usuario no autenticado");
                return "redirect:/articulos/ver/" + id;
            }
            
            System.out.println("  - Usuario ID: " + usuario.getId());
            System.out.println("  - Usuario Nombre: " + usuario.getNombre());
            
            // Step 2: Verificar si ya existe
            boolean yaExiste = articuloFavoritoRepository.existsByArticuloIdAndUsuarioId(id, usuario.getId());
            System.out.println("  - Ya existe en favoritos: " + yaExiste);
            
            if (yaExiste) {
                redirectAttributes.addFlashAttribute("mensaje", "El artículo ya está en favoritos");
                return "redirect:/articulos/ver/" + id;
            }
            
            // Step 3: Buscar artículo en cache
            articulo articulo = articulosCache.get(id);
            System.out.println("  - Artículo en cache: " + (articulo != null ? "SÍ" : "NO"));
            
            if (articulo != null) {
                System.out.println("  - Título: " + articulo.getTitulo());
                
                // Step 4: Crear favorito
                ArticuloFavorito favorito = new ArticuloFavorito();
                favorito.setUsuarioId(usuario.getId());
                favorito.setArticuloId(id);
                favorito.setTitulo(articulo.getTitulo());
                favorito.setAutores(articulo.getAutores());
                favorito.setAnio(articulo.getAnio());
                favorito.setCategoria(articulo.getCategoria());
                favorito.setDoi(articulo.getDoi());
                favorito.setUrl(articulo.getUrl());
                favorito.setFechaGuardado(LocalDateTime.now());
                
                System.out.println("  - Favorito creado: " + favorito);
                
                // Step 5: Guardar en BD
                try {
                    ArticuloFavorito favoritoGuardado = articuloFavoritoRepository.save(favorito);
                    System.out.println("✅ FAVORITO GUARDADO:");
                    System.out.println("  - ID generado: " + favoritoGuardado.getId());
                    System.out.println("  - Fecha: " + favoritoGuardado.getFechaGuardado());
                    
                    redirectAttributes.addFlashAttribute("mensaje", "¡Artículo agregado a favoritos exitosamente!");
                } catch (Exception e) {
                    System.err.println("❌ Error al guardar en BD: " + e.getMessage());
                    e.printStackTrace();
                    redirectAttributes.addFlashAttribute("error", "Error al guardar en base de datos: " + e.getMessage());
                }
            } else {
                System.err.println("❌ Artículo no encontrado en cache");
                redirectAttributes.addFlashAttribute("error", "Artículo no encontrado");
            }
        } catch (Exception e) {
            System.err.println("❌ Error general al agregar a favoritos:");
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al agregar a favoritos: " + e.getMessage());
        }
        
        return "redirect:/articulos/ver/" + id;
    }
    
    @PostMapping("/favorito/eliminar/{id}")
    public String eliminarFavorito(@PathVariable("id") Long id, 
                                   HttpServletRequest request, 
                                   RedirectAttributes redirectAttributes) {
        System.out.println("🔍 ELIMINAR DE FAVORITOS - ID: " + id);
        
        try {
            Usuario usuario = getUsuarioActual();
            
            if (usuario == null) {
                redirectAttributes.addFlashAttribute("error", "Usuario no autenticado");
                return "redirect:/articulos/ver/" + id;
            }
            
            Optional<ArticuloFavorito> favorito = articuloFavoritoRepository
                .findByArticuloIdAndUsuarioId(id, usuario.getId());
            
            if (favorito.isPresent()) {
                articuloFavoritoRepository.delete(favorito.get());
                System.out.println("✅ Artículo eliminado de favoritos");
                redirectAttributes.addFlashAttribute("mensaje", "Artículo eliminado de favoritos");
            } else {
                System.out.println("⚠️ El artículo no estaba en favoritos");
                redirectAttributes.addFlashAttribute("error", "El artículo no está en favoritos");
            }
        } catch (Exception e) {
            System.err.println("❌ Error al eliminar de favoritos: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al eliminar de favoritos");
        }
        
        // Determinar redirección
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/favoritos")) {
            return "redirect:/articulos/favoritos";
        } else {
            return "redirect:/articulos/ver/" + id;
        }
    }
    
    @GetMapping("/favoritos")
    public String listarFavoritos(Model model) {
        System.out.println("🔍 LISTAR FAVORITOS");
        
        try {
            Usuario usuario = getUsuarioActual();
            if (usuario == null) {
                return "redirect:/login";
            }
            
            List<ArticuloFavorito> favoritos = articuloFavoritoRepository
                .findAllByUsuarioIdOrderByFechaGuardadoDesc(usuario.getId());
            
            System.out.println("  - Usuario ID: " + usuario.getId());
            System.out.println("  - Favoritos encontrados: " + favoritos.size());
            
            model.addAttribute("favoritos", favoritos);
            model.addAttribute("totalFavoritos", favoritos.size());
            model.addAttribute("usuario", usuario);
            
        } catch (Exception e) {
            System.err.println("❌ Error al cargar favoritos: " + e.getMessage());
            model.addAttribute("error", "Error al cargar favoritos: " + e.getMessage());
        }
        
        return "favoritos";
    }

    // ===== MÉTODO PARA CREAR ARTÍCULOS DE PRUEBA =====
    
    private List<articulo> crearArticulosDePrueba(String query, Long usuarioId) {
        List<articulo> articulos = new ArrayList<>();
        
        System.out.println("🔍 CREANDO ARTÍCULOS DE PRUEBA:");
        System.out.println("  - Query: " + query);
        System.out.println("  - Usuario ID: " + usuarioId);
        
        String[] tiposArticulos = {
            "Investigación sobre ", "Análisis de ", "Estudio de ", 
            "Metodología para ", "Aplicaciones de "
        };
        
        String[] categorias = {
            "Ciencias de la Computación", "Inteligencia Artificial", 
            "Machine Learning", "Ingeniería de Software", "Bases de Datos"
        };
        
        for (int i = 1; i <= 8; i++) {
            articulo art = new articulo();
            art.setId((long) i);
            art.setTitulo(tiposArticulos[i % tiposArticulos.length] + query + " - Parte " + i);
            art.setAutores("Dr. Investigador " + i + ", Dra. Académica " + (i+1));
            art.setAnio(String.valueOf(2020 + (i % 4)));
            art.setCategoria(categorias[i % categorias.length]);
            art.setContenido("Este artículo presenta una investigación detallada sobre " + query + 
                           ". Se analizan diversos aspectos teóricos y prácticos, proporcionando " +
                           "nuevas perspectivas y metodologías innovadoras en el campo.");
            art.setDoi("10.1000/ejemplo" + i);
            art.setUrl("https://ejemplo.com/articulo" + i);
            art.setPuntuacion(75.0 + (i * 3.0));
            
            // Verificar si es favorito
            boolean esFavorito = articuloFavoritoRepository.existsByArticuloIdAndUsuarioId(art.getId(), usuarioId);
            art.setEsFavorito(esFavorito);
            
            // Guardar en cache
            articulosCache.put(art.getId(), art);
            articulos.add(art);
            
            System.out.println("  - Artículo " + i + ": ID=" + art.getId() + ", Favorito=" + esFavorito);
        }
        
        System.out.println("✅ " + articulos.size() + " artículos creados y guardados en cache");
        return articulos;
    }

    // ===== ENDPOINT PARA DEBUG =====
    
    @GetMapping("/debug/cache")
    @ResponseBody
    public Map<String, Object> debugCache() {
        Map<String, Object> debug = new HashMap<>();
        debug.put("articulos_en_cache", articulosCache.size());
        debug.put("ids_en_cache", articulosCache.keySet());
        
        Usuario usuario = getUsuarioActual();
        if (usuario != null) {
            debug.put("usuario_actual", Map.of(
                "id", usuario.getId(),
                "nombre", usuario.getNombre(),
                "email", usuario.getEmail()
            ));
            
            long totalFavoritos = articuloFavoritoRepository.countFavoritosByUsuario(usuario.getId());
            debug.put("total_favoritos_usuario", totalFavoritos);
        } else {
            debug.put("usuario_actual", "NO_AUTENTICADO");
        }
        
        return debug;
    }
}