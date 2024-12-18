package TurnosOnline.ScapeRoomOnline.Controller;

import TurnosOnline.ScapeRoomOnline.Persistance.entities.Turno;
import TurnosOnline.ScapeRoomOnline.Persistance.repository.TurnoRepository;
import TurnosOnline.ScapeRoomOnline.Services.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("api/mercadopago")
public class MercadoPagoWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private EmailService emailService;  // Inyectamos el servicio de correo

    @Value("APP_USR-6994450906540579-121619-7cba43b016b031f3f6c2082781149ee1-2027426021")
    private String accessToken;

    public MercadoPagoWebhookController() {
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> recibirNotificacion(
            @RequestParam(value = "data.id", required = false) String queryDataId,
            @RequestBody(required = false) String body,
            @RequestHeader(value = "X-Mercadopago-Signature", required = false) String signature) {
        try {
            logger.info("Recibida notificación con Signature: {}", signature);
            logger.info("Query parameter data.id: {}", queryDataId);
            logger.info("Cuerpo del mensaje recibido: {}", body);

            // Validar si se recibió el ID desde el query string
            String paymentId = queryDataId;

            // Si no se recibió el ID en la query, intentar extraerlo del cuerpo JSON
            if (paymentId == null && body != null) {
                paymentId = extraerPaymentId(body);
                logger.info("Payment ID extraído del cuerpo JSON: {}", paymentId);
            }

            // Validar que se obtuvo un ID de pago válido
            if (paymentId == null) {
                logger.error("No se pudo obtener el ID de pago de la solicitud");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID de pago no encontrado en la solicitud");
            }

            // Consultar el estado del pago en Mercado Pago
            Payment payment = consultarEstadoPago(paymentId);
            if (payment == null) {
                logger.error("No se encontró el pago en Mercado Pago con ID: {}", paymentId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Pago no encontrado en Mercado Pago");
            }

            // Obtener el externalReference del pago (es el preferenceId o ID de referencia que buscas)
            Long preferenceId = Long.valueOf(payment.getExternalReference());
            logger.info("Preference ID asociado al pago: {}", preferenceId);

            // Actualizar el estado del turno en la base de datos
            String estado = payment.getStatus();
            Optional<Turno> turnoOpt = turnoRepository.findById(preferenceId);
            if (turnoOpt.isPresent()) {
                Turno turno = turnoOpt.get();
                turno.setPago("approved".equals(estado) ? "true" : "false");

                // Guardar el dataId de la transacción en el turno
                turno.setDataId(payment.getId().toString()); // Usamos el ID del pago (dataId)
                turnoRepository.save(turno);
                logger.info("Turno actualizado correctamente para el pago con ID: {}", paymentId);

                // Si el pago fue aprobado, enviar un correo de confirmación
                if ("approved".equals(estado)) {
                    String subject = "Confirmación de pago de tu turno";
                    String bodyMessage = """
<html>
    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
        <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
            <!-- Título -->
            <h1 style="text-align: center; color: #ff7e00;">¡Pago confirmado!</h1>
            <hr style="border: 0; border-top: 1px solid #ddd; margin: 20px 0;">
            <!-- Introducción -->
            <p><strong>Tu número de reserva es</strong>: %s </p>
            <hr style="border: 0; border-top: 1px solid #ddd; margin: 20px 0;">
            <p>¡Nos alegra mucho anunciarte que tu pago ha sido confirmado! Te esperamos el <strong>%s</strong>, a las <strong>%s</strong>, para disfrutar juntos de una experiencia inmersiva única de la que no podrás escapar tan fácilmente. Prepárate para poner a prueba tus habilidades, resolver enigmas y enfrentar desafíos con tu equipo en nuestra sala de escape. ¿Estás listo para vivir la aventura?</p>
            <hr style="border: 0; border-top: 1px solid #ddd; margin: 20px 0;">
            <!-- Instagram -->
            <p>Te invitamos a seguirnos en <strong>Instagram</strong> para enterarte de las novedades, desafíos y sorteos exclusivos:</p>
            <p style="text-align: center; margin: 20px 0;">
                <a href="https://www.instagram.com/lockandkey.rosario?utm_source=ig_web_button_share_sheet&igsh=ZDNlZDc0MzIxNw==" style="color: #1a73e8; text-decoration: none; font-weight: bold;">Síguenos en Instagram</a>
            </p>
            <!-- Cierre -->
            <hr style="border: 0; border-top: 1px solid #ddd; margin: 20px 0;">
            <p style="text-align: center; font-weight: bold; margin-top: 20px;">¡Esperamos verte pronto para una nueva aventura!</p>
            <p style="text-align: center;">El equipo de <strong>Lock and Key</strong></p>
        </div>
    </body>
</html>
""".formatted(
                            turno.getId(),  // Número de reserva
                            turno.getDiaYHora().toLocalDate(),  // Fecha
                            turno.getDiaYHora().toLocalTime()  // Hora
                    );

                    emailService.sendEmail(turno.getMail(), subject, bodyMessage);
                    logger.info("Correo de confirmación enviado a: {}", turno.getMail());
                }

                return ResponseEntity.ok("Pago procesado y turno actualizado");
            } else {
                logger.warn("No se encontró un turno asociado al pago con ID: {}", paymentId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Turno asociado no encontrado");
            }
        } catch (Exception e) {
            logger.error("Error al procesar la notificación: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar la notificación");
        }
    }

    private String extraerPaymentId(String body) {
        try {
            logger.info("Intentando extraer el Payment ID del cuerpo: {}", body);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(body);
            return jsonNode.path("data").path("id").asText(null);
        } catch (Exception e) {
            logger.error("Error al extraer el ID del pago del cuerpo JSON: ", e);
            return null;
        }
    }

    private Payment consultarEstadoPago(String paymentId) {
        try {
            logger.info("Consultando el estado del pago con ID: {}", paymentId);
            PaymentClient paymentClient = new PaymentClient();
            return paymentClient.get(Long.valueOf(paymentId));
        } catch (MPException | MPApiException e) {
            logger.error("Error al consultar el estado del pago con ID: {}", paymentId, e);
            return null;
        }
    }
}
