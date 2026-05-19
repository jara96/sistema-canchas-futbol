package com.tucancha.backend.service;

import com.tucancha.backend.dto.ConfiguracionRequest;
import com.tucancha.backend.entity.ConfiguracionSistema;
import com.tucancha.backend.repository.ConfiguracionSistemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ConfiguracionService {

    private final ConfiguracionSistemaRepository repo;

    /** Devuelve la fila de configuración, creándola con defaults si no existe. */
    public ConfiguracionSistema obtener() {
        return repo.findById(ConfiguracionSistema.SINGLETON_ID)
                .orElseGet(() -> repo.save(ConfiguracionSistema.builder()
                        .id(ConfiguracionSistema.SINGLETON_ID)
                        .diasMaximoReserva(30)
                        .diasMaximoTorneo(90)
                        .build()));
    }

    public ConfiguracionSistema actualizar(ConfiguracionRequest req) {
        ConfiguracionSistema c = obtener();
        c.setDiasMaximoReserva(req.getDiasMaximoReserva());
        c.setDiasMaximoTorneo(req.getDiasMaximoTorneo());
        return repo.save(c);
    }
}
