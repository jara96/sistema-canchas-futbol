package com.tucancha.backend.enums;

public enum ModalidadTorneo {
    /** Reservar todos los turnos disponibles de una sola fecha. */
    DIA_ENTERO,
    /** Reservar todos los turnos disponibles en una lista de fechas. */
    VARIOS_DIAS,
    /** Reservar ciertos turnos (turnoIds) en una lista de fechas. */
    HORARIOS_VARIOS_DIAS,
    /** Reservar ciertos turnos un día de la semana específico durante un rango de fechas. */
    RECURRENTE_SEMANAL
}
