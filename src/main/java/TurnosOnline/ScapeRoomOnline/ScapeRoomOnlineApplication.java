package TurnosOnline.ScapeRoomOnline;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class ScapeRoomOnlineApplication {

	public static void main(String[] args) {
		// Cargar el archivo .env con las variables




		SpringApplication.run(ScapeRoomOnlineApplication.class, args);
	}
}
