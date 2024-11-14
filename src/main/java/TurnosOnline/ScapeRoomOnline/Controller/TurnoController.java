package TurnosOnline.ScapeRoomOnline.Controller;


import TurnosOnline.ScapeRoomOnline.Persistance.DTOs.TurnoForCreation;
import TurnosOnline.ScapeRoomOnline.Persistance.entities.Sala;
import TurnosOnline.ScapeRoomOnline.Persistance.entities.Turno;
import TurnosOnline.ScapeRoomOnline.Persistance.repository.SalaRepository;
import TurnosOnline.ScapeRoomOnline.Persistance.repository.TurnoRepository;
import TurnosOnline.ScapeRoomOnline.Services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Turno> createTurno( @RequestBody TurnoForCreation turnoDTO) {
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
            nuevoTurno.setPago(turnoDTO.getPago());
            nuevoTurno.setImportePagado(turnoDTO.getImportePagado());

            // Guardar el nuevo turno en la base de datos
            Turno savedTurno = turnoRepository.saveAndFlush(nuevoTurno);

            // Enviar correo al usuario
            String subject = "Confirmación de Turno";
            String body = "Tu turno ha sido reservado exitosamente para la sala: " + sala.get().getNombre() +
                    " a las " + turnoDTO.getDiaYHora().toString() + ". ¡Te esperamos!";
            emailService.sendEmail(turnoDTO.getMail(), subject, body); // Asegúrate de enviar al correo, no al teléfono

            return new ResponseEntity<>(savedTurno, HttpStatus.CREATED);
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
