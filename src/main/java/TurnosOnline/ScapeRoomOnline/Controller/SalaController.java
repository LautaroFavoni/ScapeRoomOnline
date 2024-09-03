package TurnosOnline.ScapeRoomOnline.Controller;


import TurnosOnline.ScapeRoomOnline.Persistance.DTOs.SalaForCreation;
import TurnosOnline.ScapeRoomOnline.Persistance.entities.Sala;
import TurnosOnline.ScapeRoomOnline.Persistance.repository.SalaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salas")

public class SalaController {


    @Autowired
    private SalaRepository salaRepository;

    @PostMapping
    public ResponseEntity<Sala> createSala(@RequestBody SalaForCreation salaDTO) {
        Sala sala = new Sala();
        sala.setNombre(salaDTO.getNombre());
        Sala savedSala = salaRepository.save(sala);
        return new ResponseEntity<>(savedSala, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Sala>> getAllSalas() {
        List<Sala> salas = salaRepository.findAll();
        return new ResponseEntity<>(salas, HttpStatus.OK);
    }


}
