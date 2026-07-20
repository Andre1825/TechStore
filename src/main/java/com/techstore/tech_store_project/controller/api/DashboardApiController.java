package com.techstore.tech_store_project.controller.api;

import com.techstore.tech_store_project.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * RF-14 / RF-15: Datos del dashboard en JSON para el frontend React.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final DashboardService dashboardService;

    public DashboardApiController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stock-bajo/count")
    public Map<String, Long> stockBajoCount() {
        return dashboardService.stockBajoCount();
    }

    @GetMapping("/stock-bajo")
    public List<Map<String, Object>> stockBajo() {
        return dashboardService.stockBajo();
    }

    @GetMapping
    public Map<String, Object> dashboard() {
        return dashboardService.dashboard();
    }
}
