package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;

/**
 * Animations prédéfinies.
 */
public enum Animation {

    NON {
        @Override
        public void jouer() {
            ParoleEvent paroleEvent = new ParoleEvent();
            paroleEvent.setTexte("Non non !");
            RobotEventBus.getInstance().publishAsync(paroleEvent);

            MouvementCouEvent mouvementCouEvent;
            for (int i = 0; i < 2; i++) {
                mouvementCouEvent = new MouvementCouEvent();
                mouvementCouEvent.setPositionGaucheDroite(20);
                mouvementCouEvent.setPositionHautBas(0);
                mouvementCouEvent.setAccelerationGaucheDroite(2000D);
                mouvementCouEvent.setAccelerationHautBas(2000D);
                mouvementCouEvent.setSynchrone(true);
                RobotEventBus.getInstance().publish(mouvementCouEvent);
                mouvementCouEvent = new MouvementCouEvent();
                mouvementCouEvent.setPositionGaucheDroite(-20);
                mouvementCouEvent.setPositionHautBas(0);
                mouvementCouEvent.setAccelerationGaucheDroite(2000D);
                mouvementCouEvent.setAccelerationHautBas(2000D);
                mouvementCouEvent.setSynchrone(true);
                RobotEventBus.getInstance().publish(mouvementCouEvent);
            }
            mouvementCouEvent = new MouvementCouEvent();
            mouvementCouEvent.setPositionGaucheDroite(0);
            mouvementCouEvent.setPositionHautBas(0);
            mouvementCouEvent.setAccelerationGaucheDroite(2000D);
            mouvementCouEvent.setAccelerationHautBas(2000D);
            mouvementCouEvent.setSynchrone(true);
            RobotEventBus.getInstance().publish(mouvementCouEvent);
        }
    },

    OUI {
        @Override
        public void jouer() {
            ParoleEvent paroleEvent = new ParoleEvent();
            paroleEvent.setTexte("Oui oui");
            RobotEventBus.getInstance().publishAsync(paroleEvent);

            MouvementCouEvent mouvementCouEvent;
            for (int i = 0; i < 2; i++) {
                mouvementCouEvent = new MouvementCouEvent();
                mouvementCouEvent.setPositionGaucheDroite(0);
                mouvementCouEvent.setPositionHautBas(10);
                mouvementCouEvent.setAccelerationGaucheDroite(2000D);
                mouvementCouEvent.setAccelerationHautBas(2000D);
                mouvementCouEvent.setSynchrone(true);
                RobotEventBus.getInstance().publish(mouvementCouEvent);
                mouvementCouEvent = new MouvementCouEvent();
                mouvementCouEvent.setPositionGaucheDroite(0);
                mouvementCouEvent.setPositionHautBas(-10);
                mouvementCouEvent.setAccelerationGaucheDroite(2000D);
                mouvementCouEvent.setAccelerationHautBas(2000D);
                mouvementCouEvent.setSynchrone(true);
                RobotEventBus.getInstance().publish(mouvementCouEvent);
            }
            mouvementCouEvent = new MouvementCouEvent();
            mouvementCouEvent.setPositionGaucheDroite(0);
            mouvementCouEvent.setPositionHautBas(0);
            mouvementCouEvent.setAccelerationGaucheDroite(2000D);
            mouvementCouEvent.setAccelerationHautBas(2000D);
            mouvementCouEvent.setSynchrone(true);
            RobotEventBus.getInstance().publish(mouvementCouEvent);
        }

    },

    TRISTE {
        @Override
        public void jouer() {

            MouvementCouEvent mouvementCouEvent;
            mouvementCouEvent = new MouvementCouEvent();
            mouvementCouEvent.setPositionGaucheDroite(0);
            mouvementCouEvent.setPositionHautBas(-20);
            mouvementCouEvent.setAccelerationGaucheDroite(2000D);
            mouvementCouEvent.setAccelerationHautBas(2000D);
            mouvementCouEvent.setSynchrone(false);
            RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
            MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
            mouvementYeuxEvent.setPositionOeilDroit(-24);
            mouvementYeuxEvent.setPositionOeilGauche(-24);
            mouvementYeuxEvent.setSynchrone(true);
            RobotEventBus.getInstance().publish(mouvementYeuxEvent);
        }
    };

    public abstract void jouer();

    public static void main(String[] args) {
        try {
            Cou cou = new Cou();
            cou.initialiser();
            RobotEventBus.getInstance().subscribe(cou);

            Yeux yeux = new Yeux();
            yeux.initialiser();
            RobotEventBus.getInstance().subscribe(yeux);

            OrganeParoleEspeak organeParole = new OrganeParoleEspeak();
            organeParole.initialiser();
            RobotEventBus.getInstance().subscribe(organeParole);

            Animation.NON.jouer();
            Thread.sleep(2000);
            Animation.OUI.jouer();
            Thread.sleep(2000);
            Animation.TRISTE.jouer();
            Thread.sleep(3000);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }
}