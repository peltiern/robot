package fr.roboteek.robot.systemenerveux.event;

public class TemperatureEvent extends RobotEvent {

    public static final String EVENT_TYPE = "temperature";

    private String nomSource;

    private double temperature;

    public TemperatureEvent() {
        super(EVENT_TYPE);
    }

    public String getNomSource() {
        return nomSource;
    }

    public void setNomSource(String nomSource) {
        this.nomSource = nomSource;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
}
