package mx.Recomendaciones.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "articulos_favoritos")
public class ArticuloFavorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "articulo_id", nullable = false)
    private Long articuloId;
    
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "titulo", length = 500, nullable = false)
    private String titulo;

    @Column(name = "autores", length = 500)
    private String autores;

    @Column(name = "anio", length = 100)
    private String anio;

    @Column(name = "categoria", length = 100)
    private String categoria;

    @Column(name = "doi", length = 500)
    private String doi;

    @Column(name = "url", length = 1000)
    private String url;

    @Column(name = "fecha_guardado")
    private LocalDateTime fechaGuardado;

    // Constructores
    public ArticuloFavorito() {
        this.fechaGuardado = LocalDateTime.now();
    }

    public ArticuloFavorito(Long usuarioId, Long articuloId, String titulo) {
        this();
        this.usuarioId = usuarioId;
        this.articuloId = articuloId;
        this.titulo = titulo;
    }

    // Getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getArticuloId() {
        return articuloId;
    }

    public void setArticuloId(Long articuloId) {
        this.articuloId = articuloId;
    }
    
    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutores() {
        return autores;
    }

    public void setAutores(String autores) {
        this.autores = autores;
    }

    public String getAnio() {
        return anio;
    }

    public void setAnio(String anio) {
        this.anio = anio;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocalDateTime getFechaGuardado() {
        return fechaGuardado;
    }

    public void setFechaGuardado(LocalDateTime fechaGuardado) {
        this.fechaGuardado = fechaGuardado;
    }

    @Override
    public String toString() {
        return "ArticuloFavorito{" +
                "id=" + id +
                ", articuloId=" + articuloId +
                ", usuarioId=" + usuarioId +
                ", titulo='" + titulo + '\'' +
                ", fechaGuardado=" + fechaGuardado +
                '}';
    }
}