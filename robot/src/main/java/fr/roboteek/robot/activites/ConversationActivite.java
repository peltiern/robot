package fr.roboteek.robot.activites;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.openimaj.image.FImage;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.ml.annotation.ScoredAnnotation;
import org.openimaj.util.pair.IndependentPair;

import fr.roboteek.robot.decisionnel.Contexte;
import fr.roboteek.robot.memoire.ReconnaissanceFaciale;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import fr.roboteek.robot.systemenerveux.event.VisagesEvent;
import fr.roboteek.robot.util.callback.AsyncCallback;
import net.engio.mbassy.bus.MBassador;

/**
 * Activité "Conversation".
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class ConversationActivite extends AbstractActivite {

    private static long TEMPS_NOUVELLE_RECONNAISSANCE_FACIALE = 20000;

    /** Flag indiquant une attente de la reconnaissance faciale. */
    private boolean attenteReconnaissanceFaciale = false;

    /** Flag indiquant une attente du nom de la personne reconnue. */
    private boolean attenteNomPersonne = false;
    
    /** Flag indiquant une attente de visage à reconnaitre (mode qui suis-je). */
    private boolean attenteVisageQuiJeSuis = false;
    
    /** Flag indiquant une attente de visage à reconnaitre (mode qui est-ce). */
    private boolean attenteVisageQuiCest = false;

    private FImage visageEnAttenteDeNom;

    private List<FImage> listeVisagesAApprendre = new ArrayList<FImage>();

    private boolean apprentissageEnCours = false;
    
    /** Logger. */
    private Logger logger = Logger.getLogger(ConversationActivite.class);

    /**
     * Constructeur.
     * @param systemeNerveux système nerveux du robot
     */
    public ConversationActivite(MBassador<RobotEvent> systemeNerveux, Contexte contexte, ReconnaissanceFaciale reconnaissanceFaciale) {
        super(systemeNerveux, contexte, reconnaissanceFaciale);
    }

    @Override
    public void initialiser() {
        System.out.println("Init Activité conversation");
        dire("Début de la conversation");
    }

    @Override
    public void handleVisagesEvent(VisagesEvent visagesEvent) {

        // Reconnaissance du visage
        final DetectedFace visagePlusGrand = suivreVisage(visagesEvent);
//        final DetectedFace visagePlusGrand = trouverVisageLePlusGrand(visagesEvent);
        
        if (visagePlusGrand != null) {
////                        // Apprentissage du visage
////                        if (listeVisagesAApprendre.size() < 10) {
////                            // Ajout de l'image contenant le visage à apprendre
////                            listeVisagesAApprendre.add(visagesEvent.getImageOrigine());
////                        } else {
////                            if (!apprentissageEnCours) {
////                                apprentissageEnCours = true;
////                                reconnaissanceFaciale.apprendreVisagesPersonne(listeVisagesAApprendre, "nicolas", new AsyncCallback<Void>() {
////            
////                                    @Override
////                                    public void onSuccess(Void resultat) {
////                                        listeVisagesAApprendre.clear();
////                                        apprentissageEnCours = false;
////                                    }
////            
////                                    @Override
////                                    public void onError(Throwable throwable) {
////                                        // TODO Auto-generated method stub
////            
////                                    }
////                                });
////                            }
////                        }


            //  La reconnaissance doit se faire s'il n'y en a pas une en cours et si la date de dernière reconnaissance est dépassée
            if (!attenteReconnaissanceFaciale && !attenteNomPersonne) {
                long tempsDepuisDerniereReconnaissance = 0;//Long.MAX_VALUE;
                if (contexte.getDateDerniereReconnaissance() != null) {
                    tempsDepuisDerniereReconnaissance = 0;//Calendar.getInstance().getTimeInMillis() - contexte.getDateDerniereReconnaissance().getTimeInMillis();
                }
                if (attenteVisageQuiCest || attenteVisageQuiJeSuis || tempsDepuisDerniereReconnaissance > TEMPS_NOUVELLE_RECONNAISSANCE_FACIALE) {
                    logger.debug("Reconnaissance faciale en cours ...");
                    attenteReconnaissanceFaciale = true;
                    visageEnAttenteDeNom = visagesEvent.getImageOrigine();
                    // Lancement de la reconnaissance faciale
                    reconnaissanceFaciale.reconnaitre(visageEnAttenteDeNom, new AsyncCallback<IndependentPair<DetectedFace,ScoredAnnotation<String>>>() {

                        public void onSuccess(IndependentPair<DetectedFace, ScoredAnnotation<String>> resultat) {
                            // TODO Auto-generated method stub

                            // Personne reconnue : on dit son nom en fonction du contexte
                            if (resultat != null && resultat.getSecondObject() != null) {
                                visageEnAttenteDeNom = null;
                                if (attenteVisageQuiCest) {
                                    attenteVisageQuiCest = false;
                                    dire("Je reconnais ce visage. C'est " + resultat.getSecondObject().annotation);
                                } else if (attenteVisageQuiJeSuis) {
                                    attenteVisageQuiJeSuis = false;
                                    dire("Je te reconnais. Tu es " + resultat.getSecondObject().annotation);
                                } else {
                                    dire("Bonjour " + resultat.getSecondObject().annotation);
                                }
                            } else {
                                // Personne non reconnue : On demande son nom en fonction du contexte
                                if (attenteVisageQuiCest) {
                                    attenteVisageQuiCest = false;
                                    dire("Je ne connais pas ce visage. Comment t'appelles tu ?");
                                } else if (attenteVisageQuiJeSuis) {
                                    attenteVisageQuiJeSuis = false;
                                    dire("Je ne te connais pas. Comment t'appelles tu ?");
                                } else {
                                    dire("Je ne te connais pas. Comment t'appelles tu ?");
                                }
                                attenteNomPersonne = true;
                            }

                            // Fin de la reconnaissance faciale
                            System.out.println("Fin de la reconnaissance faciale");
                            contexte.setDateDerniereReconnaissance(Calendar.getInstance());
                            attenteReconnaissanceFaciale = false;


                        }

                        public void onError(Throwable throwable) {
                            // TODO Auto-generated method stub

                        }
                    });

                }
            }
        }
    }

    @Override
    public void handleReconnaissanceVocalEvent(ReconnaissanceVocaleEvent reconnaissanceVocaleEvent) {
        final String texteReconnu = reconnaissanceVocaleEvent.getTexteReconnu();
        if (texteReconnu != null && !texteReconnu.equals("")) {
            System.out.println("HUMAIN : " + texteReconnu);

            if (!attenteNomPersonne) {
                if (texteReconnu.trim().equalsIgnoreCase("Bonjour Sami")) {
                    dire("Bonjour Nicolas");
                } else if (texteReconnu.trim().equalsIgnoreCase("Tu es gentil")) {
                    dire("Merci, toi aussi.");
                }  else if (texteReconnu.trim().equalsIgnoreCase("Comment vas tu")) {
                    dire("Bien. Merci. Et toi?");
                } else if (texteReconnu.trim().equalsIgnoreCase("Je vais bien")) {
                    dire("Quelle belle journée.");
                } else if (texteReconnu.trim().equalsIgnoreCase("Quel age as tu")) {
                    dire("J'ai une journée. Et toi ?");
                } else if (texteReconnu.trim().equalsIgnoreCase("Comment tu t appelle") || texteReconnu.trim().equalsIgnoreCase("Comment t appelle tu") || texteReconnu.trim().equalsIgnoreCase("Quel est ton nom")) {
                    dire("Je m'appelle Sami. Et toi ?");
                } else if (texteReconnu.trim().equalsIgnoreCase("Quelle heure il est") || texteReconnu.trim().equalsIgnoreCase("il est quelle heure")) {
                    final SimpleDateFormat sdfHeure = new SimpleDateFormat("HH 'heure' mm");
                    dire("Il est " + sdfHeure.format(Calendar.getInstance().getTime()));
                } else if (texteReconnu.trim().equalsIgnoreCase("Quel jour on est")) {
                    final SimpleDateFormat sdfJour = new SimpleDateFormat("dd MMMMMM yyyy");
                    dire("On est le " + sdfJour.format(Calendar.getInstance().getTime()));
                } else if (texteReconnu.trim().equalsIgnoreCase("Ou tu habite") || texteReconnu.trim().equalsIgnoreCase("Ou habite tu")) {
                    dire("J'habites dans un ordinateur. Et toi?");
                } else if (texteReconnu.trim().equalsIgnoreCase("Carquefou") || texteReconnu.trim().equalsIgnoreCase("Paris") || texteReconnu.trim().equalsIgnoreCase("Angoulême")) {
                    dire("Tu habites " + texteReconnu + ".");
                } else if (texteReconnu.trim().equalsIgnoreCase("Au revoir Sami")) {
                    dire("Au revoir Nicolas. A bientôt.");
                } else if (reconnaissanceVocaleEvent.getNomRegle() != null && reconnaissanceVocaleEvent.getNomRegle().equals("qui_suis_je")) {
                    if (!attenteVisageQuiJeSuis) {
                        dire("Laisse moi regarder.");
                        attenteVisageQuiJeSuis = true;
                    }
                } else if (reconnaissanceVocaleEvent.getNomRegle() != null && reconnaissanceVocaleEvent.getNomRegle().equals("qui_c_est")) {
                    if (!attenteVisageQuiCest) {
                        dire("Laisse moi regarder.");
                        attenteVisageQuiCest = true;
                    }
                } else {
                    System.out.println("Tu dis : " + texteReconnu + ".");
                    //                    dire(texteReconnu);
                }
            } else {
                if (reconnaissanceVocaleEvent.getNomRegle() != null && reconnaissanceVocaleEvent.getNomRegle().equals("noms")) {
                    attenteNomPersonne = false;
                    if (!texteReconnu.trim().equalsIgnoreCase("Personne")) {
                        // Réception du nom de la personne attendue
                        dire("Enchanté " + texteReconnu + ". Ravi de te connaître.");
                        // Apprentissage de la personne
                        attenteReconnaissanceFaciale = true;
                        final List<FImage> listeVisages = new ArrayList<FImage>();
                        listeVisages.add(visageEnAttenteDeNom);
                        reconnaissanceFaciale.apprendreVisagesPersonne(listeVisages, texteReconnu, new AsyncCallback<Void>() {

                            public void onSuccess(Void resultat) {
                                // TODO Auto-generated method stub
                                attenteReconnaissanceFaciale = false;
                            }

                            public void onError(Throwable throwable) {
                                logger.error("Erreur lors de la reconnaissance faciale", throwable);
                            }
                        });
                    } else {
                        dire("Hockey. Je laisse tomber.");
                    }
                }
            }
        }
    }

    @Override
    public void arreter() {
        dire("Fin de la conversation. Au revoir.");
    }

}
