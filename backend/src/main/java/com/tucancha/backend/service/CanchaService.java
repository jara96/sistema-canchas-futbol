package com.tucancha.backend.service;

import com.tucancha.backend.dto.CanchaRequest;
import com.tucancha.backend.entity.Cancha;
import com.tucancha.backend.exception.ResourceNotFoundException;
import com.tucancha.backend.repository.CanchaRepository;
import com.tucancha.backend.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CanchaService {

    private final CanchaRepository canchaRepository;
    private final ReservaRepository reservaRepository;

    @Transactional(readOnly = true)
    public List<Cancha> listarTodas() {
        return canchaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Cancha> listarActivas() {
        return canchaRepository.findByActivaTrue();
    }

    @Transactional(readOnly = true)
    public Cancha obtener(Long id) {
        return canchaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada: " + id));
    }

    public Cancha crear(CanchaRequest req) {
        Cancha c = Cancha.builder()
                .nombre(req.getNombre())
                .tipo(req.getTipo())
                .precioHora(req.getPrecioHora())
                .activa(req.getActiva() == null ? Boolean.TRUE : req.getActiva())
                .porcentajeSenia(req.getPorcentajeSenia() == null ? 50 : req.getPorcentajeSenia())
                .build();
        return canchaRepository.save(c);
    }

    public Cancha actualizar(Long id, CanchaRequest req) {
        Cancha c = obtener(id);
        c.setNombre(req.getNombre());
        c.setTipo(req.getTipo());
        c.setPrecioHora(req.getPrecioHora());
        if (req.getActiva() != null) c.setActiva(req.getActiva());
        if (req.getPorcentajeSenia() != null) c.setPorcentajeSenia(req.getPorcentajeSenia());
        return canchaRepository.save(c);
    }

    /**
     * Elimina la cancha si no tiene reservas. Si tiene, la archiva (activa=false)
     * para no perder integridad referencial.
     * @return true si se borró físicamente, false si se archivó.
     */
    public boolean eliminar(Long id) {
        Cancha c = obtener(id);
        long reservas = reservaRepository.countByCanchaId(id);
        if (reservas == 0) {
            canchaRepository.delete(c);
            return true;
        }
        c.setActiva(false);
        canchaRepository.save(c);
        return false;
    }
}
