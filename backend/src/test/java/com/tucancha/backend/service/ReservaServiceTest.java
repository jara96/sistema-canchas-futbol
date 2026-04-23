package com.tucancha.backend.service;

import com.tucancha.backend.dto.ReservaRequest;
import com.tucancha.backend.entity.Cancha;
import com.tucancha.backend.entity.Reserva;
import com.tucancha.backend.entity.Turno;
import com.tucancha.backend.entity.Usuario;
import com.tucancha.backend.enums.EstadoReserva;
import com.tucancha.backend.enums.TipoCancha;
import com.tucancha.backend.exception.BadRequestException;
import com.tucancha.backend.repository.CanchaRepository;
import com.tucancha.backend.repository.DiaCerradoRepository;
import com.tucancha.backend.repository.ReservaRepository;
import com.tucancha.backend.repository.TurnoRepository;
import com.tucancha.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private CanchaRepository canchaRepository;
    @Mock private TurnoRepository turnoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private DiaCerradoRepository diaCerradoRepository;

    @InjectMocks private ReservaService reservaService;

    private Cancha cancha;
    private Turno turno;
    private Usuario usuario;
    private ReservaRequest request;

    @BeforeEach
    void setUp() {
        cancha = Cancha.builder()
                .id(1L).nombre("Cancha 1").tipo(TipoCancha.F5)
                .precioHora(new BigDecimal("10000.00")).activa(true)
                .porcentajeSenia(50).build();

        turno = Turno.builder()
                .id(1L).horaInicio(LocalTime.of(18, 0))
                .horaFin(LocalTime.of(19, 0)).activo(true).build();

        usuario = Usuario.builder().id(1L).email("u@u.com").nombre("U").build();

        request = new ReservaRequest();
        request.setCanchaId(1L);
        request.setTurnoId(1L);
        request.setFecha(LocalDate.now().plusDays(1));
    }

    @Test
    void crear_calculaTotalYSeniaCorrectamente() {
        when(canchaRepository.findById(1L)).thenReturn(Optional.of(cancha));
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turno));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(reservaRepository.existsOcupado(any(), any(), any(), any())).thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        Reserva r = reservaService.crear(request, 1L);

        assertThat(r.getTotal()).isEqualByComparingTo("10000.00");
        assertThat(r.getSenia()).isEqualByComparingTo("5000.00");
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
    }

    @Test
    void crear_falla_siEsDuplicada() {
        when(canchaRepository.findById(1L)).thenReturn(Optional.of(cancha));
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turno));
        when(reservaRepository.existsOcupado(any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> reservaService.crear(request, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ya está reservado");
    }

    @Test
    void crear_falla_siFechaPasada() {
        request.setFecha(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> reservaService.crear(request, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("anterior a hoy");
    }

    @Test
    void crear_falla_siCanchaInactiva() {
        cancha.setActiva(false);
        when(canchaRepository.findById(1L)).thenReturn(Optional.of(cancha));

        assertThatThrownBy(() -> reservaService.crear(request, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no está activa");
    }

    @Test
    void cancelar_falla_siNoEsDueñoNiAdmin() {
        Reserva r = Reserva.builder().id(99L).usuario(usuario).estado(EstadoReserva.PENDIENTE).build();
        when(reservaRepository.findById(99L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> reservaService.cancelar(99L, 999L, false))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void cancelar_admin_puedeCancelarAjenas() {
        Reserva r = Reserva.builder().id(99L).usuario(usuario).estado(EstadoReserva.PENDIENTE).build();
        when(reservaRepository.findById(99L)).thenReturn(Optional.of(r));
        when(reservaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Reserva cancelada = reservaService.cancelar(99L, 999L, true);
        assertThat(cancelada.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
    }
}
