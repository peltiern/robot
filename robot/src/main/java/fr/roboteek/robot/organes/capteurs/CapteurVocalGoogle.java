package fr.roboteek.robot.organes.capteurs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import com.google.common.primitives.Bytes;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import be.tarsos.dsp.writer.WaveHeader;
import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.organes.actionneurs.OrganeParoleGoogle;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.util.reconnaissance.vocale.google.GoogleSpeechRecognizer;
import fr.roboteek.robot.util.reconnaissance.vocale.google.GoogleSpeechRecognizerRest;
import net.sourceforge.javaflacencoder.FLAC_FileEncoder;

/**
 * Capteur vocal utilisant la reconnaissance de Google.
 * @author Nicolas
 *
 */
public class CapteurVocalGoogle extends AbstractOrgane {

	/** Longueur de l'entête d'un fichier WAV. */
	private static final int WAV_FILE_HEADER_LENGTH=44;

	/** Fréquence d'échantillonage. */
	private static final float sampleRate = 44100;

	/** Taille du buffer. */
	private static final int bufferSize = 1024 * 4;

	/** ?. */
	private static final int overlap = 0 ;

	/** Moteur de reconnaissance vocale. */
	private GoogleSpeechRecognizer recognizer;

	/** Encodeur de fichiers WAV --> FLAC. */
	private FLAC_FileEncoder flacEncoder;

	/** Format audio. */
	private AudioFormat format;

	/** Timestamp précédent (permet de connaître le temps depuis le dernier flux "parlé". */
	private double previousTimeStamp = 0;

	/** Fichier WAV permettant d'enregistrer ce qui a été dit. */ 
	private RandomAccessFile fichierWav;

	/** Chemin du fichier WAV. */
	private String cheminFichierWav;

	/** Chemin du fichier FLAC. */
	private String cheminFichierFlac;

	/** Flag permettant de savoir que le fichier WAV est vide (nouvel phase de reconnaissance). */
	private boolean fichierVide = true;

	/** Buffer permettant de stocker le signal audio précédant la partie "parlée". */
	private byte[] bufferAvantParle;

	/** Buffer permettant de stocker le signal audio précédent. */
	private byte[] bufferNMoins1;

	/** Buffer permettant de stocker le signal audio précédent. */
	private byte[] bufferNMoins2;

	/** Buffer permettant de stocker le signal audio précédent. */
	private byte[] bufferNMoins3;

	/** Buffer permettant de stocker une phase de reconnaissance. */
	private byte[] content;

	/** Flag permettant de savoir si le flux en cours de traitement est un flux "parlé". */
	private boolean parle = false;

	public CapteurVocalGoogle() {
		super();

		recognizer= GoogleSpeechRecognizerRest.getInstance();
		flacEncoder = new FLAC_FileEncoder();

		format = new AudioFormat(sampleRate, 16, 1, true, false);


	}

	@Override
	public void initialiser() {

		try {

			cheminFichierWav = Constantes.DOSSIER_RECONNAISSANCE_VOCALE + File.separator + "Google";
			final Path dossierReconnaissanceVocaleGoogle = Paths.get(Constantes.DOSSIER_RECONNAISSANCE_VOCALE + File.separator + "Google");
			if (!Files.exists(dossierReconnaissanceVocaleGoogle)) {
				// Création du dossier
				Files.createDirectories(dossierReconnaissanceVocaleGoogle);
			}
			cheminFichierWav = Constantes.DOSSIER_RECONNAISSANCE_VOCALE + File.separator + "Google" + File.separator + "reconnaissance.wav";
			cheminFichierFlac = Constantes.DOSSIER_RECONNAISSANCE_VOCALE + File.separator + "Google" + File.separator + "reconnaissance.flac";

			final DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
			TargetDataLine line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
			final AudioInputStream stream = new AudioInputStream(line);
			final JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);
			final AudioDispatcher dispatcher = new AudioDispatcher(audioStream, bufferSize, overlap);

			getNextFile();

			// Ouverture du flux
			line.open(format, bufferSize);
			line.start();

			content = new byte[0];
			bufferAvantParle = new byte[0];
			bufferNMoins1 = new byte[0];
			bufferNMoins2 = new byte[0];
			bufferNMoins3 = new byte[0];

			final SilenceDetector silenceDetector = new SilenceDetector(SilenceDetector.DEFAULT_SILENCE_THRESHOLD,false);

			PitchDetectionHandler pdh = new PitchDetectionHandler() {
				public void handlePitch(PitchDetectionResult result,AudioEvent e) {
					double actualTimeStamp = e.getTimeStamp();
					//					System.out.println("SPL = " + silenceDetector.currentSPL() + ", temps = " + actualTimeStamp);
					if (silenceDetector.currentSPL() > SilenceDetector.DEFAULT_SILENCE_THRESHOLD - 20) {
						final float pitchInHz = result.getPitch();
						if (pitchInHz > 0) {
							//							System.out.println("Pitch = " + pitchInHz + "\ttime = " + e.getTimeStamp());
							if (fichierVide) {
								//								System.out.println("Concat avant parle");
								bufferAvantParle = Bytes.concat(bufferNMoins3, bufferNMoins2, bufferNMoins1);
							}
							parle = true;
							setPreviousTimeStamp(actualTimeStamp);
						} else {
							parle = false;
						}
					} else {
						parle = false;
					}
					//				buffer1 = e.getByteBuffer();
					if (parle || actualTimeStamp - previousTimeStamp < 0.6) {
						// Si ça parle, ou petit silence
						try {
							ecrire(e.getByteBuffer());
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					} else {
						// Ca ne parle pas et grand silence
						// On change de fichier
						try {
							// On lance la reconnaissance
							lancerReconnaissance();
							getNextFile();
							parle = false;
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					bufferNMoins3 = Bytes.concat(bufferNMoins2);
					bufferNMoins2 = Bytes.concat(bufferNMoins1);
					bufferNMoins1 = Bytes.concat(e.getByteBuffer());

				}
			};
			AudioProcessor p = new PitchProcessor(PitchEstimationAlgorithm.YIN, sampleRate, bufferSize, pdh);

			dispatcher.addAudioProcessor(silenceDetector);
			dispatcher.addAudioProcessor(p);

			new Thread(dispatcher,"Audio Dispatcher").start();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}

	@Override
	public void arreter() {
		// TODO Auto-generated method stub

	}

	public double getPreviousTimeStamp() {
		return previousTimeStamp;
	}

	public void setPreviousTimeStamp(double previousTimeStamp) {
		this.previousTimeStamp = previousTimeStamp;
	}

	public void lancerReconnaissance() {
		// Fermeture du fichier WAV
		if (fichierWav == null || !fichierVide) {
			// Création du fichier Wav
			if (fichierWav != null) {
				//				System.out.println("Fermeture du fichier");
				if (bufferAvantParle != null) {
					content = Bytes.concat(bufferAvantParle, content);
				}

				try {
					//write header and data to the result output
					WaveHeader waveHeader=new WaveHeader(WaveHeader.FORMAT_PCM,
							(short)format.getChannels(),
							(int)format.getSampleRate(),(short)16,content.length);//16 is for pcm, Read WaveHeader class for more details
					ByteArrayOutputStream header=new ByteArrayOutputStream();
					waveHeader.write(header);
					fichierWav.seek(0);
					fichierWav.write(header.toByteArray());
					fichierWav.seek(WAV_FILE_HEADER_LENGTH);
					fichierWav.write(content);
					fichierWav.close();
					content = new byte[0];
					bufferAvantParle = new byte[0];
					bufferNMoins1 = new byte[0];
					bufferNMoins2 = new byte[0];
					bufferNMoins3 = new byte[0];

					// Conversion en FLAC
					File fichierWav = new File(cheminFichierWav);
					File fichierFlac = new File(cheminFichierFlac);
					flacEncoder.encode(fichierWav, fichierFlac);

					// Appel du moteur de reconnaissance
					final String resultat = recognizer.reconnaitre(cheminFichierFlac);

					if (resultat != null && !resultat.trim().equals("")) {
						// Envoi de l'évènement de reconnaissance
						final ReconnaissanceVocaleEvent event = new ReconnaissanceVocaleEvent();
						event.setTexteReconnu(resultat);
						System.out.println("Résultat = " + resultat);
						RobotEventBus.getInstance().publish(event);
						dire(resultat);
					}

					// Suppression des fichiers
					fichierWav.delete();
					fichierFlac.delete();
					fichierWav = null;

				} catch (IOException e){
					e.printStackTrace();
				}
			}
		}
	}

	public synchronized void getNextFile() throws IOException {
		if (fichierWav == null || !fichierVide) {
			fichierVide = true;
			//			System.out.println("Nouveau fichier");
			fichierWav = new RandomAccessFile(cheminFichierWav, "rw");
			fichierWav.write(new byte[44]);
		}
	}

	public synchronized boolean isFichierVide() {
		return fichierVide;
	}

	public synchronized void setFichierVide(boolean fichierVide) {
		this.fichierVide = fichierVide;
	}

	public synchronized void ecrire(byte[] buffer) throws IOException {
		//		System.out.println("Ecrire " + buffer.length + ", fichier vide  = " + fichierVide + ", content = " + (content != null ? content.length : "0"));
		//		if (Arrays.equals(content, buffer)) {
		//			System.out.println("Contenu identique = " + buffer);
		//		}
		if (content != null) {
			//			System.out.println("Concaténation");
			content = Bytes.concat(content, buffer);
		}
		//		System.out.println("Fin Ecrire = " + content.length);
		setFichierVide(false);
	}

	public static void main(String[] args) {
		OrganeParoleGoogle organeParole = new OrganeParoleGoogle();
		RobotEventBus.getInstance().subscribe(organeParole);
		final CapteurVocalGoogle capteurVocal = new CapteurVocalGoogle();
		capteurVocal.initialiser();
		RobotEventBus.getInstance().subscribe(capteurVocal);
		final ParoleEvent paroleEvent = new ParoleEvent();
		paroleEvent.setTexte("test");
		RobotEventBus.getInstance().publish(paroleEvent);

	}

	/**
	 * Envoie un évènement pour dire du texte.
	 * @param texte le texte à dire
	 */
	private void dire(String texte) {
		System.out.println("Dire = " + texte);
				     final ParoleEvent paroleEvent = new ParoleEvent();
				     paroleEvent.setTexte(texte);
				     RobotEventBus.getInstance().publish(paroleEvent);
//		try {
//			Process p = Runtime.getRuntime().exec("C:/Program Files (x86)/eSpeak/command_line/espeak.exe -v fr -p 80 \"" + texte + "\"");
//			p.waitFor();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

}
