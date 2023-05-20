package fr.roboteek.robot.systemenerveux.event;

public class CourantEvent extends RobotEvent {

    public static final String EVENT_TYPE = "courant";

    private String nomSource;

    private double courant;

    public CourantEvent() {
        super(EVENT_TYPE);
    }

    public String getNomSource() {
        return nomSource;
    }

    public void setNomSource(String nomSource) {
        this.nomSource = nomSource;
    }

    public double getCourant() {
        return courant;
    }

    public void setCourant(double courant) {
        this.courant = courant;
    }
}
