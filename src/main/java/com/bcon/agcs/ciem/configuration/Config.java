package com.bcon.agcs.ciem.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class Config {
    @Bean
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }
}
