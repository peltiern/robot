package fr.roboteek.robot.systemenerveux.event;

public class AudioEvent extends RobotEvent {

    public static final String EVENT_TYPE = "audio";

    /**
     * Contenu WAV encodé en base64, tel que consommé par la webapp.
     * <p>
     * Le champ {@code byte[] content} qui doublait cette donnée a été supprimé : personne ne
     * le lisait côté Java, mais il était sérialisé (donc ré-encodé en base64) dans chaque
     * message, doublant le débit du flux audio pour rien.
     */
    private String audioContentBase64;

    public AudioEvent() {
        super(EVENT_TYPE);
    }

    public String getAudioContentBase64() {
        return audioContentBase64;
    }

    public void setAudioContentBase64(String audioContentBase64) {
        this.audioContentBase64 = audioContentBase64;
    }
}
