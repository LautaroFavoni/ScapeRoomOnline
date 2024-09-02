package TurnosOnline.ScapeRoomOnline.Controller;


import TurnosOnline.ScapeRoomOnline.Persistance.DTOs.TurnoForCreation;
import TurnosOnline.ScapeRoomOnline.Persistance.entities.Sala;
import TurnosOnline.ScapeRoomOnline.Persistance.entities.Turno;
import TurnosOnline.ScapeRoomOnline.Persistance.repository.SalaRepository;
import TurnosOnline.ScapeRoomOnline.Persistance.repository.TurnoRepository;
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

    @PostMapping("crear")
    public ResponseEntity<Turno> createTurno(@RequestBody TurnoForCreation turnoDTO) {
        Optional<Sala> sala = salaRepository.findById(turnoDTO.getSalaId());
        if (sala.isPresent()) {
            // Verificar si ya existe un turno en la misma sala a la misma hora
            Optional<Turno> turnoExistente = turnoRepository.findBySalaIdAndDiaYHora(turnoDTO.getSalaId(), turnoDTO.getDiaYHora());
            if (turnoExistente.isPresent()) {
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            }

            // Crear el nuevo turno si no existe uno en la misma sala a la misma hora
            Turno nuevoTurno = new Turno();
            nuevoTurno.setSala(sala.get());
            nuevoTurno.setDiaYHora(turnoDTO.getDiaYHora());
            nuevoTurno.setTelefono(turnoDTO.getTelefono());
            nuevoTurno.setNombre(turnoDTO.getNombre());

            // Guardar el turno
            Turno savedTurno = turnoRepository.saveAndFlush(nuevoTurno);

            // Devolver el turno guardado
            return new ResponseEntity<>(savedTurno, HttpStatus.CREATED);
        } else {
            return ResponseEntity.notFound().build();
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
