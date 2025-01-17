package TurnosOnline.ScapeRoomOnline.Persistance.DTOs;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TurnoForCreation {

    private Long salaId;
    private LocalDateTime diaYHora;
    private String telefono;
    private String nombre;
    private String apellido;

    private String mail;
    private Number jugadores;
    private Boolean cupon;
    private String pago;
    private BigDecimal importeTotal;

    private boolean asistio;

    public Boolean getAsistio() {
        return asistio;
    }

    public void setAsistio(Boolean asistio) {
        this.asistio = asistio;
    }

    // Constructores
    public TurnoForCreation() {
    }

    public TurnoForCreation(Long salaId, Boolean asistio,LocalDateTime diaYHora, String telefono, String nombre, String apellido,  String mail, Number jugadores, Boolean cupon, String pago, BigDecimal importeTotal) {
        this.salaId = salaId;
        this.asistio = asistio;
        this.diaYHora = diaYHora;
        this.telefono = telefono;
        this.nombre = nombre;
        this.apellido = apellido;
        this.mail = mail;
        this.jugadores = jugadores;
        this.cupon = cupon;
        this.pago = pago;
        this.importeTotal = importeTotal;
    }

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

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }


    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public Number getJugadores() {
        return jugadores;
    }

    public void setJugadores(Number jugadores) {
        this.jugadores = jugadores;
    }

    public Boolean getCupon() {
        return cupon;
    }

    public void setCupon(Boolean cupon) {
        this.cupon = cupon;
    }

    public String getPago() {
        return pago;
    }

    public void setPago(String pago) {
        this.pago = pago;
    }

    public BigDecimal getimporteTotal() {
        return (BigDecimal) importeTotal;
    }

    public void setimporteTotal(BigDecimal importeTotal) {
        this.importeTotal = importeTotal;
    }
}
