
package mx.Recomendaciones.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true) // Ignora propiedades desconocidas en el JSON
public class articulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("title")
    private String titulo;

    @JsonProperty("type")
    private String categoria;

    @Column(length = 5000) // Para permitir textos largos
    @JsonProperty("abstract")
    private String contenido;

    @JsonProperty("score")
    private Double puntuacion;

    // Campos adicionales que pueden ser útiles
    private String autores;
    private String anio;
    private String doi;
    private String url;
    
    // Campo para indicar si está en favoritos (no se persiste)
    @Transient
    private boolean esFavorito;

    // Getters y setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public Double getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(Double puntuacion) {
        this.puntuacion = puntuacion;
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
    
    public boolean isEsFavorito() {
        return esFavorito;
    }

    public void setEsFavorito(boolean esFavorito) {
        this.esFavorito = esFavorito;
    }

    // Para depuración
    @Override
    public String toString() {
        return "articulo{" +
                "id=" + id +
                ", titulo='" + titulo + '\'' +
                ", categoria='" + categoria + '\'' +
                ", contenido='" + (contenido != null ? contenido.substring(0, Math.min(20, contenido.length())) + "..." : "null") + '\'' +
                ", puntuacion=" + puntuacion +
                ", autores='" + autores + '\'' +
                ", anio='" + anio + '\'' +
                ", esFavorito=" + esFavorito +
                '}';
    }
}
