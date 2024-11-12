package TurnosOnline.ScapeRoomOnline.Persistance.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

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

    private Number DNI;

    private String mail;

    private Number jugadores;

    private Boolean cupon;

    private String pago;

    private Number importePagado;

    public Turno(Sala sala, LocalDateTime diaYHora, String telefono, String nombre, String apellido, Number DNI, String mail, Number jugadores, Boolean cupon, String pago, Number importePagado) {
        this.sala = sala;
        this.diaYHora = diaYHora;
        this.telefono = telefono;
        this.nombre = nombre;
        this.apellido = apellido;
        this.DNI = DNI;
        this.mail = mail;
        this.jugadores = jugadores;
        this.cupon = cupon;
        this.pago = pago;
        this.importePagado = importePagado;
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

    public Number getDNI() {
        return DNI;
    }

    public void setDNI(Number DNI) {
        this.DNI = DNI;
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

    public Number getImportePagado() {
        return importePagado;
    }

    public void setImportePagado(Number importePagado) {
        this.importePagado = importePagado;
    }
}