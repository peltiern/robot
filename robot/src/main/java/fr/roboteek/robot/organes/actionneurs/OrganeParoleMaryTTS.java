package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import net.engio.mbassy.bus.MBassador;

public class OrganeParoleMaryTTS extends AbstractOrgane {

	public OrganeParoleMaryTTS(MBassador<RobotEvent> systemeNerveux) {
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

//    private MaryInterface marytts;
//    
//    private AudioPlayer ap;
//    
//    /** Logger. */
//    private Logger logger = Logger.getLogger(OrganeParole.class);
//
//    /** Constructeur. */
//    public OrganeParoleMaryTTS(MBassador<RobotEvent> systemeNerveux) {
//        super(systemeNerveux);
//        // Initialisation
//        try {
//            marytts = new RemoteMaryInterface("localhost", 59125);
////            marytts = new LocalMaryInterface();
//            marytts.setLocale(Locale.FRENCH);
////            marytts.setVoice("enst-camille");
//            marytts.setVoice("upmc-pierre");
//            marytts.setAudioEffects("Robot(amount=100)");
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Lit un texte.
//     * @param texte le texte à dire
//     */
//    public void lire(String texte) {
//        if(texte != null && !texte.isEmpty()) {
//            // Envoi d'un évènement pour mettre en pause la reconnaissance vocale
//            final ReconnaissanceVocaleControleEvent eventPause = new ReconnaissanceVocaleControleEvent();
//            eventPause.setControle(CONTROLE.METTRE_EN_PAUSE);
//            systemeNerveux.publish(eventPause);
//
//            logger.debug("Lecture :\t" + texte);
//            try {
//                AudioInputStream audio = marytts.generateAudio(texte);
//                ap = new AudioPlayer();
//                ap.setAudio(audio);
//                ap.start();
//            } catch (SynthesisException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//
//            logger.debug("Fin lecture :\t" + texte);
//
//            // Envoi d'un évènement pour redémarrer la reconnaissance vocale
//            final ReconnaissanceVocaleControleEvent eventRedemarrage = new ReconnaissanceVocaleControleEvent();
//            eventRedemarrage.setControle(CONTROLE.DEMARRER);
//            systemeNerveux.publish(eventRedemarrage);
//        }
//    }
//    
//    /**
//     * Intercepte les évènements pour lire du texte.
//     * @param paroleEvent évènement pour lire du texte
//     */
//    @Handler
//    public void handleParoleEvent(ParoleEvent paroleEvent) {
//        if (paroleEvent.getTexte() != null && !paroleEvent.getTexte().trim().equals("")) {
//            lire(paroleEvent.getTexte().trim());
//        }
//    }
//
//    @Override
//    public void initialiser() {
//        
//    }
//
//    @Override
//    public void arreter() {
//        
//    }
//    
//    public static void main(String[] args) {
//        
//        final OrganeParoleMaryTTS organeParole = new OrganeParoleMaryTTS(new MBassador<RobotEvent>(BusConfiguration.Default()));
//        
//        System.setProperty("robot.dir", "/home/npeltier/Robot/Programme");
//        final WebSpeechServer webSpeechServer = WebSpeechServer.getInstance();
//        webSpeechServer.initialiser();
//        
//        organeParole.lire("Fin de l'initialisation");
//        
//     // Création du chat
////        String botname="amy";
////        String path=System.getProperty("robot.dir");
////        Bot bot = new Bot(botname, path);
////        final Chat chat = new Chat(bot);
////        
//        WebSpeechServer.getInstance().addListener(new WebSpeechServerListener() {
//            
//            @Override
//            public void onSpeechResult(String speechResult) {
////                WebSpeechServer.getInstance().lire("Bonjour tout le monde ! Comment ça va ?");
////                final String reponse = chat.multisentenceRespond(speechResult);
//                organeParole.lire(speechResult);
////                WebSpeechServer.getInstance().lire(speechResult);
//            }
//        });
//    }

}
