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
            Turno turno = new Turno();
            turno.setSala(sala.get());
            turno.setDiaYHora(turnoDTO.getDiaYHora());
            turno.setTelefono(turnoDTO.getTelefono());
            turno.setNombre(turnoDTO.getNombre());

            // Agregar turno a la lista de turnos de la sala
            Sala salaEntity = sala.get();
            salaEntity.getTurnos().add(turno);
            salaRepository.save(salaEntity);  // Guardar la sala para actualizar la lista de turnos

            Turno savedTurno = turnoRepository.save(turno);
            return new ResponseEntity<>(savedTurno, HttpStatus.CREATED);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public List<Turno> getAllTurnos() {
        return turnoRepository.findAll();
    }
}
