package fr.roboteek.robot.sandbox.reconnaissance.vocale.bing;

/**
 * Entête de la réponse du service Web de reconnaissance vocale de Bing.
 *
 * @author Nicolas
 */
public class BingSpeechHeader {

    /**
     * Statut.
     */
    private String status;

    /**
     * Texte reconnu.
     */
    private String lexical;

    /**
     * Texte reconnu formaté.
     */
    private String name;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLexical() {
        return lexical;
    }

    public void setLexical(String lexical) {
        this.lexical = lexical;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "BingSpeechHeader [status=" + status + ", lexical=" + lexical + ", name=" + name + "]";
    }
}
