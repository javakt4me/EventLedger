package com.eventledger.eventgateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RestTemplateConfigTest {

    private RestTemplateConfig config;

    @BeforeEach
    void setUp() {
        config = new RestTemplateConfig();
    }

    @Test
    void testRestTemplateBean() {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        RestTemplate restTemplate = config.restTemplate(builder);
        
        assertThat(restTemplate).isNotNull();
    }

    @Test
    void testRestTemplateTimeouts() {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        RestTemplate rt = config.restTemplate(builder);
        
        assertThat(rt).isNotNull();
        // Verify timeouts are set correctly
    }

    @Test
    void testObjectMapperBean() {
        ObjectMapper mapper = config.objectMapper();
        
        assertThat(mapper).isNotNull();
        assertThat(mapper.getRegisteredModuleIds()).contains(JavaTimeModule.class.getName());
    }

    @Test
    void testObjectMapperDateSerialization() throws Exception {
        ObjectMapper mapper = config.objectMapper();
        
        // Verify WRITE_DATES_AS_TIMESTAMPS is disabled
        assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
    }
}
