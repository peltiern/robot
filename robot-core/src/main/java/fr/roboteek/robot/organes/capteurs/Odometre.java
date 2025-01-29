package fr.roboteek.robot.organes.capteurs;

import com.google.common.eventbus.Subscribe;
import fr.roboteek.robot.organes.actionneurs.roues.Chassis;
import fr.roboteek.robot.systemenerveux.event.EncodeurRoueEvent;
import lombok.Getter;

public class Odometre {

    private final Chassis chassis;
    @Getter
    private long x;
    @Getter
    private long y;
    @Getter
    private double theta;

    private long ticksGauchePrecedent;
    private long ticksDroitPrecedent;

    public Odometre(Chassis chassis) {
        this.chassis = chassis;
        this.x = 0;
        this.y = 0;
        this.theta = 0;
        this.ticksGauchePrecedent = 0;
        this.ticksDroitPrecedent = 0;
    }

    @Subscribe
    public void mettreAJourPosition(EncodeurRoueEvent event) {
        long deltaTicksGauche = event.getPositionEncodeurGauche() - ticksGauchePrecedent;
        long deltaTicksDroit = event.getPositionEncodeurDroit() - ticksDroitPrecedent;

        ticksGauchePrecedent = event.getPositionEncodeurGauche();
        ticksDroitPrecedent = event.getPositionEncodeurDroit();

        double distanceGauche = chassis.ticksEnDistance(deltaTicksGauche);
        double distanceDroit = chassis.ticksEnDistance(deltaTicksDroit);
        double distanceParcourue = (distanceGauche + distanceDroit) / 2.0;
        double deltaTheta = chassis.calculerAngle(deltaTicksGauche, deltaTicksDroit);
        theta += deltaTheta;

        theta = Math.atan2(Math.sin(theta), Math.cos(theta));

        x += (long) (distanceParcourue * Math.cos(theta));
        y += (long) (distanceParcourue * Math.sin(theta));

        System.out.println("x: " + x + "\ty: " + y + "\ttheta: " + Math.toDegrees(theta));
    }
}

