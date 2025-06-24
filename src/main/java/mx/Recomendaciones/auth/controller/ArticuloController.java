package mx.Recomendaciones.auth.controller;
import mx.Recomendaciones.auth.entity.ArticuloFavorito;
import mx.Recomendaciones.auth.entity.Usuario;
import mx.Recomendaciones.auth.entity.articulo;
import mx.Recomendaciones.auth.repository.ArticuloFavoritoRepository;
import mx.Recomendaciones.auth.repository.UsuarioRepository;
import mx.Recomendaciones.auth.service.CustomUserDetails;
import mx.Recomendaciones.auth.service.CoreApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.*;
@Controller
@RequestMapping("/articulos")
public class ArticuloController {@Autowired
private ArticuloFavoritoRepository articuloFavoritoRepository;

@Autowired
private UsuarioRepository usuarioRepository;

@Autowired
private CoreApiService apiService;

// Cache para almacenar artículos temporalmente
private Map<Long, articulo> articulosCache = new HashMap<>();
private static final int TIEMPO_ESPERA_SEGUNDOS = 2;

// ===== MÉTODOS AUXILIARES =====

/**
 * Obtiene el usuario actualmente autenticado
 */
private Usuario getUsuarioActual() {
    try {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        
        // Si es CustomUserDetails, obtener directamente
        if (authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return userDetails.getUsuario();
        }
        
        // Si es String, buscar en la base de datos
        String identifier = authentication.getName();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(identifier);
        
        if (!usuarioOpt.isPresent()) {
            usuarioOpt = usuarioRepository.findByNombre(identifier);
        }
        
        return usuarioOpt.orElse(null);
        
    } catch (Exception e) {
        System.err.println("❌ Error en getUsuarioActual: " + e.getMessage());
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
    System.out.println("  - Query: '" + query + "'");
    System.out.println("  - Buscar por: '" + buscarPor + "'");
    System.out.println("  - Timestamp: " + LocalDateTime.now());
    
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
        
        System.out.println("👤 Usuario autenticado: " + usuario.getNombre() + " (ID: " + usuario.getId() + ")");
        
        // Usar el servicio de API para búsqueda real con debugging mejorado
        List<articulo> articulos;
        
        if ("autor".equals(buscarPor)) {
            System.out.println("🔍 Iniciando búsqueda por AUTOR...");
            articulos = apiService.buscarArticulosPorAutor(query, 10);
            System.out.println("📊 Resultados de búsqueda por autor: " + articulos.size() + " artículos");
        } else {
            System.out.println("🔍 Iniciando búsqueda por TÍTULO...");
            articulos = apiService.buscarArticulosPorTitulo(query, 10);
            System.out.println("📊 Resultados de búsqueda por título: " + articulos.size() + " artículos");
        }
        
        // Debug: Mostrar información de los artículos encontrados
        for (int i = 0; i < Math.min(3, articulos.size()); i++) {
            articulo art = articulos.get(i);
            System.out.println("📖 Artículo " + (i+1) + ":");
            System.out.println("   - ID: " + art.getId());
            System.out.println("   - Título: " + art.getTitulo());
            System.out.println("   - Autores: " + art.getAutores());
            System.out.println("   - Año: " + art.getAnio());
        }
        
        // Verificar cuáles son favoritos y guardar en cache
        int favoritosEncontrados = 0;
        for (articulo art : articulos) {
            boolean esFavorito = articuloFavoritoRepository.existsByArticuloIdAndUsuarioId(art.getId(), usuario.getId());
            art.setEsFavorito(esFavorito);
            if (esFavorito) favoritosEncontrados++;
            
            // Guardar en cache
            articulosCache.put(art.getId(), art);
        }
        
        System.out.println("❤️ Artículos que ya son favoritos: " + favoritosEncontrados);
        System.out.println("💾 Artículos guardados en cache: " + articulos.size());
        
        if (!articulos.isEmpty()) {
            model.addAttribute("articulos", articulos);
            
            String tipoDescripcion = "autor".equals(buscarPor) ? "del autor" : "con el título";
            model.addAttribute("mensaje", "Se encontraron " + articulos.size() + 
                               " artículos " + tipoDescripcion + " '" + query + "'");
            
            System.out.println("✅ Búsqueda completada exitosamente");
        } else {
            String tipoDescripcion = "autor".equals(buscarPor) ? "del autor" : "que contengan";
            model.addAttribute("error", "No se encontraron artículos " + tipoDescripcion + " '" + query + "'.");
            System.out.println("⚠️ No se encontraron resultados");
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
            model.addAttribute("id", id);
            return "viewPdf";
        }
        
        // Buscar artículo en cache
        articulo articuloSeleccionado = articulosCache.get(id);
        
        if (articuloSeleccionado != null) {
            // Verificar si es favorito
            boolean esFavorito = articuloFavoritoRepository.existsByArticuloIdAndUsuarioId(id, usuario.getId());
            articuloSeleccionado.setEsFavorito(esFavorito);
            
            model.addAttribute("articulo", articuloSeleccionado);
            model.addAttribute("mensaje", "Artículo cargado correctamente");
            
            System.out.println("✅ Artículo: " + articuloSeleccionado.getTitulo());
        } else {
            System.err.println("❌ No se encontró el artículo en cache");
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

// ===== FAVORITOS =====

@PostMapping("/favorito/agregar/{id}")
public String agregarFavorito(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
    System.out.println("🔍 AGREGAR A FAVORITOS - ID: " + id);
    
    try {
        Usuario usuario = getUsuarioActual();
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Usuario no autenticado");
            return "redirect:/articulos/ver/" + id;
        }
        
        // Verificar si ya existe
        boolean yaExiste = articuloFavoritoRepository.existsByArticuloIdAndUsuarioId(id, usuario.getId());
        if (yaExiste) {
            redirectAttributes.addFlashAttribute("mensaje", "El artículo ya está en favoritos");
            return "redirect:/articulos/ver/" + id;
        }
        
        // Buscar artículo en cache
        articulo articulo = articulosCache.get(id);
        
        if (articulo != null) {
            // Crear favorito
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
            
            articuloFavoritoRepository.save(favorito);
            System.out.println("✅ Favorito guardado exitosamente");
            
            redirectAttributes.addFlashAttribute("mensaje", "¡Artículo agregado a favoritos!");
        } else {
            redirectAttributes.addFlashAttribute("error", "No se pudo procesar el artículo");
        }
        
    } catch (Exception e) {
        System.err.println("❌ Error al agregar a favoritos: " + e.getMessage());
        redirectAttributes.addFlashAttribute("error", "Error al agregar a favoritos");
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
        
        model.addAttribute("favoritos", favoritos);
        model.addAttribute("totalFavoritos", favoritos.size());
        model.addAttribute("usuario", usuario);
        
    } catch (Exception e) {
        System.err.println("❌ Error al cargar favoritos: " + e.getMessage());
        model.addAttribute("error", "Error al cargar favoritos: " + e.getMessage());
    }
    
    return "favoritos";
}

// ===== ENDPOINTS PARA RECOMENDACIONES =====

@PostMapping("/cargar-en-cache")
@ResponseBody
public Map<String, Object> cargarArticuloEnCache(@RequestBody Map<String, Object> datos) {
    try {
        Long id = Long.valueOf(datos.get("id").toString());
        
        Usuario usuario = getUsuarioActual();
        if (usuario == null) {
            return Map.of("error", "Usuario no autenticado");
        }
        
        // Crear artículo en cache
        articulo art = new articulo();
        art.setId(id);
        art.setTitulo((String) datos.getOrDefault("titulo", "Artículo Recomendado"));
        art.setAutores((String) datos.getOrDefault("autores", "Autores del Sistema"));
        art.setAnio((String) datos.getOrDefault("anio", "2024"));
        art.setCategoria((String) datos.getOrDefault("categoria", "Investigación"));
        art.setContenido((String) datos.getOrDefault("contenido", "Contenido del artículo de investigación."));
        art.setUrl((String) datos.getOrDefault("url", "https://ejemplo.com/articulo/" + id));
        art.setDoi("10.1000/cache." + id);
        art.setPuntuacion(75.0 + (id % 20));
        
        // Verificar si es favorito
        boolean esFavorito = articuloFavoritoRepository.existsByArticuloIdAndUsuarioId(id, usuario.getId());
        art.setEsFavorito(esFavorito);
        
        articulosCache.put(id, art);
        
        return Map.of("success", true, "mensaje", "Artículo cargado en cache", "esFavorito", esFavorito);
        
    } catch (Exception e) {
        System.err.println("❌ Error al cargar artículo en cache: " + e.getMessage());
        return Map.of("error", "Error al cargar artículo: " + e.getMessage());
    }
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

// ===== ENDPOINT DE DEBUG PARA BÚSQUEDA POR AUTOR =====

@GetMapping("/debug/autor/{autor}")
@ResponseBody
public Map<String, Object> debugBusquedaAutor(@PathVariable String autor) {
    System.out.println("🔧 DEBUG - Probando búsqueda por autor: " + autor);
    
    try {
        List<articulo> resultados = apiService.buscarArticulosPorAutor(autor, 5);
        
        Map<String, Object> debug = new HashMap<>();
        debug.put("autor_buscado", autor);
        debug.put("total_resultados", resultados.size());
        debug.put("timestamp", LocalDateTime.now());
        
        List<Map<String, Object>> articulosInfo = new ArrayList<>();
        for (articulo art : resultados) {
            Map<String, Object> artInfo = new HashMap<>();
            artInfo.put("id", art.getId());
            artInfo.put("titulo", art.getTitulo());
            artInfo.put("autores", art.getAutores());
            artInfo.put("anio", art.getAnio());
            articulosInfo.add(artInfo);
        }
        debug.put("articulos", articulosInfo);
        
        return debug;
        
    } catch (Exception e) {
        System.err.println("❌ Error en debug de búsqueda por autor: " + e.getMessage());
        return Map.of("error", e.getMessage(), "autor", autor);
    }
}
}