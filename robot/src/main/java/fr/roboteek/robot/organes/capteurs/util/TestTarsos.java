package fr.roboteek.robot.organes.capteurs.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

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
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import be.tarsos.dsp.writer.WaveHeader;
import be.tarsos.dsp.writer.WriterProcessor;
import net.sourceforge.javaflacencoder.FLAC_FileEncoder;

public class TestTarsos {
	
	private  static final int HEADER_LENGTH=44;//byte

	private float sampleRate = 44100;
	private int bufferSize = 1024 * 4;
	private int overlap = 0 ;
	private AudioFormat format;
	private double previousTimeStamp = 0;
	private RandomAccessFile file;
	private boolean fichierVide = true;
	private byte[] buffer1;
	private byte[] bufferAvantParle;
	private byte[] content;
	private FLAC_FileEncoder flacEncoder;
	
	private GoogleRecognizer recognizer;
	
	private boolean parle = false;
	
	public TestTarsos() {
		try {
			
			recognizer= new GoogleRecognizer();
			
			flacEncoder = new FLAC_FileEncoder();
			
			format = new AudioFormat(sampleRate, 16, 1, true,
					false);
			final DataLine.Info dataLineInfo = new DataLine.Info(
					TargetDataLine.class, format);
			TargetDataLine line = null;

			line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);

			//		line = (TargetDataLine) mixer.getLine(dataLineInfo);
			final int numberOfSamples = bufferSize;
			line.open(format, numberOfSamples);
			line.start();
			final AudioInputStream stream = new AudioInputStream(line);

			JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);
			// create a new dispatcher
			AudioDispatcher dispatcher = new AudioDispatcher(audioStream, bufferSize,
					overlap);

			getNextFile();


			final SilenceDetector silenceDetector = new SilenceDetector(SilenceDetector.DEFAULT_SILENCE_THRESHOLD,false);

			PitchDetectionHandler pdh = new PitchDetectionHandler() {
				public void handlePitch(PitchDetectionResult result,AudioEvent e) {
					double actualTimeStamp = e.getTimeStamp();
					if (silenceDetector.currentSPL() > SilenceDetector.DEFAULT_SILENCE_THRESHOLD) {
						final float pitchInHz = result.getPitch();
						if (pitchInHz > 0) {
							System.out.println("Pitch = " + pitchInHz + "\ttime = " + e.getTimeStamp());
							if (!parle) {
//								bufferAvantParle = buffer1;
							}
							parle = true;
							setPreviousTimeStamp(actualTimeStamp);
						} else {
							parle = false;
						}
					} else {
						parle = false;
					}
//					buffer1 = e.getByteBuffer();
					if (parle || actualTimeStamp - previousTimeStamp < 0.5) {
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
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
			};
			AudioProcessor p = new PitchProcessor(PitchEstimationAlgorithm.YIN, sampleRate, bufferSize, pdh);

			dispatcher.addAudioProcessor(silenceDetector);
			dispatcher.addAudioProcessor(p);
			
//			WriterProcessor wp = new WriterProcessor(new TarsosDSPAudioFormat(sampleRate, 16, 1, true, false), new RandomAccessFile("testWP.wav", "rw"));
//			dispatcher.addAudioProcessor(wp);
			
			new Thread(dispatcher,"Audio Dispatcher").start();
			
			try {
				Thread.sleep(10000);
				dispatcher.stop();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		} catch (LineUnavailableException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		};
	}

	public static void main(String[] args) throws LineUnavailableException, FileNotFoundException {
		new TestTarsos();
	}

	public double getPreviousTimeStamp() {
		return previousTimeStamp;
	}

	public void setPreviousTimeStamp(double previousTimeStamp) {
		this.previousTimeStamp = previousTimeStamp;
	}
	
	public void lancerReconnaissance() {
		// Fermeture du fichier Wave
		if (file == null || !fichierVide) {
			// Création du fichier Wav
			if (file != null) {
				System.out.println("Fermeture du fichier");
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
					file.seek(0);
					file.write(header.toByteArray());
					file.seek(44);
					file.write(content);
					file.close();
					content = null;
					bufferAvantParle = null;
					
					// Conversion en FLAC
					File fichierWav = new File("test.wav");
					File fichierFlac = new File("test.flac");
					flacEncoder.encode(fichierWav, fichierFlac);
					
					String resultat = recognizer.recognize("test.flac");
					
					// Suppression des fichiers
					fichierWav.delete();
					fichierFlac.delete();
					file = null;
					
					
					System.out.println("Resultat = " + resultat);
					
					
				}catch (IOException e){
					e.printStackTrace();
				}
			}
		}
	}

	public synchronized void getNextFile() throws IOException {
		if (file == null || !fichierVide) {
			fichierVide = true;
			System.out.println("Nouveau fichier");
			file = new RandomAccessFile("test.wav", "rw");
			file.write(new byte[44]);
		}
	}

	public synchronized boolean isFichierVide() {
		return fichierVide;
	}

	public synchronized void setFichierVide(boolean fichierVide) {
		this.fichierVide = fichierVide;
	}
	
	public synchronized void ecrire(byte[] buffer) throws IOException {
		System.out.println("Ecrire " + buffer.length);
		if (content != null && content.length != 0) {
			content = Bytes.concat(content, buffer);
		} else {
			content = buffer;
		}
		setFichierVide(false);
	}

}

