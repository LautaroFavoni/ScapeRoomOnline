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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("api/mercadopago")
public class MercadoPagoWebhookController {

    @Autowired
    private TurnoRepository turnoRepository;

    public MercadoPagoWebhookController() {
        // Configurar el token de acceso de Mercado Pago
        MercadoPagoConfig.setAccessToken(System.getenv("APP_USR-3963746540724282-112213-dc5dcef92764c0c37bb055ecfaf1993f-2113837918"));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> recibirNotificacion(@RequestBody String body, @RequestHeader(value = "X-Mercadopago-Signature", required = false) String signature)  {
        try {
            // Validar la firma de la notificación (si aplica)
            //if (!validarFirma(signature, body)) {
            //    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Firma no válida");
            //}

            // Extraer el ID de la preferencia desde el cuerpo de la notificación
            String preferenceId = extraerPreferenceId(body);
            if (preferenceId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se pudo extraer el ID de la preferencia");
            }

            // Consultar el estado del pago utilizando el ID de la preferencia
            Payment payment = consultarEstadoPago(preferenceId);
            if (payment == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Pago no encontrado");
            }

            // Verificar y actualizar el estado del pago en la base de datos
            String estado = payment.getStatus(); // "approved", "pending", "rejected", etc.
            Optional<Turno> turnoOpt = turnoRepository.findByPreferenceId(preferenceId);
            if (turnoOpt.isPresent()) {
                Turno turno = turnoOpt.get();
                turno.setPago(String.valueOf("approved".equals(estado))); // Guardar "true" si es aprobado
                turnoRepository.save(turno);
                return ResponseEntity.ok("Pago procesado correctamente y turno actualizado");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Turno asociado no encontrado");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar la notificación");
        }
    }

    private boolean validarFirma(String signature, String body) {
        // Aquí puedes implementar una validación más avanzada si Mercado Pago lo requiere
        // Actualmente, esto es opcional y depende del nivel de seguridad que necesites
        return signature != null && !signature.isEmpty();
    }

    private String extraerPreferenceId(String body) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(body);
            return jsonNode.path("preference_id").asText(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Payment consultarEstadoPago(String preferenceId) {
        try {
            PaymentClient paymentClient = new PaymentClient();
            return paymentClient.get(Long.valueOf(preferenceId));
        } catch (MPException | MPApiException e) {
            e.printStackTrace();
            return null;
        }
    }
}
