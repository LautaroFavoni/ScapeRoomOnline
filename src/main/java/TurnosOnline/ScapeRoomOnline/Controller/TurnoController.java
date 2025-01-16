package TurnosOnline.ScapeRoomOnline.Controller;


import TurnosOnline.ScapeRoomOnline.Persistance.DTOs.TurnoConLinkDePago;
import TurnosOnline.ScapeRoomOnline.Persistance.DTOs.TurnoForCreation;
import TurnosOnline.ScapeRoomOnline.Persistance.entities.Sala;
import TurnosOnline.ScapeRoomOnline.Persistance.entities.Turno;
import TurnosOnline.ScapeRoomOnline.Persistance.repository.SalaRepository;
import TurnosOnline.ScapeRoomOnline.Persistance.repository.TurnoRepository;
import TurnosOnline.ScapeRoomOnline.Services.EmailService;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.not;

@RestController
@RequestMapping("public/api/turnos")
public class TurnoController {
    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private SalaRepository salaRepository;

    @Autowired
    private EmailService emailService;

    @Value("APP_USR-6994450906540579-121619-7cba43b016b031f3f6c2082781149ee1-2027426021")
    private String accessToken;

    @PostMapping("crear")
    public ResponseEntity<?> createTurno(@RequestBody TurnoForCreation turnoDTO) {
        // Validar los datos del DTO antes de continuar
        if (turnoDTO.getimporteTotal().compareTo(BigDecimal.ZERO) <= 0) {
            return new ResponseEntity<>("El importe pagado debe ser mayor que cero", HttpStatus.BAD_REQUEST);
        }

        System.out.println("LLegue a 1 " );

        Optional<Sala> sala = salaRepository.findById(turnoDTO.getSalaId());
        if (sala.isEmpty()) {
            return new ResponseEntity<>("Sala no encontrada", HttpStatus.NOT_FOUND);
        }

        System.out.println("LLegue a 2 " );
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
            nuevoTurno.setAsistio(false);

            Turno savedTurno = turnoRepository.saveAndFlush(nuevoTurno);

            MercadoPagoConfig.setAccessToken(accessToken);

            System.out.println("LLegue a 3 " );

            // Crear un ítem para la preferencia
            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .title("Turno en sala " + sala.get().getNombre())
                    .quantity(1)

                    .unitPrice(turnoDTO.getimporteTotal()) // Asegúrate de que sea BigDecimal
                    .build();

            System.out.println("LLegue a 4 " );

            // Calcular la fecha y hora de expiración (15 minutos a partir de ahora)
            LocalDateTime expirationDate = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            String expirationDateTo = expirationDate.format(formatter);

            // Configurar las URLs de retorno usando PreferenceBackUrlsRequest.Builder
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success("https://www.lockandkey.com.ar/")
                    .failure("https://www.lockandkey.com.ar/")
                    .pending("https://www.lockandkey.com.ar/")
                    .build();



            // Crear la preferencia
            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(List.of(itemRequest))
                    .externalReference(String.valueOf(savedTurno.getId()))
                    .backUrls(backUrls) // Usar las Back URLs configuradas con el Builder
                    .expirationDateTo(OffsetDateTime.parse(expirationDateTo)) // Establecer la fecha de expiración
                    .build();

            PreferenceClient preferenceClient = new PreferenceClient();

            System.out.println("LLegue a 5 " );

            try {
                Preference preference = preferenceClient.create(preferenceRequest);
                String paymentLink = preference.getInitPoint(); // Usar getInitPoint() para producción

                // Guardar el preferenceId en el turno y actualizar en la base de datos
                savedTurno.setPreferenceId(preference.getId());
                turnoRepository.save(savedTurno);

                System.out.println("LLegue a 8 " );

                // Enviar correo con el enlace de pago
                String subject = "Confirmación de Turno - Pago Pendiente";
                String body = """
<html>
    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
        <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
            <!-- Título -->
            <h1 style="text-align: center; color: #ff7e00;">¡Tu reserva ha sido realizada con éxito!</h1>
            <!-- Introducción -->
            <p>Hola <strong>%s</strong>,</p>
            <p>Gracias por reservar con nosotros. Estamos emocionados de recibirte y hacer que vivas una experiencia inolvidable. Aquí están los detalles de tu reserva:</p>
            <ul style="list-style-type: none; padding: 0;">
                <li><strong>Fecha:</strong> %s</li>
                <li><strong>Hora:</strong> %s</li>
                <li><strong>Sala:</strong> %s</li>
                <li><strong>Cantidad de jugadores:</strong> %d</li>
                <li><strong>Total a abonar:</strong> $%s</li>
            </ul>

            <!-- Separador -->
            <hr style="border: 0; border-top: 1px solid #ddd; margin: 20px 0;">

            <!-- Estado de la reserva -->
            <h3 style="color: #F57C00;">Estado de tu reserva</h3>
            <p>Tu turno ha sido reservado y está actualmente <strong>pendiente de pago</strong>. Para confirmar tu reserva, por favor realiza el pago a través del siguiente enlace:</p>
            <p style="text-align: center; margin: 20px 0;">
                <a href="%s" style="background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;">Haz clic aquí para pagar</a>
            </p>
            <p>Una vez procesado el pago, recibirás un correo confirmando tu turno.</p>

            <!-- Separador -->
            <hr style="border: 0; border-top: 1px solid #ddd; margin: 20px 0;">

            <!-- Información adicional -->
            <h3 style="color: #F57C00;">Información adicional</h3>
            <ul style="list-style-type: none; padding: 0;">
                <li>Por favor, llegar <strong>15 minutos antes</strong> de la hora programada para recibir las instrucciones.</li>
                <li>Si necesitas reprogramar tu reserva, contáctanos con al menos <strong>24 horas de anticipación</strong>.</li>
            </ul>

            <!-- Separador -->
            <hr style="border: 0; border-top: 1px solid #ddd; margin: 20px 0;">

            <!-- Ubicación y contacto -->
            <h3 style="color: #FF5722;">Ubicación y contacto</h3>
            <ul style="list-style-type: none; padding: 0;">
                <li><strong>Dirección:</strong> Buenos Aires 1415, Rosario, Santa Fe</li>
                <li><strong>Teléfono:</strong> +54 9 3417 03-7222</li>
                <li><strong>Email:</strong> contacto@lockandkey.com.ar</li>
            </ul>

            <!-- Cierre -->
            <p style="text-align: center; font-weight: bold; margin-top: 20px;">¡Te esperamos para vivir esta increíble aventura!</p>
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

                System.out.println("LLegue a 9 " );

                emailService.sendEmail(turnoDTO.getMail(), subject, body);

                System.out.println("LLegue a 6 " );
                // Crear el objeto de respuesta con el turno y el enlace de pago
                TurnoConLinkDePago response = new TurnoConLinkDePago(savedTurno, paymentLink);

                System.out.println("LLegue a 7" );
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

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTurno(@PathVariable Long id, @RequestBody TurnoForCreation updatedTurnoDTO) {
        // Buscar el turno existente por ID
        Optional<Turno> turnoOptional = turnoRepository.findById(id);

        if (turnoOptional.isEmpty()) {
            return new ResponseEntity<>("Turno no encontrado", HttpStatus.NOT_FOUND);
        }

        Turno existingTurno = turnoOptional.get();

        // Verificar si la sala existe
        Optional<Sala> salaOptional = salaRepository.findById(updatedTurnoDTO.getSalaId());
        if (salaOptional.isEmpty()) {
            return new ResponseEntity<>("Sala no encontrada", HttpStatus.NOT_FOUND);
        }

        Sala sala = salaOptional.get();


        // Validar si la sala ha cambiado
        if (!existingTurno.getSala().getId().equals(updatedTurnoDTO.getSalaId())) {
            List<Turno> conflictingTurnos = turnoRepository.findPaidTurnosBySalaIdAndDiaYHora(updatedTurnoDTO.getSalaId(), updatedTurnoDTO.getDiaYHora());
            if (!conflictingTurnos.isEmpty() && !conflictingTurnos.get(0).getId().equals(id)) {
                return new ResponseEntity<>("Ya existe un turno reservado y pagado para esta sala a la misma hora", HttpStatus.CONFLICT);
            }

            // Actualizar la sala
            Optional<Sala> nuevaSala = salaRepository.findById(updatedTurnoDTO.getSalaId());
            if (nuevaSala.isEmpty()) {
                return new ResponseEntity<>("Sala no encontrada", HttpStatus.NOT_FOUND);
            }
            existingTurno.setSala(nuevaSala.get());
        }

        // Validar si la hora del turno ha cambiado
        if (!existingTurno.getDiaYHora().equals(updatedTurnoDTO.getDiaYHora())) {
            List<Turno> conflictingTurnos = turnoRepository.findPaidTurnosBySalaIdAndDiaYHora(updatedTurnoDTO.getSalaId(), updatedTurnoDTO.getDiaYHora());
            if (!conflictingTurnos.isEmpty() && !conflictingTurnos.get(0).getId().equals(id)) {
                return new ResponseEntity<>("Ya existe un turno reservado y pagado para esta sala a la misma hora", HttpStatus.CONFLICT);
            }

            // Actualizar la hora
            existingTurno.setDiaYHora(updatedTurnoDTO.getDiaYHora());
        }

        // Actualizar los valores del turno existente
        existingTurno.setTelefono(updatedTurnoDTO.getTelefono());
        existingTurno.setNombre(updatedTurnoDTO.getNombre());
        existingTurno.setApellido(updatedTurnoDTO.getApellido());
        existingTurno.setMail(updatedTurnoDTO.getMail());
        existingTurno.setJugadores(updatedTurnoDTO.getJugadores());
        existingTurno.setCupon(updatedTurnoDTO.getCupon());
        existingTurno.setimporteTotal(updatedTurnoDTO.getimporteTotal());
        existingTurno.setAsistio(updatedTurnoDTO.getAsistio());
        existingTurno.setPago(updatedTurnoDTO.getPago());

        // Guardar los cambios en la base de datos
        Turno updatedTurno = turnoRepository.save(existingTurno);

        return new ResponseEntity<>(updatedTurno, HttpStatus.OK);
    }

    @PutMapping("/marcarAsistencia")
    public ResponseEntity<?> marcarAsistencia(@RequestParam Long id, @RequestParam boolean asistio) {
        // Buscar el turno en la base de datos
        Optional<Turno> turnoOptional = turnoRepository.findById(id);

        if (turnoOptional.isEmpty()) {
            return new ResponseEntity<>("Turno no encontrado", HttpStatus.NOT_FOUND);
        }

        Turno turno = turnoOptional.get();
        turno.setAsistio(asistio);
        turnoRepository.save(turno);

        // Si el cliente asistió, enviar correo de agradecimiento
        if (asistio) {
            String subject = "Gracias por tu visita ";
            String body = """
                <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                            <!-- Título -->
                            <h1 style="text-align: center; color: #ff7e00;">¡Gracias por tu visita a Lock and Key!</h1>

                            <!-- Introducción -->
                            <hr style="border: 0; border-top: 1px solid #ddd; margin: 20px 0;">
                            <p>¡Hola <strong>%s</strong>!</p>
                            <p>Gracias por elegir <strong>Lock and Key</strong> para tu aventura. Nos encantó tenerte en nuestro juego y esperamos que lo hayas disfrutado tanto como nosotros disfrutamos de tu visita.</p>

                            <!-- Solicitud de reseña -->
                            <p>Si te divertiste resolviendo enigmas y superando desafíos, <strong>nos ayudaría mucho si compartes tu experiencia</strong>.</p>
                            <p style="text-align: center; margin: 20px 0;">
                                <a href="https://maps.app.goo.gl/LgNjnXwxuBCqftpT8" style="color: #1a73e8; text-decoration: none; font-weight: bold;">Déjanos tu reseña <span style="text-decoration: underline;">aquí</span>.</a>
                            </p>

                            <hr style="border: 0; border-top: 1px solid #ddd; margin: 20px 0;">
                            <!-- Instagram -->
                            <p>Además, te invitamos a seguirnos en <strong>Instagram</strong> para enterarte de las novedades, desafíos y sorteos exclusivos:</p>
                            <p style="text-align: center; margin: 20px 0;">
                                <a href="https://www.instagram.com/lockandkey.rosario?utm_source=ig_web_button_share_sheet&igsh=ZDNlZDc0MzIxNw==" style="color: #1a73e8; text-decoration: none; font-weight: bold;">Síguenos en Instagram</a>
                            </p>

                            <hr style="border: 0; border-top: 1px solid #ddd; margin: 20px 0;">
                            <!-- Cierre -->
                            <p style="text-align: center; font-weight: bold; margin-top: 20px;">¡Esperamos verte pronto para una nueva aventura!</p>
                            <p style="text-align: center;">El equipo de <strong>Lock and Key</strong></p>
                        </div>
                    </body>
                </html>
        """.formatted(turno.getNombre());  // Nombre del cliente

            emailService.sendEmail(turno.getMail(), subject, body);
        }

        String message = asistio
                ? "El estado de asistencia se ha actualizado exitosamente y se envió un correo de agradecimiento."
                : "El estado de asistencia se ha actualizado exitosamente.";

        return new ResponseEntity<>(message, HttpStatus.OK);
    }


}
