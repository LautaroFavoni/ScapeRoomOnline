package TurnosOnline.ScapeRoomOnline.Persistance.DTOs;



import java.time.LocalDateTime;

public class TurnoForCreation {

    private Long salaId;
    private LocalDateTime diaYHora;
    private String telefono;
    private String nombre;

    // Getters y Setters
    public Long getSalaId() {
        return salaId;
    }

    public void setSalaId(Long salaId) {
        this.salaId = salaId;
    }

    public LocalDateTime getDiaYHora() {
        return diaYHora;
    }

    public void setDiaYHora(LocalDateTime diaYHora) {
        this.diaYHora = diaYHora;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
