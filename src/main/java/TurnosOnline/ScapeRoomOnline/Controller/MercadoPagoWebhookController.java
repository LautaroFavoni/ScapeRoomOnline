package TurnosOnline.ScapeRoomOnline.Controller;

import TurnosOnline.ScapeRoomOnline.Persistance.entities.Turno;
import TurnosOnline.ScapeRoomOnline.Persistance.repository.TurnoRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.client.payment.PaymentClient;
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


    @PostMapping("/webhook")
    public ResponseEntity<String> recibirNotificacion(@RequestBody String body, @RequestHeader("X-Mercadopago-Signature") String signature) {
        try {
            // Validar la firma de la notificación (opcional, para seguridad)
            if (!validarFirma(signature, body)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Firma no válida");
            }

            // Procesar la notificación JSON
            // Mercado Pago envía el estado del pago en el cuerpo de la solicitud.
            // Aquí supongo que el cuerpo tiene información en formato JSON con el estado del pago
            // En este caso, deberías ajustar la deserialización según el formato real.

            // Este es el ID del pago, que puedes usar para consultar el estado del pago
            String preferenceId = extraerPreferenceId(body);

            // Consultar el estado del pago utilizando el ID de la preferencia
            Payment payment = consultarEstadoPago(preferenceId);

            if (payment != null) {
                // Aquí puedes verificar el estado y actualizar la base de datos
                String estado = payment.getStatus(); // Puede ser "approved", "pending", "rejected", etc.

                // Obtener el turno asociado con el preferenceId
                Optional<Turno> turnoOpt = turnoRepository.findByPreferenceId(preferenceId);
                if (turnoOpt.isPresent()) {
                    Turno turno = turnoOpt.get();
                    turno.setPago(estado.equals("approved") ? "true" : "false");
                    turnoRepository.save(turno);
                }

                return ResponseEntity.ok("Pago procesado correctamente");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Pago no encontrado");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar la notificación");
        }
    }

    private boolean validarFirma(String signature, String body) {
        // Aquí debes implementar la validación de la firma para asegurarte de que la notificación es legítima.
        // Mercado Pago te proporciona una forma de validar la firma.
        return true; // Para fines de este ejemplo, consideramos que la firma es válida.
    }

    private String extraerPreferenceId(String body) {
        // Extraer el preferenceId del cuerpo del JSON
        // Esto dependerá del formato exacto de la notificación de Mercado Pago
        // Por ejemplo, si la notificación es un JSON, deberías hacer algo como:
        // JSONObject json = new JSONObject(body);
        // return json.getString("preference_id");
        return "id-del-pago"; // Para fines de ejemplo
    }

    private Payment consultarEstadoPago(String preferenceId) {
        // Configurar el SDK de Mercado Pago
        MercadoPagoConfig.setAccessToken(System.getenv("MERCADO_PAGO_ACCESS_TOKEN"));

        PaymentClient paymentClient = new PaymentClient();
        try {
            // Consultar el estado del pago utilizando el preferenceId
            Payment payment = paymentClient.get(Long.valueOf(preferenceId));
            return payment;
        } catch (MPException | MPApiException e) {
            e.printStackTrace();
            return null;
        }
    }
}
