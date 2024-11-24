package TurnosOnline.ScapeRoomOnline.Controller;


import TurnosOnline.ScapeRoomOnline.Persistance.DTOs.TurnoConLinkDePago;
import TurnosOnline.ScapeRoomOnline.Persistance.DTOs.TurnoForCreation;
import TurnosOnline.ScapeRoomOnline.Persistance.entities.Sala;
import TurnosOnline.ScapeRoomOnline.Persistance.entities.Turno;
import TurnosOnline.ScapeRoomOnline.Persistance.repository.SalaRepository;
import TurnosOnline.ScapeRoomOnline.Persistance.repository.TurnoRepository;
import TurnosOnline.ScapeRoomOnline.Services.EmailService;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("public/api/turnos")
public class TurnoController {
    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private SalaRepository salaRepository;

    @Autowired
    private EmailService emailService;

    @PostMapping("crear")
    public ResponseEntity<?> createTurno(@RequestBody TurnoForCreation turnoDTO) {
        // Validar los datos del DTO antes de continuar
        if (turnoDTO.getimporteTotal().compareTo(BigDecimal.ZERO) <= 0) {
            return new ResponseEntity<>("El importe pagado debe ser mayor que cero", HttpStatus.BAD_REQUEST);
        }

        Optional<Sala> sala = salaRepository.findById(turnoDTO.getSalaId());
        if (sala.isPresent()) {
            // Verificar si ya existe un turno en la misma sala a la misma hora
            Optional<Turno> turnoExistente = turnoRepository.findBySalaIdAndDiaYHora(turnoDTO.getSalaId(), turnoDTO.getDiaYHora());
            if (turnoExistente.isPresent()) {
                return new ResponseEntity<>(HttpStatus.CONFLICT); // Conflicto si ya existe un turno
            }

            // Crear el nuevo turno
            Turno nuevoTurno = new Turno();
            nuevoTurno.setSala(sala.get());
            nuevoTurno.setDiaYHora(turnoDTO.getDiaYHora());
            nuevoTurno.setTelefono(turnoDTO.getTelefono());
            nuevoTurno.setNombre(turnoDTO.getNombre());
            nuevoTurno.setApellido(turnoDTO.getApellido());
            nuevoTurno.setdni(turnoDTO.getdni());
            nuevoTurno.setMail(turnoDTO.getMail());
            nuevoTurno.setJugadores(turnoDTO.getJugadores());
            nuevoTurno.setCupon(turnoDTO.getCupon());
            nuevoTurno.setimporteTotal(turnoDTO.getimporteTotal());
            nuevoTurno.setPago("false");

            Turno savedTurno = turnoRepository.saveAndFlush(nuevoTurno);

            // Configurar el SDK de Mercado Pago
            MercadoPagoConfig.setAccessToken("APP_USR-1593157515372911-112213-2494993db59cc5afd3d80634ce2641ee-264117743"); // Mejor usar variable de entorno

            // Crear un ítem para la preferencia
            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .title("Turno en sala " + sala.get().getNombre())
                    .quantity(1)
                    .unitPrice(turnoDTO.getimporteTotal()) // Asegúrate de que sea BigDecimal
                    .build();

            // Crear la preferencia
            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(List.of(itemRequest))
                    .build();

            PreferenceClient preferenceClient = new PreferenceClient();
            try {
                Preference preference = preferenceClient.create(preferenceRequest);
                String paymentLink = preference.getSandboxInitPoint(); // Usar getInitPoint() para producción

                // Guardar el preferenceId en el turno y actualizar en la base de datos
                savedTurno.setPreferenceId(preference.getId());
                turnoRepository.save(savedTurno);

                // Enviar correo con el enlace de pago
                String subject = "Confirmación de Turno - Pago Pendiente";
                String body = "Tu turno ha sido reservado exitosamente para la sala: " + sala.get().getNombre() +
                        " a las " + turnoDTO.getDiaYHora() + ". Para completar el pago, haz clic en el siguiente enlace: " +
                        paymentLink;
                emailService.sendEmail(turnoDTO.getMail(), subject, body);

                // Crear el objeto de respuesta con el turno y el enlace de pago
                TurnoConLinkDePago response = new TurnoConLinkDePago(savedTurno, paymentLink);

                return new ResponseEntity<>(response, HttpStatus.CREATED);

            } catch (MPException | MPApiException e) {
                // Manejo de excepciones más limpio
                e.printStackTrace();
                return new ResponseEntity<>("Error al procesar el pago con Mercado Pago: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return ResponseEntity.notFound().build(); // Sala no encontrada
        }
    }





    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteTurno(@PathVariable Long id) {
        Optional<Turno> turno = turnoRepository.findById(id);
        if (turno.isPresent()) {
            turnoRepository.delete(turno.get());
            return ResponseEntity.noContent().build(); // Retorna 204 No Content al eliminar correctamente
        } else {
            return ResponseEntity.notFound().build(); // Retorna 404 Not Found si no se encuentra el turno
        }
    }


    @GetMapping("/today")
    public List<Turno> getTurnosFromToday() {
        LocalDateTime today = LocalDateTime.now();
        return turnoRepository.findByDiaYHoraAfter(today);
    }
    @GetMapping("/all")
    public List<Turno> getAllTurnos() {
        return turnoRepository.findAll();
    }
}
