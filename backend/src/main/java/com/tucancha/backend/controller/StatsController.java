package com.tucancha.backend.controller;

import com.tucancha.backend.dto.StatsResponse;
import com.tucancha.backend.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public StatsResponse dashboard() {
        return statsService.dashboard();
    }
}
