package fr.roboteek.robot.spring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Vérifie que chaque bean du projet est instanciable par Spring, sans démarrer le contexte —
 * lequel exigerait les Phidgets, la webcam et le micro, donc le robot lui-même.
 * <p>
 * Motivé par une panne réelle : un {@code @Component} doté de deux constructeurs (celui de
 * production et un second, plus testable, qui injecte l'horloge) laisse Spring sans candidat.
 * Il se rabat alors sur le constructeur vide, n'en trouve pas, et <b>tout</b> le contexte
 * échoue au démarrage. Rien ne l'attrapait à la compilation : il a fallu déployer 247 Mo sur
 * le Jetson et lire les logs pour le découvrir.
 */
class CablageDesBeansTest {

    private static final String PACKAGE_RACINE = "fr.roboteek.robot";

    @Test
    void chaqueBeanOffreUnConstructeurQueSpringSaitChoisir() {
        List<String> anomalies = new ArrayList<>();
        List<Class<?>> beans = beansDuProjet();

        assertTrue(beans.size() > 10, "le scan n'a rien trouvé, le test ne vérifierait rien : " + beans.size());

        for (Class<?> bean : beans) {
            Constructor<?>[] constructeurs = bean.getDeclaredConstructors();
            if (constructeurs.length <= 1) {
                // Constructeur unique : Spring le prend, quels que soient ses paramètres.
                continue;
            }
            boolean unConstructeurDesigne = false;
            boolean constructeurVideDisponible = false;
            for (Constructor<?> constructeur : constructeurs) {
                if (constructeur.isAnnotationPresent(Autowired.class)) {
                    unConstructeurDesigne = true;
                }
                if (constructeur.getParameterCount() == 0) {
                    constructeurVideDisponible = true;
                }
            }
            if (!unConstructeurDesigne && !constructeurVideDisponible) {
                anomalies.add(bean.getName() + " (" + constructeurs.length + " constructeurs, aucun annoté @Autowired,"
                        + " aucun sans paramètre)");
            }
        }

        if (!anomalies.isEmpty()) {
            fail("Beans que Spring ne saura pas instancier — annoter le constructeur de production"
                    + " avec @Autowired :\n  " + String.join("\n  ", anomalies));
        }
    }

    /**
     * Vérifie que ce qu'un bean réclame par constructeur existe bien comme bean.
     * <p>
     * Motivé par la même panne, à un cran près : {@code EnrolementEnCours} demandait un
     * {@code ServiceReconnaissanceVisage}, que l'organe de vision charge lui-même au démarrage et
     * qui n'est pas un composant Spring. Le constructeur était pourtant irréprochable — le test
     * précédent laissait donc passer, et le contexte entier échouait au démarrage sur le Jetson.
     * <p>
     * Seuls les types du projet sont examinés : ce qui vient de Spring ou de l'extérieur
     * (l'{@code ApplicationEventPublisher}, un {@code ChatClient.Builder}...) est fourni par le
     * cadre, et lui seul sait ce qu'il sait construire.
     */
    @Test
    void chaqueDependanceDunBeanEstElleMemeUnBean() {
        List<Class<?>> beans = beansDuProjet();
        List<String> anomalies = new ArrayList<>();

        for (Class<?> bean : beans) {
            for (Constructor<?> constructeur : constructeursCandidats(bean)) {
                for (Class<?> dependance : constructeur.getParameterTypes()) {
                    if (!estDuProjet(dependance) || estFourniParUnBean(dependance, beans)) {
                        continue;
                    }
                    anomalies.add(bean.getSimpleName() + " réclame " + dependance.getSimpleName()
                            + ", qui n'est pas un composant Spring");
                }
            }
        }

        if (!anomalies.isEmpty()) {
            fail("Dépendances introuvables au démarrage — en faire des composants, ou les passer"
                    + " en paramètre de méthode plutôt qu'au constructeur :\n  "
                    + String.join("\n  ", anomalies));
        }
    }

    /** Le constructeur que Spring choisira : celui annoté, ou l'unique, ou celui sans paramètre. */
    private static List<Constructor<?>> constructeursCandidats(Class<?> bean) {
        Constructor<?>[] constructeurs = bean.getDeclaredConstructors();
        List<Constructor<?>> annotes = Arrays.stream(constructeurs)
                .filter(constructeur -> constructeur.isAnnotationPresent(Autowired.class))
                .collect(Collectors.toList());
        if (!annotes.isEmpty()) {
            return annotes;
        }
        return constructeurs.length == 1 ? List.of(constructeurs[0]) : List.of();
    }

    private static boolean estDuProjet(Class<?> type) {
        return type.getName().startsWith(PACKAGE_RACINE);
    }

    /** Vrai si ce type est un bean, ou l'interface / la classe mère de l'un d'eux. */
    private static boolean estFourniParUnBean(Class<?> type, List<Class<?>> beans) {
        return beans.stream().anyMatch(type::isAssignableFrom);
    }

    private static List<Class<?>> beansDuProjet() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
        List<Class<?>> beans = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(PACKAGE_RACINE)) {
            try {
                // initialize=false : charger la classe ne doit pas déclencher ses blocs statiques
                // (chargement de modèles, chemins de bibliothèques natives...).
                beans.add(Class.forName(definition.getBeanClassName(), false, CablageDesBeansTest.class.getClassLoader()));
            } catch (ClassNotFoundException e) {
                fail("Classe candidate introuvable : " + definition.getBeanClassName());
            }
        }
        return beans;
    }
}
