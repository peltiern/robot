package fr.roboteek.robot.server;

/**
 * Interface d'un écouteur du serveur.
 * @author Java Developer
 */
public interface WebSpeechServerListener {

    /**
     * Méthode à lancer lors d'un évènement.
     * @param speechResult le résultat
     */
    void onSpeechResult(String speechResult);
}
