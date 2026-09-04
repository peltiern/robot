package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.organes.AbstractOrganeWithThread;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Axe;
import fr.roboteek.robot.organes.actionneurs.animation.modele.ImageCle;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Piste;
import fr.roboteek.robot.securite.NatureOrgane;
import fr.roboteek.robot.securite.OrganeSurveille;
import fr.roboteek.robot.systemenerveux.event.ArretUrgenceEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import fr.roboteek.robot.systemenerveux.event.OrigineMouvement;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Déroule une animation en <b>régime trajectoire</b> : la courbe est calculée en Java et envoyée
 * au cou et aux yeux par échantillons réguliers.
 * <p>
 * L'autre régime possible était le régime <i>pose</i> — une consigne par image-clé, la douceur
 * venant de la rampe du contrôleur. Il a été écarté parce qu'il ne sait pas rendre une courbe :
 * entre deux images-clés le servo suit son propre profil trapézoïdal, pas celui que l'éditeur
 * dessine. Le régime trajectoire fait exécuter au robot la courbe affichée à l'écran.
 * <p>
 * <b>Toute la conception tient à une mesure</b> : le contrôleur coûte environ 5 ms par canal écrit
 * plus 12 ms par écriture, soit ~55 consignes de position par seconde pour cinq axes, budget que
 * l'animation partage avec le regard, la manette et les curseurs du HUD. D'où trois choix :
 * <ul>
 *   <li><b>10 Hz</b>, et non 30 : cinq axes en mouvement coûtent déjà 85 ms par tour ;</li>
 *   <li><b>un axe qui n'a pas bougé n'est pas réécrit</b> — un canal engagé mais non écrit ne coûte
 *       rien, mesuré, et c'est ce qui rend l'échelle tenable : une animation bouge rarement plus de
 *       deux ou trois axes à la fois ;</li>
 *   <li>vitesse et accélération sont renvoyées à chaque tour mais {@code PhidgetsServoMotor} les
 *       dédoublonne, donc une seule écriture au premier échantillon — et le jour où une image-clé
 *       en change, la nouvelle valeur part sans qu'on ait à s'en occuper.</li>
 * </ul>
 * <p>
 * Le mode aléatoire a disparu avec l'ancien lecteur : il ne sera pas porté sur ce modèle
 * (décision du 2026-08-30).
 */
@Component
public class LecteurAnimation extends AbstractOrganeWithThread
        implements SmartLifecycle, OrganeSurveille, AnimationEnCours {

    private static final Logger logger = LoggerFactory.getLogger(LecteurAnimation.class);

    /** Cadence de repli si la configuration porte une valeur absurde (zéro, négative). */
    private static final double CADENCE_DE_REPLI = 10;

    /** Durée d'un tour à vide, quand aucune animation ne se joue : rien à calculer, on somnole. */
    private static final long PAUSE_AU_REPOS_MS = 100;

    /**
     * Durée pendant laquelle un déplacement au curseur continue de compter comme « une animation
     * se joue ».
     * <p>
     * Un tirage de curseur n'a pas de fin : l'éditeur envoie des instants tant que la souris
     * bouge, et s'arrête sans rien dire. Sans cette fenêtre, le regard reprendrait la main entre
     * deux envois et se battrait avec l'éditeur — la même divergence que pendant une lecture.
     */
    private static final long FENETRE_CURSEUR_MS = 1000;

    private final Clock horloge;

    /**
     * Butées de chaque axe, relues à chaque lecture : la configuration Phidgets est rechargée à
     * chaud, et une butée corrigée sur le robot doit valoir pour l'animation suivante.
     */
    private volatile Map<Axe, LimitesMoteur> limites = Map.of();

    /** L'animation en cours, ou {@code null}. Écrite par les listeners, lue par la boucle. */
    private volatile Lecture lecture;

    /** Instant du dernier déplacement au curseur, pour la fenêtre {@link #FENETRE_CURSEUR_MS}. */
    private volatile long dernierCurseur;

    /** Ce que le curseur a envoyé en dernier, pour ne pas réécrire un axe qui n'a pas bougé. */
    private final Map<Axe, Double> dernieresConsignesCurseur = new EnumMap<>(Axe.class);

    private volatile boolean running = false;

    private volatile boolean arretUrgence = false;

    /**
     * {@code @Autowired} obligatoire : cette classe a deux constructeurs, et Spring n'en choisit
     * aucun d'office — il se rabat sur un constructeur vide, qui n'existe pas, et le contexte
     * entier échoue au démarrage (voir {@code CablageDesBeansTest}).
     */
    @Autowired
    public LecteurAnimation() {
        this(Clock.systemDefaultZone());
    }

    /** Permet aux tests de dérouler une animation sans attendre sa durée réelle. */
    LecteurAnimation(Clock horloge) {
        super("LecteurAnimation");
        this.horloge = horloge;
    }

    @Override
    public void initialiser() {
        limites = LimitesMoteur.parAxe(Configurations.phidgetsConfig());
    }

    /** Pose les butées sans passer par la configuration Phidgets, pour les tests. */
    void limites(Map<Axe, LimitesMoteur> limites) {
        this.limites = limites;
    }

    /**
     * Lance une animation, en remplaçant celle qui se jouait.
     * <p>
     * Remplacer plutôt qu'enchaîner : deux animations superposées se disputeraient les mêmes axes
     * et le résultat ne ressemblerait à aucune des deux.
     *
     * @return faux si le lecteur ne l'a pas prise — organe arrêté, ou arrêt d'urgence en cours.
     *         Un refus silencieux ferait croire à l'appelant que la tête va bouger, et il
     *         attendrait un mouvement qui ne viendra pas.
     */
    public boolean jouer(Animation animation) {
        if (!running || arretUrgence || animation == null) {
            return false;
        }
        limites = LimitesMoteur.parAxe(Configurations.phidgetsConfig());
        dernieresConsignesCurseur.clear();
        lecture = new Lecture(animation, horloge.millis());
        logger.info("Animation « {} » lancée ({} ms, {} piste(s))",
                animation.nom(), animation.dureeTotale(), animation.pistes().size());
        return true;
    }

    /**
     * Interrompt l'animation en cours. Les axes restent où ils sont : les ramener au repos serait
     * un mouvement que personne n'a demandé, et l'appelant sait mieux que le lecteur ce qui doit
     * suivre.
     */
    public void stopper() {
        Lecture enCours = lecture;
        lecture = null;
        if (enCours != null) {
            logger.info("Animation « {} » interrompue", enCours.animation.nom());
        }
    }

    /** L'animation en train d'être déroulée, si le robot en joue une. */
    public Optional<Animation> animationEnCours() {
        Lecture enCours = lecture;
        return enCours == null ? Optional.empty() : Optional.of(enCours.animation);
    }

    /**
     * Une animation occupe les moteurs : lecture en cours, ou curseur de l'éditeur tiré à
     * l'instant. Ce que {@code Regard} interroge pour savoir s'il doit s'effacer.
     */
    @Override
    public boolean enLecture() {
        return lecture != null || horloge.millis() - dernierCurseur < FENETRE_CURSEUR_MS;
    }

    /**
     * Place les axes à un instant donné de l'animation, sans rien dérouler : ce que fait
     * l'éditeur quand on tire le curseur de la timeline.
     * <p>
     * Écrêté au même budget que la lecture. L'éditeur envoie autant d'instants que la souris
     * produit d'évènements — bien plus que les 55 écritures par seconde du contrôleur — et rien
     * n'oblige un client à se brider. Le refus est silencieux : un curseur tiré vite doit sauter
     * des positions intermédiaires, pas remplir une file.
     *
     * @return faux si le lecteur ne l'a pas prise, pour la même raison que {@link #jouer}
     */
    public boolean positionner(Animation animation, long instant) {
        if (!running || arretUrgence || animation == null) {
            return false;
        }
        long maintenant = horloge.millis();
        double cadence = Configurations.robotConfig().animationCadenceHertz();
        if (maintenant - dernierCurseur < 1000 / (cadence > 0 ? cadence : CADENCE_DE_REPLI)) {
            return true;
        }
        if (lecture != null) {
            // Tirer le curseur pendant une lecture, c'est en reprendre la main : la lecture
            // continuerait sinon à écrire par-dessus, et les deux se disputeraient les axes.
            stopper();
        }
        // Une ligne par tirage, et non par position : le curseur en envoie dix par seconde, mais
        // ce qu'on veut savoir en lisant le journal, c'est qu'un tirage a bien atteint le robot.
        // Sans elle, un curseur qui ne fait rien bouger — parce que la timeline est plate, par
        // exemple — est indiscernable d'un curseur qui n'arrive pas.
        if (maintenant - dernierCurseur > FENETRE_CURSEUR_MS) {
            logger.info("Curseur : « {} » suivie à la main", animation.nom());
        }
        limites = LimitesMoteur.parAxe(Configurations.phidgetsConfig());
        dernierCurseur = maintenant;

        Lecture ponctuelle = new Lecture(animation, 0);
        ponctuelle.dernieresConsignes.putAll(dernieresConsignesCurseur);
        Map<Axe, Double> consignes = consignes(ponctuelle, instant);
        dernieresConsignesCurseur.putAll(ponctuelle.dernieresConsignes);
        publier(animation, consignes, instant);
        return true;
    }

    @Override
    public void loop() {
        while (!Thread.interrupted()) {
            long debutTour = System.nanoTime();
            Lecture enCours = lecture;
            long budgetMs = avancerSiBesoin(enCours);

            // Signe de vie en fin de tour : c'est un tour ABOUTI qui est attesté, pas le simple
            // fait que le thread existe.
            battement();

            long resteMs = budgetMs - (System.nanoTime() - debutTour) / 1_000_000L;
            try {
                Thread.sleep(Math.max(1, resteMs));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Fait avancer la lecture d'un échantillon et rend le budget du tour, en millisecondes.
     *
     * @return la période à respecter avant le tour suivant
     */
    private long avancerSiBesoin(Lecture enCours) {
        if (enCours == null || arretUrgence) {
            return PAUSE_AU_REPOS_MS;
        }

        double cadence = Configurations.robotConfig().animationCadenceHertz();
        long budgetMs = (long) (1000 / (cadence > 0 ? cadence : CADENCE_DE_REPLI));

        long instant = horloge.millis() - enCours.instantDebut;
        if (instant > enCours.animation.dureeTotale()) {
            terminer(enCours);
            return budgetMs;
        }

        long avant = System.nanoTime();
        publier(enCours.animation, consignes(enCours, instant), instant);
        long dureeMs = (System.nanoTime() - avant) / 1_000_000L;
        enCours.tours++;
        if (dureeMs > budgetMs) {
            enCours.toursTropLongs++;
        }
        return budgetMs;
    }

    /**
     * Les axes à commander pour cet instant : interpolés, écrêtés aux butées, et privés de ceux
     * qui n'ont pas assez bougé depuis le tour précédent.
     * <p>
     * Volontairement séparée de la publication et de la boucle : c'est ici que tient la frugalité
     * du lecteur, et elle se vérifie sans thread ni matériel.
     */
    Map<Axe, Double> consignes(Lecture enCours, long instant) {
        double seuil = Configurations.robotConfig().animationSeuilDegres();
        Map<Axe, Double> aCommander = new EnumMap<>(Axe.class);

        for (Map.Entry<Axe, Double> entree : Interpolateur.positionsA(enCours.animation, instant).entrySet()) {
            Axe axe = entree.getKey();
            LimitesMoteur limitesAxe = limites.get(axe);
            double position = limitesAxe != null ? limitesAxe.ecreter(entree.getValue()) : entree.getValue();

            Double precedente = enCours.dernieresConsignes.get(axe);
            if (precedente != null && Math.abs(position - precedente) < seuil) {
                continue;
            }
            aCommander.put(axe, position);
            enCours.dernieresConsignes.put(axe, position);
        }
        return aCommander;
    }

    /**
     * Traduit les consignes en un évènement de cou et un évènement d'yeux, les axes absents restant
     * à leur valeur neutre — c'est ainsi que {@code Cou} et {@code Yeux} savent ne pas y toucher.
     * Un évènement n'est publié que s'il porte au moins un axe.
     */
    private void publier(Animation animation, Map<Axe, Double> consignes, long instant) {
        if (consignes.isEmpty()) {
            return;
        }

        MouvementCouEvent cou = new MouvementCouEvent();
        cou.setOrigine(OrigineMouvement.ANIMATION);
        boolean couConcerne = false;
        MouvementYeuxEvent yeux = new MouvementYeuxEvent();
        boolean yeuxConcernes = false;

        for (Map.Entry<Axe, Double> consigne : consignes.entrySet()) {
            Axe axe = consigne.getKey();
            double position = consigne.getValue();
            Piste piste = animation.piste(axe).orElseThrow();
            ImageCle visee = imageCleVisee(piste, instant);
            double vitesse = piste.vitesseDe(visee);
            double acceleration = piste.accelerationDe(visee);

            switch (axe) {
                case COU_GAUCHE_DROITE -> {
                    cou.setPositionPanoramique(position);
                    cou.setVitessePanoramique(vitesse);
                    cou.setAccelerationPanoramique(acceleration);
                    couConcerne = true;
                }
                case COU_HAUT_BAS -> {
                    cou.setPositionInclinaison(position);
                    cou.setVitesseInclinaison(vitesse);
                    cou.setAccelerationInclinaison(acceleration);
                    couConcerne = true;
                }
                case COU_MONTER_DESCENDRE -> {
                    cou.setPositionMonterDescendre(position);
                    cou.setVitesseMonterDescendre(vitesse);
                    cou.setAccelerationMonterDescendre(acceleration);
                    couConcerne = true;
                }
                case OEIL_GAUCHE -> {
                    yeux.setPositionOeilGauche(position);
                    yeux.setVitesseOeilGauche(vitesse);
                    yeux.setAccelerationOeilGauche(acceleration);
                    yeuxConcernes = true;
                }
                case OEIL_DROIT -> {
                    yeux.setPositionOeilDroit(position);
                    yeux.setVitesseOeilDroit(vitesse);
                    yeux.setAccelerationOeilDroit(acceleration);
                    yeuxConcernes = true;
                }
            }
        }

        if (couConcerne) {
            applicationEventPublisher.publishEvent(cou);
        }
        if (yeuxConcernes) {
            applicationEventPublisher.publishEvent(yeux);
        }
    }

    /**
     * Le compte des tours trop longs est journalisé avec son dénominateur, et ce n'est pas de la
     * coquetterie : <b>un</b> tour sur quarante et <b>trente</b> sur quarante n'appellent pas la
     * même réaction, et le message ne le disait pas.
     * <p>
     * Un tour isolé sur la <b>première</b> animation jouée après le démarrage est normal et n'a
     * rien d'une contention : c'est la compilation à la volée du code d'échantillonnage. Observé
     * les deux fois le 2026-08-30, toujours sur la première animation, jamais sur les suivantes —
     * y compris quand celles-ci menaient cinq axes de front au lieu de trois.
     */
    private void terminer(Lecture enCours) {
        lecture = null;
        if (enCours.toursTropLongs > 0) {
            logger.warn("Animation « {} » terminée, mais {} tour(s) sur {} ont dépassé leur budget : "
                            + "trop d'axes en mouvement pour la cadence, ou un autre organe écrit en même temps",
                    enCours.animation.nom(), enCours.toursTropLongs, enCours.tours);
        } else {
            logger.info("Animation « {} » terminée", enCours.animation.nom());
        }
    }

    /**
     * La première image-clé pas encore franchie, ou la dernière si l'animation est au-delà.
     * <p>
     * C'est elle qui porte, le cas échéant, une vitesse et une accélération propres — « rejoins ce
     * point à cette allure ». À défaut, {@link Piste#vitesseDe} rend celles de la piste, et comme
     * elles ne changent alors pas d'un tour à l'autre, {@code PhidgetsServoMotor} n'écrit rien.
     */
    private static ImageCle imageCleVisee(Piste piste, long instant) {
        List<ImageCle> imagesCles = piste.imagesCles();
        for (ImageCle imageCle : imagesCles) {
            if (imageCle.instant() >= instant) {
                return imageCle;
            }
        }
        return imagesCles.getLast();
    }

    /**
     * La manette l'emporte : un ordre de cou venu d'un humain interrompt l'animation.
     * <p>
     * Interrompre plutôt que céder le pas — l'animation est déjà écrite, la reprendre là où elle
     * en était partirait d'une posture que la manette a changée entre-temps, et le reste du geste
     * n'aurait plus de sens. Le filtre sur l'origine est aussi ce qui empêche le lecteur de
     * s'interrompre lui-même : il publie sur ce même bus.
     */
    @EventListener
    public void handleMouvementCouEvent(MouvementCouEvent mouvementCouEvent) {
        if (mouvementCouEvent.getOrigine() != OrigineMouvement.MANETTE || lecture == null) {
            return;
        }
        Lecture enCours = lecture;
        lecture = null;
        logger.info("Animation « {} » interrompue : la manette prend la main", enCours.animation.nom());
    }

    /**
     * Arrêt d'urgence : l'animation en cours est abandonnée, pas mise en pause.
     * <p>
     * Sans ça le cou et les yeux refuseraient bien les mouvements, mais le lecteur continuerait à
     * dérouler dans le vide — et au réarmement la fin de l'animation repartirait d'un coup,
     * plusieurs secondes après le geste de l'utilisateur.
     */
    @EventListener
    public void handleArretUrgenceEvent(ArretUrgenceEvent evenement) {
        arretUrgence = evenement.isActif();
        if (!evenement.isActif()) {
            return;
        }
        Lecture enCours = lecture;
        lecture = null;
        if (enCours != null) {
            logger.warn("Lecteur d'animation : arrêt d'urgence, « {} » abandonnée", enCours.animation.nom());
        }
    }

    @Override
    public void start() {
        initialiser();
        super.start();
        running = true;
        logger.info("Lecteur d'animation démarré");
    }

    @Override
    public void stop() {
        running = false;
        lecture = null;
        arreter();
        logger.info("Lecteur d'animation arrêté");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return RobotLifecyclePhases.ACTIONNEURS;
    }

    // --- Surveillance (watchdog + pastilles d'état de l'interface) ---

    @Override
    public String idOrgane() {
        return "lecteur-animation";
    }

    @Override
    public String libelleOrgane() {
        return "Lecteur d'animation";
    }

    @Override
    public NatureOrgane nature() {
        return NatureOrgane.ACTIONNEUR;
    }

    @Override
    public boolean enService() {
        return running;
    }

    /**
     * Le lecteur ne bouge rien lui-même, mais il commande le cou et les yeux : sa boucle morte en
     * plein déroulé laisserait une animation à moitié jouée, les servos figés à mi-course.
     */
    @Override
    public boolean provoqueUnMouvement() {
        return true;
    }

    @Override
    public boolean enMouvement() {
        return lecture != null;
    }

    /**
     * L'état d'une animation en cours. Mutable et confinée à la boucle une fois publiée : seul le
     * thread du lecteur touche à {@link #dernieresConsignes} et à {@link #toursTropLongs}, les
     * autres threads ne font que remplacer la référence.
     */
    static final class Lecture {

        private final Animation animation;

        private final long instantDebut;

        /** Dernière consigne envoyée à chaque axe, pour ne pas réécrire ce qui n'a pas bougé. */
        private final Map<Axe, Double> dernieresConsignes = new EnumMap<>(Axe.class);

        private int tours;

        private int toursTropLongs;

        Lecture(Animation animation, long instantDebut) {
            this.animation = animation;
            this.instantDebut = instantDebut;
        }
    }
}
