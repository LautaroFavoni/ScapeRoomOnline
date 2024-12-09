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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
        if (sala.isEmpty()) {
            return new ResponseEntity<>("Sala no encontrada", HttpStatus.NOT_FOUND);
        }

        try {
            // Buscar turnos pagados en la misma sala y horario
            List<Turno> turnos = turnoRepository.findPaidTurnosBySalaIdAndDiaYHora(turnoDTO.getSalaId(), turnoDTO.getDiaYHora());
            if (!turnos.isEmpty()) {
                return new ResponseEntity<>("Ya existe un turno reservado y pagado para esta sala a la misma hora", HttpStatus.CONFLICT);
            }

            // Crear el nuevo turno
            Turno nuevoTurno = new Turno();
            nuevoTurno.setSala(sala.get());
            nuevoTurno.setDiaYHora(turnoDTO.getDiaYHora());
            nuevoTurno.setTelefono(turnoDTO.getTelefono());
            nuevoTurno.setNombre(turnoDTO.getNombre());
            nuevoTurno.setApellido(turnoDTO.getApellido());
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

            // Calcular la fecha y hora de expiración (15 minutos a partir de ahora)
            LocalDateTime expirationDate = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(15);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            String expirationDateTo = expirationDate.format(formatter);


            // Crear la preferencia
            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(List.of(itemRequest))
                    .externalReference(String.valueOf(savedTurno.getId()))
                    .expirationDateTo(OffsetDateTime.parse(expirationDateTo)) // Establecer la fecha de expiración
                    .build();

            PreferenceClient preferenceClient = new PreferenceClient();
            try {
                Preference preference = preferenceClient.create(preferenceRequest);
                String paymentLink = preference.getInitPoint(); // Usar getInitPoint() para producción

                // Guardar el preferenceId en el turno y actualizar en la base de datos
                savedTurno.setPreferenceId(preference.getId());
                turnoRepository.save(savedTurno);

                // Enviar correo con el enlace de pago
                String subject = "Confirmación de Turno - Pago Pendiente";
                String body = """
                        <html>
                            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                                <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                                    <h1 style="text-align: center; color: #4CAF50;">¡Tu reserva ha sido realizada con éxito!</h1>
                                    <p>Hola <strong>%s</strong>,</p>
                                    <p>Gracias por reservar con nosotros. Estamos emocionados de recibirte y hacer que vivas una experiencia inolvidable. Aquí están los detalles de tu reserva:</p>
                                    <ul style="list-style-type: none; padding: 0;">
                                        <li><strong>Fecha:</strong> %s</li>
                                        <li><strong>Hora:</strong> %s</li>
                                        <li><strong>Sala:</strong> %s</li>
                                        <li><strong>Cantidad de jugadores:</strong> %d</li>
                                        <li><strong>Total a abonar:</strong> $%s</li>
                                    </ul>
                                    <h3 style="color: #F57C00;">Estado de tu reserva</h3>
                                    <p>Tu turno ha sido reservado y está actualmente <strong>pendiente de pago</strong>. Para confirmar tu reserva, por favor realiza el pago a través del siguiente enlace:</p>
                                    <p style="text-align: center;">
                                        <a href="%s" style="background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Haz clic aquí para pagar</a>
                                    </p>
                                    <p>Una vez procesado el pago, recibirás un correo confirmando tu turno.</p>
                                    <h3 style="color: #2196F3;">Información adicional</h3>
                                    <ul style="list-style-type: none; padding: 0;">
                                        <li>Por favor, llegar <strong>15 minutos antes</strong> de la hora programada para recibir las instrucciones.</li>
                                        <li>Si necesitas reprogramar tu reserva, contáctanos con al menos <strong>24 horas de anticipación</strong>.</li>
                                    </ul>
                                    <h3 style="color: #FF5722;">Ubicación y contacto</h3>
                                    <ul style="list-style-type: none; padding: 0;">
                                        <li><strong>Dirección:</strong> Buenos Aires 1415, Rosario, Santa Fe</li>
                                        <li><strong>Teléfono:</strong> +54 9 3417 03-7222</li>
                                        <li><strong>Email:</strong> contacto@lockandkey.com.ar</li>
                                    </ul>
                                    <p style="text-align: center; font-weight: bold;">¡Te esperamos para vivir esta increíble aventura!</p>
                                    <p style="text-align: center;">El equipo de <strong>Lock & Key</strong></p>
                                </div>
                            </body>
                        </html>
                        """.formatted(
                        turnoDTO.getNombre(),  // Nombre del cliente
                        turnoDTO.getDiaYHora().toLocalDate(),  // Fecha
                        turnoDTO.getDiaYHora().toLocalTime(),  // Hora
                        sala.get().getNombre(),  // Nombre de la sala
                        turnoDTO.getJugadores(),  // Cantidad de jugadores
                        turnoDTO.getimporteTotal(),  // Total a abonar
                        paymentLink  // Enlace de pago
                );

                emailService.sendEmail(turnoDTO.getMail(), subject, body);


                // Crear el objeto de respuesta con el turno y el enlace de pago
                TurnoConLinkDePago response = new TurnoConLinkDePago(savedTurno, paymentLink);

                return new ResponseEntity<>(response, HttpStatus.CREATED);

            } catch (MPException | MPApiException e) {
                // Manejo de excepciones más limpio
                e.printStackTrace();
                return new ResponseEntity<>("Error al procesar el pago con Mercado Pago: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }catch (Exception e) {
            return new ResponseEntity<>("Error al procesar la solicitud: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

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
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay(); // Obtiene la medianoche de hoy
        return turnoRepository.findByDiaYHoraAfter(todayStart);
    }
    @GetMapping("/all")
    public List<Turno> getAllTurnos() {
        return turnoRepository.findAll();
    }
}
