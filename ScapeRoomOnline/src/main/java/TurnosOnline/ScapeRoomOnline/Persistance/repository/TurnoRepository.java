package TurnosOnline.ScapeRoomOnline.Persistance.repository;

import TurnosOnline.ScapeRoomOnline.Persistance.entities.Turno;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TurnoRepository extends JpaRepository <Turno, Long> {
}
