package fr.roboteek.robot.memoire.longterme.personne;

import fr.roboteek.robot.memoire.courtterme.MemoireCourtTerme;
import fr.roboteek.robot.memoire.longterme.conversation.ConversationRepository;
import fr.roboteek.robot.memoire.longterme.rencontre.JournalDesRencontres;
import fr.roboteek.robot.memoire.longterme.rencontre.Rencontre;
import fr.roboteek.robot.memoire.longterme.visage.ApprentissageParPhoto;
import fr.roboteek.robot.memoire.longterme.visage.VisageConnuRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Le répertoire du robot : tout ce qu'on peut faire à une personne prise dans son entier.
 * <p>
 * Une personne est éparpillée — sa fiche, ses empreintes, ses rencontres, son fil de conversation,
 * et ce que le robot a d'elle en tête à l'instant. Personne d'autre ne devrait avoir à savoir
 * combien de morceaux cela fait : une suppression qui en oublie un laisse une empreinte orpheline,
 * et le robot « reconnaît » alors quelqu'un qui n'existe plus.
 * <p>
 * Les dépôts restent accessibles par ailleurs pour ce qui ne concerne qu'eux — écrire une
 * empreinte, dater une rencontre. C'est <b>ce qui touche à la personne entière</b> qui passe ici.
 */
@Component
public class RepertoireDesPersonnes {

    private static final Logger logger = LoggerFactory.getLogger(RepertoireDesPersonnes.class);

    private final PersonneRepository personneRepository;

    private final VisageConnuRepository visageConnuRepository;

    private final ConversationRepository conversationRepository;

    private final JournalDesRencontres journalDesRencontres;

    private final MemoireCourtTerme memoireCourtTerme;

    private final ApprentissageParPhoto apprentissageParPhoto;

    public RepertoireDesPersonnes(PersonneRepository personneRepository,
                                  VisageConnuRepository visageConnuRepository,
                                  ConversationRepository conversationRepository,
                                  JournalDesRencontres journalDesRencontres,
                                  MemoireCourtTerme memoireCourtTerme,
                                  ApprentissageParPhoto apprentissageParPhoto) {
        this.personneRepository = personneRepository;
        this.visageConnuRepository = visageConnuRepository;
        this.conversationRepository = conversationRepository;
        this.journalDesRencontres = journalDesRencontres;
        this.memoireCourtTerme = memoireCourtTerme;
        this.apprentissageParPhoto = apprentissageParPhoto;
    }

    /** Tout le monde, avec de quoi remplir une liste sans interroger la base à chaque ligne. */
    public List<FichePersonne> toutes() {
        Map<String, Integer> visages = visageConnuRepository.nombreParPersonne();
        Map<String, Integer> rencontres = journalDesRencontres.nombreParPersonne();
        Set<String> avecVignette = personneRepository.idsAvecVignette();
        return personneRepository.toutes().stream()
                .map(personne -> new FichePersonne(personne,
                        visages.getOrDefault(personne.id(), 0),
                        rencontres.getOrDefault(personne.id(), 0),
                        avecVignette.contains(personne.id())))
                .toList();
    }

    /**
     * Crée quelqu'un à partir d'une ou plusieurs photos.
     * <p>
     * La personne est écrite <b>avant</b> ses empreintes, qui la référencent en base — l'ordre
     * inverse a déjà coûté une panne, l'insertion échouant sur la clé étrangère (voir
     * {@code PresentationActivity}). Et si aucune photo n'est exploitable, elle est effacée : une
     * fiche sans empreinte serait quelqu'un que le robot ne saurait jamais reconnaître.
     *
     * @throws ApprentissageParPhoto.PhotoInexploitable si aucune photo ne donne un visage
     */
    public Personne creerDepuisPhotos(String prenom, List<byte[]> photos) {
        if (StringUtils.isBlank(prenom)) {
            throw new IllegalArgumentException("Le prénom ne peut pas être vide");
        }
        Personne personne = Personne.nouvelle(prenom.trim());
        personneRepository.enregistrer(personne);
        try {
            apprendre(personne.id(), photos);
        } catch (RuntimeException e) {
            personneRepository.supprimer(personne.id());
            throw e;
        }
        logger.info("Personne {} créée par photo : {} ({} photo(s))", personne.id(), personne.prenom(), photos.size());
        return personne;
    }

    /**
     * Ajoute des photos à quelqu'un de déjà connu — d'autres angles, d'autres éclairages, ou une
     * tête qui a changé.
     *
     * @return la personne, ou {@code null} si elle est inconnue
     */
    public Personne ajouterDesVisages(String id, List<byte[]> photos) {
        Personne personne = personneRepository.parId(id);
        if (personne == null) {
            return null;
        }
        apprendre(id, photos);
        return personne;
    }

    /**
     * Apprend les photos exploitables, et ne se plaint que si aucune ne l'est.
     * <p>
     * Tolérant à dessein : sur cinq photos envoyées d'un coup, l'une peut être floue ou de dos.
     * Refuser le lot entier pour celle-là ferait recommencer toute la manœuvre.
     */
    private void apprendre(String idPersonne, List<byte[]> photos) {
        RuntimeException dernierRefus = null;
        int apprises = 0;
        for (byte[] photo : photos) {
            try {
                ApprentissageParPhoto.Empreinte empreinte = apprentissageParPhoto.lire(photo);
                visageConnuRepository.ajouter(idPersonne, empreinte.embedding());
                // Le portrait de la dernière photo exploitable fait foi, comme après un enrôlement
                // devant la caméra : c'est la plus récente qui ressemble le plus à la personne.
                if (empreinte.vignette() != null) {
                    personneRepository.enregistrerVignette(idPersonne, empreinte.vignette());
                }
                apprises++;
            } catch (ApprentissageParPhoto.PhotoInexploitable e) {
                dernierRefus = e;
                logger.info("Photo ignorée pour la personne {} : {}", idPersonne, e.getMessage());
            }
        }
        if (apprises == 0) {
            throw dernierRefus == null
                    ? new ApprentissageParPhoto.PhotoInexploitable("aucune photo envoyée")
                    : dernierRefus;
        }
    }

    /** Le portrait d'une personne, en JPEG, ou {@code null} si elle n'en a pas. */
    public byte[] vignette(String id) {
        return personneRepository.vignette(id);
    }

    /** @return la personne, ou {@code null} si elle est inconnue */
    public Personne parId(String id) {
        return personneRepository.parId(id);
    }

    /** @return la fiche d'une seule personne, comptes compris, ou {@code null} si elle est inconnue */
    public FichePersonne ficheDe(String id) {
        Personne personne = personneRepository.parId(id);
        if (personne == null) {
            return null;
        }
        return new FichePersonne(personne,
                visageConnuRepository.parPersonne(id).size(),
                journalDesRencontres.pourPersonne(id).size(),
                personneRepository.vignette(id) != null);
    }

    /** La timeline d'apparition d'une personne, de la plus récente à la plus ancienne. */
    public List<Rencontre> rencontres(String idPersonne) {
        return journalDesRencontres.pourPersonne(idPersonne);
    }

    /** Le fil de conversation d'une personne, dans l'ordre où il s'est tenu. */
    public List<Message> conversation(String idPersonne) {
        return conversationRepository.findByConversationId(ConversationRepository.idConversationDe(idPersonne));
    }

    /**
     * Corrige le prénom de quelqu'un, sans rien perdre du reste : ni ses visages, ni son histoire,
     * ni ce qu'ils se sont dit. C'est tout l'intérêt d'une identité portée par un identifiant.
     *
     * @return la personne à jour, ou {@code null} si elle est inconnue
     */
    public Personne renommer(String id, String nouveauPrenom) {
        Personne personne = personneRepository.parId(id);
        if (personne == null) {
            return null;
        }
        if (StringUtils.isBlank(nouveauPrenom)) {
            throw new IllegalArgumentException("Le prénom ne peut pas être vide");
        }
        Personne renommee = personne.renommee(nouveauPrenom.trim());
        personneRepository.enregistrer(renommee);
        logger.info("Personne {} renommée : {} devient {}", id, personne.prenom(), renommee.prenom());
        return renommee;
    }

    /**
     * Oublie une personne partout à la fois.
     * <p>
     * Ses empreintes et ses rencontres partent avec elle, la base s'en charge
     * ({@code ON DELETE CASCADE}). Restent deux choses que la base ne peut pas savoir : son fil de
     * conversation, qui n'a pas de clé étrangère parce que tous les fils n'appartiennent pas à
     * quelqu'un, et ce que le robot a d'elle en tête à l'instant même.
     * <p>
     * La conversation est effacée <b>avant</b> la fiche : dans l'autre sens, un incident entre les
     * deux laisserait un fil que plus rien ne désigne, invisible et impossible à retrouver.
     *
     * @return vrai si quelqu'un a effectivement été supprimé
     */
    public boolean supprimer(String id) {
        Personne personne = personneRepository.parId(id);
        if (personne == null) {
            return false;
        }
        conversationRepository.deleteByConversationId(ConversationRepository.idConversationDe(id));
        personneRepository.supprimer(id);
        memoireCourtTerme.oublier(id);
        logger.info("Personne {} ({}) oubliée : fiche, visages, rencontres et conversation", personne.prenom(), id);
        return true;
    }

    /**
     * Oublie le fil de conversation sans oublier la personne : elle reste connue et reconnue, mais
     * le robot repart d'une page blanche avec elle.
     */
    public boolean oublierLaConversation(String id) {
        if (personneRepository.parId(id) == null) {
            return false;
        }
        conversationRepository.deleteByConversationId(ConversationRepository.idConversationDe(id));
        logger.info("Conversation de la personne {} effacée", id);
        return true;
    }

    /**
     * Une personne telle qu'une liste la montre : sa fiche, plus les deux comptes qu'on veut voir
     * sans ouvrir le détail.
     *
     * @param personne         la personne
     * @param nombreDeVisages  combien d'empreintes la reconnaissent ; zéro veut dire qu'elle est
     *                         connue mais ne sera jamais reconnue
     * @param nombreDeRencontres longueur de sa timeline
     * @param aUneVignette       vrai si un portrait est enregistré ; l'image elle-même se demande
     *                           à part, une liste n'a pas à traîner trente photos derrière elle
     */
    public record FichePersonne(Personne personne,
                                int nombreDeVisages,
                                int nombreDeRencontres,
                                boolean aUneVignette) {
    }
}
