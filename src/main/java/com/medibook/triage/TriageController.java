package com.medibook.triage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/triage")
public class TriageController {
    private final TriageService triageService;

    public TriageController(TriageService triageService) {
        this.triageService = triageService;
    }

    @PostMapping
    public Map<String, String> triage(@Valid @RequestBody TriageRequest request) {
        return triageService.triage(request.symptoms());
    }

    public record TriageRequest(@NotBlank @Size(max = 2000) String symptoms) {}
}