package fr.roboteek.robot.memoire.longterme;

import fr.roboteek.robot.memoire.longterme.conversation.ConversationRepository;
import fr.roboteek.robot.memoire.longterme.personne.Personne;
import fr.roboteek.robot.memoire.longterme.personne.PersonneRepository;
import fr.roboteek.robot.memoire.longterme.visage.VisageConnuRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Vérifie que la mémoire longue démarre dans un vrai contexte Spring.
 * <p>
 * Le contexte complet du robot ne peut pas être monté ici — il réclame les Phidgets, la webcam et le
 * micro. Ce test n'assemble donc que la mémoire et les configurations automatiques dont elle dépend,
 * ce qui suffit à répondre à la seule question qui reste ouverte après coup : le
 * {@code @Transactional} de {@link ConversationRepository} trouvera-t-il un gestionnaire de
 * transactions ? Sans lui, l'échec ne surviendrait pas au démarrage mais au premier échange avec
 * l'IA, sur le robot.
 */
class CablageMemoireTest {

    @TempDir
    File dossierTemp;

    @Test
    void laMemoireDemarreAvecUnGestionnaireDeTransactions() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class,
                        TransactionAutoConfiguration.class))
                .withUserConfiguration(MemoireDeTest.class)
                .withBean(PersonneRepository.class)
                .withBean(VisageConnuRepository.class)
                .withBean(ConversationRepository.class)
                .withPropertyValues("dossier=" + dossierTemp.getAbsolutePath())
                .run(contexte -> {
                    assertNotNull(contexte.getBean(PlatformTransactionManager.class));

                    // Et de bout en bout : une personne, son visage, sa conversation.
                    Personne marie = Personne.nouvelle("Marie");
                    contexte.getBean(PersonneRepository.class).enregistrer(marie);
                    contexte.getBean(VisageConnuRepository.class).ajouter(marie.id(), new float[]{1f, 2f});
                    contexte.getBean(ConversationRepository.class)
                            .saveAll("personne-" + marie.id(), List.of(new UserMessage("Bonjour")));

                    assertEquals(1, contexte.getBean(VisageConnuRepository.class).parPersonne(marie.id()).size());
                    assertEquals(1, contexte.getBean(ConversationRepository.class)
                            .findByConversationId("personne-" + marie.id()).size());
                });
    }

    /** La même source de données que {@code BaseMemoire}, dans un dossier jetable. */
    @Configuration
    static class MemoireDeTest {

        @Bean
        DataSource sourceDeDonneesMemoire(org.springframework.core.env.Environment environnement) {
            return BaseMemoireDeTest.dans(new File(environnement.getProperty("dossier")));
        }
    }
}
