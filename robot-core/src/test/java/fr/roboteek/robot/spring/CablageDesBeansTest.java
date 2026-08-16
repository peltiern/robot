package fr.roboteek.robot.spring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

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
