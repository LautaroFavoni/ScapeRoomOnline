package TurnosOnline.ScapeRoomOnline.Controller;

import TurnosOnline.ScapeRoomOnline.Persistance.entities.Turno;
import TurnosOnline.ScapeRoomOnline.Persistance.repository.TurnoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import org.apache.logging.log4j.Logger;
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

    private static final Logger logger = (Logger) LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    @Autowired
    private TurnoRepository turnoRepository;

    @Value("${mercadopago.access-token}")
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
            // Validar si se recibió el ID desde el query string
            String paymentId = queryDataId;

            // Si no se recibió el ID en la query, intentar extraerlo del cuerpo JSON
            if (paymentId == null && body != null) {
                paymentId = extraerPaymentId(body);
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

            // Actualizar el estado del turno en la base de datos
            String estado = payment.getStatus();
            Optional<Turno> turnoOpt = turnoRepository.findByPreferenceId(paymentId);
            if (turnoOpt.isPresent()) {
                Turno turno = turnoOpt.get();
                turno.setPago(String.valueOf("approved".equals(estado)));
                turnoRepository.save(turno);
                logger.info("Turno actualizado correctamente para el pago con ID: {}", paymentId);
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
            PaymentClient paymentClient = new PaymentClient();
            return paymentClient.get(Long.valueOf(paymentId));
        } catch (MPException | MPApiException e) {
            logger.error("Error al consultar el estado del pago con ID: {}", paymentId, e);
            return null;
        }
    }
}
