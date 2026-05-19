package com.tucancha.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Cuando MercadoPago hace el back_url al backend (vía ngrok), redirigimos al
 * frontend público para que el usuario llegue a la SPA.
 */
@RestController
public class RedirectController {

    /** URL del frontend al que mandar al usuario después del pago. */
    @Value("${app.frontend.url:http://localhost}")
    private String frontendUrl;

    @GetMapping({"/mis-reservas", "/canchas", "/admin"})
    public ResponseEntity<Void> redirectToFrontend(HttpServletRequest req) {
        String path = req.getRequestURI();
        String qs = req.getQueryString();
        String target = frontendUrl.replaceAll("/+$", "") + path
                + (qs != null && !qs.isEmpty() ? "?" + qs : "");
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, target)
                .build();
    }
}
