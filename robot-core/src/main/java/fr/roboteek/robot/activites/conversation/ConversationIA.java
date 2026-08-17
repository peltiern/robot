package fr.roboteek.robot.activites.conversation;

import fr.roboteek.robot.activites.main.ReponseIntelligenceArtificielle;
import fr.roboteek.robot.activites.main.RequeteIntelligenceArtificielle;
import fr.roboteek.robot.memoire.longterme.conversation.ConversationRepository;
import fr.roboteek.robot.memoire.longterme.personne.Personne;
import fr.roboteek.robot.systemenerveux.spring.RobotEventsConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Conversation avec l'IA (Claude via Spring AI).
 * <p>
 * Remplace l'ancien {@code OpenAIConversation} (langchain4j) : le modèle est configuré
 * dans {@code application.properties} ({@code spring.ai.anthropic.*}, clé d'API lue dans
 * le {@code .env}), la mémoire de conversation persiste en base via
 * {@link ConversationRepository}.
 */
@Component
public class ConversationIA {

    /**
     * Identifiant de la conversation tenue avec un interlocuteur non identifié.
     * <p>
     * Valeur historique conservée : c'est la clé sous laquelle toute la mémoire de conversation
     * a été écrite jusqu'ici, en changer ferait repartir le robot de zéro.
     */
    private static final String ID_CONVERSATION_PAR_DEFAUT = "wall-e";

    /** Fil de la chauffe : un échange sans intérêt, qui ne doit polluer la mémoire de personne. */
    private static final String ID_CONVERSATION_CHAUFFE = "chauffe";

    /** Nombre maximal de messages conservés dans la fenêtre de mémoire. */
    private static final int TAILLE_MEMOIRE = 100;

    private static final String PROMPT_SYSTEME = "Dans cette discussion, tu t'appelleras Wall-E et tu seras un robot. "
            + "On est le %s. Tu répondras par des phrases courtes et rigolotes de moins de 15 mots. "
            + "Tes réponses sont lues à voix haute par une synthèse vocale : jamais d'emoji, de symbole ou de mise en forme. "
            + "Tu tutoieras ton interlocuteur. %s";

    /** Interlocuteur non identifié : le robot suppose que c'est son créateur, comme avant. */
    private static final String INTERLOCUTEUR_PAR_DEFAUT = "Tu parles avec Nicolas, né en 1981, ton créateur.";

    /** Interlocuteur identifié : Nicolas n'est alors plus qu'un fait à connaître. */
    private static final String INTERLOCUTEUR_IDENTIFIE = "Tu parles avec %s. Tu as été créé par Nicolas, né en 1981.";

    /** Ce qu'on attend d'une phrase de retrouvailles, en plus du prompt système habituel. */
    private static final String CONSIGNE_RETROUVAILLES = "Salue cette personne par son prénom, en une seule phrase, "
            + "en évoquant naturellement ce dont vous aviez parlé la dernière fois si tu t'en souviens. "
            + "Ne pose pas de question sur son identité, tu la connais.";

    /**
     * Ce qu'on attend après une courte absence : reprendre, pas accueillir.
     * <p>
     * Quelqu'un qui s'est absenté une minute et se fait dire bonjour a l'impression que le robot
     * l'a oublié. Ce qu'il attend, c'est qu'on reprenne où on en était.
     */
    private static final String CONSIGNE_REPRISE = "Cette personne s'était absentée un court instant et revient. "
            + "NE LA SALUE PAS, vous étiez déjà en train de discuter : reprends simplement la conversation en une "
            + "seule phrase, là où vous l'aviez laissée. Ne pose pas de question sur son identité, tu la connais.";

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);

    private static final Logger logger = LoggerFactory.getLogger(ConversationIA.class);

    private final ChatClient chatClient;

    public ConversationIA(ChatClient.Builder chatClientBuilder, ConversationRepository depotMemoireConversation) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(depotMemoireConversation)
                .maxMessages(TAILLE_MEMOIRE)
                .build();
        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /**
     * Répond à une requête.
     *
     * @param requete      la requête demandée (une phrase)
     * @param interlocuteur la personne qui parle, {@code null} si elle n'est pas identifiée
     * @return la réponse issue de l'intelligence artificielle
     */
    public ReponseIntelligenceArtificielle repondreARequete(RequeteIntelligenceArtificielle requete, Personne interlocuteur) {

        if (StringUtils.isNotEmpty(requete.getInputText())) {
            return traiterRequeteTexte(requete.getInputText(), interlocuteur);
        }

        return null;
    }

    private ReponseIntelligenceArtificielle traiterRequeteTexte(String inputText, Personne interlocuteur) {
        String outputText = chatClient.prompt()
                // Prompt système à chaque appel pour que la date du jour reste juste
                .system(PROMPT_SYSTEME.formatted(LocalDate.now().format(FORMAT_DATE), presentationInterlocuteur(interlocuteur)))
                .user(inputText)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, idConversation(interlocuteur)))
                .call()
                .content();
        ReponseIntelligenceArtificielle response = new ReponseIntelligenceArtificielle();
        response.setInputText(inputText);
        response.setOutputText(outputText);
        return response;
    }

    /**
     * Paie d'avance le démarrage à froid du client d'IA, dès que le robot est prêt.
     * <p>
     * <b>Le premier appel de chaque séance coûte dix secondes</b>, les suivants une seule —
     * chronométré deux fois sur le robot le 2026-08-15 : 10,0 s puis 1,1 s, 11,0 s puis 1,1 s.
     * Client HTTP, poignée de main TLS, premières classes chargées. Tant que le robot ne parlait
     * qu'en réponse, ce coût passait inaperçu ; depuis qu'il engage la conversation lui-même
     * (retrouvailles, accueil), il tombe en plein dessus et laisse la personne devant un robot
     * muet. Autant le payer pendant l'initialisation.
     * <p>
     * Sur un fil de conversation dédié : la réponse ne sert à rien et n'a rien à faire dans la
     * mémoire de qui que ce soit. En tâche de fond, pour ne pas retarder le démarrage — et sans
     * jamais le faire échouer, un robot qui ne peut pas joindre l'IA devant continuer à vivre.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async(RobotEventsConfig.ROBOT_EVENT_EXECUTOR)
    public void chauffer() {
        long debut = System.currentTimeMillis();
        try {
            chatClient.prompt()
                    .system("Réponds uniquement par : prêt.")
                    .user("Es-tu là ?")
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, ID_CONVERSATION_CHAUFFE))
                    .call()
                    .content();
            logger.info("Client d'IA prêt ({} ms)", System.currentTimeMillis() - debut);
        } catch (RuntimeException e) {
            logger.warn("Chauffe du client d'IA impossible ({} ms) : la première phrase sera lente",
                    System.currentTimeMillis() - debut, e);
        }
    }

    /**
     * Demande une phrase de retrouvailles pour quelqu'un qu'on connaît.
     * <p>
     * Tout l'intérêt est de poser la question <b>sur le fil de mémoire de cette personne</b> : le
     * modèle a sous les yeux vos cent derniers messages, il peut donc évoquer de quoi vous aviez
     * parlé sans qu'on ait à en stocker le moindre résumé.
     * <p>
     * L'arrivée est formulée comme un fait, et non comme un ordre (« Marie vient d'arriver… »),
     * parce que ce message reste dans l'historique : une consigne s'y lirait plus tard comme une
     * phrase que la personne aurait prononcée.
     *
     * @param personne                      la personne retrouvée
     * @param secondesDAbsence temps écoulé depuis la dernière fois, {@code -1} si inconnu
     * @param reprise                       vrai si l'absence a été si courte qu'il faut reprendre
     *                                      la conversation plutôt que saluer
     * @return la phrase à dire, ou {@code null} si l'IA n'a rien rendu
     */
    public String saluerRetrouvailles(Personne personne, long secondesDAbsence, boolean reprise) {
        try {
            return chatClient.prompt()
                    .system(PROMPT_SYSTEME.formatted(LocalDate.now().format(FORMAT_DATE), presentationInterlocuteur(personne))
                            + " " + (reprise ? CONSIGNE_REPRISE : CONSIGNE_RETROUVAILLES))
                    .user("%s vient d'arriver devant toi, %s.".formatted(personne.prenom(), depuisQuand(secondesDAbsence)))
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, idConversation(personne)))
                    .call()
                    .content();
        } catch (RuntimeException e) {
            // L'appel réseau peut échouer, et un bonjour ne vaut pas de laisser tomber la
            // rencontre : l'activité dira une phrase toute faite.
            logger.error("Impossible de composer la phrase de retrouvailles pour {}", personne.prenom(), e);
            return null;
        }
    }

    /**
     * Dit le temps écoulé en clair plutôt qu'en secondes : le modèle doit pouvoir saluer autrement
     * quelqu'un vu il y a dix minutes et quelqu'un vu il y a trois semaines.
     * <p>
     * <b>Une durée précise, et non une tranche.</b> « Moins d'une heure » pour vingt minutes
     * d'absence s'est fait rendre le 2026-08-15 par « content de te revoir après cette petite
     * heure », puis « tu as fait quoi pendant cette heure sans moi ». Le modèle prend au mot ce
     * qu'on lui donne : autant lui donner le chiffre.
     */
    private static String depuisQuand(long secondes) {
        if (secondes < 0) {
            return "vous ne vous étiez encore jamais parlé";
        }
        Duration duree = Duration.ofSeconds(secondes);
        if (duree.toMinutes() < 1) {
            return "vous venez tout juste de vous parler";
        }
        if (duree.toMinutes() < 60) {
            return "vous vous étiez parlé il y a %d minutes".formatted(duree.toMinutes());
        }
        if (duree.toHours() < 24) {
            return "vous vous étiez parlé il y a %d heures et %d minutes"
                    .formatted(duree.toHours(), duree.toMinutesPart());
        }
        return "vous ne vous étiez pas parlé depuis %d jours".formatted(duree.toDays());
    }

    /**
     * Identifiant de la conversation propre à l'interlocuteur : chacun a sa mémoire.
     * <p>
     * Cet identifiant était en dur : tout le monde partageait le même fil, et le robot pouvait
     * resservir à Marie ce que Paul venait de lui raconter.
     * <p>
     * <b>Puis il a été bâti sur le seul prénom, ce qui rouvrait le même trou en plus discret</b> :
     * deux personnes prénommées Nicolas se seraient partagé un fil, et le robot aurait raconté à
     * l'une ce que l'autre lui avait confié. C'est précisément ce contre quoi
     * {@link Personne#id()} existe, et c'est lui seul qui bâtit la clé — voir
     * {@link ConversationRepository#idConversationDe}.
     */
    private static String idConversation(Personne interlocuteur) {
        if (interlocuteur == null || StringUtils.isBlank(interlocuteur.id())) {
            return ID_CONVERSATION_PAR_DEFAUT;
        }
        return ConversationRepository.idConversationDe(interlocuteur.id());
    }

    /** Phrase du prompt système présentant la personne en face du robot. */
    private static String presentationInterlocuteur(Personne interlocuteur) {
        if (interlocuteur == null || StringUtils.isBlank(interlocuteur.prenom())) {
            return INTERLOCUTEUR_PAR_DEFAUT;
        }
        return INTERLOCUTEUR_IDENTIFIE.formatted(interlocuteur.prenom().trim());
    }
}
