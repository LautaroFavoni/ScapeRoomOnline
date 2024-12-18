package TurnosOnline.ScapeRoomOnline.Services;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    public void sendEmail(String to, String subject, String text) {
        try {
            // Crear el mensaje MIME
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);

            // Establecer el remitente explícitamente
            messageHelper.setFrom("contacto@lockandkey.com.ar");  // Aquí estableces el remitente

            // Establecer el destinatario, el asunto y el contenido
            messageHelper.setTo(to);
            messageHelper.setSubject(subject);
            messageHelper.setText(text, true);  // El segundo parámetro indica que es HTML

            // Enviar el correo
            javaMailSender.send(mimeMessage);
            System.out.println("Correo enviado correctamente a " + to);
        } catch (MailException | MessagingException e) {
            e.printStackTrace();
            System.out.println("Error al enviar el correo a " + to);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error inesperado al enviar el correo a " + to);
        }
    }

}
