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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.json.JSONObject;


import java.util.Optional;

@RestController
@RequestMapping("api/mercadopago")
public class MercadoPagoWebhookController {
    public String extraerPaymentId(String body) {
        try {
            JSONObject json = new JSONObject(body); // Usa org.json para parsear el JSON
            return json.optString("id", null); // Devuelve el ID si existe
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Manejo básico de errores
        }
    }



    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private EmailService emailService;

    public MercadoPagoWebhookController() {
        String accessToken = "APP_USR-1593157515372911-112213-2494993db59cc5afd3d80634ce2641ee-264117743";
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> recibirNotificacion(
            @RequestParam(value = "data.id", required = false) String queryDataId,
            @RequestBody(required = false) String body,
            @RequestHeader(value = "X-Mercadopago-Signature", required = false) String signature) {
        try {
            String paymentId = queryDataId;

            // Extraer el ID de pago del cuerpo JSON si no viene en el query
            if (paymentId == null && body != null) {
                paymentId = extraerPaymentId(body);
            }

            if (paymentId == null) {
                logger.error("No se pudo obtener el ID de pago de la solicitud");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID de pago no encontrado en la solicitud");
            }

            Payment payment = consultarEstadoPago(paymentId);
            if (payment == null) {
                logger.error("No se encontró el pago en Mercado Pago con ID: {}", paymentId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Pago no encontrado en Mercado Pago");
            }

            String estado = payment.getStatus();
            Optional<Turno> turnoOpt = turnoRepository.findByPreferenceId(paymentId);
            if (turnoOpt.isPresent()) {
                Turno turno = turnoOpt.get();

                if ("approved".equals(estado)) {
                    turno.setPago("true");
                    turnoRepository.save(turno);

                    // Enviar correo de confirmación
                    String subject = "Turno Confirmado - Pago Recibido";
                    String bodyEmail = "Tu turno ha sido confirmado exitosamente para la sala: " + turno.getSala().getNombre() +
                            " a las " + turno.getDiaYHora() + ". ¡Gracias por tu pago!";
                    emailService.sendEmail(turno.getMail(), subject, bodyEmail);

                    logger.info("Turno actualizado y correo enviado para el pago con ID: {}", paymentId);
                    return ResponseEntity.ok("Pago procesado, turno confirmado y correo enviado");
                } else {
                    logger.info("Pago no aprobado. Estado actual: {}", estado);
                    return ResponseEntity.ok("Pago no aprobado. Estado: " + estado);
                }
            } else {
                logger.warn("No se encontró un turno asociado al pago con ID: {}", paymentId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Turno asociado no encontrado");
            }
        } catch (Exception e) {
            logger.error("Error al procesar la notificación: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar la notificación");
        }
    }

    public Payment consultarEstadoPago(String paymentId) {
        try {
            PaymentClient paymentClient = new PaymentClient();
            Long id = Long.parseLong(paymentId);
            return paymentClient.get(id); // Obtiene el objeto Payment desde el SDK
        } catch (MPException | MPApiException e) {
            e.printStackTrace();
            return null; // Manejo básico de errores
        }


    }




}
