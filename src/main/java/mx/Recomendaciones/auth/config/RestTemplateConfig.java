package mx.Recomendaciones.auth.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

@Configuration
public class RestTemplateConfig {

    // Método para crear un RestTemplate como bean
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();  // Crea una instancia de RestTemplate
    }
    
    // Método para crear un ObjectMapper como bean
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // Configurar para no fallar si hay propiedades desconocidas
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }
}   