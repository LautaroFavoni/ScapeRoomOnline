package TurnosOnline.ScapeRoomOnline.Persistance.repository;

import TurnosOnline.ScapeRoomOnline.Persistance.entities.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TurnoRepository extends JpaRepository <Turno, Long> {
    List<Turno> findByDiaYHoraAfter(LocalDateTime dateTime);

    Optional<Turno> findBySalaIdAndDiaYHora(Long salaId, LocalDateTime diaYHora);


    Optional<Turno> findByPreferenceId(String preferenceId);

    @Query("SELECT t FROM Turno t WHERE t.sala.id = :salaId AND t.diaYHora = :diaYHora AND t.pago = 'true'")
    List<Turno> findPaidTurnosBySalaIdAndDiaYHora(@Param("salaId") Long salaId, @Param("diaYHora") LocalDateTime diaYHora);

}
