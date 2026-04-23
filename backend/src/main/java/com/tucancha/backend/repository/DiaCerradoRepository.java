package com.tucancha.backend.repository;

import com.tucancha.backend.entity.DiaCerrado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DiaCerradoRepository extends JpaRepository<DiaCerrado, Long> {
    boolean existsByFecha(LocalDate fecha);
    List<DiaCerrado> findByFechaGreaterThanEqualOrderByFechaAsc(LocalDate desde);
}
