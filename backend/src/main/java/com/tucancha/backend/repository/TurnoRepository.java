package com.tucancha.backend.repository;

import com.tucancha.backend.entity.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {
    List<Turno> findByActivoTrueOrderByHoraInicioAsc();
}
