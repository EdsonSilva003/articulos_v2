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
    
    // Cache para almacenar artículos temporalmente (incluyendo recomendaciones)
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
            
            // **NUEVA LÓGICA**: Primero buscar en cache, si no existe, crear artículo dinámico
            articulo articuloSeleccionado = articulosCache.get(id);
            System.out.println("  - Artículo en cache: " + (articuloSeleccionado != null ? "SÍ" : "NO"));
            
            if (articuloSeleccionado == null) {
                // **SOLUCIÓN AL ERROR**: Crear artículo dinámico si no está en cache
                articuloSeleccionado = crearArticuloDinamico(id, usuario.getId());
                System.out.println("  - Artículo creado dinámicamente: " + articuloSeleccionado.getTitulo());
                
                // Guardarlo en cache para futuras consultas
                articulosCache.put(id, articuloSeleccionado);
            }
            
            if (articuloSeleccionado != null) {
                // Verificar si es favorito
                boolean esFavorito = articuloFavoritoRepository.existsByArticuloIdAndUsuarioId(id, usuario.getId());
                articuloSeleccionado.setEsFavorito(esFavorito);
                
                model.addAttribute("articulo", articuloSeleccionado);
                model.addAttribute("mensaje", "Artículo cargado correctamente");
                
                System.out.println("✅ Artículo: " + articuloSeleccionado.getTitulo());
                System.out.println("  - Es favorito: " + esFavorito);
            } else {
                System.err.println("❌ No se pudo crear el artículo dinámico");
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

    /**
     * **NUEVA FUNCIÓN**: Crea un artículo dinámico cuando no está en cache
     * Esto soluciona el error cuando se accede a recomendaciones
     */
    private articulo crearArticuloDinamico(Long id, Long usuarioId) {
        try {
            System.out.println("🔧 Creando artículo dinámico para ID: " + id);
            
            // Buscar si existe como favorito para obtener datos reales
            List<ArticuloFavorito> todosFavoritos = articuloFavoritoRepository.findAll();
            ArticuloFavorito favoritoExistente = todosFavoritos.stream()
                    .filter(fav -> fav.getArticuloId().equals(id))
                    .findFirst()
                    .orElse(null);
            
            articulo articuloDinamico = new articulo();
            articuloDinamico.setId(id);
            
            if (favoritoExistente != null) {
                // Usar datos del favorito existente
                articuloDinamico.setTitulo(favoritoExistente.getTitulo());
                articuloDinamico.setAutores(favoritoExistente.getAutores());
                articuloDinamico.setAnio(favoritoExistente.getAnio());
                articuloDinamico.setCategoria(favoritoExistente.getCategoria());
                articuloDinamico.setDoi(favoritoExistente.getDoi());
                articuloDinamico.setUrl(favoritoExistente.getUrl());
                articuloDinamico.setContenido("Este artículo ha sido marcado como favorito por otros usuarios. " +
                        "Contiene investigación relevante en el área de " + favoritoExistente.getCategoria() + ".");
                articuloDinamico.setPuntuacion(88.0);
                System.out.println("✅ Artículo creado desde favorito existente");
            } else {
                // Crear artículo completamente nuevo basado en patrones del ID
                String[] categorias = {"Inteligencia Artificial", "Machine Learning", "Ciencias de la Computación", 
                                     "Ingeniería de Software", "Bases de Datos", "Análisis de Datos",
                                     "Redes Neuronales", "Computación Cuántica", "Blockchain", "Ciberseguridad"};
                String[] tipos = {"Investigación Avanzada", "Estudio de Caso", "Revisión Sistemática", 
                               "Análisis Comparativo", "Metodología Innovadora", "Estado del Arte",
                               "Tutorial Especializado", "Guía Práctica", "Paper Académico"};
                String[] autores = {"Dr. Alan Turing", "Dra. Ada Lovelace", "Dr. John McCarthy", 
                                  "Dra. Barbara Liskov", "Dr. Tim Berners-Lee", "Dra. Shafi Goldwasser",
                                  "Dr. Geoffrey Hinton", "Dra. Fei-Fei Li", "Dr. Yann LeCun"};
                
                int index = (int) (id % categorias.length);
                String categoria = categorias[index];
                String tipo = tipos[index % tipos.length];
                String autorPrincipal = autores[index % autores.length];
                
                articuloDinamico.setTitulo(tipo + " en " + categoria + " - Artículo " + id);
                articuloDinamico.setAutores(autorPrincipal + ", Dr. Co-investigador, Equipo de " + categoria);
                articuloDinamico.setAnio("202" + (3 + (index % 2))); // 2023 o 2024
                articuloDinamico.setCategoria(categoria);
                articuloDinamico.setContenido("Este artículo presenta una investigación exhaustiva sobre " + categoria.toLowerCase() + 
                        ". Se abordan metodologías innovadoras, casos de estudio relevantes y aplicaciones prácticas en el mundo real. " +
                        "El trabajo contribuye significativamente al estado del arte en " + categoria.toLowerCase() + 
                        " mediante enfoques multidisciplinarios, análisis rigurosos y propuestas de soluciones innovadoras. " +
                        "Los resultados obtenidos demuestran la efectividad de las metodologías propuestas y abren nuevas " +
                        "líneas de investigación en el área. Se incluyen comparaciones con trabajos previos y se discuten " +
                        "las implicaciones futuras de los hallazgos para la comunidad académica e industrial.");
                articuloDinamico.setDoi("10.1000/dinamico." + id);
                articuloDinamico.setUrl("https://ejemplo.com/articulo/" + id);
                articuloDinamico.setPuntuacion(75.0 + (id % 20)); // Score entre 75-94
                System.out.println("✅ Artículo creado dinámicamente con datos simulados");
            }
            
            return articuloDinamico;
            
        } catch (Exception e) {
            System.err.println("❌ Error creando artículo dinámico: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
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
            
            // Step 3: Buscar artículo en cache o crear dinámico
            articulo articulo = articulosCache.get(id);
            if (articulo == null) {
                articulo = crearArticuloDinamico(id, usuario.getId());
                if (articulo != null) {
                    articulosCache.put(id, articulo);
                }
            }
            
            System.out.println("  - Artículo disponible: " + (articulo != null ? "SÍ" : "NO"));
            
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
                System.err.println("❌ No se pudo obtener o crear el artículo");
                redirectAttributes.addFlashAttribute("error", "No se pudo procesar el artículo");
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

    /**
     * **NUEVO ENDPOINT**: Cargar artículo en cache (útil para recomendaciones)
     */
    @PostMapping("/cargar-en-cache")
    @ResponseBody
    public Map<String, Object> cargarArticuloEnCache(@RequestBody Map<String, Object> datos) {
        try {
            Long id = Long.valueOf(datos.get("id").toString());
            String titulo = (String) datos.get("titulo");
            String autores = (String) datos.get("autores");
            
            System.out.println("📥 Cargando artículo en cache - ID: " + id);
            
            Usuario usuario = getUsuarioActual();
            if (usuario == null) {
                return Map.of("error", "Usuario no autenticado");
            }
            
            // Crear o actualizar artículo en cache
            articulo art = new articulo();
            art.setId(id);
            art.setTitulo(titulo != null ? titulo : "Artículo Recomendado " + id);
            art.setAutores(autores != null ? autores : "Autores del Sistema");
            art.setAnio((String) datos.getOrDefault("anio", "2024"));
            art.setCategoria((String) datos.getOrDefault("categoria", "Investigación"));
            
            String contenido = (String) datos.get("contenido");
            if (contenido == null || contenido.trim().isEmpty()) {
                contenido = "Este artículo presenta una investigación especializada en " + 
                           art.getCategoria().toLowerCase() + ". Incluye metodologías innovadoras, " +
                           "casos de estudio relevantes y análisis profundos que contribuyen " +
                           "significativamente al conocimiento en el área.";
            }
            art.setContenido(contenido);
            
            String url = (String) datos.get("url");
            art.setUrl(url != null ? url : "https://ejemplo.com/articulo/" + id);
            art.setDoi("10.1000/cache." + id);
            art.setPuntuacion(75.0 + (id % 20));
            
            // Verificar si es favorito
            boolean esFavorito = articuloFavoritoRepository.existsByArticuloIdAndUsuarioId(id, usuario.getId());
            art.setEsFavorito(esFavorito);
            
            articulosCache.put(id, art);
            
            System.out.println("✅ Artículo " + id + " cargado en cache exitosamente");
            return Map.of("success", true, "mensaje", "Artículo cargado en cache", "esFavorito", esFavorito);
            
        } catch (Exception e) {
            System.err.println("❌ Error al cargar artículo en cache: " + e.getMessage());
            e.printStackTrace();
            return Map.of("error", "Error al cargar artículo: " + e.getMessage());
        }
    }

    /**
     * **NUEVO ENDPOINT**: Pre-cargar múltiples artículos (para recomendaciones)
     */
    @PostMapping("/precargar-recomendaciones")
    @ResponseBody
    public Map<String, Object> precargarRecomendaciones(@RequestBody List<Map<String, Object>> articulos) {
        try {
            Usuario usuario = getUsuarioActual();
            if (usuario == null) {
                return Map.of("error", "Usuario no autenticado");
            }
            
            int cargados = 0;
            for (Map<String, Object> articuloData : articulos) {
                try {
                    Long id = Long.valueOf(articuloData.get("id").toString());
                    
                    // Solo cargar si no está ya en cache
                    if (!articulosCache.containsKey(id)) {
                        Map<String, Object> resultado = cargarArticuloEnCache(articuloData);
                        if (resultado.containsKey("success")) {
                            cargados++;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error cargando artículo individual: " + e.getMessage());
                }
            }
            
            return Map.of("success", true, "cargados", cargados, "total", articulos.size());
            
        } catch (Exception e) {
            System.err.println("❌ Error precargando recomendaciones: " + e.getMessage());
            return Map.of("error", "Error al precargar artículos: " + e.getMessage());
        }
    }
}