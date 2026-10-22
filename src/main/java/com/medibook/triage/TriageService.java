package com.medibook.triage;

import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class TriageService {
    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
    private final RestClient restClient;
    private final String projectId;
    private final String location;
    private final String model;

    public TriageService(RestClient.Builder restClientBuilder,
                         @Value("${app.vertex.project-id}") String projectId,
                         @Value("${app.vertex.location}") String location,
                         @Value("${app.vertex.model}") String model) {
        this.restClient = restClientBuilder.build();
        this.projectId = projectId;
        this.location = location;
        this.model = model;
    }

    public Map<String, String> triage(String symptoms) {
        if (projectId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI triage is not configured. Set VERTEX_PROJECT_ID and Google credentials.");
        }
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped(CLOUD_PLATFORM_SCOPE);
            credentials.refreshIfExpired();
            String endpoint = "https://aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent"
                    .formatted(projectId, location, model);
            String prompt = "You are a health information assistant, not a doctor. Do not diagnose or prescribe. "
                    + "Briefly summarize possible urgency, suggest an appropriate medical department, and "
                    + "recommend professional evaluation. Clearly tell the user to seek emergency services "
                    + "for severe or rapidly worsening symptoms. Symptoms: " + symptoms;
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("temperature", 0.2, "maxOutputTokens", 350));
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post().uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + credentials.getAccessToken().getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class);
            if (response == null) throw new IllegalStateException("Empty Vertex AI response");
            List<?> candidates = (List<?>) response.get("candidates");
            Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) candidate.get("content");
            List<?> parts = (List<?>) content.get("parts");
            String text = String.valueOf(((Map<?, ?>) parts.get(0)).get("text"));
            return Map.of("result", text);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Google Cloud credentials are unavailable for AI triage");
        } catch (RuntimeException exception) {
            if (exception instanceof ResponseStatusException statusException) throw statusException;
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Vertex AI triage request failed");
        }
    }
}