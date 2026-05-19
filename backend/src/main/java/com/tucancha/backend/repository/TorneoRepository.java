package com.tucancha.backend.repository;

import com.tucancha.backend.entity.Torneo;
import com.tucancha.backend.enums.EstadoTorneo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TorneoRepository extends JpaRepository<Torneo, Long> {

    List<Torneo> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    @Query("SELECT t FROM Torneo t WHERE t.estado = :estado AND t.fechaCreacion < :limite")
    List<Torneo> findPorEstadoYAnterioresA(@Param("estado") EstadoTorneo estado,
                                           @Param("limite") LocalDateTime limite);
}
