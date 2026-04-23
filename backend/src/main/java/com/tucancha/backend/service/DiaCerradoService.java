package com.tucancha.backend.service;

import com.tucancha.backend.dto.DiaCerradoRequest;
import com.tucancha.backend.entity.DiaCerrado;
import com.tucancha.backend.exception.BadRequestException;
import com.tucancha.backend.repository.DiaCerradoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DiaCerradoService {

    private final DiaCerradoRepository repository;

    @Transactional(readOnly = true)
    public List<DiaCerrado> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DiaCerrado> listarProximos() {
        return repository.findByFechaGreaterThanEqualOrderByFechaAsc(LocalDate.now());
    }

    public DiaCerrado crear(DiaCerradoRequest req) {
        if (repository.existsByFecha(req.getFecha())) {
            throw new BadRequestException("Esa fecha ya está marcada como cerrada");
        }
        return repository.save(DiaCerrado.builder()
                .fecha(req.getFecha()).motivo(req.getMotivo()).build());
    }

    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new BadRequestException("Día cerrado no encontrado");
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean estaCerrado(LocalDate fecha) {
        return repository.existsByFecha(fecha);
    }
}
