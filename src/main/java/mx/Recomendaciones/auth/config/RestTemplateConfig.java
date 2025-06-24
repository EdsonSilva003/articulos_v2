package mx.Recomendaciones.auth.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.time.Duration;
import java.nio.charset.StandardCharsets;

@Configuration
public class RestTemplateConfig {

    /**
     * Configuración mejorada de RestTemplate con soporte para GZIP y timeouts apropiados
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(15))  // Timeout de conexión: 15 segundos
                .setReadTimeout(Duration.ofSeconds(45))     // Timeout de lectura: 45 segundos
                .requestFactory(this::clientHttpRequestFactory)
                .build();
        
        // Configurar convertidores de mensajes para manejar diferentes tipos de respuesta
        restTemplate.getMessageConverters().add(0, new ByteArrayHttpMessageConverter());
        restTemplate.getMessageConverters().add(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        
        return restTemplate;
    }
    
    /**
     * Factory personalizada para configuraciones adicionales de HTTP
     */
    private SimpleClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);  // 15 segundos
        factory.setReadTimeout(45000);     // 45 segundos
        // Habilitar decompresión automática si es posible
        factory.setOutputStreaming(false);
        return factory;
    }
    
    /**
     * ObjectMapper configurado para manejar respuestas de APIs externas
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // Configurar para no fallar si hay propiedades desconocidas
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        return objectMapper;
    }
}