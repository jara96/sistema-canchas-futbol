package com.tucancha.backend.enums;

public enum EstadoReserva {
    PENDIENTE,
    CONFIRMADA,
    /** Reserva cuyo día ya pasó (00:00 del día siguiente) y estaba CONFIRMADA. */
    FINALIZADA,
    CANCELADA
}
