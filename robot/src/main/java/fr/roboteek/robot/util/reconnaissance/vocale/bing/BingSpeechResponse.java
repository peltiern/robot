package fr.roboteek.robot.util.reconnaissance.vocale.bing;

/**
 * Réponse du service Web de reconnaissance vocale de Bing.
 * @author Nicolas
 *
 */
public class BingSpeechResponse {
	
	/** Version. */
	private String version;
	
	/** Entête. */
	private BingSpeechHeader header;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public BingSpeechHeader getHeader() {
		return header;
	}

	public void setHeader(BingSpeechHeader header) {
		this.header = header;
	}

	@Override
	public String toString() {
		return "BingSpeechResponse [version=" + version + ", header=" + header + "]";
	}

}
