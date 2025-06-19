package mx.Recomendaciones.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "mx.Recomendaciones.auth.config",
    "mx.Recomendaciones.auth.controller",
    "mx.Recomendaciones.auth.entity",
    "mx.Recomendaciones.auth.repository",
    "mx.Recomendaciones.auth.service",
    "mx.Recomendaciones.auth.SistemaAutenticacion"
})
@EnableJpaRepositories(basePackages = "mx.Recomendaciones.auth.repository")
@EntityScan(basePackages = "mx.Recomendaciones.auth.entity")
public class RecomendacionesApplication {
    public static void main(String[] args) {
        SpringApplication.run(RecomendacionesApplication.class, args);
    }
}
