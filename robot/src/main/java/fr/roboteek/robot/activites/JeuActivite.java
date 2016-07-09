package fr.roboteek.robot.activites;

import fr.roboteek.robot.activites.MoteurJeuPlusMoins.MODE;
import fr.roboteek.robot.activites.MoteurJeuPlusMoins.RESULTAT;
import fr.roboteek.robot.decisionnel.Contexte;
import fr.roboteek.robot.memoire.ReconnaissanceFaciale;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import fr.roboteek.robot.systemenerveux.event.VisagesEvent;
import fr.roboteek.robot.util.callback.convertisseur.ConvertisseurNombres;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;

/**
 * Activité "Jeu Plus - Moins".
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class JeuActivite extends AbstractActivite {

    /** Moteur de jeu. */
    private MoteurJeuPlusMoins moteurJeu;

    /** Mode de jeu. */
    private MODE modeJeu;

    /** Nombre minimum. */
    private int nombreMinimum;

    /** Nombre maximum. */
    private int nombreMaximum;

    /** Flag "Attente mode jeu". */
    private boolean attenteModeJeu = false;

    /** Flag "Attente intervalle". */
    private boolean attenteIntervalle = false;

    /** Flag "Attente nombre". */
    private boolean attenteNombre = false;

    /** Flag "Attente résultat". */
    private boolean attenteResultat = false;

    /** Flag "Attente recommencer". */
    private boolean attenteRecommencer = false;

    /** Flag "Attente mêmes règles". */
    private boolean attenteMemesRegles = false;

    /**
     * Constructeur
     * @param systemeNerveux système nerveux du robot
     */
    public JeuActivite(MBassador<RobotEvent> systemeNerveux, Contexte contexte, ReconnaissanceFaciale reconnaissanceFaciale) {
        super(systemeNerveux, contexte, reconnaissanceFaciale);
    }

    @Override
    public void initialiser() {
        // Initialisation du moteur de jeu
        moteurJeu = new MoteurJeuPlusMoins();
        dire("Jouons à deviner des nombres.");
        dire("Tu veux chercher le nombre ?");
        attenteModeJeu = true;
    }

    @Override
    @Handler
    public void handleVisagesEvent(VisagesEvent visagesEvent) {
//        suivreVisage(visagesEvent);

    }

    @Override
    @Handler
    public void handleReconnaissanceVocalEvent(ReconnaissanceVocaleEvent reconnaissanceVocaleEvent) {
        final String texteReconnu = reconnaissanceVocaleEvent.getTexteReconnu();
        if (texteReconnu != null && !texteReconnu.equals("")) {

            // Mode de jeu
            if (attenteModeJeu) {
                attenteModeJeu = false;
                modeJeu = null;
                if (reconnaissanceVocaleEvent.getNomRegle() != null && reconnaissanceVocaleEvent.getNomRegle().equals("non")) {
                    // Mode Deviner
                    modeJeu = MODE.DEVINER;
                } else if (reconnaissanceVocaleEvent.getNomRegle() != null && reconnaissanceVocaleEvent.getNomRegle().equals("oui")) {
                    modeJeu = MODE.FAIRE_DEVINER;
                } else {
                    dire("Désolé. Je n'ai pas compris. Tu veux chercher le nombre ?");
                    attenteModeJeu = true;
                }
                if (modeJeu != null) {
                    // Demande de l'intervalle
                    if (modeJeu == MODE.DEVINER) {
                        dire("Je dois chercher un nombre jusqu'à combien ?");
                    } else {
                        dire("Tu veux chercher un nombre jusqu'à combien ?");
                    }
                    attenteIntervalle = true;
                }
            }

            // Intervalle
            else if (attenteIntervalle) {
                attenteIntervalle = false;
                nombreMinimum = 1;
                nombreMaximum = 100;
                if (reconnaissanceVocaleEvent.getNomRegle() != null && (reconnaissanceVocaleEvent.getNomRegle().equals("intervalle_nombre") || reconnaissanceVocaleEvent.getNomRegle().equals("nombres"))) {
//                    if (texteReconnu.contains("entre")) {
//                        // Intervalle
//                        // Récupération du nombre minimum et maximum
//                        final String[] nombresReconnus = texteReconnu.replaceAll("entre", "").trim().split("et");
//                        final Integer nombreMinimumReconnu = ConvertisseurNombres.convertirNombre(nombresReconnus[0]);
//                        final Integer nombreMaximumReconnu = ConvertisseurNombres.convertirNombre(nombresReconnus[1]);
//                        if (nombreMinimumReconnu != null && nombreMaximumReconnu != null) {
//                            nombreMinimum = nombreMinimumReconnu.intValue();
//                            nombreMaximum = nombreMaximumReconnu.intValue();
//
//                            if (nombreMinimum > nombreMaximum) {
//                                dire(String.valueOf(nombreMinimum) + " est supérieur à " + String.valueOf(nombreMaximum)); 
//                                if (modeJeu == MODE.DEVINER) {
//                                    dire("Je dois chercher un nombre entre combien et combien ?");
//                                } else {
//                                    dire("Tu veux chercher un nombre entre combien et combien ?");
//                                }
//                                attenteIntervalle = true;
//                            } else {
//
//                                // Début du jeu
//                                dire("C'est parti !");
//                                moteurJeu.initialiser(modeJeu, nombreMinimum, nombreMaximum);
//                                if (modeJeu == MODE.DEVINER) {
//                                    int nombrePropose = moteurJeu.devinerNombre(RESULTAT.PLUS);
//                                    dire(String.valueOf(nombrePropose));
//                                    attenteResultat = true;
//                                } else {
//                                    dire("Dis un nombre entre " + nombreMinimum + " et " + nombreMaximum);
//                                    attenteNombre = true;
//                                }
//                            }
//
//                        } else {
//                            if (modeJeu == MODE.DEVINER) {
//                                dire("Désolé. Je n'ai pas compris. Je dois chercher un nombre entre combien et combien ?");
//                            } else {
//                                dire("Désolé. Je n'ai pas compris. Tu veux chercher un nombre entre combien et combien ?");
//                            }
//                            attenteIntervalle = true;
//                        }
//
//                    } else {
                        // Jusqu'à
                        final Integer nombreMaximumReconnu = ConvertisseurNombres.convertirNombre(texteReconnu.replaceAll("jusque a", "").trim());
                        if (nombreMaximumReconnu != null) {
                            nombreMinimum = 1;
                            nombreMaximum = nombreMaximumReconnu.intValue();
                            // Début du jeu
                            dire("C'est parti !");
                            moteurJeu.initialiser(modeJeu, nombreMinimum, nombreMaximum);
                            if (modeJeu == MODE.DEVINER) {
                                int nombrePropose = moteurJeu.devinerNombre(RESULTAT.PLUS);
                                dire(String.valueOf(nombrePropose));
                                attenteResultat = true;
                            } else {
                                dire("Dis un nombre entre " + nombreMinimum + " et " + nombreMaximum);
                                attenteNombre = true;
                            }
                        } else {
                            if (modeJeu == MODE.DEVINER) {
                                dire("Désolé. Je n'ai pas compris. Je dois chercher un nombre jusqu'à combien ?");
                            } else {
                                dire("Désolé. Je n'ai pas compris. Tu veux chercher un nombre jusqu'à combien ?");
                            }
                            attenteIntervalle = true;
                        }
//                    }
                } else {
                    if (modeJeu == MODE.DEVINER) {
                        dire("Désolé. Je n'ai pas compris. Je dois chercher un nombre entre jusqu'à combien ?");
                    } else {
                        dire("Désolé. Je n'ai pas compris. Tu veux chercher un nombre entre jusqu'à combien ?");
                    }
                    attenteIntervalle = true;
                }
            }

            // Nombre proposé par Humain
            else if (attenteNombre) {
                attenteNombre = false;
                if (reconnaissanceVocaleEvent.getNomRegle() != null && reconnaissanceVocaleEvent.getNomRegle().equals("nombres")) {
                    final Integer nombrePropose = ConvertisseurNombres.convertirNombre(texteReconnu);
                    if (nombrePropose != null) {
                        final RESULTAT resultat = moteurJeu.proposerNombre(nombrePropose);
                        if (resultat == RESULTAT.PLUS) {
                            dire(moteurJeu.getDernierNombrePropose() + " ?" + "C'est plusse.");
                            attenteNombre = true;
                        } else if (resultat == RESULTAT.PLUS_DEJA_PROPOSE) {
                            dire(moteurJeu.getDernierNombrePropose() + " ?" + " Tu l'as déjà dit. C'est plusse.");
                            attenteNombre = true;
                        } else if (resultat == RESULTAT.MOINS) {
                            dire(moteurJeu.getDernierNombrePropose() + " ?" + "C'est moins.");
                            attenteNombre = true;
                        } else if (resultat == RESULTAT.MOINS_DEJA_PROPOSE) {
                            dire(moteurJeu.getDernierNombrePropose() + " ?" + " Tu l'as déjà dit. C'est moins.");
                            attenteNombre = true;
                        } else if (resultat == RESULTAT.PAS_DANS_INTERVALLE) {
                            dire(moteurJeu.getDernierNombrePropose() + " ?" + " Ce n'est pas dans l'intervalle.");
                            attenteNombre = true;
                        } else if (resultat == RESULTAT.GAGNE) {
                            dire("Bravo ! C'est gagné !");
                            dire("Veux-tu rejouer ?");
                            attenteRecommencer = true;
                        }
                    } else {
                        dire("Désolé. Je n'ai pas compris. Peux-tu répéter ?");
                        attenteNombre = true;
                    }


                } else {
                    dire("Désolé. Je n'ai pas compris. Peux-tu répéter ?");
                    attenteNombre = true;
                }
            }

            // Nombre proposé par robot : résultat
            else if (attenteResultat) {
                attenteResultat = false;
                if (reconnaissanceVocaleEvent.getNomRegle() != null && reconnaissanceVocaleEvent.getNomRegle().equals("resultat_jeu")) {
                    if (texteReconnu.trim().equals("plus")) {
                        int nombrePropose = moteurJeu.devinerNombre(RESULTAT.PLUS);
                        dire(String.valueOf(nombrePropose));
                        attenteResultat = true;
                    } else if (texteReconnu.trim().equals("moins")) {
                        int nombrePropose = moteurJeu.devinerNombre(RESULTAT.MOINS);
                        dire(String.valueOf(nombrePropose));
                        attenteResultat = true;
                    } else if (texteReconnu.trim().contains("gagné")) {
                        dire("Supère! J'ai gagné!");
                        dire("Veux-tu rejouer ?");
                        attenteRecommencer = true;
                    }
                } else {
                    dire("Désolé. Je n'ai pas compris. Peux-tu répéter ?");
                    attenteResultat = true;
                }
            }

            // Recommencer le jeu ?
            else if (attenteRecommencer) {
                attenteRecommencer = false;
                if (reconnaissanceVocaleEvent.getNomRegle() != null && (reconnaissanceVocaleEvent.getNomRegle().equals("oui") || reconnaissanceVocaleEvent.getNomRegle().equals("non"))) {
                    if (reconnaissanceVocaleEvent.getNomRegle().equals("oui")) {
                        dire("Avec les mêmes règles ?");
                        attenteMemesRegles = true;
                    } else if (reconnaissanceVocaleEvent.getNomRegle().equals("non")) {
                        dire("Hockey, très bien. On s'est quand même bien amusé !");
                        // TODO Voir pour sortir de l'activité
                    }
                } else {
                    dire("Désolé. Je n'ai pas compris. Veux-tu rejouer ?");
                    attenteRecommencer = true;
                }
            }

            // Réponse mêmes règles ?
            else if (attenteMemesRegles) {
                attenteMemesRegles = false;
                if (reconnaissanceVocaleEvent.getNomRegle() != null && (reconnaissanceVocaleEvent.getNomRegle().equals("oui") || reconnaissanceVocaleEvent.getNomRegle().equals("non"))) {
                    if (reconnaissanceVocaleEvent.getNomRegle().equals("oui")) {
                        // Début du jeu
                        dire("C'est parti !");
                        moteurJeu.initialiser(modeJeu, nombreMinimum, nombreMaximum);
                        if (modeJeu == MODE.DEVINER) {
                            int nombrePropose = moteurJeu.devinerNombre(RESULTAT.PLUS);
                            dire(String.valueOf(nombrePropose));
                            attenteResultat = true;
                        } else {
                            dire("Dis un nombre entre " + nombreMinimum + " et " + nombreMaximum);
                            attenteNombre = true;
                        }
                    } else if (reconnaissanceVocaleEvent.getNomRegle().equals("non")) {
                        dire("Hockey, très bien.");
                        dire("Tu veux chercher le nombre ?");
                        attenteModeJeu = true;
                    }
                } else {
                    dire("Désolé. Je n'ai pas compris. Tu veux rejouer avec les mêmes règles ?");
                    attenteMemesRegles = true;
                }
            }
        }
    }

    @Override
    public void arreter() {
        // TODO Auto-generated method stub

    }


}
