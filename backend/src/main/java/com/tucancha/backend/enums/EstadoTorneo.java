package com.tucancha.backend.enums;

public enum EstadoTorneo {
    PENDIENTE,    // creado, esperando pago
    CONFIRMADO,   // seña pagada, todas las reservas confirmadas
    EXPIRADO,     // no pagó dentro del tiempo límite
    CANCELADO
}
