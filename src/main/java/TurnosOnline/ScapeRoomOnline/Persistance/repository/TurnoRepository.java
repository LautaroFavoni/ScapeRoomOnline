package TurnosOnline.ScapeRoomOnline.Persistance.repository;

import TurnosOnline.ScapeRoomOnline.Persistance.entities.Turno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TurnoRepository extends JpaRepository <Turno, Long> {
    List<Turno> findByDiaYHoraAfter(LocalDateTime dateTime);

    Optional<Turno> findBySalaIdAndDiaYHora(Long salaId, LocalDateTime diaYHora);
}
