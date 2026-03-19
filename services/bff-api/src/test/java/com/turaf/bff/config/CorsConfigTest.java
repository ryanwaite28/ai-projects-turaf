package com.turaf.bff.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "cors.allowed-origins=http://localhost:4200,https://app.dev.turafapp.com"
})
class CorsConfigTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testPreflightRequest_AllowedOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
            .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,PATCH"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
            .andExpect(header().exists("Access-Control-Max-Age"));
    }
    
    @Test
    void testPreflightRequest_MultipleAllowedOrigins() throws Exception {
        mockMvc.perform(options("/api/v1/organizations")
                .header("Origin", "https://app.dev.turafapp.com")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "https://app.dev.turafapp.com"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
    
    @Test
    void testActualRequest_WithOriginHeader() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login")
                .header("Origin", "http://localhost:4200"))
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
    
    @Test
    void testCorsHeaders_AllowedMethods() throws Exception {
        mockMvc.perform(options("/api/v1/experiments")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "DELETE"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,PATCH"));
    }
    
    @Test
    void testCorsHeaders_ExposedHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/metrics")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Access-Control-Expose-Headers"));
    }
    
    @Test
    void testCorsHeaders_MaxAge() throws Exception {
        mockMvc.perform(options("/api/v1/dashboard/overview")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Max-Age", "3600"));
    }
}
