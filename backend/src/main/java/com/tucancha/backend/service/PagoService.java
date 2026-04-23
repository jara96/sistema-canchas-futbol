package com.tucancha.backend.service;

import com.tucancha.backend.entity.Pago;
import com.tucancha.backend.entity.Reserva;
import com.tucancha.backend.enums.EstadoPago;
import com.tucancha.backend.enums.EstadoReserva;
import com.tucancha.backend.exception.ResourceNotFoundException;
import com.tucancha.backend.repository.PagoRepository;
import com.tucancha.backend.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PagoService {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;

    /**
     * Crea un pago en estado PENDIENTE asociado a la reserva.
     * En la integración real con MercadoPago se setea mpPreferenceId luego de crear la preferencia.
     */
    public Pago crearPagoPendiente(Long reservaId, String mpPreferenceId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));

        Pago pago = Pago.builder()
                .reserva(reserva)
                .monto(reserva.getSenia())
                .estado(EstadoPago.PENDIENTE)
                .mpPreferenceId(mpPreferenceId)
                .build();

        return pagoRepository.save(pago);
    }

    /**
     * Procesa una notificación de MercadoPago (webhook).
     * Actualiza el pago y, si fue aprobado, confirma la reserva.
     */
    public Pago procesarNotificacion(String mpPaymentId, EstadoPago nuevoEstado, String metodoPago) {
        Pago pago = pagoRepository.findByMpPaymentId(mpPaymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado: " + mpPaymentId));

        pago.setEstado(nuevoEstado);
        if (metodoPago != null) pago.setMetodoPago(metodoPago);

        if (nuevoEstado == EstadoPago.APROBADO) {
            Reserva reserva = pago.getReserva();
            reserva.setEstado(EstadoReserva.CONFIRMADA);
            reservaRepository.save(reserva);
        }

        return pagoRepository.save(pago);
    }

    @Transactional(readOnly = true)
    public List<Pago> listarPorReserva(Long reservaId) {
        return pagoRepository.findByReservaId(reservaId);
    }
}
