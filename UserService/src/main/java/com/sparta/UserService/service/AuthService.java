package com.sparta.UserService.service;

import com.sparta.UserService.model.UserDetails;
import com.sparta.UserService.repository.RegisterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AuthService {

    @Value("${supabase.auth.url}")
    private String supabaseAuthUrl;

    @Value("${supabase.api.key}")
    private String supabaseApiKey;

    @Value("${supabase.service.key}")
    private String supabseServiceRole;



    @Autowired
    private RegisterRepository register;



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
                    supabaseAuthUrl+"token?grant_type=password",  // make sure the URL has grant_type=password
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
    public Map<String, Object> signup(String email, String password,String firstname, String lastname, String username) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", supabaseApiKey);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    supabaseAuthUrl+"signup",  // e.g., https://<project>.supabase.co/auth/v1/signup
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                Map<String, Object> responseBody = response.getBody();
                if (!responseBody.isEmpty() && responseBody.containsKey("user")) {
                    Map<String, Object> userMap = (Map<String, Object>) responseBody.get("user");
                    String userIdString = (String) userMap.get("id");

                    UserDetails details=new UserDetails();
                    details.setId(UUID.fromString(userIdString));
                    details.setFirstname(firstname);
                    details.setLastname(lastname);
                    details.setUsername(username);

                    register.save(details);


                    return responseBody;
                }
            }
            throw new RuntimeException("Unexpected error during signup");

        } catch (HttpClientErrorException e) {
            System.err.println("Supabase error body: " + e.getResponseBodyAsString());
            throw new RuntimeException("Signup failed: " + e.getResponseBodyAsString());
        }
    }
    public boolean emailExists(String email) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", supabseServiceRole);  // Can also use supabaseServiceKey
        headers.set("Authorization", "Bearer " + supabseServiceRole); // Use service role key here
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = supabaseAuthUrl + "/admin/users?email=" + email;

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                List<?> users = (List<?>) response.getBody().get("users");
                return users != null && !users.isEmpty();
            }
            return false;

        } catch (HttpClientErrorException e) {
            System.err.println("Error checking email existence: " + e.getResponseBodyAsString());
            throw new RuntimeException("Email check failed: " + e.getResponseBodyAsString());
        }
    }

    public ResponseEntity<String> updatePasswordByUID(UUID userId, String newPassword) {


        String url = supabaseAuthUrl + "admin/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + supabseServiceRole);

        Map<String, Object> body = new HashMap<>();
        body.put("password", newPassword);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = new RestTemplate().exchange(
                    url,
                    HttpMethod.PATCH,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return ResponseEntity.ok("Password updated successfully");
            } else {
                return ResponseEntity.status(response.getStatusCode()).body("Failed to update password");
            }

        } catch (HttpClientErrorException e) {
            System.err.println("Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body("Failed to update password: " + e.getResponseBodyAsString());
        }
    }





}
