package mx.Recomendaciones.auth.controller;
import mx.Recomendaciones.auth.entity.Usuario;
import mx.Recomendaciones.auth.repository.UsuarioRepository;
import mx.Recomendaciones.auth.service.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
@Controller
public class PerfilController {
    @Autowired
private UsuarioRepository usuarioRepository;

@Autowired
private PasswordEncoder passwordEncoder;

// Constantes para el redimensionamiento de imágenes
private static final int MAX_WIDTH = 500;
private static final int MAX_HEIGHT = 500;
private static final long MAX_FILE_SIZE = 1024 * 1024; // 1MB

@GetMapping("/perfil")
public String perfil(Model model) {
    try {
        Usuario usuario = obtenerUsuarioActual();
        
        if (usuario != null) {
            model.addAttribute("usuario", usuario);
            model.addAttribute("tieneImagen", usuario.getImagen() != null);
            System.out.println("✅ Perfil cargado para usuario: " + usuario.getNombre());
        } else {
            System.err.println("❌ No se pudo obtener el usuario actual");
            model.addAttribute("mensaje", "Error: No se pudo cargar el perfil de usuario");
            model.addAttribute("tipoMensaje", "error");
            return "redirect:/login";
        }
    } catch (Exception e) {
        System.err.println("❌ Error en perfil: " + e.getMessage());
        e.printStackTrace();
        model.addAttribute("mensaje", "Error al cargar el perfil: " + e.getMessage());
        model.addAttribute("tipoMensaje", "error");
    }
    
    return "perfil";
}

@GetMapping("/usuario/imagen/{id}")
@ResponseBody
public String obtenerImagen(@PathVariable Long id) {
    try {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        
        if (usuario != null && usuario.getImagen() != null) {
            byte[] imagenBytes = usuario.getImagen();
            String base64Image = Base64.getEncoder().encodeToString(imagenBytes);
            System.out.println("✅ Imagen obtenida para usuario ID: " + id);
            return base64Image;
        }
    } catch (Exception e) {
        System.err.println("❌ Error obteniendo imagen: " + e.getMessage());
        e.printStackTrace();
    }
    
    return "";
}

@PostMapping("/perfil/actualizar-info")
public String actualizarInfo(
        @RequestParam("nombre") String nuevoNombre,
        @RequestParam("email") String nuevoEmail,
        Model model) {
    
    try {
        Usuario usuario = obtenerUsuarioActual();
        
        if (usuario == null) {
            model.addAttribute("mensaje", "Error: No se pudo encontrar el usuario.");
            model.addAttribute("tipoMensaje", "error");
            return "perfil";
        }
        
        // Verificar que el email no esté en uso por otro usuario
        Usuario usuarioExistente = usuarioRepository.findByEmail(nuevoEmail).orElse(null);
        if (usuarioExistente != null && !usuarioExistente.getId().equals(usuario.getId())) {
            model.addAttribute("mensaje", "Error: El email ya está en uso por otro usuario.");
            model.addAttribute("tipoMensaje", "error");
            model.addAttribute("usuario", usuario);
            model.addAttribute("tieneImagen", usuario.getImagen() != null);
            return "perfil";
        }
        
        // Actualizar la información del usuario
        usuario.setNombre(nuevoNombre);
        usuario.setEmail(nuevoEmail);
        usuarioRepository.save(usuario);
        
        System.out.println("✅ Información actualizada para usuario: " + usuario.getNombre());
        
        // Añadir mensaje de éxito y datos del usuario al modelo
        model.addAttribute("mensaje", "¡Información actualizada con éxito!");
        model.addAttribute("tipoMensaje", "exito");
        model.addAttribute("usuario", usuario);
        model.addAttribute("tieneImagen", usuario.getImagen() != null);
        
    } catch (Exception e) {
        System.err.println("❌ Error actualizando información: " + e.getMessage());
        e.printStackTrace();
        model.addAttribute("mensaje", "Error al actualizar la información: " + e.getMessage());
        model.addAttribute("tipoMensaje", "error");
    }
    
    return "perfil";
}

@PostMapping("/perfil/actualizar-password")
public String actualizarPassword(
        @RequestParam("password") String nuevaPassword,
        @RequestParam("confirmPassword") String confirmPassword,
        @RequestParam("currentPassword") String currentPassword,
        Model model) {
    
    try {
        Usuario usuario = obtenerUsuarioActual();
        
        if (usuario == null) {
            model.addAttribute("mensaje", "Error: No se pudo encontrar el usuario.");
            model.addAttribute("tipoMensaje", "error");
            return "perfil";
        }
        
        // Verificar que la contraseña actual sea correcta
        if (!passwordEncoder.matches(currentPassword, usuario.getPassword())) {
            model.addAttribute("mensaje", "Error: La contraseña actual es incorrecta.");
            model.addAttribute("tipoMensaje", "error");
            model.addAttribute("usuario", usuario);
            model.addAttribute("tieneImagen", usuario.getImagen() != null);
            return "perfil";
        }
        
        // Verificar que las contraseñas nuevas coincidan
        if (!nuevaPassword.equals(confirmPassword)) {
            model.addAttribute("mensaje", "Error: Las contraseñas nuevas no coinciden.");
            model.addAttribute("tipoMensaje", "error");
            model.addAttribute("usuario", usuario);
            model.addAttribute("tieneImagen", usuario.getImagen() != null);
            return "perfil";
        }
        
        // Actualizar la contraseña
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
        
        System.out.println("✅ Contraseña actualizada para usuario: " + usuario.getNombre());
        
        // Añadir mensaje de éxito y datos del usuario al modelo
        model.addAttribute("mensaje", "¡Contraseña actualizada con éxito!");
        model.addAttribute("tipoMensaje", "exito");
        model.addAttribute("usuario", usuario);
        model.addAttribute("tieneImagen", usuario.getImagen() != null);
        
    } catch (Exception e) {
        System.err.println("❌ Error actualizando contraseña: " + e.getMessage());
        e.printStackTrace();
        model.addAttribute("mensaje", "Error al actualizar la contraseña: " + e.getMessage());
        model.addAttribute("tipoMensaje", "error");
    }
    
    return "perfil";
}

@PostMapping("/perfil/actualizar-imagen")
public String actualizarImagen(@RequestParam("imagen") MultipartFile imagen, Model model) {
    try {
        Usuario usuario = obtenerUsuarioActual();
        
        if (usuario == null) {
            model.addAttribute("mensaje", "Error: No se pudo encontrar el usuario.");
            model.addAttribute("tipoMensaje", "error");
            return "perfil";
        }
        
        // Verificar que la imagen no esté vacía
        if (imagen.isEmpty()) {
            model.addAttribute("mensaje", "Error: Por favor selecciona una imagen.");
            model.addAttribute("tipoMensaje", "error");
            model.addAttribute("usuario", usuario);
            model.addAttribute("tieneImagen", usuario.getImagen() != null);
            return "perfil";
        }
        
        // Verificar el tipo de archivo
        String contentType = imagen.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            model.addAttribute("mensaje", "Error: Por favor selecciona un archivo de imagen válido.");
            model.addAttribute("tipoMensaje", "error");
            model.addAttribute("usuario", usuario);
            model.addAttribute("tieneImagen", usuario.getImagen() != null);
            return "perfil";
        }
        
        // Obtener los bytes de la imagen
        byte[] imagenBytes = imagen.getBytes();
        
        // Redimensionar la imagen si es demasiado grande
        if (imagen.getSize() > MAX_FILE_SIZE) {
            imagenBytes = redimensionarImagen(imagenBytes);
        }
        
        // Guardar la imagen en el usuario
        usuario.setImagen(imagenBytes);
        usuarioRepository.save(usuario);
        
        System.out.println("✅ Imagen actualizada para usuario: " + usuario.getNombre());
        
        // Añadir mensaje de éxito y datos del usuario al modelo
        model.addAttribute("mensaje", "¡Imagen de perfil actualizada con éxito!");
        model.addAttribute("tipoMensaje", "exito");
        model.addAttribute("usuario", usuario);
        model.addAttribute("tieneImagen", true);
        
    } catch (IOException e) {
        System.err.println("❌ Error procesando imagen: " + e.getMessage());
        e.printStackTrace();
        model.addAttribute("mensaje", "Error al procesar la imagen: " + e.getMessage());
        model.addAttribute("tipoMensaje", "error");
    } catch (Exception e) {
        System.err.println("❌ Error general actualizando imagen: " + e.getMessage());
        e.printStackTrace();
        model.addAttribute("mensaje", "Error al actualizar la imagen: " + e.getMessage());
        model.addAttribute("tipoMensaje", "error");
    }
    
    return "perfil";
}

/**
 * Redimensiona la imagen para reducir su tamaño
 */
private byte[] redimensionarImagen(byte[] imagenOriginal) throws IOException {
    // Leer la imagen original
    ByteArrayInputStream bis = new ByteArrayInputStream(imagenOriginal);
    BufferedImage imagen = ImageIO.read(bis);
    
    if (imagen == null) {
        throw new IOException("No se pudo leer la imagen");
    }
    
    // Calcular nuevas dimensiones manteniendo la relación de aspecto
    int originalWidth = imagen.getWidth();
    int originalHeight = imagen.getHeight();
    
    // Si la imagen ya es pequeña, devolverla sin cambios
    if (originalWidth <= MAX_WIDTH && originalHeight <= MAX_HEIGHT) {
        return imagenOriginal;
    }
    
    int newWidth, newHeight;
    
    if (originalWidth > originalHeight) {
        newWidth = MAX_WIDTH;
        newHeight = (int) (originalHeight * ((double) MAX_WIDTH / originalWidth));
    } else {
        newHeight = MAX_HEIGHT;
        newWidth = (int) (originalWidth * ((double) MAX_HEIGHT / originalHeight));
    }
    
    // Crear la nueva imagen redimensionada
    BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = resizedImage.createGraphics();
    g.drawImage(imagen, 0, 0, newWidth, newHeight, null);
    g.dispose();
    
    // Convertir la imagen redimensionada a bytes
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    
    // Determinar el formato de la imagen
    String format = "jpeg"; // Formato por defecto
    
    // Guardar la imagen en el formato determinado
    ImageIO.write(resizedImage, format, bos);
    
    return bos.toByteArray();
}

/**
 * Método mejorado para obtener el usuario actual
 */
private Usuario obtenerUsuarioActual() {
    try {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getPrincipal())) {
            System.out.println("⚠️ Usuario no autenticado");
            return null;
        }
        
        // Si es CustomUserDetails, obtener directamente
        if (authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Usuario usuario = userDetails.getUsuario();
            System.out.println("✅ Usuario obtenido via CustomUserDetails: " + usuario.getNombre());
            return usuario;
        }
        
        // Si es String (email), buscar en la base de datos
        String email = authentication.getName();
        System.out.println("🔍 Buscando usuario por email: " + email);
        
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            System.out.println("✅ Usuario encontrado por email: " + usuario.getNombre());
            return usuario;
        } else {
            // Intentar buscar por nombre como fallback
            usuarioOpt = usuarioRepository.findByNombre(email);
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                System.out.println("✅ Usuario encontrado por nombre: " + usuario.getNombre());
                return usuario;
            }
        }
        
        System.err.println("❌ Usuario no encontrado: " + email);
        return null;
        
    } catch (Exception e) {
        System.err.println("❌ Error obteniendo usuario actual: " + e.getMessage());
        e.printStackTrace();
        return null;
    }
}
}