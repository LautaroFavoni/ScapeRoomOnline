package TurnosOnline.ScapeRoomOnline.Persistance.DTOs;

import TurnosOnline.ScapeRoomOnline.Persistance.entities.Turno;

public class TurnoConLinkDePago {
    private Turno turno;
    private String paymentLink;

    // Constructor
    public TurnoConLinkDePago(Turno turno, String paymentLink) {
        this.turno = turno;
        this.paymentLink = paymentLink;
    }

    // Getters y setters
    public Turno getTurno() {
        return turno;
    }

    public void setTurno(Turno turno) {
        this.turno = turno;
    }

    public String getPaymentLink() {
        return paymentLink;
    }

    public void setPaymentLink(String paymentLink) {
        this.paymentLink = paymentLink;
    }
}
