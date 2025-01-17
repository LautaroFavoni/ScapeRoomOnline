package TurnosOnline.ScapeRoomOnline.Persistance.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    private LocalDateTime diaYHora;
    private String telefono;
    private String nombre;

    private String apellido;

    public Boolean getAsistio() {
        return asistio;
    }

    public void setAsistio(Boolean asistio) {
        this.asistio = asistio;
    }

    private Boolean asistio;



    private String mail;

    private Number jugadores;

    private Boolean cupon;

    private String preferenceId;

    private String pago;



    private String dataId;

    private BigDecimal importeTotal;

    public Turno(Sala sala, Boolean asistio ,String preferenceId, LocalDateTime diaYHora, String telefono, String nombre, String apellido, String mail, Number jugadores, Boolean cupon, String pago, BigDecimal importeTotal) {
        this.sala = sala;
        this.preferenceId = preferenceId;
        this.diaYHora = diaYHora;
        this.telefono = telefono;
        this.nombre = nombre;
        this.apellido = apellido;
        this.asistio = asistio;
        this.mail = mail;
        this.jugadores = jugadores;
        this.cupon = cupon;
        this.pago = pago;
        this.importeTotal = importeTotal;
    }

    public Turno() {
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public String getPreferenceId() {
        return preferenceId;
    }

    public void setPreferenceId(String preferenceId) {
        this.preferenceId = preferenceId;
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
        return importeTotal;
    }

    public void setimporteTotal(BigDecimal importeTotal) {
        this.importeTotal = importeTotal;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

}