package com.sparta.UserService.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Value("${supabase.auth.url}")
    private String supabaseAuthUrl;

    @Value("${supabase.api.key}")
    private String supabaseApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> login(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", supabaseApiKey);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    supabaseAuthUrl,  // make sure the URL has grant_type=password
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null) {
                    return responseBody; // Return full JSON: { access_token, refresh_token, etc. }
                }
            }
            throw new RuntimeException("Unexpected error during login");

        } catch (HttpClientErrorException e) {
            // Handle Supabase errors (400, 401, etc.)
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST || e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new RuntimeException("Invalid email or password");
            }
            // Re-throw other errors
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }
}
