package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import net.engio.mbassy.bus.MBassador;

/**
 * Capteur lié à la reconnaissance vocale.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class CapteurVocal extends AbstractOrgane /*implements ResultListener*/ {

	public CapteurVocal(MBassador<RobotEvent> systemeNerveux) {
		super(systemeNerveux);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void initialiser() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void arreter() {
		// TODO Auto-generated method stub
		
	}

//    /** Moteur de reconnaissance vocale. */
//    private Recognizer recognizer;
//
//    /** Capteur micro. */
//    private Microphone microphone;
//    
//    /** Grammaire. */
//    private JSGFGrammar jsgfGrammar;
//    
//    private BaseRecognizer baseRecognizer;
//
//    /** Thread lancer pour permettre la reconnaissance vocale en parallèle des autres tâches. */
//    private Thread threadReconnaissance;
//
//    /** Flag indiquant de stopper le thread. */
//    private boolean stopperThread = false;
//
//    /** Flag indiquant que la reconnaissance est mise en pause. */
//    private boolean misEnPause = false;
//
//    /** Logger. */
//    private Logger logger = Logger.getLogger(CapteurVocal.class);
//
//    public CapteurVocal(MBassador<RobotEvent> systemeNerveux) {
//        super(systemeNerveux);
//
//        final ConfigurationManager configuration = new ConfigurationManager(System.getProperty("robot.dir") + File.separator + "reconnaissance-vocale" + File.separator + "configuration.fr.xml");
//
//        // Création du moteur de reconnaissance
//        recognizer = (Recognizer) configuration.lookup("recognizer");
//        recognizer.allocate();
//
//        // Flux d'entrée sur le micro
//        microphone = (Microphone) configuration.lookup("microphone");
//        
//        // Grammaire
//        jsgfGrammar = (JSGFGrammar) configuration.lookup("jsgfGrammar");
//        
//        // Moteur de reconnaissance de base pour la récupération des règles
//        baseRecognizer = new BaseRecognizer(jsgfGrammar.getGrammarManager());
//    }
//
//    @Override
//    public void initialiser() {
//        try {
//            baseRecognizer.allocate();
//        } catch (EngineException | EngineStateError e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        
//        if (!microphone.startRecording()) {
//            System.out.println("Cannot start microphone.");
//            recognizer.deallocate();
//        }
//
//
//        if(microphone.isRecording()) {
//            // Ajout d'un écouteur de résultats
//            recognizer.addResultListener(this);
//            // Lancement de la reconnaissance dans un thread
//            threadReconnaissance = new Thread() {
//                @Override
//                public void run() {
//                    while (!stopperThread) {
//                        recognizer.recognize();
//                    }
//                }
//            };
//        }
//        threadReconnaissance.start();
//
//    }
//
//    @Override
//    public void arreter() {
//        stopperThread = true;
//        // Arrêt du micro
//        microphone.stopRecording();
//        microphone.clear();
//        // Arrêt de la reconnaissance
//        if (recognizer.getState().equals(State.READY)) {
//            recognizer.deallocate();
//        }
//    }
//
//    /**
//     * Intercepte les évènements de contrôle de la reconnaissance vocale.
//     * @param reconnaissanceVocaleControleEvent évènement de contrôle de la reconnaissance vocale
//     */
//    @Handler
//    public void handleReconnaissanceVocaleControleEvent(ReconnaissanceVocaleControleEvent reconnaissanceVocaleControleEvent) {
//        if (reconnaissanceVocaleControleEvent.getControle() == CONTROLE.DEMARRER) {
//            logger.debug("Démarrage de la reconnaissance vocale");
//            microphone.clear();
//            microphone.startRecording();
//            misEnPause = false;
//            recognizer.addResultListener(this);
//        } else if (reconnaissanceVocaleControleEvent.getControle() == CONTROLE.METTRE_EN_PAUSE) {
//            logger.debug("Mise en pause de la reconnaissance vocale");
//            recognizer.removeResultListener(this);
//            misEnPause = true;
//            microphone.stopRecording();
//        }
//    }
//
//    @Override
//    public void newProperties(PropertySheet arg0) throws PropertyException {
//        // TODO Auto-generated method stub
//
//    }
//
//    @Override
//    public void newResult(Result result) {
//        if (!misEnPause && result != null) {
//            if (result != null) {
//                String resultText = result.getBestFinalResultNoFiller();
//
//                if (resultText != null && !resultText.trim().equals("")) {
//
//                    RuleGrammar ruleGrammar = new BaseRuleGrammar(baseRecognizer, jsgfGrammar.getRuleGrammar());
//                    RuleParse ruleParse = null;
//                    try {
//                        ruleParse = ruleGrammar.parse(resultText, null);
//                        if (ruleParse != null) {
//                            logger.debug("[REGLE] = " + ruleParse.getRuleName().getRuleName() + " - [TEXTE] = " + resultText);
//                        } else {
//                            logger.debug("[REGLE] = Aucune - [TEXTE] = " + resultText);
//                        }
//
//                    } catch (GrammarException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                        logger.error("Erreur", e);
//                    }
//                    //                    dire(resultText);
//
//                    // Envoi de l'évènement de reconnaissance
//                    final ReconnaissanceVocaleEvent event = new ReconnaissanceVocaleEvent();
//                    event.setTexteReconnu(resultText);
//                    if (ruleParse != null) {
//                        event.setNomRegle(ruleParse.getRuleName().getRuleName());
//                    }
//                    systemeNerveux.publish(event);
//                }
//            }
//        } else {
//            logger.debug("EN PAUSE");
//        }
//    }
//    
//    /**
//     * Envoie un évènement pour dire du texte.
//     * @param texte le texte à dire
//     */
//    private void dire(String texte) {
//        System.out.println("Dire = " + texte);
//        final ParoleEvent paroleEvent = new ParoleEvent();
//        paroleEvent.setTexte(texte);
//        systemeNerveux.publish(paroleEvent);
//    }
//    
//    public static void main(String[] args) {
//        System.setProperty("robot.dir", "/home/npeltier/Robot/Programme");
//        MBassador<RobotEvent> systeme = new MBassador<RobotEvent>(BusConfiguration.Default());
//        OrganeParole organeParole = new OrganeParole(systeme);
//        systeme.subscribe(organeParole);
//        final CapteurVocal capteurVocal = new CapteurVocal(systeme);
//        capteurVocal.initialiser();
//        systeme.subscribe(capteurVocal);
//        
//    }

}
